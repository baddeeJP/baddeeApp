package badee.updaterComponent.spokes.kanji.kanjivg;

import java.util.List;

/** One parsed KanjiVG character: its SVG stroke paths in stroke order. */
public record KanjiStrokes(String character, List<String> strokePaths) {
}
