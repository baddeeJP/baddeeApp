package badee.updaterComponent.spokes.jmnedict.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.spokes.jmnedict.domain.NameEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JmnedictParserTest {

	private static final String HEADER = """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE JMnedict [
			<!ELEMENT JMnedict (entry*)>
			<!ENTITY surname "family or surname">
			<!ENTITY place "place name">
			<!ENTITY given "given name or forename, gender not specified">
			]>
			<JMnedict>
			""";

	private static final String ENTRIES = """
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
			<entry>
			<ent_seq>5000002</ent_seq>
			<r_ele><reb>あいこ</reb></r_ele>
			<trans>
			<name_type>&given;</name_type>
			<trans_det>Aiko</trans_det>
			</trans>
			</entry>
			""";

	@Test
	void parsesWritingsReadingsExpandedNameTypesAndTranslations(@TempDir Path tempDir) throws IOException {
		List<NameEntry> entries = parse(tempDir, HEADER + ENTRIES + "</JMnedict>");

		assertEquals(2, entries.size());

		NameEntry first = entries.get(0);
		assertEquals(5000001L, first.getEntSeq());
		assertEquals(List.of("小泉"), first.getKanji());
		assertEquals(List.of("こいずみ"), first.getReadings());
		assertEquals(List.of("family or surname", "place name"), first.getNameTypes());
		assertEquals(List.of("Koizumi"), first.getTranslations());

		NameEntry second = entries.get(1);
		assertTrue(second.getKanji().isEmpty(), "kana-only name has no kanji writing");
		assertEquals(List.of("given name or forename, gender not specified"), second.getNameTypes());
	}

	/** The real file has ~740k entity refs; recent JDKs refuse more than 2,500 by default. */
	@Test
	void expandsMoreEntitiesThanTheJdkDefaultLimit(@TempDir Path tempDir) throws IOException {
		StringBuilder xml = new StringBuilder(HEADER);
		int count = 3000;
		for (int i = 0; i < count; i++) {
			xml.append("<entry><ent_seq>").append(i)
					.append("</ent_seq><trans><name_type>&surname;</name_type></trans></entry>\n");
		}
		xml.append("</JMnedict>");

		List<NameEntry> entries = parse(tempDir, xml.toString());

		assertEquals(count, entries.size());
		assertEquals(List.of("family or surname"), entries.get(count - 1).getNameTypes());
	}

	private static List<NameEntry> parse(Path tempDir, String content) throws IOException {
		Path xml = tempDir.resolve("JMnedict.xml");
		Files.writeString(xml, content, StandardCharsets.UTF_8);
		List<NameEntry> entries = new ArrayList<>();
		new JmnedictParser().parse(xml, entries::add);
		return entries;
	}
}
