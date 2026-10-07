package badee.updaterComponent.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RetirementGuardTest {

	private final List<Long> hundredActive = LongStream.rangeClosed(1, 100).boxed().toList();

	@Test
	void retiresActiveKeysMissingFromTheNewFile() {
		RetirementGuard guard = new RetirementGuard(
				new MockEnvironment().withProperty("updater.jmdict.retire-guard", "0"));

		assertEquals(Set.of(2L), guard.keysToRetire("jmdict", List.of(1L, 2L, 3L), Set.of(1L, 3L, 4L)));
	}

	@Test
	void defaultGuardAllowsRetiringUpToTwoPercent() {
		RetirementGuard guard = new RetirementGuard(new MockEnvironment());

		assertEquals(Set.of(1L, 2L), guard.keysToRetire("jmdict", hundredActive, seenExcept(1L, 2L)));
	}

	@Test
	void defaultGuardBlocksRetiringMoreThanTwoPercent() {
		RetirementGuard guard = new RetirementGuard(new MockEnvironment());

		assertEquals(Set.of(), guard.keysToRetire("jmdict", hundredActive, seenExcept(1L, 2L, 3L)));
	}

	@Test
	void guardIsConfigurablePerFeed() {
		RetirementGuard guard = new RetirementGuard(
				new MockEnvironment().withProperty("updater.jmdict.retire-guard", "0.9"));

		assertEquals(10, guard.keysToRetire("jmdict", hundredActive,
				seenExcept(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)).size());
		assertEquals(Set.of(), guard.keysToRetire("tatoeba", hundredActive, seenExcept(1L, 2L, 3L)),
				"other feeds keep the default");
	}

	@Test
	void nothingActiveMeansNothingToRetire() {
		RetirementGuard guard = new RetirementGuard(new MockEnvironment());

		assertEquals(Set.of(), guard.keysToRetire("jmdict", List.<Long>of(), Set.of(1L)));
	}

	private Set<Long> seenExcept(Long... missing) {
		Set<Long> seen = new HashSet<>(hundredActive);
		List.of(missing).forEach(seen::remove);
		return seen;
	}
}
