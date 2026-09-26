package badee.updaterComponent.spokes.kanji.kanjidic2;

import badee.updaterComponent.hub.Hashing;
import badee.updaterComponent.hub.SourceVersionService;
import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import badee.updaterComponent.spokes.kanji.domain.Kanji;
import badee.updaterComponent.spokes.kanji.domain.KanjiUpserter;
import badee.updaterComponent.spokes.kanji.fetch.KanjiFileFetcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * KanjiDic2 sub-source: core kanji fields (readings, meanings, stroke count,
 * grade, JLPT, frequency, classical radical). Runs first so the other
 * sub-sources usually find fully populated rows to enrich.
 */
@Component
@Order(1)
public class KanjiDic2Source implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(KanjiDic2Source.class);
	private static final String FEED_ID = "kanji.kanjidic2";
	private static final int BATCH_SIZE = 1000;

	private final KanjiFileFetcher fetcher;
	private final KanjiDic2Parser parser;
	private final KanjiUpserter upserter;
	private final SourceVersionService sourceVersionService;
	private final String downloadUrl;

	public KanjiDic2Source(KanjiFileFetcher fetcher, KanjiDic2Parser parser, KanjiUpserter upserter,
			SourceVersionService sourceVersionService,
			@Value("${updater.kanjidic2.url:http://ftp.edrdg.org/pub/Nihongo/kanjidic2.xml.gz}") String downloadUrl) {
		this.fetcher = fetcher;
		this.parser = parser;
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
			Path xmlFile = fetcher.fetch(downloadUrl, "kanjidic2.xml");
			String hash = Hashing.sha256(xmlFile);
			if (!sourceVersionService.hasChanged(FEED_ID, hash)) {
				log.info("KanjiDic2 unchanged (hash {}), skipping reparse", hash);
				return;
			}

			List<KanjiDic2Character> batch = new ArrayList<>(BATCH_SIZE);
			int[] total = {0};
			parser.parse(xmlFile, character -> {
				batch.add(character);
				if (batch.size() >= BATCH_SIZE) {
					total[0] += flush(batch);
				}
			});
			total[0] += flush(batch);

			sourceVersionService.markUpdated(FEED_ID, hash);
			log.info("KanjiDic2 update complete: {} kanji persisted", total[0]);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new IllegalStateException("KanjiDic2 update failed", e);
		}
	}

	private int flush(List<KanjiDic2Character> batch) {
		upserter.upsert(batch, KanjiDic2Character::literal, KanjiDic2Source::apply);
		int count = batch.size();
		batch.clear();
		return count;
	}

	private static void apply(Kanji kanji, KanjiDic2Character c) {
		kanji.setCodepoint(c.codepoint());
		kanji.setClassicalRadical(c.classicalRadical());
		kanji.setGrade(c.grade());
		kanji.setJoyo(c.joyo());
		kanji.setStrokeCount(c.strokeCount());
		kanji.setFrequency(c.frequency());
		kanji.setJlpt(c.jlpt());
		kanji.setOnyomi(c.onyomi());
		kanji.setKunyomi(c.kunyomi());
		kanji.setNanori(c.nanori());
		kanji.setMeanings(c.meanings());
	}
}
