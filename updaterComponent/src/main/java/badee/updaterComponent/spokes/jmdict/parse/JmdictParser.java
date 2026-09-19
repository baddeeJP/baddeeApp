package badee.updaterComponent.spokes.jmdict.parse;

import badee.updaterComponent.spokes.jmdict.domain.Reading;
import badee.updaterComponent.spokes.jmdict.domain.Sense;
import badee.updaterComponent.spokes.jmdict.domain.VocabEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;

/**
 * Streaming (StAX) parser for the JMdict XML file. Emits one {@link VocabEntry}
 * per {@code <entry>} element via the supplied consumer so the caller can batch
 * and persist without holding the whole file in memory (JMdict is large).
 */
@Component
public class JmdictParser {

	/** Priority markers that flag an entry as "common". */
	private static final Set<String> COMMON_MARKERS = Set.of(
			"news1", "ichi1", "spec1", "spec2", "gai1");

	public void parse(Path xmlFile, Consumer<VocabEntry> entryConsumer) throws IOException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		// JMdict inlines its DTD and uses entity refs (e.g. &n;) for pos/misc.
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

		try (InputStream in = Files.newInputStream(xmlFile)) {
			XMLStreamReader reader = factory.createXMLStreamReader(in);
			try {
				readEntries(reader, entryConsumer);
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException("Failed to parse JMdict XML", e);
		}
	}

	private void readEntries(XMLStreamReader reader, Consumer<VocabEntry> entryConsumer)
			throws XMLStreamException {
		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT && "entry".equals(reader.getLocalName())) {
				entryConsumer.accept(readEntry(reader));
			}
		}
	}

	private VocabEntry readEntry(XMLStreamReader reader) throws XMLStreamException {
		VocabEntry entry = null;
		boolean common = false;

		// Buffers for the current k_ele / r_ele / sense being read.
		Reading pendingReading = null;
		Sense pendingSense = null;

		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				switch (reader.getLocalName()) {
					case "ent_seq" -> entry = new VocabEntry(Long.parseLong(reader.getElementText().trim()));
					case "keb" -> pendingReading = new Reading(reader.getElementText(), true);
					case "reb" -> pendingReading = new Reading(reader.getElementText(), false);
					case "ke_pri", "re_pri" -> {
						if (COMMON_MARKERS.contains(reader.getElementText().trim())) {
							common = true;
						}
					}
					case "sense" -> pendingSense = new Sense();
					case "pos" -> {
						if (pendingSense != null) {
							pendingSense.getPartsOfSpeech().add(reader.getElementText());
						}
					}
					case "misc" -> {
						if (pendingSense != null) {
							pendingSense.getMiscTags().add(reader.getElementText());
						}
					}
					case "gloss" -> {
						if (pendingSense != null) {
							pendingSense.getGlosses().add(reader.getElementText());
						}
					}
					default -> {
						// ignore other elements
					}
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				switch (reader.getLocalName()) {
					case "k_ele", "r_ele" -> {
						if (entry != null && pendingReading != null) {
							entry.addReading(pendingReading);
						}
						pendingReading = null;
					}
					case "sense" -> {
						if (entry != null && pendingSense != null) {
							entry.addSense(pendingSense);
						}
						pendingSense = null;
					}
					case "entry" -> {
						if (entry != null) {
							entry.setCommon(common);
						}
						return entry;
					}
					default -> {
						// ignore
					}
				}
			}
		}
		throw new XMLStreamException("Reached end of stream inside <entry>");
	}
}
