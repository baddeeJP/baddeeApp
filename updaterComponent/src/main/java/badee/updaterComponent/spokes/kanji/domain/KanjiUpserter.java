package badee.updaterComponent.spokes.kanji.domain;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
 */
@Component
public class KanjiUpserter {

	private final KanjiRepository kanjiRepository;
	private final TransactionTemplate transactionTemplate;

	public KanjiUpserter(KanjiRepository kanjiRepository, PlatformTransactionManager transactionManager) {
		this.kanjiRepository = kanjiRepository;
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
				apply.accept(kanji, record);
			}
		});
	}
}
