package badee.updaterComponent.spokes.kanji.radkfile;

import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import badee.updaterComponent.spokes.kanji.domain.KanjiUpserter;
import badee.updaterComponent.spokes.kanji.domain.Radical;
import badee.updaterComponent.spokes.kanji.domain.RadicalRepository;
import badee.updaterComponent.spokes.kanji.fetch.KanjiFileFetcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RADKFILE sub-source: upserts the {@code Radical} table and replaces each
 * kanji's radical decomposition. RADKFILE lists radical → kanji, so the
 * mapping is inverted to kanji → radicals before being applied.
 */
@Component
@Order(2)
public class RadkfileSource implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(RadkfileSource.class);
	private static final String FEED_ID = "kanji.radkfile";
	private static final int BATCH_SIZE = 1000;

	private final KanjiFileFetcher fetcher;
	private final RadkfileParser parser;
	private final RadicalRepository radicalRepository;
	private final KanjiUpserter upserter;
	private final SourceVersionService sourceVersionService;
	private final String downloadUrl;

	public RadkfileSource(KanjiFileFetcher fetcher, RadkfileParser parser,
			RadicalRepository radicalRepository, KanjiUpserter upserter,
			SourceVersionService sourceVersionService,
			@Value("${updater.radkfile.url:http://ftp.edrdg.org/pub/Nihongo/radkfile.gz}") String downloadUrl) {
		this.fetcher = fetcher;
		this.parser = parser;
		this.radicalRepository = radicalRepository;
		this.upserter = upserter;
		this.sourceVersionService = sourceVersionService;
		this.downloadUrl = downloadUrl;
	}

	@Override
	public String feedId() {
		return FEED_ID;
	}

	@Override
	public void sync() {
		try {
			Path file = fetcher.fetch(downloadUrl, "radkfile");
			String hash = Hashing.sha256(file);
			if (!sourceVersionService.hasChanged(FEED_ID, hash)) {
				log.info("RADKFILE unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<Radical> radicals = new ArrayList<>();
			Map<String, List<String>> radicalsByKanji = new LinkedHashMap<>();
			parser.parse(file, block -> {
				radicals.add(new Radical(block.radical(), block.strokeCount()));
				for (String kanji : block.kanji()) {
					radicalsByKanji.computeIfAbsent(kanji, k -> new ArrayList<>()).add(block.radical());
				}
			});
			radicalRepository.saveAll(radicals);

			List<Map.Entry<String, List<String>>> entries = new ArrayList<>(radicalsByKanji.entrySet());
			for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
				upserter.upsert(entries.subList(i, Math.min(i + BATCH_SIZE, entries.size())),
						Map.Entry::getKey,
						(kanji, entry) -> kanji.replaceRadicals(entry.getValue().stream()
								.map(radicalRepository::getReferenceById).toList()));
			}

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("RADKFILE update complete: {} radicals, {} kanji decomposed",
					radicals.size(), entries.size());
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("RADKFILE update failed", e);
		}
	}
}
