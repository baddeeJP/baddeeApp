package badee.updaterComponent.spokes.jmnedict;

import badee.updaterComponent.hub.DataSourceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JMnedict spoke: proper-name dictionary (persons, places, organizations),
 * written to Postgres only (not indexed for search). Stub — wired into the
 * scheduler; fetch/parse/persist to be implemented.
 */
@Component
public class JmnedictModule implements DataSourceModule {

	private static final Logger log = LoggerFactory.getLogger(JmnedictModule.class);

	@Override
	public String id() {
		return "jmnedict";
	}

	@Override
	public void run() {
		// TODO: download JMnedict.xml.gz, hash-check via SourceVersionService,
		// StAX-parse <entry> elements, upsert NameEntry rows.
		log.info("JMnedict sync not yet implemented (stub)");
	}
}
