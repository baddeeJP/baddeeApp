package badee.updaterComponent.hub;

/**
 * One implementation per upstream data source (a "spoke"). Each module is
 * fully responsible for fetching its own upstream file(s), checking them
 * against {@link SourceVersionService} for changes, parsing, and persisting.
 */
public interface DataSourceModule {

	String id();

	void run();
}
