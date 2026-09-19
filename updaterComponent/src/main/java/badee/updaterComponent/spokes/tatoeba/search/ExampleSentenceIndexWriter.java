package badee.updaterComponent.spokes.tatoeba.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * Write-only facade over the {@code example-sentence} Elasticsearch index.
 * Exposes only saving; this app indexes data for a separate search application
 * and never queries Elasticsearch itself.
 */
@Component
public class ExampleSentenceIndexWriter {

	interface ExampleSentenceEsRepository
			extends ElasticsearchRepository<ExampleSentenceDocument, String> {
	}

	private final ExampleSentenceEsRepository repository;

	public ExampleSentenceIndexWriter(ExampleSentenceEsRepository repository) {
		this.repository = repository;
	}

	public void save(ExampleSentenceDocument document) {
		repository.save(document);
	}

	public void saveAll(Iterable<ExampleSentenceDocument> documents) {
		repository.saveAll(documents);
	}
}
