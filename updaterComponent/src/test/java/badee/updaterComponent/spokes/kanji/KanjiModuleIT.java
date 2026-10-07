package badee.updaterComponent.spokes.kanji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.spokes.kanji.kanjidic2.KanjiDic2Source;
import badee.updaterComponent.spokes.kanji.kanjivg.KanjiVGSource;
import badee.updaterComponent.spokes.kanji.radkfile.RadkfileSource;
import badee.updaterComponent.support.IntegrationTest;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class KanjiModuleIT extends IntegrationTest {

	private static final String KANJIDIC2 = """
			<?xml version="1.0" encoding="UTF-8"?>
			<kanjidic2>
			<character>
			<literal>丁</literal>
			<codepoint><cp_value cp_type="ucs">4e01</cp_value></codepoint>
			<radical><rad_value rad_type="classical">1</rad_value></radical>
			<misc><grade>3</grade><stroke_count>2</stroke_count><freq>1312</freq><jlpt>1</jlpt></misc>
			<reading_meaning>
			<rmgroup>
			<reading r_type="ja_on">チョウ</reading>
			<reading r_type="ja_kun">ひのと</reading>
			<meaning>street</meaning>
			<meaning>ward</meaning>
			</rmgroup>
			</reading_meaning>
			</character>
			<character>
			<literal>亜</literal>
			<codepoint><cp_value cp_type="ucs">4e9c</cp_value></codepoint>
			<misc><grade>8</grade><stroke_count>7</stroke_count></misc>
			</character>
			</kanjidic2>
			""";

	private static final String RADKFILE_V1 = """
			# RADKFILE fixture
			$ 一 1
			丁亜
			$ 亅 1
			丁
			""";

	private static final String RADKFILE_V2 = """
			# RADKFILE fixture, 丁 no longer lists 一
			$ 一 1
			亜
			$ 亅 1
			丁
			""";

	private static final String KANJIVG = """
			<?xml version="1.0" encoding="UTF-8"?>
			<kanjivg xmlns:kvg='http://kanjivg.tagaini.net'>
			<kanji id="kvg:kanji_04e01">
			<g><path d="M14,24c2,0,60,-6,79,-6"/><path d="M52,25c1,1,1,60,-8,69"/></g>
			</kanji>
			<kanji id="kvg:kanji_03042">
			<g><path d="M1,1"/></g>
			</kanji>
			</kanjivg>
			""";

	@Autowired
	private KanjiDic2Source kanjiDic2;

	@Autowired
	private RadkfileSource radkfile;

	@Autowired
	private KanjiVGSource kanjiVG;

	@BeforeEach
	void publishUpstreamFiles() {
		fixtures.publishGzipped("/kanjidic2.xml.gz", KANJIDIC2);
		publishRadkfile(RADKFILE_V1);
		publishKanjiVG(KANJIVG);
		// Like github.com: releases/latest redirects to the newest tag's page.
		fixtures.publishRedirect("/KanjiVG/kanjivg/releases/latest",
				fixtures.url("/KanjiVG/kanjivg/releases/tag/r20250816"));
	}

	@Test
	void combinesAllThreeSubSourcesOntoOneKanjiRow() {
		run("kanji");

		Map<String, Object> tei = kanjiRow("丁");
		assertEquals("4e01", tei.get("codepoint"));
		assertEquals("チョウ", tei.get("onyomi"));
		assertEquals("ひのと", tei.get("kunyomi"));
		assertEquals("street|ward", tei.get("meanings"));
		assertEquals(2, tei.get("stroke_count"));
		assertEquals(3, tei.get("grade"));
		assertEquals(true, tei.get("joyo"));
		assertEquals(1312, tei.get("frequency"));
		assertEquals(1, tei.get("classical_radical"));
		assertEquals("M14,24c2,0,60,-6,79,-6|M52,25c1,1,1,60,-8,69", tei.get("stroke_paths"));
		assertEquals(List.of("一", "亅"), radicalsOf("丁"));
		assertEquals(List.of("一"), radicalsOf("亜"));
		assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM radical", Integer.class));

		assertEquals(List.of("丁", "亜"), jdbc.queryForList(
				"SELECT character FROM kanji ORDER BY codepoint", String.class),
				"kana from KanjiVG (あ) is not stored as a kanji");
		assertEquals(List.of("kanji.kanjidic2", "kanji.kanjivg", "kanji.radkfile"), jdbc.queryForList(
				"SELECT feed_id FROM source_version WHERE content_hash IS NOT NULL ORDER BY feed_id",
				String.class));
	}

	@Test
	void subSourcesAreOrderIndependentAndDoNotClobberEachOther() {
		radkfile.sync();
		kanjiVG.sync();
		// KanjiDic2 last: it must enrich the bare rows without wiping radicals or strokes.
		kanjiDic2.sync();

		Map<String, Object> tei = kanjiRow("丁");
		assertEquals("4e01", tei.get("codepoint"));
		assertEquals("street|ward", tei.get("meanings"));
		assertEquals("M14,24c2,0,60,-6,79,-6|M52,25c1,1,1,60,-8,69", tei.get("stroke_paths"));
		assertEquals(List.of("一", "亅"), radicalsOf("丁"));
	}

	@Test
	void changedRadkfileReplacesRatherThanAppendsDecomposition() {
		radkfile.sync();
		publishRadkfile(RADKFILE_V2);

		radkfile.sync();

		assertEquals(List.of("亅"), radicalsOf("丁"));
		assertEquals(List.of("一"), radicalsOf("亜"));
	}

	@Test
	void subSourceDroppingAKanjiClearsOnlyItsOwnDataWhileAnotherStillHasIt() {
		run("kanji");

		publishRadkfile("""
				# 亜 dropped from every radical
				$ 一 1
				丁
				$ 亅 1
				丁
				""");
		radkfile.sync();
		fixtures.publishGzipped("/kanjidic2.xml.gz",
				KANJIDIC2.replaceAll("(?s)<character>\\s*<literal>丁.*?</character>", ""));
		kanjiDic2.sync();

		assertEquals(List.of(), radicalsOf("亜"), "RADKFILE cleared its links");
		assertEquals("4e9c", kanjiRow("亜").get("codepoint"), "KanjiDic2 data untouched");
		assertNull(kanjiRow("丁").get("codepoint"), "KanjiDic2 cleared its fields");
		assertEquals(0, jdbc.queryForObject(
				"SELECT cardinality(meanings) FROM kanji WHERE character = '丁'", Integer.class),
				"KanjiDic2 cleared its lists to empty");
		assertEquals(List.of("一", "亅"), radicalsOf("丁"), "RADKFILE data untouched");
		assertEquals("M14,24c2,0,60,-6,79,-6|M52,25c1,1,1,60,-8,69", kanjiRow("丁").get("stroke_paths"));
		assertFalse(isRetired("丁"));
		assertFalse(isRetired("亜"));
	}

	@Test
	void kanjiIsRetiredOnlyOnceNoSubSourceHasItAndUnretiredWhenOneListsItAgain() {
		String withOtsu = KANJIVG.replace("</kanjivg>", """
				<kanji id="kvg:kanji_04e59"><g><path d="M2,2"/></g></kanji>
				</kanjivg>""");
		publishKanjiVG(withOtsu);
		run("kanji");
		assertFalse(isRetired("乙"));

		publishKanjiVG(KANJIVG); // 乙 dropped; KanjiVG was its only sub-source
		kanjiVG.sync();

		assertTrue(isRetired("乙"));
		assertEquals(0, jdbc.queryForObject(
				"SELECT cardinality(stroke_paths) FROM kanji WHERE character = '乙'", Integer.class),
				"stroke data cleared, row kept");
		assertFalse(isRetired("丁"));

		publishKanjiVG(withOtsu);
		kanjiVG.sync();

		assertFalse(isRetired("乙"));
		assertEquals("M2,2", kanjiRow("乙").get("stroke_paths"));
	}

	@Test
	void retireGuardBlocksASubSourceFromClearingMostOfItsData() {
		run("kanji");

		publishRadkfile("""
				# truncated mirror: a single radical block for neither kanji
				$ 乙 1
				九
				""");
		radkfile.sync();

		assertEquals(List.of("一", "亅"), radicalsOf("丁"));
		assertEquals(List.of("一"), radicalsOf("亜"));
		assertEquals(List.of("乙"), radicalsOf("九"), "upserts still applied");
	}

	/** Real KanjiVG stroke paths run past 255 chars; they used to abort the whole sub-source. */
	@Test
	void storesStrokePathsLongerThanAVarchar255() {
		String longPath = "M10,10c1,2,3,4,5,6".repeat(20);
		publishKanjiVG("""
				<?xml version="1.0" encoding="UTF-8"?>
				<kanjivg><kanji id="kvg:kanji_04e01"><g><path d="%s"/></g></kanji></kanjivg>
				""".formatted(longPath));

		kanjiVG.sync();

		assertEquals(longPath, kanjiRow("丁").get("stroke_paths"));
	}

	/** Publishes the asset at the URL the locator derives from tag r20250816. */
	private void publishKanjiVG(String content) {
		fixtures.publishGzipped("/KanjiVG/kanjivg/releases/download/r20250816/kanjivg-20250816.xml.gz", content);
	}

	private void publishRadkfile(String content) {
		fixtures.publishGzipped("/radkfile.gz", content, Charset.forName("EUC-JP"));
	}

	private Map<String, Object> kanjiRow(String character) {
		return jdbc.queryForMap("""
				SELECT codepoint, array_to_string(onyomi, '|') AS onyomi,
				       array_to_string(kunyomi, '|') AS kunyomi, array_to_string(meanings, '|') AS meanings,
				       stroke_count, grade, joyo, frequency, classical_radical,
				       array_to_string(stroke_paths, '|') AS stroke_paths
				FROM kanji WHERE character = ?""", character);
	}

	private boolean isRetired(String character) {
		return jdbc.queryForObject(
				"SELECT retired_at IS NOT NULL FROM kanji WHERE character = ?", Boolean.class, character);
	}

	private List<String> radicalsOf(String character) {
		return jdbc.queryForList(
				"SELECT radical FROM kanji_radical WHERE kanji = ? ORDER BY radical", String.class, character);
	}
}
