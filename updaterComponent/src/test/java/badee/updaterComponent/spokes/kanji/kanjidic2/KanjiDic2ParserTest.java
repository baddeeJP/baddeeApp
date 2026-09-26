package badee.updaterComponent.spokes.kanji.kanjidic2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KanjiDic2ParserTest {

	private static final String FIXTURE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<kanjidic2>
			<header><file_version>4</file_version></header>
			<character>
			<literal>亜</literal>
			<codepoint>
			<cp_value cp_type="ucs">4e9c</cp_value>
			<cp_value cp_type="jis208">1-16-01</cp_value>
			</codepoint>
			<radical>
			<rad_value rad_type="classical">7</rad_value>
			<rad_value rad_type="nelson_c">1</rad_value>
			</radical>
			<misc>
			<grade>8</grade>
			<stroke_count>7</stroke_count>
			<stroke_count>8</stroke_count>
			<freq>1509</freq>
			<jlpt>1</jlpt>
			</misc>
			<reading_meaning>
			<rmgroup>
			<reading r_type="pinyin">ya4</reading>
			<reading r_type="ja_on">ア</reading>
			<reading r_type="ja_kun">つ.ぐ</reading>
			<meaning>Asia</meaning>
			<meaning>rank next</meaning>
			<meaning m_lang="fr">Asie</meaning>
			</rmgroup>
			<nanori>や</nanori>
			</reading_meaning>
			</character>
			<character>
			<literal>丂</literal>
			<codepoint><cp_value cp_type="ucs">4e02</cp_value></codepoint>
			<radical><rad_value rad_type="classical">1</rad_value></radical>
			<misc><stroke_count>2</stroke_count></misc>
			</character>
			</kanjidic2>
			""";

	@Test
	void parsesCoreFieldsAndSkipsNonEnglishMeaningsAndMiscounts(@TempDir Path tempDir) throws IOException {
		Path xml = tempDir.resolve("kanjidic2.xml");
		Files.writeString(xml, FIXTURE, StandardCharsets.UTF_8);

		List<KanjiDic2Character> characters = new ArrayList<>();
		new KanjiDic2Parser().parse(xml, characters::add);

		assertEquals(2, characters.size());

		KanjiDic2Character a = characters.get(0);
		assertEquals("亜", a.literal());
		assertEquals("4e9c", a.codepoint());
		assertEquals(7, a.classicalRadical());
		assertEquals(8, a.grade());
		assertTrue(a.joyo(), "grade 8 is jouyou");
		assertEquals(7, a.strokeCount(), "first stroke_count wins over miscounts");
		assertEquals(1509, a.frequency());
		assertEquals(1, a.jlpt());
		assertEquals(List.of("ア"), a.onyomi());
		assertEquals(List.of("つ.ぐ"), a.kunyomi());
		assertEquals(List.of("や"), a.nanori());
		assertEquals(List.of("Asia", "rank next"), a.meanings());

		KanjiDic2Character rare = characters.get(1);
		assertNull(rare.grade());
		assertFalse(rare.joyo());
		assertTrue(rare.meanings().isEmpty());
	}
}
