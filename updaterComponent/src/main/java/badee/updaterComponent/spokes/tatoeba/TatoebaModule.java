package badee.updaterComponent.spokes.tatoeba;

import badee.updaterComponent.hub.DataSourceModule;
import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentence;
import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentenceRepository;
import badee.updaterComponent.spokes.tatoeba.fetch.TatoebaFetcher;
import badee.updaterComponent.spokes.tatoeba.parse.TatoebaParser;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceDocument;
import badee.updaterComponent.spokes.tatoeba.search.ExampleSentenceIndexWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

	public TatoebaModule(TatoebaFetcher fetcher, TatoebaParser parser,
			ExampleSentenceRepository repository, ExampleSentenceIndexWriter indexWriter,
			SourceVersionService sourceVersionService) {
		this.fetcher = fetcher;
		this.parser = parser;
		this.repository = repository;
		this.indexWriter = indexWriter;
		this.sourceVersionService = sourceVersionService;
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
			parser.parse(textFile, sentence -> {
				batch.add(sentence);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("Tatoeba update complete: {} sentences persisted and indexed", total[0]);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("Tatoeba update failed", e);
		}
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
