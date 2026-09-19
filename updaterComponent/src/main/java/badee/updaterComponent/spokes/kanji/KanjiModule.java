package badee.updaterComponent.spokes.kanji;

import badee.updaterComponent.hub.DataSourceModule;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Combined Kanji spoke: a single {@link DataSourceModule} that orchestrates
 * three internally separated sub-sources (KanjiDic2, KanjiVG, RADKFILE), each
 * tracked independently via its own feed id. They are combined into one spoke
 * because they jointly build the same {@code Kanji}/{@code Radical} entities.
 *
 * <p>Stub: the module and its sub-sources are wired so the scheduler already
 * includes them; per-source fetch/parse/persist logic is still TODO.
 */
@Component
public class KanjiModule implements DataSourceModule {

	private static final Logger log = LoggerFactory.getLogger(KanjiModule.class);

	private final List<KanjiSubSource> subSources;

	public KanjiModule(List<KanjiSubSource> subSources) {
		this.subSources = subSources;
	}

	@Override
	public String id() {
		return "kanji";
	}

	@Override
	public void run() {
		for (KanjiSubSource subSource : subSources) {
			try {
				log.info("Running kanji sub-source [{}]", subSource.feedId());
				subSource.sync();
			} catch (Exception e) {
				log.error("Kanji sub-source [{}] failed", subSource.feedId(), e);
			}
		}
	}
}
