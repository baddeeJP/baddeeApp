package badee.updaterComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import badee.updaterComponent.support.IntegrationTest;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/** Boots the whole application against real Postgres + Elasticsearch (Testcontainers). */
class UpdaterComponentApplicationIT extends IntegrationTest {

	@Autowired
	private Environment environment;

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
	}

	/**
	 * Flyway owns the schema; Hibernate only validates it against the entities
	 * (a context that boots with ddl-auto=validate proves they agree).
	 */
	@Test
	void schemaIsCreatedByFlywayMigrations() {
		List<String> applied = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

		assertEquals("1", applied.getFirst());
		assertEquals(0, flyway.info().pending().length, "every migration applied");
		assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
	}

	/**
	 * Postgres doesn't index foreign-key columns by itself. Without an index, every
	 * changed-file run turns each child lookup and orphan delete (a JMdict entry's
	 * readings and senses) into a full scan of a 500k-row table: the first real
	 * JMdict update ran at ~25 entries/s.
	 */
	@Test
	void everyForeignKeyColumnIsIndexed() {
		List<String> unindexed = jdbc.queryForList("""
				SELECT c.conrelid::regclass || '.' || a.attname
				FROM pg_constraint c
				JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
				WHERE c.contype = 'f' AND c.connamespace = 'public'::regnamespace
				  AND NOT EXISTS (SELECT 1 FROM pg_index i
				                  WHERE i.indrelid = c.conrelid AND i.indkey[0] = c.conkey[1])
				ORDER BY 1""", String.class);

		assertEquals(List.of(), unindexed);
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
				  AND c.relname <> 'flyway_schema_history'
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
