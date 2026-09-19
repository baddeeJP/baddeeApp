package badee.updaterComponent.hub;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual trigger for the update jobs, so a run can be kicked off on demand
 * without waiting for the schedule (handy for local testing). Runs jobs on a
 * background thread and returns immediately, since a full source update can take
 * minutes.
 */
@RestController
@RequestMapping("/admin/updates")
public class UpdateController {

	private final UpdateScheduler scheduler;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public UpdateController(UpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/** Lists the ids of all registered source modules. */
	@GetMapping
	public List<String> list() {
		return scheduler.moduleIds();
	}

	/** Triggers a run of every source module in the background. */
	@PostMapping
	public ResponseEntity<String> runAll() {
		executor.submit(scheduler::runAll);
		return ResponseEntity.accepted().body("Update started for all sources");
	}

	/** Triggers a run of a single source module by id, in the background. */
	@PostMapping("/{id}")
	public ResponseEntity<String> runOne(@PathVariable String id) {
		if (!scheduler.moduleIds().contains(id)) {
			return ResponseEntity.notFound().build();
		}
		executor.submit(() -> scheduler.runById(id));
		return ResponseEntity.accepted().body("Update started for source [" + id + "]");
	}
}
