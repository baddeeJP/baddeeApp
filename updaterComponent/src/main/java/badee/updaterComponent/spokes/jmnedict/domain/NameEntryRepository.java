package badee.updaterComponent.spokes.jmnedict.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface NameEntryRepository extends JpaRepository<NameEntry, Long> {

	/** Keys of the entries not retired, i.e. those the last upstream file still had. */
	@Query("select e.entSeq from NameEntry e where e.retiredAt is null")
	List<Long> findActiveKeys();

	/** Marks entries retired; the rows (and their children) stay (see {@code V2__retired_at.sql}). */
	@Transactional
	@Modifying
	@Query("update NameEntry e set e.retiredAt = :retiredAt where e.entSeq in :keys")
	int retire(Collection<Long> keys, Instant retiredAt);
}
