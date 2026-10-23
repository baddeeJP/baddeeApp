package badee.updaterComponent.hub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * With {@code updater.run-on-startup=true} (the docker profile), queues a run of
 * every module once the application is ready, so {@code docker compose up}
 * brings the databases up to date without waiting for the cron. The run is in
 * the background: the admin API and health endpoint are available meanwhile.
 */
@Component
@ConditionalOnProperty(name = "updater.run-on-startup", havingValue = "true")
class StartupUpdateRunner {

	private final UpdateScheduler scheduler;

	StartupUpdateRunner(UpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	@EventListener(ApplicationReadyEvent.class)
	void runAllOnStartup() {
		scheduler.runAllInBackground();
	}
}
