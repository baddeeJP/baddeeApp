package badee.updaterComponent.spokes.kanji.kanjivg;

import badee.updaterComponent.spokes.kanji.fetch.KanjiFileFetcher;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the download URL of the combined KanjiVG XML file. KanjiVG release
 * assets are date-stamped ({@code kanjivg-20250816.xml.gz} in release
 * {@code r20250816}), so there is no stable URL; unless one is pinned via
 * {@code updater.kanjivg.url}, the latest release tag is looked up and the
 * asset URL is derived from it.
 *
 * <p>The tag comes from the redirect of GitHub's {@code releases/latest} web
 * page, not the REST API: unauthenticated API calls are limited to 60/hour per
 * IP, shared with everything behind the same address (e.g. a cloud NAT
 * gateway), which made the nightly run fail with HTTP 403. If KanjiVG ever
 * changes its asset naming, pin {@code updater.kanjivg.url} until this is updated.
 */
@Component
public class KanjiVGReleaseLocator {

	private static final Pattern RELEASE_TAG = Pattern.compile("/releases/tag/r(\\d{8})$");

	private final KanjiFileFetcher fetcher;
	private final String pinnedUrl;
	private final String releasesUrl;

	public KanjiVGReleaseLocator(KanjiFileFetcher fetcher,
			@Value("${updater.kanjivg.url:}") String pinnedUrl,
			@Value("${updater.kanjivg.releases-url:https://github.com/KanjiVG/kanjivg/releases}") String releasesUrl) {
		this.fetcher = fetcher;
		this.pinnedUrl = pinnedUrl;
		this.releasesUrl = releasesUrl;
	}

	public String downloadUrl() throws IOException, InterruptedException {
		if (!pinnedUrl.isBlank()) {
			return pinnedUrl;
		}
		String latestTagUrl = fetcher.redirectLocation(releasesUrl + "/latest");
		Matcher tag = RELEASE_TAG.matcher(latestTagUrl);
		if (!tag.find()) {
			throw new IOException("Unexpected KanjiVG latest-release redirect: " + latestTagUrl);
		}
		String date = tag.group(1);
		return releasesUrl + "/download/r" + date + "/kanjivg-" + date + ".xml.gz";
	}
}
