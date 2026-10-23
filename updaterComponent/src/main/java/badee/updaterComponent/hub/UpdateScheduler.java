package badee.updaterComponent.hub;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every registered {@link DataSourceModule} on a fixed schedule. Each
 * module is responsible for its own fetch/version-check/parse/persist steps;
 * a failure in one module is logged and does not prevent the others from
 * running.
 *
 * <p>Runs started on demand (admin API, start-up) go through one background
 * thread, so they queue behind each other instead of updating a feed twice at
 * once. The cron run uses the scheduler's own thread.
 */
@Component
public class UpdateScheduler {

	private static final Logger log = LoggerFactory.getLogger(UpdateScheduler.class);

	private final List<DataSourceModule> modules;
	private final ExecutorService background = Executors.newSingleThreadExecutor();

	public UpdateScheduler(List<DataSourceModule> modules) {
		this.modules = modules;
	}

	@Scheduled(cron = "${updater.schedule.cron}")
	public void runAll() {
		for (DataSourceModule module : modules) {
			runModule(module);
		}
	}

	/**
	 * Runs a single module by its {@link DataSourceModule#id()}.
	 *
	 * @return {@code true} if a module with that id was found and run,
	 *         {@code false} if no such module exists.
	 */
	public boolean runById(String id) {
		return modules.stream()
				.filter(module -> module.id().equals(id))
				.findFirst()
				.map(module -> {
					runModule(module);
					return true;
				})
				.orElse(false);
	}

	/** Queues a run of every module on the background thread and returns immediately. */
	public void runAllInBackground() {
		background.submit(this::runAll);
	}

	/**
	 * Queues a run of a single module on the background thread.
	 *
	 * @return {@code false} if no module has that id (nothing is queued).
	 */
	public boolean runInBackground(String id) {
		if (!moduleIds().contains(id)) {
			return false;
		}
		background.submit(() -> runById(id));
		return true;
	}

	/** Ids of all registered modules, for discovery by the manual-trigger API. */
	public List<String> moduleIds() {
		return modules.stream().map(DataSourceModule::id).toList();
	}

	private void runModule(DataSourceModule module) {
		try {
			log.info("Starting update for source [{}]", module.id());
			module.run();
			log.info("Finished update for source [{}]", module.id());
		} catch (Exception e) {
			log.error("Update failed for source [{}]", module.id(), e);
		}
	}
}
