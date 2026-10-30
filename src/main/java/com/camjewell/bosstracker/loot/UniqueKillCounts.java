package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The kill count each of a session's uniques dropped on. A drop takes the last kill count seen
 * for its boss. A kill's count and its drop arrive on the same tick in either order, so a count
 * that arrives just after a drop is that drop's kill and replaces the earlier one.
 */
public class UniqueKillCounts
{
	/** How far apart a kill's count and its drop can be: the same tick, with room to spare. */
	static final Duration SAME_KILL = Duration.ofSeconds(2);

	private static final class Drop
	{
		final int itemId;
		final Boss boss;
		final Instant at;
		int killCount;

		Drop(int itemId, Boss boss, Instant at, int killCount)
		{
			this.itemId = itemId;
			this.boss = boss;
			this.at = at;
			this.killCount = killCount;
		}
	}

	private final List<Drop> drops = new ArrayList<>();
	private Boss killCountBoss;
	private int killCount;

	public synchronized void onKillCount(Boss boss, int count, Instant now)
	{
		for (Drop drop : drops)
		{
			if (drop.boss == boss && drop.killCount < count
				&& Duration.between(drop.at, now).compareTo(SAME_KILL) <= 0)
			{
				drop.killCount = count;
			}
		}
		killCountBoss = boss;
		killCount = count;
	}

	public synchronized void onUnique(Boss boss, int itemId, Instant now)
	{
		drops.add(new Drop(itemId, boss, now, boss == killCountBoss ? killCount : 0));
	}

	/** Forgets the session's drops, keeping the last kill count for the next session's first drop. */
	public synchronized void clearDrops()
	{
		drops.clear();
	}

	/** Each unique's kill counts, oldest first. Drops whose count was never seen are left out. */
	public synchronized Map<Integer, List<Integer>> byItem()
	{
		Map<Integer, List<Integer>> byItem = new LinkedHashMap<>();
		for (Drop drop : drops)
		{
			if (drop.killCount > 0)
			{
				byItem.computeIfAbsent(drop.itemId, id -> new ArrayList<>()).add(drop.killCount);
			}
		}
		return byItem;
	}
}
