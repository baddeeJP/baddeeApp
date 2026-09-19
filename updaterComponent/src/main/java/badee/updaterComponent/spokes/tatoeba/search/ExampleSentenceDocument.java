package badee.updaterComponent.spokes.tatoeba.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Write-only Elasticsearch mirror of an example sentence, for the separate
 * search application to query. This app never reads from it.
 */
@Document(indexName = "example-sentence")
public class ExampleSentenceDocument {

	@Id
	private String id;

	@Field(type = FieldType.Text)
	private String japanese;

	@Field(type = FieldType.Text)
	private String english;

	@Field(type = FieldType.Keyword)
	private String indexWords;

	public ExampleSentenceDocument() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJapanese() {
		return japanese;
	}

	public void setJapanese(String japanese) {
		this.japanese = japanese;
	}

	public String getEnglish() {
		return english;
	}

	public void setEnglish(String english) {
		this.english = english;
	}

	public String getIndexWords() {
		return indexWords;
	}

	public void setIndexWords(String indexWords) {
		this.indexWords = indexWords;
	}
}
