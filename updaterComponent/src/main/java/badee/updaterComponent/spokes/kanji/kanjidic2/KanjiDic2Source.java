package badee.updaterComponent.spokes.kanji.kanjidic2;

import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * KanjiDic2 sub-source: core kanji fields (readings, meanings, stroke count,
 * grade, JLPT). Stub — fetch/parse/persist to be implemented.
 */
@Component
public class KanjiDic2Source implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(KanjiDic2Source.class);

	@Override
	public String feedId() {
		return "kanji.kanjidic2";
	}

	@Override
	public void sync() {
		// TODO: download kanjidic2.xml.gz, hash-check via SourceVersionService,
		// StAX-parse <character> elements, upsert Kanji core fields.
		log.info("KanjiDic2 sync not yet implemented (stub)");
	}
}
