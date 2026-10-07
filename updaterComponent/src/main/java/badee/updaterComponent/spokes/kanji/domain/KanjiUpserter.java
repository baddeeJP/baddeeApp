package badee.updaterComponent.spokes.kanji.domain;

import badee.updaterComponent.hub.RetirementGuard;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Applies one sub-source's data onto {@link Kanji} rows without disturbing the
 * fields other sub-sources own. Each batch runs in its own transaction: the
 * existing rows are loaded (one query), the sub-source's fields are applied to
 * the managed entities, and bare rows are created for characters seen for the
 * first time. This makes the three sub-sources order-independent — e.g.
 * RADKFILE can run before KanjiDic2 has ever populated the table.
 *
 * <p>Removals follow the same ownership: a sub-source that no longer lists a
 * character clears only its own fields ({@link #clearDropped}), and the row is
 * retired only once no sub-source has data on it.
 */
@Component
public class KanjiUpserter {

	private static final Logger log = LoggerFactory.getLogger(KanjiUpserter.class);
	private final KanjiRepository kanjiRepository;
	private final RetirementGuard retirementGuard;
	private final TransactionTemplate transactionTemplate;

	public KanjiUpserter(KanjiRepository kanjiRepository, RetirementGuard retirementGuard,
			PlatformTransactionManager transactionManager) {
		this.kanjiRepository = kanjiRepository;
		this.retirementGuard = retirementGuard;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * Upserts one batch.
	 *
	 * @param records   parsed sub-source records
	 * @param character extracts the kanji character a record belongs to
	 * @param apply     copies the record's fields onto the (managed) kanji
	 */
	public <T> void upsert(List<T> records, Function<T, String> character, BiConsumer<Kanji, T> apply) {
		if (records.isEmpty()) {
			return;
		}
		transactionTemplate.executeWithoutResult(status -> {
			Map<String, Kanji> existing = kanjiRepository
					.findAllById(records.stream().map(character).toList()).stream()
					.collect(Collectors.toMap(Kanji::getCharacter, Function.identity()));
			for (T record : records) {
				String key = character.apply(record);
				Kanji kanji = existing.get(key);
				if (kanji == null) {
					kanji = kanjiRepository.save(new Kanji(key));
					existing.put(key, kanji);
				}
				kanji.unretire();
				apply.accept(kanji, record);
			}
		});
	}

	/**
	 * After a sub-source's fully successful changed-file run: clears that
	 * sub-source's fields from the characters it had data on but no longer lists
	 * (subject to the feed's retire guard), then retires kanji that no sub-source
	 * has data on any more. Returns how many characters were cleared.
	 *
	 * @param currentKeys characters the sub-source currently has data on
	 * @param seen        characters in the new file
	 * @param clear       removes the sub-source's fields from a (managed) kanji
	 */
	public int clearDropped(String feedId, Collection<String> currentKeys, Set<String> seen,
			Consumer<Kanji> clear) {
		int cleared = retirementGuard.retireMissing(feedId, currentKeys, seen, batch -> transactionTemplate
				.executeWithoutResult(status -> kanjiRepository.findAllById(batch).forEach(clear)));
		if (cleared > 0) {
			int retired = kanjiRepository.retireKanjiWithoutData(Instant.now());
			log.info("[{}] cleared its data from {} kanji; {} kanji retired (no sub-source has them)",
					feedId, cleared, retired);
		}
		return cleared;
	}
}
