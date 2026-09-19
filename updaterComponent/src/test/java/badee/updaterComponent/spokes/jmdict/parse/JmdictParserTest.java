package badee.updaterComponent.spokes.jmdict.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import badee.updaterComponent.spokes.jmdict.domain.Reading;
import badee.updaterComponent.spokes.jmdict.domain.VocabEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JmdictParserTest {

	private static final String FIXTURE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<JMdict>
			<entry>
			<ent_seq>1000001</ent_seq>
			<k_ele><keb>日本語</keb><ke_pri>news1</ke_pri></k_ele>
			<r_ele><reb>にほんご</reb></r_ele>
			<sense>
			<pos>noun</pos>
			<gloss>Japanese (language)</gloss>
			</sense>
			</entry>
			<entry>
			<ent_seq>1000002</ent_seq>
			<r_ele><reb>あの</reb></r_ele>
			<sense>
			<gloss>that</gloss>
			<gloss>those</gloss>
			</sense>
			</entry>
			</JMdict>
			""";

	@Test
	void parsesEntriesReadingsSensesAndCommonFlag(@TempDir Path tempDir) throws IOException {
		Path xml = tempDir.resolve("JMdict_e.xml");
		Files.writeString(xml, FIXTURE, StandardCharsets.UTF_8);

		List<VocabEntry> entries = new ArrayList<>();
		new JmdictParser().parse(xml, entries::add);

		assertEquals(2, entries.size());

		VocabEntry first = entries.get(0);
		assertEquals(1000001L, first.getEntSeq());
		assertTrue(first.isCommon(), "news1 priority should flag entry as common");
		assertEquals("日本語", first.getReadings().stream()
				.filter(Reading::isKanji).findFirst().orElseThrow().getText());
		assertEquals("にほんご", first.getReadings().stream()
				.filter(r -> !r.isKanji()).findFirst().orElseThrow().getText());
		assertEquals(List.of("Japanese (language)"), first.getSenses().get(0).getGlosses());
		assertEquals(List.of("noun"), first.getSenses().get(0).getPartsOfSpeech());

		VocabEntry second = entries.get(1);
		assertEquals(1000002L, second.getEntSeq());
		assertTrue(!second.isCommon(), "no priority markers should leave entry uncommon");
		assertEquals(List.of("that", "those"), second.getSenses().get(0).getGlosses());
	}
}
