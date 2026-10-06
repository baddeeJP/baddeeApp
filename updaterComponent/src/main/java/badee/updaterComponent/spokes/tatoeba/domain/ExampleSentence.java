package badee.updaterComponent.spokes.tatoeba.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A Japanese/English example sentence pair from the Tanaka Corpus (Tatoeba),
 * keyed by the corpus sentence id. The vocab it illustrates is referenced
 * loosely by the surface headword string (indexWords), not a hard FK, to keep
 * spokes decoupled.
 *
 * <p>Long strings are plain Postgres {@code text}, not {@code @Lob}: Hibernate
 * maps a {@code @Lob String} to an {@code oid} large object on Postgres, which
 * other apps can't read as text and which leaks a large object on every reparse.
 */
@Entity
@Table(name = "example_sentence")
public class ExampleSentence {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "japanese", nullable = false, columnDefinition = "text")
	private String japanese;

	@Column(name = "english", columnDefinition = "text")
	private String english;

	/** Space-separated headwords the sentence indexes against (from the B-line). */
	@Column(name = "index_words", columnDefinition = "text")
	private String indexWords;

	@Column(name = "source")
	private String source;

	/** Set when the entry disappeared from upstream; null while active. Cleared if it comes back. */
	@Column(name = "retired_at")
	private Instant retiredAt;

	protected ExampleSentence() {
	}

	public ExampleSentence(Long id, String japanese, String english, String indexWords, String source) {
		this.id = id;
		this.japanese = japanese;
		this.english = english;
		this.indexWords = indexWords;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public String getJapanese() {
		return japanese;
	}

	public String getEnglish() {
		return english;
	}

	public String getIndexWords() {
		return indexWords;
	}

	public String getSource() {
		return source;
	}
}
