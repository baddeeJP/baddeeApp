package badee.updaterComponent.spokes.jmdict;

import badee.updaterComponent.hub.DataSourceModule;
import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.RetirementGuard;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.jmdict.domain.Reading;
import badee.updaterComponent.spokes.jmdict.domain.VocabEntry;
import badee.updaterComponent.spokes.jmdict.domain.VocabEntryRepository;
import badee.updaterComponent.spokes.jmdict.fetch.JmdictFetcher;
import badee.updaterComponent.spokes.jmdict.parse.JmdictParser;
import badee.updaterComponent.spokes.jmdict.search.VocabDocument;
import badee.updaterComponent.spokes.jmdict.search.VocabIndexWriter;
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
 * JMdict spoke orchestrator: downloads the JMdict (English) dictionary, and if
 * it has changed since the last run, parses it into {@code VocabEntry}/
 * {@code Reading}/{@code Sense} rows in Postgres and mirrors each entry into the
 * {@code vocab} Elasticsearch index for the separate search application.
 *
 * <p>Concerns are split into sub-packages: {@code fetch} (download), {@code parse}
 * (StAX parsing), {@code domain} (JPA entities + repository), {@code search}
 * (Elasticsearch document + write-only index writer).
 */
@Component
public class JmdictModule implements DataSourceModule {

	private static final Logger log = LoggerFactory.getLogger(JmdictModule.class);
	private static final String FEED_ID = "jmdict";
	private static final int BATCH_SIZE = 1000;

	private final JmdictFetcher fetcher;
	private final JmdictParser parser;
	private final VocabEntryRepository vocabEntryRepository;
	private final VocabIndexWriter indexWriter;
	private final SourceVersionService sourceVersionService;
	private final RetirementGuard retirementGuard;

	public JmdictModule(JmdictFetcher fetcher, JmdictParser parser,
			VocabEntryRepository vocabEntryRepository, VocabIndexWriter indexWriter,
			SourceVersionService sourceVersionService,
			RetirementGuard retirementGuard) {
		this.fetcher = fetcher;
		this.parser = parser;
		this.vocabEntryRepository = vocabEntryRepository;
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
			Path xmlFile = fetcher.fetch();
			String hash = Hashing.sha256(xmlFile);
			if (!sourceVersionService.hasChanged(FEED_ID, hash)) {
				log.info("JMdict unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<VocabEntry> batch = new ArrayList<>(BATCH_SIZE);
			int[] total = {0};
			Set<Long> seen = new HashSet<>();
			parser.parse(xmlFile, entry -> {
				seen.add(entry.getEntSeq());
				batch.add(entry);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);
			// Only reached when the whole file parsed and persisted: a failure throws above.
			int retired = retireMissing(seen);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("JMdict update complete: {} entries persisted and indexed, {} retired", total[0], retired);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("JMdict update failed", e);
		}
	}

	/**
	 * Retires active entries the new file no longer has: deleted from search first, then
	 * marked in Postgres, so a failure between the two is redone by the next run.
	 * Returns how many were retired (0 if the retire guard blocked it).
	 */
	private int retireMissing(Set<Long> seen) {
		Instant now = Instant.now();
		return retirementGuard.retireMissing(FEED_ID, vocabEntryRepository.findActiveKeys(), seen, batch -> {
			indexWriter.deleteAll(batch.stream().map(String::valueOf).toList());
			vocabEntryRepository.retire(batch, now);
		});
	}

	/** Persists the batch to Postgres, mirrors it to Elasticsearch, and clears it. */
	private int flush(List<VocabEntry> batch) {
		if (batch.isEmpty()) {
			return 0;
		}
		vocabEntryRepository.saveAll(batch);
		indexWriter.saveAll(batch.stream().map(this::toDocument).toList());
		int count = batch.size();
		batch.clear();
		return count;
	}

	private VocabDocument toDocument(VocabEntry entry) {
		VocabDocument doc = new VocabDocument();
		doc.setId(String.valueOf(entry.getEntSeq()));
		doc.setCommon(entry.isCommon());
		doc.setKanji(entry.getReadings().stream()
				.filter(Reading::isKanji).map(Reading::getText).toList());
		doc.setReadings(entry.getReadings().stream()
				.filter(r -> !r.isKanji()).map(Reading::getText).toList());
		doc.setGlosses(entry.getSenses().stream()
				.flatMap(s -> s.getGlosses().stream()).toList());
		return doc;
	}
}
