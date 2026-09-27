package badee.updaterComponent.spokes.jmdict.search;

import org.springframework.stereotype.Component;

/**
 * Write-only facade over the {@code vocab} Elasticsearch index. Exposes only
 * saving, matching this app's role: it indexes data for a separate search
 * application and never queries Elasticsearch itself.
 */
@Component
public class VocabIndexWriter {

	private final VocabEsRepository repository;

	public VocabIndexWriter(VocabEsRepository repository) {
		this.repository = repository;
	}

	public void save(VocabDocument document) {
		repository.save(document);
	}

	public void saveAll(Iterable<VocabDocument> documents) {
		repository.saveAll(documents);
	}
}
