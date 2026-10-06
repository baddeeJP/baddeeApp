package badee.updaterComponent.spokes.jmdict.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A JMdict dictionary entry (the {@code <entry>} element), keyed by the JMdict
 * sequence id (ent_seq). Owns its readings and senses. Postgres source of truth.
 */
@Entity
@Table(name = "vocab_entry")
public class VocabEntry {

	@Id
	@Column(name = "ent_seq")
	private Long entSeq;

	@Column(name = "common")
	private boolean common;

	/** Set when the entry disappeared from upstream; null while active. Cleared if it comes back. */
	@Column(name = "retired_at")
	private Instant retiredAt;

	@OneToMany(mappedBy = "vocabEntry", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Reading> readings = new ArrayList<>();

	@OneToMany(mappedBy = "vocabEntry", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Sense> senses = new ArrayList<>();

	protected VocabEntry() {
	}

	public VocabEntry(Long entSeq) {
		this.entSeq = entSeq;
	}

	public Long getEntSeq() {
		return entSeq;
	}

	public boolean isCommon() {
		return common;
	}

	public void setCommon(boolean common) {
		this.common = common;
	}

	public List<Reading> getReadings() {
		return readings;
	}

	public List<Sense> getSenses() {
		return senses;
	}

	public void addReading(Reading reading) {
		reading.setVocabEntry(this);
		this.readings.add(reading);
	}

	public void addSense(Sense sense) {
		sense.setVocabEntry(this);
		this.senses.add(sense);
	}
}
