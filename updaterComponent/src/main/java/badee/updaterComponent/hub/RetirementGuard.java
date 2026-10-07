package badee.updaterComponent.hub;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Decides which entries a changed upstream file has dropped, so the spoke can
 * retire them (see {@code docs/adr/0001-retire-in-postgres-delete-from-search.md}).
 * Spokes call it only after a fully successful changed-file run, never after a
 * failed run or a hash-skip.
 *
 * <p>Guard: {@code updater.<feedId>.retire-guard} (default 0.98) is the smallest
 * fraction of currently active entries a run may leave active. A file that
 * would drop more is assumed wrong (partial mirror, parser bug): nothing is
 * retired and a WARN is logged, while the spoke's upserts still stand. The
 * would-retire count is logged on every call so each guard can be tuned from
 * real churn.
 *
 * <p>Like {@link SourceVersionService}, the hub owns the mechanism and each
 * spoke the policy: which keys are active, and what retiring means for it.
 */
@Component
public class RetirementGuard {

	private static final Logger log = LoggerFactory.getLogger(RetirementGuard.class);
	static final double DEFAULT_GUARD = 0.98;
	private static final int BATCH_SIZE = 1000;

	private final Environment environment;

	public RetirementGuard(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Retires the active keys missing from the new file, unless that would trip
	 * the guard, handing them to {@code retireBatch} in bounded batches.
	 * Returns how many were retired.
	 *
	 * @param activeKeys keys of the feed's currently active (non-retired) entries
	 * @param seenKeys   keys present in the new upstream file
	 */
	public <K> int retireMissing(String feedId, Collection<K> activeKeys, Set<K> seenKeys,
			Consumer<List<K>> retireBatch) {
		Set<K> missing = keysToRetire(feedId, activeKeys, seenKeys);
		Batches.of(missing, BATCH_SIZE).forEach(retireBatch);
		return missing.size();
	}

	/**
	 * @param activeKeys keys of the feed's currently active (non-retired) entries
	 * @param seenKeys   keys present in the new upstream file
	 * @return the active keys missing from the file, or none if that would trip the guard
	 */
	<K> Set<K> keysToRetire(String feedId, Collection<K> activeKeys, Set<K> seenKeys) {
		Set<K> missing = new LinkedHashSet<>();
		for (K key : activeKeys) {
			if (!seenKeys.contains(key)) {
				missing.add(key);
			}
		}
		if (missing.isEmpty()) {
			log.info("Retirement [{}]: 0 of {} active entries missing from the new file", feedId, activeKeys.size());
			return Set.of();
		}

		double guard = environment.getProperty("updater." + feedId + ".retire-guard", Double.class, DEFAULT_GUARD);
		int remaining = activeKeys.size() - missing.size();
		if (remaining < guard * activeKeys.size()) {
			log.warn("Retirement [{}] blocked: {} of {} active entries missing from the new file would leave "
					+ "{} active, below retire-guard {}. Nothing retired; upserts applied and the file is "
					+ "marked processed, so these removals are re-checked only when upstream changes again. "
					+ "Check the upstream file; if the removals are real, lower updater.{}.retire-guard.",
					feedId, missing.size(), activeKeys.size(), remaining, guard, feedId);
			return Set.of();
		}
		log.info("Retirement [{}]: {} of {} active entries missing from the new file, retiring",
				feedId, missing.size(), activeKeys.size());
		return missing;
	}
}
