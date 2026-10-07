package badee.updaterComponent.spokes.jmnedict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JmnedictModuleIT extends IntegrationTest {

	private static final String HEADER = """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE JMnedict [
			<!ELEMENT JMnedict (entry*)>
			<!ENTITY surname "family or surname">
			<!ENTITY place "place name">
			]>
			<JMnedict>
			""";

	private static final String KOIZUMI = """
			<entry>
			<ent_seq>5000001</ent_seq>
			<k_ele><keb>小泉</keb></k_ele>
			<r_ele><reb>こいずみ</reb></r_ele>
			<trans>
			<name_type>&surname;</name_type>
			<name_type>&place;</name_type>
			<trans_det>Koizumi</trans_det>
			</trans>
			</entry>
			""";

	private static final String TANAKA = """
			<entry>
			<ent_seq>5000002</ent_seq>
			<k_ele><keb>田中</keb></k_ele>
			<r_ele><reb>たなか</reb></r_ele>
			<trans><name_type>&surname;</name_type><trans_det>Tanaka</trans_det></trans>
			</entry>
			""";

	@Test
	void persistsNamesAsArrayColumnsAndIsIdempotentOnRerun() {
		fixtures.publishGzipped("/JMnedict.xml.gz", HEADER + KOIZUMI + "</JMnedict>");

		run("jmnedict");
		run("jmnedict");

		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM name_entry", Integer.class));
		Map<String, Object> row = jdbc.queryForMap("""
				SELECT array_to_string(kanji, '|') AS kanji, array_to_string(readings, '|') AS readings,
				       array_to_string(name_types, '|') AS name_types,
				       array_to_string(translations, '|') AS translations
				FROM name_entry WHERE ent_seq = 5000001""");
		assertEquals("小泉", row.get("kanji"));
		assertEquals("こいずみ", row.get("readings"));
		assertEquals("family or surname|place name", row.get("name_types"));
		assertEquals("Koizumi", row.get("translations"));
		assertEquals(List.of("jmnedict"), jdbc.queryForList(
				"SELECT feed_id FROM source_version WHERE content_hash IS NOT NULL", String.class));
	}

	@Test
	void changedNameIsUpdatedInPlace() {
		publish(KOIZUMI + TANAKA);
		run("jmnedict");

		publish(KOIZUMI.replace("Koizumi", "Koizumi (surname)") + TANAKA);
		run("jmnedict");

		assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM name_entry", Integer.class),
				"same ent_seq, no new row");
		assertEquals("Koizumi (surname)", jdbc.queryForObject(
				"SELECT array_to_string(translations, '|') FROM name_entry WHERE ent_seq = 5000001", String.class));
	}

	@Test
	void nameRemovedUpstreamIsRetiredThenUnretiredWhenBack() {
		publish(KOIZUMI + TANAKA);
		run("jmnedict");

		publish(KOIZUMI);
		run("jmnedict");

		assertTrue(isRetired(5000002), "row kept, marked retired");
		assertFalse(isRetired(5000001));

		publish(KOIZUMI + TANAKA);
		run("jmnedict");

		assertFalse(isRetired(5000002));
	}

	private void publish(String entries) {
		fixtures.publishGzipped("/JMnedict.xml.gz", HEADER + entries + "</JMnedict>");
	}

	private boolean isRetired(long entSeq) {
		return jdbc.queryForObject(
				"SELECT retired_at IS NOT NULL FROM name_entry WHERE ent_seq = ?", Boolean.class, entSeq);
	}
}
