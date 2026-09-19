package badee.updaterComponent.spokes.jmdict.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * A single sense of a {@link VocabEntry} (the {@code <sense>} element): a set of
 * English glosses plus part-of-speech and misc tags.
 */
@Entity
@Table(name = "sense")
public class Sense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "ent_seq")
	private VocabEntry vocabEntry;

	@ElementCollection
	@CollectionTable(name = "sense_gloss", joinColumns = @JoinColumn(name = "sense_id"))
	@Column(name = "gloss")
	private List<String> glosses = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "sense_pos", joinColumns = @JoinColumn(name = "sense_id"))
	@Column(name = "pos")
	private List<String> partsOfSpeech = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "sense_misc", joinColumns = @JoinColumn(name = "sense_id"))
	@Column(name = "misc")
	private List<String> miscTags = new ArrayList<>();

	public Sense() {
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

	public List<String> getGlosses() {
		return glosses;
	}

	public List<String> getPartsOfSpeech() {
		return partsOfSpeech;
	}

	public List<String> getMiscTags() {
		return miscTags;
	}
}
