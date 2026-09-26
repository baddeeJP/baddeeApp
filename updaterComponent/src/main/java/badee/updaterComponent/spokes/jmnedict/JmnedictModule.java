package badee.updaterComponent.spokes.jmnedict;

import badee.updaterComponent.hub.DataSourceModule;
import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.jmnedict.domain.NameEntry;
import badee.updaterComponent.spokes.jmnedict.domain.NameEntryRepository;
import badee.updaterComponent.spokes.jmnedict.fetch.JmnedictFetcher;
import badee.updaterComponent.spokes.jmnedict.parse.JmnedictParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JMnedict spoke orchestrator: downloads the JMnedict proper-name dictionary
 * (persons, places, organizations), and if it has changed since the last run,
 * parses it into {@code NameEntry} rows in Postgres. Not indexed for search.
 *
 * <p>Concerns are split into sub-packages: {@code fetch} (download),
 * {@code parse} (StAX parsing), and {@code domain} (JPA entity + repository).
 */
@Component
public class JmnedictModule implements DataSourceModule {

	private static final Logger log = LoggerFactory.getLogger(JmnedictModule.class);
	private static final String FEED_ID = "jmnedict";
	private static final int BATCH_SIZE = 1000;

	private final JmnedictFetcher fetcher;
	private final JmnedictParser parser;
	private final NameEntryRepository nameEntryRepository;
	private final SourceVersionService sourceVersionService;

	public JmnedictModule(JmnedictFetcher fetcher, JmnedictParser parser,
			NameEntryRepository nameEntryRepository, SourceVersionService sourceVersionService) {
		this.fetcher = fetcher;
		this.parser = parser;
		this.nameEntryRepository = nameEntryRepository;
		this.sourceVersionService = sourceVersionService;
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
				log.info("JMnedict unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<NameEntry> batch = new ArrayList<>(BATCH_SIZE);
			int[] total = {0};
			parser.parse(xmlFile, entry -> {
				batch.add(entry);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("JMnedict update complete: {} names persisted", total[0]);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("JMnedict update failed", e);
		}
	}

	/** Persists the batch to Postgres and clears it. */
	private int flush(List<NameEntry> batch) {
		if (batch.isEmpty()) {
			return 0;
		}
		nameEntryRepository.saveAll(batch);
		int count = batch.size();
		batch.clear();
		return count;
	}
}
