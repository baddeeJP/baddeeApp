package badee.updaterComponent.spokes.kanji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A radical (kanji component) from RADKFILE, keyed by its glyph. Some RADKFILE
 * radicals have no Unicode form of their own and are represented by a
 * stand-in kanji (e.g. 化 for the 亻 variant), exactly as RADKFILE lists them.
 * Which kanji contain it is recorded on {@link Kanji#getRadicals()}.
 */
@Entity
@Table(name = "radical")
public class Radical {

	@Id
	@Column(name = "character")
	private String character;

	@Column(name = "stroke_count")
	private Integer strokeCount;

	protected Radical() {
	}

	public Radical(String character, Integer strokeCount) {
		this.character = character;
		this.strokeCount = strokeCount;
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
}
