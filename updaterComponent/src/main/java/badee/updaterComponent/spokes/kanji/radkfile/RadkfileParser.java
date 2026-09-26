package badee.updaterComponent.spokes.kanji.radkfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Parser for RADKFILE (EUC-JP encoded). The format is a series of blocks:
 *
 * <pre>
 * # comment lines
 * $ 一 1              (radical, stroke count, optional JIS code / image name)
 * 亜唖阿姶悪芦...     (one or more lines of kanji containing the radical)
 * </pre>
 *
 * Emits one {@link RadkfileRadical} per {@code $} block.
 */
@Component
public class RadkfileParser {

	private static final Charset EUC_JP = Charset.forName("EUC-JP");

	public void parse(Path file, Consumer<RadkfileRadical> consumer) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, EUC_JP)) {
			String radical = null;
			int strokeCount = 0;
			List<String> kanji = new ArrayList<>();

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				if (line.startsWith("$")) {
					if (radical != null) {
						consumer.accept(new RadkfileRadical(radical, strokeCount, kanji));
					}
					String[] fields = line.trim().split("\\s+");
					if (fields.length < 3) {
						throw new IOException("Malformed RADKFILE radical line: " + line);
					}
					radical = fields[1];
					strokeCount = Integer.parseInt(fields[2]);
					kanji = new ArrayList<>();
				} else if (radical != null) {
					for (int cp : line.strip().codePoints().toArray()) {
						kanji.add(Character.toString(cp));
					}
				}
			}
			if (radical != null) {
				consumer.accept(new RadkfileRadical(radical, strokeCount, kanji));
			}
		}
	}
}
