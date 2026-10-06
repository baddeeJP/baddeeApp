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
