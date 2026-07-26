package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
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

	public BossGoal(Boss boss)
	{
		this.boss = boss;
	}

	public boolean isSet()
	{
		return endKc > 0;
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
}
