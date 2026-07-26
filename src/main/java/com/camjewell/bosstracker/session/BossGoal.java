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
}
