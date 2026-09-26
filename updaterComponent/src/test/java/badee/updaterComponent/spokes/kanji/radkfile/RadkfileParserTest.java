package badee.updaterComponent.spokes.kanji.radkfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RadkfileParserTest {

	private static final String FIXTURE = """
			# RADKFILE comment header
			#
			$ 一 1
			亜唖
			阿
			$ 化 2 js01
			化花
			""";

	@Test
	void parsesEucJpRadicalBlocksAcrossLines(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("radkfile");
		Files.writeString(file, FIXTURE, Charset.forName("EUC-JP"));

		List<RadkfileRadical> radicals = new ArrayList<>();
		new RadkfileParser().parse(file, radicals::add);

		assertEquals(List.of(
				new RadkfileRadical("一", 1, List.of("亜", "唖", "阿")),
				new RadkfileRadical("化", 2, List.of("化", "花"))), radicals);
	}
}
