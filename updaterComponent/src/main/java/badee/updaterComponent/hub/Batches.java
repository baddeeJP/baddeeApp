package badee.updaterComponent.hub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Splits a key collection into bounded batches (Postgres caps a statement at 65,535 bind parameters). */
public final class Batches {

	private Batches() {
	}

	public static <T> List<List<T>> of(Collection<T> items, int size) {
		List<T> all = new ArrayList<>(items);
		List<List<T>> batches = new ArrayList<>();
		for (int i = 0; i < all.size(); i += size) {
			batches.add(all.subList(i, Math.min(i + size, all.size())));
		}
		return batches;
	}
}
