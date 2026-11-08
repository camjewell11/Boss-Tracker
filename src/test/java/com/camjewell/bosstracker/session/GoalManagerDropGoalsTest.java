package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.ItemGoal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Where loot and item goals start counting from. Without an executor the goal manager keeps the
 * goal in memory only, which is all these need.
 */
public class GoalManagerDropGoalsTest
{
	private static final int SHARD = 1;
	private static final int BONES = 2;

	private final Map<Integer, Integer> lifetimeLoot = new HashMap<>();
	private GoalManager goalManager;

	@Before
	public void setUp()
	{
		goalManager = new GoalManager();
		goalManager.ensureLoaded(Boss.PHANTOM_MUSPAH);
		lifetimeLoot.put(SHARD, 2);
		lifetimeLoot.put(BONES, 40);
	}

	private static List<ItemGoal> shards(int quantity)
	{
		return Collections.singletonList(new ItemGoal("Venator shard", Collections.singletonList(SHARD), quantity));
	}

	private ItemGoal onlyItemGoal()
	{
		assertEquals(1, goalManager.getGoal().getItemGoals().size());
		return goalManager.getGoal().getItemGoals().get(0);
	}

	@Test
	public void anItemGoalFromNowCountsOnlyLaterDrops()
	{
		goalManager.setDropGoals(0, shards(3), true, lifetimeLoot);

		assertTrue(goalManager.getGoal().isLootGoalFromNow());
		assertEquals(0, onlyItemGoal().obtained(lifetimeLoot));
		lifetimeLoot.put(SHARD, 3);
		assertEquals(1, onlyItemGoal().obtained(lifetimeLoot));
	}

	@Test
	public void anItemGoalForAllTimeCountsEveryDrop()
	{
		goalManager.setDropGoals(0, shards(3), false, lifetimeLoot);

		assertFalse(goalManager.getGoal().isLootGoalFromNow());
		assertEquals(2, onlyItemGoal().obtained(lifetimeLoot));
	}

	@Test
	public void changingTheQuantityKeepsTheStartingPoint()
	{
		goalManager.setDropGoals(0, shards(3), true, lifetimeLoot);
		lifetimeLoot.put(SHARD, 3);

		goalManager.setDropGoals(0, shards(5), true, lifetimeLoot);

		assertEquals(1, onlyItemGoal().obtained(lifetimeLoot));
		assertEquals(5, onlyItemGoal().getQuantity());
	}

	@Test
	public void anItemAddedLaterCountsFromWhenItWasAdded()
	{
		goalManager.setDropGoals(0, shards(3), true, lifetimeLoot);
		lifetimeLoot.put(SHARD, 3);
		lifetimeLoot.put(BONES, 50);

		goalManager.setDropGoals(0, Arrays.asList(new ItemGoal("Venator shard", Collections.singletonList(SHARD), 3), new ItemGoal("Big bones", Collections.singletonList(BONES), 20)),
			true, lifetimeLoot);
		lifetimeLoot.put(BONES, 55);

		List<ItemGoal> items = goalManager.getGoal().getItemGoals();
		assertEquals(1, items.get(0).obtained(lifetimeLoot));
		assertEquals(5, items.get(1).obtained(lifetimeLoot));
	}

	@Test
	public void switchingToFromNowStartsCountingAgain()
	{
		goalManager.setDropGoals(41_000_000, shards(3), false, lifetimeLoot);

		goalManager.setDropGoals(41_000_000, shards(3), true, lifetimeLoot);

		assertEquals(0, onlyItemGoal().obtained(lifetimeLoot));
		assertEquals(0, goalManager.getGoal().lootProgressGp(lifetimeLoot, itemId -> 1_000));
	}

	@Test
	public void resettingClearsItemGoals()
	{
		goalManager.setDropGoals(0, shards(3), true, lifetimeLoot);

		goalManager.resetGoal();

		assertTrue(goalManager.getGoal().getItemGoals().isEmpty());
		assertFalse(goalManager.getGoal().isLootGoalFromNow());
	}
}
