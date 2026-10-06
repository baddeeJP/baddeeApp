package badee.updaterComponent.support;

import badee.updaterComponent.hub.DataSourceModule;
import badee.updaterComponent.hub.SourceVersion;
import badee.updaterComponent.hub.SourceVersionRepository;
import badee.updaterComponent.spokes.jmdict.search.VocabDocument;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceDocument;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for integration tests: boots the full application against real Postgres
 * and Elasticsearch containers (same images as {@code docker-compose.yml}) and
 * points every spoke's upstream URL at an in-process {@link FixtureServer}.
 *
 * <p>The containers and fixture server are started once per JVM and shared by
 * every subclass (and the single cached Spring context), so the suite pays the
 * container start-up cost only once. Each test starts from empty tables and
 * empty indices. The cron schedule is disabled so only the test drives runs.
 * Retire guards are lowered from the production 0.98 to 0.5 so a two-entry
 * fixture can drop one entry, while dropping two of three still trips the guard.
 * MockMvc is configured here (not per subclass) so all tests share one context.
 */
@SpringBootTest(properties = {
		"updater.schedule.cron=-",
		"updater.jmdict.retire-guard=0.5",
		"updater.jmnedict.retire-guard=0.5",
		"updater.tatoeba.retire-guard=0.5",
		"updater.kanji.kanjidic2.retire-guard=0.5",
		"updater.kanji.radkfile.retire-guard=0.5",
		"updater.kanji.kanjivg.retire-guard=0.5"})
@AutoConfigureMockMvc
public abstract class IntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

	@ServiceConnection
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
			"docker.elastic.co/elasticsearch/elasticsearch:9.4.5")
			.withEnv("xpack.security.enabled", "false")
			.withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

	protected static final FixtureServer fixtures = FixtureServer.start();

	static {
		postgres.start();
		elasticsearch.start();
	}

	@DynamicPropertySource
	static void upstreamUrls(DynamicPropertyRegistry registry) {
		registry.add("updater.jmdict.url", () -> fixtures.url("/JMdict_e.gz"));
		registry.add("updater.jmnedict.url", () -> fixtures.url("/JMnedict.xml.gz"));
		registry.add("updater.tatoeba.url", () -> fixtures.url("/examples.utf.gz"));
		registry.add("updater.kanjidic2.url", () -> fixtures.url("/kanjidic2.xml.gz"));
		registry.add("updater.radkfile.url", () -> fixtures.url("/radkfile.gz"));
		registry.add("updater.kanjivg.releases-url", () -> fixtures.url("/KanjiVG/kanjivg/releases"));
	}

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected ElasticsearchOperations elasticsearchOperations;

	@Autowired
	protected SourceVersionRepository sourceVersionRepository;

	@Autowired
	private List<DataSourceModule> modules;

	@BeforeEach
	void resetState() {
		fixtures.clear();
		jdbc.execute("""
				TRUNCATE source_version, vocab_entry, reading, sense, sense_gloss, sense_pos, sense_misc,
				example_sentence, name_entry, kanji, radical, kanji_radical CASCADE""");
		for (Class<?> document : List.of(VocabDocument.class, ExampleSentenceDocument.class)) {
			IndexOperations index = elasticsearchOperations.indexOps(document);
			index.delete();
			index.createWithMapping();
		}
	}

	/**
	 * Runs a module directly (not via {@code UpdateScheduler}, which logs and
	 * swallows failures) so a broken spoke fails the test.
	 */
	protected void run(String moduleId) {
		modules.stream()
				.filter(module -> module.id().equals(moduleId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No module " + moduleId))
				.run();
	}

	protected SourceVersion sourceVersion(String feedId) {
		return sourceVersionRepository.findById(feedId).orElseThrow();
	}

	/** Makes freshly indexed documents visible to searches/counts. */
	protected void refresh(Class<?> document) {
		elasticsearchOperations.indexOps(document).refresh();
	}
}
