package badee.updaterComponent.spokes.tatoeba.fetch;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Downloads the gzipped Tanaka Corpus example-sentence file (examples.utf.gz)
 * and decompresses it to a local staging path for parsing.
 */
@Component
public class TatoebaFetcher {

	private final String downloadUrl;
	private final HttpClient httpClient = HttpClient.newHttpClient();

	public TatoebaFetcher(
			@Value("${updater.tatoeba.url:http://ftp.edrdg.org/pub/Nihongo/examples.utf.gz}") String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	/** Downloads and decompresses the corpus, returning the path to the text file. */
	public Path fetch() throws IOException, InterruptedException {
		Path stagingDir = Files.createTempDirectory("tatoeba-");
		Path gzFile = stagingDir.resolve("examples.utf.gz");
		Path textFile = stagingDir.resolve("examples.utf");

		HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
		HttpResponse<Path> response = httpClient.send(request,
				HttpResponse.BodyHandlers.ofFile(gzFile));
		if (response.statusCode() != 200) {
			throw new IOException("Failed to download Tatoeba corpus, HTTP " + response.statusCode());
		}

		try (InputStream in = new GZIPInputStream(Files.newInputStream(gzFile))) {
			Files.copy(in, textFile, StandardCopyOption.REPLACE_EXISTING);
		}
		return textFile;
	}
}
