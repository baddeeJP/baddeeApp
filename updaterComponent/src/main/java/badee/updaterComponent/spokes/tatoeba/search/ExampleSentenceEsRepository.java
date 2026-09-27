package badee.updaterComponent.spokes.tatoeba.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository behind {@link ExampleSentenceIndexWriter}. Package-private
 * so the rest of the app can only reach the index through the write-only facade.
 * (Top-level rather than nested: Spring Data skips nested repository interfaces.)
 */
interface ExampleSentenceEsRepository extends ElasticsearchRepository<ExampleSentenceDocument, String> {
}
