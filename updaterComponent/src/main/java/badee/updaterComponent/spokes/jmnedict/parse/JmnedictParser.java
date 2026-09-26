package badee.updaterComponent.spokes.jmnedict.parse;

import badee.updaterComponent.hub.XmlStreams;
import badee.updaterComponent.spokes.jmnedict.domain.NameEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Streaming (StAX) parser for the JMnedict XML file. Emits one
 * {@link NameEntry} per {@code <entry>} element via the supplied consumer so
 * the caller can batch and persist (JMnedict has ~740k entries).
 */
@Component
public class JmnedictParser {

	public void parse(Path xmlFile, Consumer<NameEntry> entryConsumer) throws IOException {
		// JMnedict inlines its DTD and uses entity refs (e.g. &surname;) for name types.
		try (InputStream in = Files.newInputStream(xmlFile)) {
			XMLStreamReader reader = XmlStreams.newInputFactory().createXMLStreamReader(in);
			try {
				while (reader.hasNext()) {
					if (reader.next() == XMLStreamConstants.START_ELEMENT
							&& "entry".equals(reader.getLocalName())) {
						entryConsumer.accept(readEntry(reader));
					}
				}
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException("Failed to parse JMnedict XML", e);
		}
	}

	private NameEntry readEntry(XMLStreamReader reader) throws XMLStreamException {
		NameEntry entry = null;
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				// ent_seq precedes all other children, so entry is set before they're read.
				switch (reader.getLocalName()) {
					case "ent_seq" -> entry = new NameEntry(Long.parseLong(reader.getElementText().trim()));
					case "keb" -> entry.getKanji().add(reader.getElementText());
					case "reb" -> entry.getReadings().add(reader.getElementText());
					case "name_type" -> {
						String type = reader.getElementText();
						if (!entry.getNameTypes().contains(type)) {
							entry.getNameTypes().add(type);
						}
					}
					case "trans_det" -> entry.getTranslations().add(reader.getElementText());
					default -> {
						// ignore other elements
					}
				}
			} else if (event == XMLStreamConstants.END_ELEMENT && "entry".equals(reader.getLocalName())) {
				return entry;
			}
		}
		throw new XMLStreamException("Reached end of stream inside <entry>");
	}
}
