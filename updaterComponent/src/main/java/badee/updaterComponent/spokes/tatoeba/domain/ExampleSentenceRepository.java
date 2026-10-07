package badee.updaterComponent.spokes.tatoeba.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

	/** Keys of the entries not retired, i.e. those the last upstream file still had. */
	@Query("select e.id from ExampleSentence e where e.retiredAt is null and e.source = :source")
	List<Long> findActiveKeys(String source);

	/** Marks entries retired; the rows (and their children) stay (see {@code V2__retired_at.sql}). */
	@Transactional
	@Modifying
	@Query("update ExampleSentence e set e.retiredAt = :retiredAt where e.id in :keys")
	int retire(Collection<Long> keys, Instant retiredAt);
}
