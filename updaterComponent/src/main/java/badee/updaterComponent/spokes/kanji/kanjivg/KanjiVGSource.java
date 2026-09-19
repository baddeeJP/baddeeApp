package badee.updaterComponent.spokes.kanji.kanjivg;

import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * KanjiVG sub-source: stroke-order and SVG path data, enriching existing Kanji
 * rows. Stub — fetch/parse/persist to be implemented.
 */
@Component
public class KanjiVGSource implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(KanjiVGSource.class);

	@Override
	public String feedId() {
		return "kanji.kanjivg";
	}

	@Override
	public void sync() {
		// TODO: download the KanjiVG release, hash-check, parse per-character SVG,
		// enrich Kanji rows with stroke/SVG data.
		log.info("KanjiVG sync not yet implemented (stub)");
	}
}
