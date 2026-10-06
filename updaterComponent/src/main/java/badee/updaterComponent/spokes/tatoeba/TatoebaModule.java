package badee.updaterComponent.spokes.tatoeba;

import badee.updaterComponent.hub.DataSourceModule;
import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.RetirementGuard;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentence;
import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentenceRepository;
import badee.updaterComponent.spokes.tatoeba.fetch.TatoebaFetcher;
import badee.updaterComponent.spokes.tatoeba.parse.TatoebaParser;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceDocument;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceIndexWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tatoeba (Tanaka Corpus) spoke orchestrator: downloads the example-sentence
 * corpus, and if it has changed since the last run, parses it into
 * {@code ExampleSentence} rows in Postgres and mirrors each into the
 * {@code example-sentence} Elasticsearch index for the separate search app.
 *
 * <p>Concerns are split into sub-packages: {@code fetch}, {@code parse},
 * {@code domain} (JPA entity + repository), and {@code search} (Elasticsearch
 * document + write-only index writer).
 */
@Component
public class TatoebaModule implements DataSourceModule {

	private static final Logger log = LoggerFactory.getLogger(TatoebaModule.class);
	private static final String FEED_ID = "tatoeba";
	private static final int BATCH_SIZE = 1000;

	private final TatoebaFetcher fetcher;
	private final TatoebaParser parser;
	private final ExampleSentenceRepository repository;
	private final ExampleSentenceIndexWriter indexWriter;
	private final SourceVersionService sourceVersionService;
	private final RetirementGuard retirementGuard;

	public TatoebaModule(TatoebaFetcher fetcher, TatoebaParser parser,
			ExampleSentenceRepository repository, ExampleSentenceIndexWriter indexWriter,
			SourceVersionService sourceVersionService,
			RetirementGuard retirementGuard) {
		this.fetcher = fetcher;
		this.parser = parser;
		this.repository = repository;
		this.indexWriter = indexWriter;
		this.sourceVersionService = sourceVersionService;
		this.retirementGuard = retirementGuard;
	}

	@Override
	public String id() {
		return FEED_ID;
	}

	@Override
	public void run() {
		try {
			Path textFile = fetcher.fetch();
			String hash = Hashing.sha256(textFile);
			if (!sourceVersionService.hasChanged(FEED_ID, hash)) {
				log.info("Tatoeba corpus unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<ExampleSentence> batch = new ArrayList<>(BATCH_SIZE);
			int[] total = {0};
			Set<Long> seen = new HashSet<>();
			parser.parse(textFile, sentence -> {
				seen.add(sentence.getId());
				batch.add(sentence);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);
			// Only reached when the whole file parsed and persisted: a failure throws above.
			int retired = retireMissing(seen);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("Tatoeba update complete: {} sentences persisted and indexed, {} retired", total[0], retired);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("Tatoeba update failed", e);
		}
	}

	/**
	 * Retires active sentences the new file no longer has: deleted from search first, then
	 * marked in Postgres, so a failure between the two is redone by the next run.
	 * Returns how many were retired (0 if the retire guard blocked it).
	 */
	private int retireMissing(Set<Long> seen) {
		Instant now = Instant.now();
		return retirementGuard.retireMissing(FEED_ID, repository.findActiveKeys(TatoebaParser.SOURCE), seen, batch -> {
			indexWriter.deleteAll(batch.stream().map(String::valueOf).toList());
			repository.retire(batch, now);
		});
	}

	/** Persists the batch to Postgres, mirrors it to Elasticsearch, and clears it. */
	private int flush(List<ExampleSentence> batch) {
		if (batch.isEmpty()) {
			return 0;
		}
		repository.saveAll(batch);
		indexWriter.saveAll(batch.stream().map(this::toDocument).toList());
		int count = batch.size();
		batch.clear();
		return count;
	}

	private ExampleSentenceDocument toDocument(ExampleSentence sentence) {
		ExampleSentenceDocument doc = new ExampleSentenceDocument();
		doc.setId(String.valueOf(sentence.getId()));
		doc.setJapanese(sentence.getJapanese());
		doc.setEnglish(sentence.getEnglish());
		doc.setIndexWords(sentence.getIndexWords());
		return doc;
	}
}
