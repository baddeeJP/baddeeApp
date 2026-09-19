package badee.updaterComponent.hub;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Small helper for computing content hashes of downloaded source files. */
public final class Hashing {

	private Hashing() {
	}

	/** Streams the file through SHA-256 and returns the digest as lowercase hex. */
	public static String sha256(Path file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream in = Files.newInputStream(file);
					DigestInputStream dis = new DigestInputStream(in, digest)) {
				byte[] buffer = new byte[8192];
				while (dis.read(buffer) != -1) {
					// reading advances the digest
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
