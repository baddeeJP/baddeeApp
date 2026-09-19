package badee.updaterComponent.spokes.jmdict.search;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Write-only Elasticsearch mirror of a JMdict entry, denormalized for the
 * separate search application to query. This app never reads from it.
 */
@Document(indexName = "vocab")
public class VocabDocument {

	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private List<String> kanji;

	@Field(type = FieldType.Keyword)
	private List<String> readings;

	@Field(type = FieldType.Text)
	private List<String> glosses;

	@Field(type = FieldType.Boolean)
	private boolean common;

	public VocabDocument() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getKanji() {
		return kanji;
	}

	public void setKanji(List<String> kanji) {
		this.kanji = kanji;
	}

	public List<String> getReadings() {
		return readings;
	}

	public void setReadings(List<String> readings) {
		this.readings = readings;
	}

	public List<String> getGlosses() {
		return glosses;
	}

	public void setGlosses(List<String> glosses) {
		this.glosses = glosses;
	}

	public boolean isCommon() {
		return common;
	}

	public void setCommon(boolean common) {
		this.common = common;
	}
}
