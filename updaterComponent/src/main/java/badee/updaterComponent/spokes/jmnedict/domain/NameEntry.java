package badee.updaterComponent.spokes.jmnedict.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A proper-name entry from JMnedict (person / place / organization). Schema is
 * a stub for now — fields grow as the spoke is implemented.
 */
@Entity
@Table(name = "name_entry")
public class NameEntry {

	@Id
	@Column(name = "ent_seq")
	private Long entSeq;

	@Column(name = "kanji")
	private String kanji;

	@Column(name = "reading")
	private String reading;

	@Column(name = "type")
	private String type;

	protected NameEntry() {
	}

	public NameEntry(Long entSeq) {
		this.entSeq = entSeq;
	}

	public Long getEntSeq() {
		return entSeq;
	}

	public String getKanji() {
		return kanji;
	}

	public void setKanji(String kanji) {
		this.kanji = kanji;
	}

	public String getReading() {
		return reading;
	}

	public void setReading(String reading) {
		this.reading = reading;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
