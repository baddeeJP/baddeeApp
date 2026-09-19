package badee.updaterComponent.spokes.kanji;

/**
 * One upstream source feeding the combined Kanji spoke (KanjiDic2, KanjiVG,
 * RADKFILE). Each owns its own fetch/parse/persist logic and its own feed id
 * ("kanji.kanjidic2" / "kanji.kanjivg" / "kanji.radkfile") for independent
 * change-tracking, while {@link KanjiModule} orchestrates all three.
 */
public interface KanjiSubSource {

	String feedId();

	void sync();
}
