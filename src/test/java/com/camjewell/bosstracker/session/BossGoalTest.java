package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossGoalTest
{
	private static final int SHARD = 1;
	private static final int BONES = 2;

	private static long price(int itemId)
	{
		return itemId == SHARD ? 1_000_000 : 1_000;
	}

	private static Map<Integer, Integer> loot(int shards, int bones)
	{
		Map<Integer, Integer> loot = new HashMap<>();
		loot.put(SHARD, shards);
		loot.put(BONES, bones);
		return loot;
	}

	@Test
	public void allTimeCountsEveryTrackedDrop()
	{
		BossGoal goal = new BossGoal(Boss.PHANTOM_MUSPAH);

		assertFalse(goal.isLootGoalFromNow());
		assertEquals(3_010_000, goal.lootProgressGp(loot(3, 10), BossGoalTest::price));
	}

	@Test
	public void fromNowCountsOnlyWhatDroppedAfterTheBaseline()
	{
		BossGoal goal = new BossGoal(Boss.PHANTOM_MUSPAH);
		goal.setLootGoalBaseline(loot(3, 10));

		assertTrue(goal.isLootGoalFromNow());
		assertEquals(0, goal.lootProgressGp(loot(3, 10), BossGoalTest::price));
		assertEquals(1_005_000, goal.lootProgressGp(loot(4, 15), BossGoalTest::price));
	}

	@Test
	public void fromNowCountsItemsFirstDroppedAfterTheBaseline()
	{
		BossGoal goal = new BossGoal(Boss.PHANTOM_MUSPAH);
		Map<Integer, Integer> baseline = new HashMap<>();
		baseline.put(BONES, 10);
		goal.setLootGoalBaseline(baseline);

		assertEquals(2_000_000, goal.lootProgressGp(loot(2, 10), BossGoalTest::price));
	}
}
