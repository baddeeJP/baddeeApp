package badee.updaterComponent.spokes.kanji.radkfile;

import badee.updaterComponent.spokes.kanji.KanjiSubSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RADKFILE sub-source: radical decomposition (which radicals compose each
 * kanji) and the Radical table. Stub — fetch/parse/persist to be implemented.
 */
@Component
public class RadkfileSource implements KanjiSubSource {

	private static final Logger log = LoggerFactory.getLogger(RadkfileSource.class);

	@Override
	public String feedId() {
		return "kanji.radkfile";
	}

	@Override
	public void sync() {
		// TODO: download radkfile, hash-check, parse radical->kanji mappings,
		// upsert Radical rows and kanji/radical associations.
		log.info("RADKFILE sync not yet implemented (stub)");
	}
}
