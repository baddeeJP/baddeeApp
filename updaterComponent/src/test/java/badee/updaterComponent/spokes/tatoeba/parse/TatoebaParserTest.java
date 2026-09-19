package badee.updaterComponent.spokes.tatoeba.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentence;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TatoebaParserTest {

	// A-line: japanese <tab> english #ID=<jpn>_<eng>; B-line: index headwords.
	private static final String FIXTURE =
			"A: 私は日本語を話します。\tI speak Japanese.#ID=100_200\n"
			+ "B: 私 は 日本語 話す\n"
			+ "A: これはペンです。\tThis is a pen.#ID=101_201\n"
			+ "B: 此れ は ペン\n";

	@Test
	void parsesPairsAndKeysOnJapaneseId(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("examples.utf");
		Files.writeString(file, FIXTURE, StandardCharsets.UTF_8);

		List<ExampleSentence> sentences = new ArrayList<>();
		new TatoebaParser().parse(file, sentences::add);

		assertEquals(2, sentences.size());

		ExampleSentence first = sentences.get(0);
		assertEquals(100L, first.getId());
		assertEquals("私は日本語を話します。", first.getJapanese());
		assertEquals("I speak Japanese.", first.getEnglish());
		assertEquals("私 は 日本語 話す", first.getIndexWords());

		assertEquals(101L, sentences.get(1).getId());
		assertEquals("This is a pen.", sentences.get(1).getEnglish());
	}
}
