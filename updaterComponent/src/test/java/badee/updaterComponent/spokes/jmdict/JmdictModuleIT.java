package badee.updaterComponent.spokes.jmdict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.hub.SourceVersion;
import badee.updaterComponent.spokes.jmdict.search.VocabDocument;
import badee.updaterComponent.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class JmdictModuleIT extends IntegrationTest {

	private static final String HEADER = """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE JMdict [
			<!ELEMENT JMdict (entry*)>
			<!ENTITY n "noun (common) (futsuumeishi)">
			<!ENTITY uk "word usually written using kana alone">
			]>
			<JMdict>
			""";

	private static final String NIHONGO = """
			<entry>
			<ent_seq>1000001</ent_seq>
			<k_ele><keb>日本語</keb><ke_pri>news1</ke_pri></k_ele>
			<r_ele><reb>にほんご</reb></r_ele>
			<sense>
			<pos>&n;</pos>
			<gloss>Japanese (language)</gloss>
			</sense>
			</entry>
			""";

	private static final String ANO_V1 = """
			<entry>
			<ent_seq>1000002</ent_seq>
			<r_ele><reb>あの</reb></r_ele>
			<sense>
			<misc>&uk;</misc>
			<gloss>that</gloss>
			<gloss>those</gloss>
			</sense>
			</entry>
			""";

	private static final String ANO_V2 = """
			<entry>
			<ent_seq>1000002</ent_seq>
			<r_ele><reb>あの</reb></r_ele>
			<sense>
			<gloss>that over there</gloss>
			</sense>
			</entry>
			""";

	@Test
	void persistsEntriesToPostgresAndIndexesThemInElasticsearch() {
		publish(NIHONGO + ANO_V1);

		run("jmdict");

		assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM vocab_entry", Integer.class));
		assertEquals(true, jdbc.queryForObject(
				"SELECT common FROM vocab_entry WHERE ent_seq = 1000001", Boolean.class));
		assertEquals(List.of("日本語:true", "にほんご:false"), jdbc.queryForList(
				"SELECT text || ':' || kanji FROM reading WHERE ent_seq = 1000001 ORDER BY kanji DESC",
				String.class));
		assertEquals(List.of("noun (common) (futsuumeishi)"), jdbc.queryForList("""
				SELECT p.pos FROM sense_pos p JOIN sense s ON s.id = p.sense_id
				WHERE s.ent_seq = 1000001""", String.class), "DTD entity expanded to its description");
		assertEquals(List.of("that", "those"), glosses(1000002));

		VocabDocument doc = elasticsearchOperations.get("1000001", VocabDocument.class);
		assertEquals(List.of("日本語"), doc.getKanji());
		assertEquals(List.of("にほんご"), doc.getReadings());
		assertEquals(List.of("Japanese (language)"), doc.getGlosses());
		assertTrue(doc.isCommon());
		refresh(VocabDocument.class);
		assertEquals(2, elasticsearchOperations.count(
				elasticsearchOperations.matchAllQuery(), VocabDocument.class));

		SourceVersion version = sourceVersion("jmdict");
		assertEquals(64, version.getContentHash().length());
		assertTrue(version.getLastUpdatedAt() != null);
	}

	@Test
	void skipsReparseWhenUpstreamFileIsUnchanged() {
		publish(NIHONGO + ANO_V1);
		run("jmdict");
		SourceVersion first = sourceVersion("jmdict");
		// Tamper with the data: an unchanged file must not be reparsed, so this survives.
		jdbc.update("UPDATE vocab_entry SET common = true WHERE ent_seq = 1000002");

		run("jmdict");

		SourceVersion second = sourceVersion("jmdict");
		assertEquals(first.getLastUpdatedAt(), second.getLastUpdatedAt());
		assertTrue(second.getLastCheckedAt().isAfter(first.getLastCheckedAt()));
		assertEquals(true, jdbc.queryForObject(
				"SELECT common FROM vocab_entry WHERE ent_seq = 1000002", Boolean.class));
	}

	@Test
	void changedUpstreamFileReplacesEntriesWithoutDuplicatingChildren() {
		publish(NIHONGO + ANO_V1);
		run("jmdict");
		String firstHash = sourceVersion("jmdict").getContentHash();

		publish(NIHONGO + ANO_V2);
		run("jmdict");

		assertFalse(firstHash.equals(sourceVersion("jmdict").getContentHash()));
		assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM vocab_entry", Integer.class),
				"updated in place under the same ent_seq, no new row");
		assertEquals(List.of("that over there"), glosses(1000002));
		assertEquals(1, jdbc.queryForObject(
				"SELECT count(*) FROM sense WHERE ent_seq = 1000002", Integer.class));
		assertEquals(1, jdbc.queryForObject(
				"SELECT count(*) FROM reading WHERE ent_seq = 1000002", Integer.class));
		assertEquals(0, jdbc.queryForObject("""
				SELECT count(*) FROM sense_misc m JOIN sense s ON s.id = m.sense_id
				WHERE s.ent_seq = 1000002""", Integer.class));
		assertEquals(List.of("that over there"),
				elasticsearchOperations.get("1000002", VocabDocument.class).getGlosses());
	}

	@Test
	void entryRemovedUpstreamIsRetiredInPostgresAndDeletedFromSearch() {
		publish(NIHONGO + ANO_V1);
		run("jmdict");

		publish(NIHONGO);
		run("jmdict");

		assertTrue(isRetired(1000002), "row kept, marked retired");
		assertFalse(isRetired(1000001));
		assertEquals(List.of("that", "those"), glosses(1000002), "children kept");
		assertNull(elasticsearchOperations.get("1000002", VocabDocument.class));
		assertNotNull(elasticsearchOperations.get("1000001", VocabDocument.class));
	}

	@Test
	void entryReAddedUpstreamIsUnretiredAndReindexed() {
		publish(NIHONGO + ANO_V1);
		run("jmdict");
		publish(NIHONGO);
		run("jmdict");

		publish(NIHONGO + ANO_V2);
		run("jmdict");

		assertFalse(isRetired(1000002));
		assertEquals(List.of("that over there"),
				elasticsearchOperations.get("1000002", VocabDocument.class).getGlosses());
	}

	@Test
	void failedRunRetiresNothing() {
		publish(NIHONGO + ANO_V1);
		run("jmdict");

		fixtures.publishGzipped("/JMdict_e.gz", HEADER + NIHONGO); // truncated: no closing </JMdict>
		assertThrows(RuntimeException.class, () -> run("jmdict"));

		assertFalse(isRetired(1000002));
		assertNotNull(elasticsearchOperations.get("1000002", VocabDocument.class));
	}

	@Test
	void retireGuardBlocksRetiringTooManyEntriesButStillAppliesUpserts() {
		publish(NIHONGO + ANO_V1 + entry(1000004, "これ", "this"));
		run("jmdict");

		// Only 1 of 3 active entries would remain: below the test guard of 0.5.
		publish(entry(1000004, "これ", "this one"));
		run("jmdict");

		assertEquals(0, jdbc.queryForObject(
				"SELECT count(*) FROM vocab_entry WHERE retired_at IS NOT NULL", Integer.class));
		assertEquals(List.of("this one"), glosses(1000004));
		refresh(VocabDocument.class);
		assertEquals(3, elasticsearchOperations.count(
				elasticsearchOperations.matchAllQuery(), VocabDocument.class));
	}

	/** The real JMdict has glosses up to ~350 chars; one overlong value used to abort the run. */
	@Test
	void storesGlossesLongerThanAVarchar255() {
		String longGloss = "a very long explanatory gloss ".repeat(12).trim();
		publish("""
				<entry>
				<ent_seq>1000003</ent_seq>
				<r_ele><reb>ながい</reb></r_ele>
				<sense><gloss>%s</gloss></sense>
				</entry>
				""".formatted(longGloss));

		run("jmdict");

		assertEquals(List.of(longGloss), glosses(1000003));
	}

	private void publish(String entries) {
		fixtures.publishGzipped("/JMdict_e.gz", HEADER + entries + "</JMdict>");
	}

	private static String entry(long entSeq, String reading, String gloss) {
		return """
				<entry>
				<ent_seq>%d</ent_seq>
				<r_ele><reb>%s</reb></r_ele>
				<sense><gloss>%s</gloss></sense>
				</entry>
				""".formatted(entSeq, reading, gloss);
	}

	private boolean isRetired(long entSeq) {
		return jdbc.queryForObject(
				"SELECT retired_at IS NOT NULL FROM vocab_entry WHERE ent_seq = ?", Boolean.class, entSeq);
	}

	private List<String> glosses(long entSeq) {
		return jdbc.queryForList("""
				SELECT g.gloss FROM sense_gloss g JOIN sense s ON s.id = g.sense_id
				WHERE s.ent_seq = ? ORDER BY g.gloss""", String.class, entSeq);
	}
}
