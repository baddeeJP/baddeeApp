package badee.updaterComponent.spokes.jmnedict.fetch;

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
 * Downloads the gzipped JMnedict file and decompresses it to a local staging
 * path for parsing.
 */
@Component
public class JmnedictFetcher {

	private final String downloadUrl;
	private final HttpClient httpClient = HttpClient.newHttpClient();

	public JmnedictFetcher(
			@Value("${updater.jmnedict.url:http://ftp.edrdg.org/pub/Nihongo/JMnedict.xml.gz}") String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	/** Downloads and decompresses JMnedict, returning the path to the XML file. */
	public Path fetch() throws IOException, InterruptedException {
		Path stagingDir = Files.createTempDirectory("jmnedict-");
		Path gzFile = stagingDir.resolve("JMnedict.xml.gz");
		Path xmlFile = stagingDir.resolve("JMnedict.xml");

		HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
		HttpResponse<Path> response = httpClient.send(request,
				HttpResponse.BodyHandlers.ofFile(gzFile));
		if (response.statusCode() != 200) {
			throw new IOException("Failed to download JMnedict, HTTP " + response.statusCode());
		}

		try (InputStream in = new GZIPInputStream(Files.newInputStream(gzFile))) {
			Files.copy(in, xmlFile, StandardCopyOption.REPLACE_EXISTING);
		}
		return xmlFile;
	}
}
