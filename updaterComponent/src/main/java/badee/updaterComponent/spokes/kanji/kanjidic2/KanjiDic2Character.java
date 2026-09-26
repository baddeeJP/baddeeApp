package badee.updaterComponent.spokes.kanji.kanjidic2;

import java.util.List;

/**
 * One parsed KANJIDIC2 {@code <character>} element. Kept separate from the
 * {@code Kanji} entity because KanjiDic2 only owns a subset of its fields.
 */
public record KanjiDic2Character(
		String literal,
		String codepoint,
		Integer classicalRadical,
		Integer grade,
		Integer strokeCount,
		Integer frequency,
		Integer jlpt,
		List<String> onyomi,
		List<String> kunyomi,
		List<String> nanori,
		List<String> meanings) {

	/** Grades 1-6 (kyouiku) and 8 (secondary school) together form the jouyou list. */
	public boolean joyo() {
		return grade != null && grade <= 8;
	}
}
