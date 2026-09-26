package badee.updaterComponent.spokes.kanji.kanjivg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KanjiVGParserTest {

	private static final String FIXTURE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE kanjivg [
			<!ATTLIST g
			xmlns:kvg CDATA #FIXED "http://kanjivg.tagaini.net"
			kvg:element CDATA #IMPLIED >
			]>
			<kanjivg xmlns:kvg='http://kanjivg.tagaini.net'>
			<kanji id="kvg:kanji_04e01">
			<g id="kvg:04e01" kvg:element="丁">
				<g id="kvg:04e01-g1" kvg:element="一">
					<path id="kvg:04e01-s1" kvg:type="㇐" d="M14,24c2,0,60,-6,79,-6"/>
				</g>
				<g id="kvg:04e01-g2" kvg:element="亅">
					<path id="kvg:04e01-s2" kvg:type="㇚" d="M52,25c1,1,1,60,-8,69"/>
				</g>
			</g>
			</kanji>
			<kanji id="kvg:kanji_04e01-Kaisho">
			<g><path d="M0,0"/></g>
			</kanji>
			<kanji id="kvg:kanji_03042">
			<g><path d="M1,1"/></g>
			</kanji>
			</kanjivg>
			""";

	@Test
	void decodesCharacterFromIdAndKeepsStrokeOrderSkippingVariants(@TempDir Path tempDir) throws IOException {
		Path xml = tempDir.resolve("kanjivg.xml");
		Files.writeString(xml, FIXTURE, StandardCharsets.UTF_8);

		List<KanjiStrokes> kanji = new ArrayList<>();
		new KanjiVGParser().parse(xml, kanji::add);

		assertEquals(List.of(
				new KanjiStrokes("丁", List.of("M14,24c2,0,60,-6,79,-6", "M52,25c1,1,1,60,-8,69")),
				// Kana are emitted by the parser; KanjiVGSource filters to Han characters.
				new KanjiStrokes("あ", List.of("M1,1"))), kanji);
	}
}
