package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.persistence.TeammateUnique;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class UniqueSplitsTest
{
	private static final int CROSSBOWS = 34681;
	private static final int RONDACHE = 34719;
	private static final int BONES = 526;

	private static long price(int itemId)
	{
		return itemId == CROSSBOWS ? 410_000_000L : 100;
	}

	@Test
	public void aFiveWaySplitKeepsAFifthAfterTax()
	{
		// The crossbows from the 2026-10-29 raid: 410m less the 5m tax cap, between 5.
		Map<Integer, Integer> loot = new HashMap<>();
		loot.put(CROSSBOWS, 1);
		loot.put(BONES, 10);
		double raw = 410_000_000 + 1_000;

		double after = UniqueSplits.afterSplits(raw, loot, Collections.singletonMap(CROSSBOWS, 5),
			Collections.emptyList(), UniqueSplitsTest::price);

		assertEquals(81_000_000 + 1_000, after, 0.001);
	}

	@Test
	public void taxIsTwoPercentUpToFiveMillion()
	{
		assertEquals(98_000_000, UniqueSplits.afterTax(100_000_000));
		assertEquals(405_000_000, UniqueSplits.afterTax(410_000_000));
	}

	@Test
	public void freeForAllAndUnsetKeepEverythingUntaxed()
	{
		Map<Integer, Integer> loot = Collections.singletonMap(CROSSBOWS, 1);

		assertEquals(0, UniqueSplits.givenAway(loot, Collections.singletonMap(CROSSBOWS, UniqueSplits.FREE_FOR_ALL),
			UniqueSplitsTest::price), 0.001);
		assertEquals(0, UniqueSplits.givenAway(loot, Collections.emptyMap(), UniqueSplitsTest::price), 0.001);
	}

	@Test
	public void aSplitForAnItemNoLongerInTheLootIsIgnored()
	{
		assertEquals(0, UniqueSplits.givenAway(Collections.singletonMap(BONES, 3),
			Collections.singletonMap(CROSSBOWS, 5), UniqueSplitsTest::price), 0.001);
	}

	@Test
	public void byItemListsEverySplitItemInTheLoot()
	{
		Map<Integer, Integer> loot = new HashMap<>();
		loot.put(CROSSBOWS, 1);
		loot.put(BONES, 10);
		Map<Integer, Integer> splits = new HashMap<>();
		splits.put(CROSSBOWS, 5);
		splits.put(BONES, UniqueSplits.FREE_FOR_ALL);
		splits.put(RONDACHE, 3);

		Map<Integer, Double> byItem = UniqueSplits.givenAwayByItem(loot, splits, UniqueSplitsTest::price);

		assertEquals(2, byItem.size());
		assertEquals(410_000_000 - 81_000_000, byItem.get(CROSSBOWS), 0.001);
		assertEquals(0, byItem.get(BONES), 0.001);
	}

	@Test
	public void aTeammatesUniqueAddsTheShare()
	{
		TeammateUnique rondache = new TeammateUnique(RONDACHE, "Rondache", 4, 100_000_000L);

		double after = UniqueSplits.afterSplits(1_000, Collections.singletonMap(BONES, 10), Collections.emptyMap(),
			Collections.singletonList(rondache), UniqueSplitsTest::price);

		assertEquals(1_000 + 24_500_000, after, 0.001);
	}
}
