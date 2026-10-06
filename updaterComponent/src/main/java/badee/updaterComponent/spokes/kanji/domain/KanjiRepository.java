package badee.updaterComponent.spokes.kanji.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface KanjiRepository extends JpaRepository<Kanji, String> {

	/** Characters KanjiDic2 has data on (it always sets the code point). */
	@Query("select k.character from Kanji k where k.codepoint is not null")
	List<String> findKanjiDic2Characters();

	/** Characters RADKFILE has data on (at least one radical link). */
	@Query(value = "select distinct kanji from kanji_radical", nativeQuery = true)
	List<String> findRadkfileCharacters();

	/** Characters KanjiVG has data on (at least one stroke path). */
	@Query(value = "select character from kanji where cardinality(stroke_paths) > 0", nativeQuery = true)
	List<String> findKanjiVGCharacters();

	/**
	 * Retires active kanji that no sub-source has data on any more. The row stays
	 * (see {@code V2__retired_at.sql}); an upsert from any sub-source un-retires it.
	 */
	@Transactional
	@Modifying
	@Query(value = """
			update kanji set retired_at = :retiredAt
			where retired_at is null
			  and codepoint is null
			  and coalesce(cardinality(stroke_paths), 0) = 0
			  and not exists (select 1 from kanji_radical r where r.kanji = kanji.character)""",
			nativeQuery = true)
	int retireKanjiWithoutData(Instant retiredAt);
}
