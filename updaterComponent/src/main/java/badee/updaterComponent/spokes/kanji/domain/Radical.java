package badee.updaterComponent.spokes.kanji.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A radical (kanji component). Populated primarily from RADKFILE. Schema is a
 * stub for now — fields grow as the sub-source is implemented.
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

	public Radical(String character) {
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
}
