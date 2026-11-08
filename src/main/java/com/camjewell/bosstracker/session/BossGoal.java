package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.ItemGoal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntToLongFunction;
import lombok.Getter;
import lombok.Setter;

/**
 * A KC goal for a single boss: kill from {@code startKc} to {@code endKc}. {@code endKc == 0}
 * means no goal is set. {@code notified} tracks whether the goal-complete chat message has
 * already been sent, so it only fires once per goal.
 */
@Getter
@Setter
public class BossGoal
{
	private final Boss boss;
	private int startKc;
	private int endKc;
	private boolean notified;

	/**
	 * Optional loot-value goal in GP; 0 means no loot goal is set. {@code lootGoalNotified}
	 * tracks whether the loot-goal-complete chat message has already been sent, so it only
	 * fires once per goal.
	 */
	private long lootGoalGp;
	private boolean lootGoalNotified;

	/**
	 * The boss's lifetime loot quantities when the loot or item goals were set to count from then,
	 * or null when they count all-time loot. Quantities rather than a GP total, so price changes
	 * alone don't move the progress.
	 */
	private Map<Integer, Integer> lootGoalBaseline;

	/**
	 * Items to get from the boss. Replaced as a whole rather than edited in place, as it's read on
	 * the client thread while the panel sets it.
	 */
	private List<ItemGoal> itemGoals = Collections.emptyList();

	public BossGoal(Boss boss)
	{
		this.boss = boss;
	}

	/**
	 * A KC goal only counts as set if there's an actual range to work through. Guards against the
	 * degenerate case where the goal dialog is used only to set a loot goal and leaves the KC
	 * fields at their auto-filled default (the live KC for both start and end) — without this
	 * check, that would read as an instantly-complete zero-kill KC goal.
	 */
	public boolean isSet()
	{
		return endKc > startKc;
	}

	public int killsDone(int currentKc)
	{
		return Math.max(0, currentKc - startKc);
	}

	public int totalKillsToGet()
	{
		return Math.max(0, endKc - startKc);
	}

	public boolean isComplete(int currentKc)
	{
		return isSet() && currentKc >= endKc;
	}

	public boolean isLootGoalSet()
	{
		return lootGoalGp > 0;
	}

	public boolean isLootGoalComplete(long currentGp)
	{
		return isLootGoalSet() && currentGp >= lootGoalGp;
	}

	public boolean isLootGoalFromNow()
	{
		return lootGoalBaseline != null;
	}

	/**
	 * The loot that counts toward the loot goal, at current prices: all of {@code lifetimeLoot},
	 * or only what dropped after the baseline was taken.
	 */
	public long lootProgressGp(Map<Integer, Integer> lifetimeLoot, IntToLongFunction unitPrice)
	{
		long total = 0;
		for (Map.Entry<Integer, Integer> entry : lifetimeLoot.entrySet())
		{
			int quantity = entry.getValue();
			if (lootGoalBaseline != null)
			{
				quantity -= lootGoalBaseline.getOrDefault(entry.getKey(), 0);
			}
			if (quantity > 0)
			{
				total += unitPrice.applyAsLong(entry.getKey()) * quantity;
			}
		}
		return total;
	}
}
