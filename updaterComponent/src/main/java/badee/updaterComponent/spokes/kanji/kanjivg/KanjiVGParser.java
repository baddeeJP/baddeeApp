package badee.updaterComponent.spokes.kanji.kanjivg;

import badee.updaterComponent.hub.XmlStreams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Streaming (StAX) parser for the combined KanjiVG XML file. Each
 * {@code <kanji id="kvg:kanji_04e00">} element holds nested {@code <g>} groups
 * whose {@code <path d="...">} elements appear in stroke order; the character
 * is decoded from the hex code point in the id. Variant glyphs (ids with a
 * suffix such as {@code -Kaisho}) are skipped.
 */
@Component
public class KanjiVGParser {

	private static final Pattern KANJI_ID = Pattern.compile("kvg:kanji_([0-9a-fA-F]+)");

	public void parse(Path xmlFile, Consumer<KanjiStrokes> consumer) throws IOException {
		try (InputStream in = Files.newInputStream(xmlFile)) {
			XMLStreamReader reader = XmlStreams.newInputFactory().createXMLStreamReader(in);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT
							&& "kanji".equals(reader.getLocalName())) {
						KanjiStrokes strokes = readKanji(reader);
						if (strokes != null) {
							consumer.accept(strokes);
						}
					}
				}
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException("Failed to parse KanjiVG XML", e);
		}
	}

	/** Reads one {@code <kanji>} element; returns null for variants / unrecognised ids. */
	private KanjiStrokes readKanji(XMLStreamReader reader) throws XMLStreamException {
		Matcher id = KANJI_ID.matcher(String.valueOf(reader.getAttributeValue(null, "id")));
		String character = id.matches()
				? Character.toString(Integer.parseInt(id.group(1), 16))
				: null;
		List<String> paths = new ArrayList<>();

		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT && "path".equals(reader.getLocalName())) {
				String d = reader.getAttributeValue(null, "d");
				if (d != null) {
					paths.add(d);
				}
			} else if (event == XMLStreamConstants.END_ELEMENT && "kanji".equals(reader.getLocalName())) {
				return character != null ? new KanjiStrokes(character, paths) : null;
			}
		}
		throw new XMLStreamException("Reached end of stream inside <kanji>");
	}
}
