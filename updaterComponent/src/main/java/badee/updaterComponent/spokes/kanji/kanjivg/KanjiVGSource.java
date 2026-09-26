package badee.updaterComponent.spokes.kanji.kanjivg;

import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import badee.updaterComponent.spokes.kanji.domain.KanjiUpserter;
import badee.updaterComponent.spokes.kanji.fetch.KanjiFileFetcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * KanjiVG sub-source: ordered SVG stroke paths, enriching Kanji rows. KanjiVG
 * also covers kana and symbols; only Han characters are kept, since those are
 * what the {@code kanji} table models.
 */
@Component
@Order(3)
public class KanjiVGSource implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(KanjiVGSource.class);
	private static final String FEED_ID = "kanji.kanjivg";
	private static final int BATCH_SIZE = 1000;

	private final KanjiVGReleaseLocator releaseLocator;
	private final KanjiFileFetcher fetcher;
	private final KanjiVGParser parser;
	private final KanjiUpserter upserter;
	private final SourceVersionService sourceVersionService;

	public KanjiVGSource(KanjiVGReleaseLocator releaseLocator, KanjiFileFetcher fetcher,
			KanjiVGParser parser, KanjiUpserter upserter, SourceVersionService sourceVersionService) {
		this.releaseLocator = releaseLocator;
		this.fetcher = fetcher;
		this.parser = parser;
		this.upserter = upserter;
		this.sourceVersionService = sourceVersionService;
	}

	@Override
	public String feedId() {
		return FEED_ID;
	}

	@Override
	public void sync() {
		try {
			Path xmlFile = fetcher.fetch(releaseLocator.downloadUrl(), "kanjivg.xml");
			String hash = Hashing.sha256(xmlFile);
			if (!sourceVersionService.hasChanged(FEED_ID, hash)) {
				log.info("KanjiVG unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<KanjiStrokes> batch = new ArrayList<>(BATCH_SIZE);
			int[] total = {0};
			parser.parse(xmlFile, strokes -> {
				if (!isHan(strokes.character())) {
					return;
				}
				batch.add(strokes);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("KanjiVG update complete: {} kanji enriched with stroke data", total[0]);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("KanjiVG update failed", e);
		}
	}

	private int flush(List<KanjiStrokes> batch) {
		upserter.upsert(batch, KanjiStrokes::character,
				(kanji, strokes) -> kanji.setStrokePaths(strokes.strokePaths()));
		int count = batch.size();
		batch.clear();
		return count;
	}

	private static boolean isHan(String character) {
		return Character.UnicodeScript.of(character.codePointAt(0)) == Character.UnicodeScript.HAN;
	}
}
