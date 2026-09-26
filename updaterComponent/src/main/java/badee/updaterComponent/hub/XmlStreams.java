package badee.updaterComponent.hub;

import javax.xml.stream.XMLInputFactory;

/** Shared StAX configuration for the EDRDG / KanjiVG XML sources. */
public final class XmlStreams {

	private XmlStreams() {
	}

	/**
	 * Returns a StAX factory that expands the internal-DTD entity references the
	 * EDRDG files use (e.g. {@code &n;}, {@code &surname;}) without the JDK's
	 * entity limits. Recent JDKs cap a document at 2,500 expansions / 100 KB of
	 * expanded text, which the real JMdict and JMnedict files exceed by orders of
	 * magnitude. External entities stay disabled, so only the file's own internal
	 * DTD is ever expanded.
	 */
	public static XMLInputFactory newInputFactory() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		factory.setProperty("jdk.xml.entityExpansionLimit", "0");
		factory.setProperty("jdk.xml.totalEntitySizeLimit", "0");
		return factory;
	}
}
