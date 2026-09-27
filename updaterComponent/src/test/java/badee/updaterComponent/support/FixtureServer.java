package badee.updaterComponent.support;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

/**
 * Tiny in-process HTTP server standing in for the upstream mirrors (edrdg.org,
 * GitHub releases), so integration tests exercise the real fetch → gunzip →
 * hash → parse → persist path without network access. Each test publishes the
 * file contents it wants a spoke to download; a 404 is served for anything
 * not published.
 */
public final class FixtureServer {

	private final HttpServer server;
	private final Map<String, byte[]> files = new ConcurrentHashMap<>();
	private final Map<String, String> redirects = new ConcurrentHashMap<>();

	private FixtureServer(HttpServer server) {
		this.server = server;
	}

	public static FixtureServer start() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			FixtureServer fixtures = new FixtureServer(server);
			server.createContext("/", exchange -> {
				String path = exchange.getRequestURI().getPath();
				byte[] body = fixtures.files.get(path);
				String redirect = fixtures.redirects.get(path);
				if (redirect != null) {
					exchange.getResponseHeaders().add("Location", redirect);
					exchange.sendResponseHeaders(302, -1);
				} else if (body == null) {
					exchange.sendResponseHeaders(404, -1);
				} else {
					exchange.sendResponseHeaders(200, body.length);
					try (OutputStream out = exchange.getResponseBody()) {
						out.write(body);
					}
				}
				exchange.close();
			});
			server.start();
			return fixtures;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Absolute URL for a path on this server, e.g. {@code url("/JMdict_e.gz")}. */
	public String url(String path) {
		return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + path;
	}

	/** Publishes {@code content} gzipped as UTF-8, as the EDRDG/KanjiVG files are served. */
	public void publishGzipped(String path, String content) {
		publishGzipped(path, content, StandardCharsets.UTF_8);
	}

	public void publishGzipped(String path, String content, Charset charset) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
			gzip.write(content.getBytes(charset));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		files.put(path, bytes.toByteArray());
	}

	/** Answers {@code path} with a 302 to {@code location} (e.g. GitHub's releases/latest). */
	public void publishRedirect(String path, String location) {
		redirects.put(path, location);
	}

	public void clear() {
		files.clear();
		redirects.clear();
	}
}
