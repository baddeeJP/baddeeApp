package badee.updaterComponent.spokes.kanji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A kanji character. Populated jointly by the KanjiDic2 (core fields), KanjiVG
 * (stroke/SVG data), and RADKFILE (radical decomposition) sub-sources of the
 * Kanji spoke. Schema is a stub for now — fields grow as those sub-sources are
 * implemented.
 */
@Entity
@Table(name = "kanji")
public class Kanji {

	@Id
	@Column(name = "character")
	private String character;

	@Column(name = "stroke_count")
	private Integer strokeCount;

	@Column(name = "grade")
	private Integer grade;

	protected Kanji() {
	}

	public Kanji(String character) {
		this.character = character;
	}

	public String getCharacter() {
		return character;
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
}
