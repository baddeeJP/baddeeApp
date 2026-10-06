package badee.updaterComponent.spokes.tatoeba.parse;

import badee.updaterComponent.spokes.tatoeba.domain.ExampleSentence;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Parser for the Tanaka Corpus example file. Each sentence is a pair of lines:
 *
 * <pre>
 * A: 日本語の文。	The English sentence.#ID=123456_789012
 * B: 単語(はんしゃ)[01]{表層形} ...
 * </pre>
 *
 * The A-line carries the Japanese text, the tab-separated English, and an
 * {@code #ID=<eng>_<jpn>} trailer (we key on the Japanese id). The following
 * B-line carries the space-separated index headwords. Emits one
 * {@link ExampleSentence} per pair so the caller can batch and persist.
 */
@Component
public class TatoebaParser {

	public static final String SOURCE = "tanaka-corpus";

	public void parse(Path textFile, Consumer<ExampleSentence> consumer) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(textFile, StandardCharsets.UTF_8)) {
			String line;
			String pendingA = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("A: ")) {
					pendingA = line.substring(3);
				} else if (line.startsWith("B: ") && pendingA != null) {
					ExampleSentence sentence = build(pendingA, line.substring(3).trim());
					if (sentence != null) {
						consumer.accept(sentence);
					}
					pendingA = null;
				}
			}
		}
	}

	private ExampleSentence build(String aLine, String indexWords) {
		int hashIdx = aLine.indexOf("#ID=");
		if (hashIdx < 0) {
			return null;
		}
		String pair = aLine.substring(0, hashIdx);
		String idPart = aLine.substring(hashIdx + 4); // after "#ID="

		int tab = pair.indexOf('\t');
		if (tab < 0) {
			return null;
		}
		String japanese = pair.substring(0, tab).trim();
		String english = pair.substring(tab + 1).trim();

		Long id = parseJapaneseId(idPart);
		if (id == null) {
			return null;
		}
		return new ExampleSentence(id, japanese, english, indexWords, SOURCE);
	}

	/**
	 * The id trailer is "<engId>_<jpnId>"; we key on the Japanese id. (In the
	 * real corpus the first id is shared by Japanese variants that have the same
	 * English translation, while the second is unique per Japanese sentence.)
	 */
	private Long parseJapaneseId(String idPart) {
		String[] ids = idPart.split("_", 2);
		if (ids.length < 2) {
			return null;
		}
		String jpnId = ids[1].trim();
		try {
			return Long.parseLong(jpnId);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
