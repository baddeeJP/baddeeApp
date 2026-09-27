package badee.updaterComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import badee.updaterComponent.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Boots the whole application against real Postgres + Elasticsearch (Testcontainers). */
class UpdaterComponentApplicationIT extends IntegrationTest {

	@Test
	void contextLoads() {
	}

	/**
	 * Hibernate defaults strings to varchar(255), and a single longer upstream
	 * value (a JMdict gloss, a KanjiVG stroke path) aborts a whole feed update.
	 * Only short, fixed-shape identifier columns may stay length-limited.
	 */
	@Test
	void freeTextColumnsAreNotLengthLimited() {
		List<String> limited = jdbc.queryForList("""
				SELECT c.relname || '.' || a.attname || ' ' || format_type(a.atttypid, a.atttypmod)
				FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid
				JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'public' AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped
				  AND format_type(a.atttypid, a.atttypmod) LIKE 'character varying(%'
				ORDER BY 1""", String.class);

		assertEquals(List.of(
				"example_sentence.source character varying(255)",
				"kanji.character character varying(255)",
				"kanji.codepoint character varying(255)",
				"kanji_radical.kanji character varying(255)",
				"kanji_radical.radical character varying(255)",
				"radical.character character varying(255)",
				"reading.text character varying(255)",
				"sense_misc.misc character varying(255)",
				"sense_pos.pos character varying(255)",
				"source_version.content_hash character varying(255)",
				"source_version.feed_id character varying(255)"), limited);
	}

}
