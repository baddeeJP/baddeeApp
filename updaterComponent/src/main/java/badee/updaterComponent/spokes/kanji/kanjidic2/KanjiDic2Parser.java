package badee.updaterComponent.spokes.kanji.kanjidic2;

import badee.updaterComponent.hub.XmlStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Streaming (StAX) parser for KANJIDIC2. Emits one {@link KanjiDic2Character}
 * per {@code <character>} element via the supplied consumer.
 */
@Component
public class KanjiDic2Parser {

	public void parse(Path xmlFile, Consumer<KanjiDic2Character> consumer) throws IOException {
		try (InputStream in = Files.newInputStream(xmlFile)) {
			XMLStreamReader reader = XmlStreams.newInputFactory().createXMLStreamReader(in);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT
							&& "character".equals(reader.getLocalName())) {
						consumer.accept(readCharacter(reader));
					}
				}
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException("Failed to parse KANJIDIC2 XML", e);
		}
	}

	private KanjiDic2Character readCharacter(XMLStreamReader reader) throws XMLStreamException {
		String literal = null;
		String codepoint = null;
		Integer classicalRadical = null;
		Integer grade = null;
		Integer strokeCount = null;
		Integer frequency = null;
		Integer jlpt = null;
		List<String> onyomi = new ArrayList<>();
		List<String> kunyomi = new ArrayList<>();
		List<String> nanori = new ArrayList<>();
		List<String> meanings = new ArrayList<>();

		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				switch (reader.getLocalName()) {
					case "literal" -> literal = reader.getElementText().trim();
					case "cp_value" -> {
						boolean ucs = "ucs".equals(reader.getAttributeValue(null, "cp_type"));
						String value = reader.getElementText().trim();
						if (ucs) {
							codepoint = value;
						}
					}
					case "rad_value" -> {
						boolean classical = "classical".equals(reader.getAttributeValue(null, "rad_type"));
						String value = reader.getElementText().trim();
						if (classical) {
							classicalRadical = Integer.valueOf(value);
						}
					}
					case "grade" -> grade = Integer.valueOf(reader.getElementText().trim());
					case "stroke_count" -> {
						// Additional stroke_count elements are common miscounts; the first is correct.
						String value = reader.getElementText().trim();
						if (strokeCount == null) {
							strokeCount = Integer.valueOf(value);
						}
					}
					case "freq" -> frequency = Integer.valueOf(reader.getElementText().trim());
					case "jlpt" -> jlpt = Integer.valueOf(reader.getElementText().trim());
					case "reading" -> {
						String type = reader.getAttributeValue(null, "r_type");
						String value = reader.getElementText().trim();
						if ("ja_on".equals(type)) {
							onyomi.add(value);
						} else if ("ja_kun".equals(type)) {
							kunyomi.add(value);
						}
					}
					case "meaning" -> {
						// Meanings without m_lang are English; skip the other languages.
						boolean english = reader.getAttributeValue(null, "m_lang") == null;
						String value = reader.getElementText().trim();
						if (english) {
							meanings.add(value);
						}
					}
					case "nanori" -> nanori.add(reader.getElementText().trim());
					default -> {
						// ignore other elements
					}
				}
			} else if (event == XMLStreamConstants.END_ELEMENT && "character".equals(reader.getLocalName())) {
				return new KanjiDic2Character(literal, codepoint, classicalRadical, grade, strokeCount,
						frequency, jlpt, onyomi, kunyomi, nanori, meanings);
			}
		}
		throw new XMLStreamException("Reached end of stream inside <character>");
	}
}
