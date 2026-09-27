package badee.updaterComponent.spokes.kanji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A kanji character, populated jointly by the Kanji spoke's sub-sources:
 * KanjiDic2 (readings, meanings, stroke count, grade, JLPT, frequency),
 * RADKFILE (radical decomposition), and KanjiVG (ordered stroke SVG paths).
 *
 * <p>Each sub-source only ever writes its own fields onto an existing row (or
 * creates a bare row if it runs first), so they can update independently
 * without clobbering each other. String lists are Postgres {@code text[]}
 * columns so each kanji stays a single row.
 */
@Entity
@Table(name = "kanji")
public class Kanji {

	@Id
	@Column(name = "character")
	private String character;

	// --- KanjiDic2 ---

	/** Unicode code point as lowercase hex, e.g. "4e9c". */
	@Column(name = "codepoint")
	private String codepoint;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "onyomi", columnDefinition = "text[]")
	private List<String> onyomi = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "kunyomi", columnDefinition = "text[]")
	private List<String> kunyomi = new ArrayList<>();

	/** Readings used only in names. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "nanori", columnDefinition = "text[]")
	private List<String> nanori = new ArrayList<>();

	/** English meanings. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "meanings", columnDefinition = "text[]")
	private List<String> meanings = new ArrayList<>();

	@Column(name = "stroke_count")
	private Integer strokeCount;

	/** KanjiDic2 grade: 1-6 kyouiku, 8 remaining jouyou, 9-10 jinmeiyou. */
	@Column(name = "grade")
	private Integer grade;

	@Column(name = "joyo")
	private boolean joyo;

	/** Pre-2010 JLPT level (1-4) as recorded by KanjiDic2. */
	@Column(name = "jlpt")
	private Integer jlpt;

	/** Newspaper frequency rank (1 = most frequent) for the top ~2500 kanji. */
	@Column(name = "frequency")
	private Integer frequency;

	/** Classical (Kangxi) radical number, 1-214. */
	@Column(name = "classical_radical")
	private Integer classicalRadical;

	// --- RADKFILE ---

	@ManyToMany
	@JoinTable(name = "kanji_radical",
			joinColumns = @JoinColumn(name = "kanji"),
			inverseJoinColumns = @JoinColumn(name = "radical"))
	private Set<Radical> radicals = new HashSet<>();

	// --- KanjiVG ---

	/** SVG path data ({@code d} attribute) for each stroke, in stroke order. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "stroke_paths", columnDefinition = "text[]")
	private List<String> strokePaths = new ArrayList<>();

	protected Kanji() {
	}

	public Kanji(String character) {
		this.character = character;
	}

	public String getCharacter() {
		return character;
	}

	public String getCodepoint() {
		return codepoint;
	}

	public void setCodepoint(String codepoint) {
		this.codepoint = codepoint;
	}

	public List<String> getOnyomi() {
		return onyomi;
	}

	public void setOnyomi(List<String> onyomi) {
		this.onyomi = new ArrayList<>(onyomi);
	}

	public List<String> getKunyomi() {
		return kunyomi;
	}

	public void setKunyomi(List<String> kunyomi) {
		this.kunyomi = new ArrayList<>(kunyomi);
	}

	public List<String> getNanori() {
		return nanori;
	}

	public void setNanori(List<String> nanori) {
		this.nanori = new ArrayList<>(nanori);
	}

	public List<String> getMeanings() {
		return meanings;
	}

	public void setMeanings(List<String> meanings) {
		this.meanings = new ArrayList<>(meanings);
	}

	public Integer getStrokeCount() {
		return strokeCount;
	}

	public void setStrokeCount(Integer strokeCount) {
		this.strokeCount = strokeCount;
	}

	public Integer getGrade() {
		return grade;
	}

	public void setGrade(Integer grade) {
		this.grade = grade;
	}

	public boolean isJoyo() {
		return joyo;
	}

	public void setJoyo(boolean joyo) {
		this.joyo = joyo;
	}

	public Integer getJlpt() {
		return jlpt;
	}

	public void setJlpt(Integer jlpt) {
		this.jlpt = jlpt;
	}

	public Integer getFrequency() {
		return frequency;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}

	public Integer getClassicalRadical() {
		return classicalRadical;
	}

	public void setClassicalRadical(Integer classicalRadical) {
		this.classicalRadical = classicalRadical;
	}

	public Set<Radical> getRadicals() {
		return radicals;
	}

	/** Replaces this kanji's radical decomposition in place. */
	public void replaceRadicals(Collection<Radical> newRadicals) {
		radicals.clear();
		radicals.addAll(newRadicals);
	}

	public List<String> getStrokePaths() {
		return strokePaths;
	}

	public void setStrokePaths(List<String> strokePaths) {
		this.strokePaths = new ArrayList<>(strokePaths);
	}
}
