package badee.updaterComponent.spokes.kanji.fetch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import org.springframework.stereotype.Component;

/**
 * Shared downloader for the Kanji spoke's sub-sources, all of which publish a
 * single gzipped file (kanjidic2.xml.gz, radkfile.gz, kanjivg-*.xml.gz).
 */
@Component
public class KanjiFileFetcher {

	private final HttpClient httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/**
	 * Downloads {@code url} and decompresses it into a fresh staging directory,
	 * returning the path to the decompressed file named {@code fileName}.
	 */
	public Path fetch(String url, String fileName) throws IOException, InterruptedException {
		Path stagingDir = Files.createTempDirectory("kanji-");
		Path gzFile = stagingDir.resolve(fileName + ".gz");
		Path file = stagingDir.resolve(fileName);

		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		HttpResponse<Path> response = httpClient.send(request,
				HttpResponse.BodyHandlers.ofFile(gzFile));
		if (response.statusCode() != 200) {
			throw new IOException("Failed to download " + url + ", HTTP " + response.statusCode());
		}

		try (InputStream in = new GZIPInputStream(Files.newInputStream(gzFile))) {
			Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
		}
		return file;
	}

	/** Performs a plain GET and returns the body as a string (small metadata lookups). */
	public String fetchText(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Accept", "application/json")
				.GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("Failed to fetch " + url + ", HTTP " + response.statusCode());
		}
		return response.body();
	}
}
