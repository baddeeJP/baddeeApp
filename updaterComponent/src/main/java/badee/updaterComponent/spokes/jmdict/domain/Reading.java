package badee.updaterComponent.spokes.jmdict.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A reading of a {@link VocabEntry}. Covers both kanji writings ({@code <keb>},
 * kanji=true) and kana readings ({@code <reb>}, kanji=false) from JMdict.
 */
@Entity
@Table(name = "reading")
public class Reading {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "ent_seq")
	private VocabEntry vocabEntry;

	@Column(name = "text", nullable = false)
	private String text;

	/** True for a kanji writing (keb), false for a kana reading (reb). */
	@Column(name = "kanji")
	private boolean kanji;

	protected Reading() {
	}

	public Reading(String text, boolean kanji) {
		this.text = text;
		this.kanji = kanji;
	}

	public Long getId() {
		return id;
	}

	public VocabEntry getVocabEntry() {
		return vocabEntry;
	}

	void setVocabEntry(VocabEntry vocabEntry) {
		this.vocabEntry = vocabEntry;
	}

	public String getText() {
		return text;
	}

	public boolean isKanji() {
		return kanji;
	}
}
