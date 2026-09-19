package badee.updaterComponent.hub;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Tracks the last known content hash of an upstream feed, keyed by feedId
 * (e.g. "jmdict", "tatoeba", "kanji.kanjidic2"). Used to make spoke updates
 * idempotent: a feed is only reparsed when its hash changes.
 */
@Entity
@Table(name = "source_version")
public class SourceVersion {

	@Id
	@Column(name = "feed_id")
	private String feedId;

	@Column(name = "content_hash")
	private String contentHash;

	@Column(name = "last_checked_at")
	private Instant lastCheckedAt;

	@Column(name = "last_updated_at")
	private Instant lastUpdatedAt;

	protected SourceVersion() {
	}

	public SourceVersion(String feedId) {
		this.feedId = feedId;
	}

	public String getFeedId() {
		return feedId;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	public Instant getLastCheckedAt() {
		return lastCheckedAt;
	}

	public void setLastCheckedAt(Instant lastCheckedAt) {
		this.lastCheckedAt = lastCheckedAt;
	}

	public Instant getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public void setLastUpdatedAt(Instant lastUpdatedAt) {
		this.lastUpdatedAt = lastUpdatedAt;
	}
}
