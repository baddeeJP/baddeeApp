package badee.updaterComponent.spokes.jmdict.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository behind {@link VocabIndexWriter}. Package-private so the
 * rest of the app can only reach the index through the write-only facade.
 * (Top-level rather than nested: Spring Data skips nested repository interfaces.)
 */
interface VocabEsRepository extends ElasticsearchRepository<VocabDocument, String> {
}
