package badee.updaterComponent.spokes.kanji.kanjivg;

import badee.updaterComponent.spokes.kanji.fetch.KanjiFileFetcher;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the download URL of the combined KanjiVG XML file. KanjiVG release
 * assets are date-stamped ({@code kanjivg-20250816.xml.gz}), so there is no
 * stable URL; unless one is pinned via {@code updater.kanjivg.url}, the latest
 * GitHub release is looked up and its {@code .xml.gz} asset is used.
 */
@Component
public class KanjiVGReleaseLocator {

	private static final Pattern XML_ASSET = Pattern.compile(
			"\"browser_download_url\"\\s*:\\s*\"([^\"]*/kanjivg-\\d+\\.xml\\.gz)\"");

	private final KanjiFileFetcher fetcher;
	private final String pinnedUrl;
	private final String latestReleaseApiUrl;

	public KanjiVGReleaseLocator(KanjiFileFetcher fetcher,
			@Value("${updater.kanjivg.url:}") String pinnedUrl,
			@Value("${updater.kanjivg.latest-release-api:https://api.github.com/repos/KanjiVG/kanjivg/releases/latest}") String latestReleaseApiUrl) {
		this.fetcher = fetcher;
		this.pinnedUrl = pinnedUrl;
		this.latestReleaseApiUrl = latestReleaseApiUrl;
	}

	public String downloadUrl() throws IOException, InterruptedException {
		if (!pinnedUrl.isBlank()) {
			return pinnedUrl;
		}
		String releaseJson = fetcher.fetchText(latestReleaseApiUrl);
		Matcher matcher = XML_ASSET.matcher(releaseJson);
		if (!matcher.find()) {
			throw new IOException("No kanjivg-*.xml.gz asset in latest KanjiVG release");
		}
		return matcher.group(1);
	}
}
