package badee.updaterComponent.hub;

import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Idempotency helper shared by all spokes: compares a freshly computed
 * content hash against the last recorded one for a given feed.
 *
 * <p>Design note: only the version <em>bookkeeping</em> lives in the hub, not
 * the checking of the datasource itself. Each spoke still owns where to fetch
 * from, fetching the file, and computing the hash; it merely delegates
 * "what hash did I see last time?" to this shared component. This lives in the
 * hub because (1) the read/write-a-content-hash logic is identical for every
 * feed, so keeping it in one place avoids duplicating the same JPA calls across
 * spokes; (2) a single {@code source_version} table lets us answer "when did
 * each feed last update?" in one query for monitoring; and (3) a per-feedId API
 * lets the combined Kanji spoke track its three upstream files
 * ("kanji.kanjidic2" / "kanji.kanjivg" / "kanji.radkfile") independently
 * without reinventing storage. In short: the hub owns the mechanism (how we
 * remember versions), the spoke owns the policy (what its source is and whether
 * it changed).
 */
@Service
public class SourceVersionService {

	private final SourceVersionRepository sourceVersionRepository;

	public SourceVersionService(SourceVersionRepository sourceVersionRepository) {
		this.sourceVersionRepository = sourceVersionRepository;
	}

	public boolean hasChanged(String feedId, String newHash) {
		SourceVersion version = sourceVersionRepository.findById(feedId).orElse(null);
		version = recordChecked(feedId, version);
		return version.getContentHash() == null || !version.getContentHash().equals(newHash);
	}

	public void markUpdated(String feedId, String newHash) {
		SourceVersion version = sourceVersionRepository.findById(feedId)
				.orElseGet(() -> new SourceVersion(feedId));
		version.setContentHash(newHash);
		version.setLastUpdatedAt(Instant.now());
		sourceVersionRepository.save(version);
	}

	private SourceVersion recordChecked(String feedId, SourceVersion existing) {
		SourceVersion version = existing != null ? existing : new SourceVersion(feedId);
		version.setLastCheckedAt(Instant.now());
		return sourceVersionRepository.save(version);
	}
}
