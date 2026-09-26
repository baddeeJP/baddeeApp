package badee.updaterComponent.spokes.kanji.radkfile;

import java.util.List;

/** One RADKFILE radical block: the radical, its stroke count, and every kanji containing it. */
public record RadkfileRadical(String radical, int strokeCount, List<String> kanji) {
}
