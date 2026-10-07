package badee.updaterComponent.spokes.tatoeba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentence;
import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentenceRepository;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceDocument;
import badee.updaterComponent.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TatoebaModuleIT extends IntegrationTest {

	/** Longer than a default varchar(255), to prove the text columns hold real corpus lines. */
	private static final String LONG_ENGLISH = "This sentence is deliberately long. ".repeat(10).trim();

	private static final String BUSY = """
			A: 彼は忙しい。\tHe is busy.#ID=1234_5678
			B: 彼(かれ)[01] は 忙しい
			""";

	private static final String LONG = """
			A: 長い文です。\t%s#ID=2000_2001
			B: 長い 文 です
			""".formatted(LONG_ENGLISH);

	private static final String CORPUS = BUSY + LONG + """
			A: 壊れた行\tNo id trailer here
			B: 壊れる
			""";

	@Autowired
	private ExampleSentenceRepository repository;

	@Test
	void persistsSentencePairsToPostgresAndIndexesThemInElasticsearch() {
		fixtures.publishGzipped("/examples.utf.gz", CORPUS);

		run("tatoeba");

		assertEquals(2, repository.count(), "the pair without an #ID trailer is skipped");
		Map<String, Object> row = jdbc.queryForMap(
				"SELECT japanese, english, index_words, source FROM example_sentence WHERE id = 5678");
		assertEquals("彼は忙しい。", row.get("japanese"));
		assertEquals("He is busy.", row.get("english"));
		assertEquals("彼(かれ)[01] は 忙しい", row.get("index_words"));
		assertEquals("tanaka-corpus", row.get("source"));

		ExampleSentence longSentence = repository.findById(2001L).orElseThrow();
		assertEquals(LONG_ENGLISH, longSentence.getEnglish());

		ExampleSentenceDocument doc = elasticsearchOperations.get("5678", ExampleSentenceDocument.class);
		assertEquals("彼は忙しい。", doc.getJapanese());
		assertEquals("He is busy.", doc.getEnglish());
		refresh(ExampleSentenceDocument.class);
		assertEquals(2, elasticsearchOperations.count(
				elasticsearchOperations.matchAllQuery(), ExampleSentenceDocument.class));
	}

	@Test
	void changedSentenceIsUpdatedInPlace() {
		fixtures.publishGzipped("/examples.utf.gz", BUSY + LONG);
		run("tatoeba");

		fixtures.publishGzipped("/examples.utf.gz", BUSY.replace("He is busy.", "He is very busy.") + LONG);
		run("tatoeba");

		assertEquals(2, repository.count(), "same id, no new row");
		assertEquals("He is very busy.", repository.findById(5678L).orElseThrow().getEnglish());
		assertEquals("He is very busy.",
				elasticsearchOperations.get("5678", ExampleSentenceDocument.class).getEnglish());
	}

	@Test
	void sentenceRemovedUpstreamIsRetiredAndDeletedFromSearchThenUnretiredWhenBack() {
		fixtures.publishGzipped("/examples.utf.gz", BUSY + LONG);
		run("tatoeba");

		fixtures.publishGzipped("/examples.utf.gz", BUSY);
		run("tatoeba");

		assertTrue(isRetired(2001), "row kept, marked retired");
		assertFalse(isRetired(5678));
		assertNull(elasticsearchOperations.get("2001", ExampleSentenceDocument.class));

		fixtures.publishGzipped("/examples.utf.gz", BUSY + LONG);
		run("tatoeba");

		assertFalse(isRetired(2001));
		assertNotNull(elasticsearchOperations.get("2001", ExampleSentenceDocument.class));
	}

	@Test
	void retiresOnlySentencesOfItsOwnSource() {
		jdbc.update("""
				INSERT INTO example_sentence (id, japanese, english, source)
				VALUES (9999, '別の文。', 'Another sentence.', 'another-source')""");
		fixtures.publishGzipped("/examples.utf.gz", BUSY + LONG);
		run("tatoeba");

		fixtures.publishGzipped("/examples.utf.gz", BUSY);
		run("tatoeba");

		assertTrue(isRetired(2001));
		assertFalse(isRetired(9999), "not a Tatoeba sentence, so never in its file");
	}

	private boolean isRetired(long id) {
		return jdbc.queryForObject(
				"SELECT retired_at IS NOT NULL FROM example_sentence WHERE id = ?", Boolean.class, id);
	}
}
