package badee.updaterComponent.spokes.jmnedict.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A proper-name entry from JMnedict (person / place / organization / ...),
 * keyed by the JMnedict sequence id. String lists are Postgres {@code text[]}
 * columns so each of the ~740k entries is a single row. Postgres only; not
 * indexed for search.
 */
@Entity
@Table(name = "name_entry")
public class NameEntry {

	@Id
	@Column(name = "ent_seq")
	private Long entSeq;

	/** Kanji writings ({@code <keb>}). Empty for kana-only names. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "kanji", columnDefinition = "text[]")
	private List<String> kanji = new ArrayList<>();

	/** Kana readings ({@code <reb>}). */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "readings", columnDefinition = "text[]")
	private List<String> readings = new ArrayList<>();

	/**
	 * Name types ({@code <name_type>}), stored as JMnedict's expanded entity
	 * descriptions, e.g. "family or surname", "place name", "company name".
	 */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "name_types", columnDefinition = "text[]")
	private List<String> nameTypes = new ArrayList<>();

	/** Romanizations / translations ({@code <trans_det>}), e.g. "Koizumi". */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "translations", columnDefinition = "text[]")
	private List<String> translations = new ArrayList<>();

	protected NameEntry() {
	}

	public NameEntry(Long entSeq) {
		this.entSeq = entSeq;
	}

	public Long getEntSeq() {
		return entSeq;
	}

	public List<String> getKanji() {
		return kanji;
	}

	public List<String> getReadings() {
		return readings;
	}

	public List<String> getNameTypes() {
		return nameTypes;
	}

	public List<String> getTranslations() {
		return translations;
	}
}
