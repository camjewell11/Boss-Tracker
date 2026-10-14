package com.camjewell.bosstracker.persistence;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All-time persisted stats for a single boss, one JSON file per boss per account. Defined with
 * its full eventual shape now so later phases (goals, loot) only need to populate additional
 * fields rather than migrate the file format.
 */
@Data
@NoArgsConstructor
public class BossStats
{
	private long totalTimeActualSeconds;
	private long totalTimeVirtualSeconds;
	private int killsTracked;
	private int fastestKillSeconds = Integer.MAX_VALUE;
	private int totalKc;

	/**
	 * Boss-goal KC range (Phase 2); 0 until a goal is set.
	 */
	private int goalStartKc;
	private int goalEndKc;
	private boolean goalNotified;

	/**
	 * Optional loot-value goal in GP; 0 means no loot goal is set.
	 */
	private long lootGoalGp;
	private boolean lootGoalNotified;

	/**
	 * Map regions this boss has been killed in, learned by BossAreaTracker and used to end a trip
	 * when the player leaves. Empty in stats saved before trips existed.
	 */
	private Set<Integer> regionIds = new LinkedHashSet<>();

	/**
	 * Folds another boss's stats into these, for bosses that were merged. Totals add up, the
	 * fastest kill and highest KC win, and goals are only taken over where none is set here.
	 */
	public void mergeFrom(BossStats other)
	{
		totalTimeActualSeconds += other.totalTimeActualSeconds;
		totalTimeVirtualSeconds += other.totalTimeVirtualSeconds;
		killsTracked += other.killsTracked;
		fastestKillSeconds = Math.min(fastestKillSeconds, other.fastestKillSeconds);
		totalKc = Math.max(totalKc, other.totalKc);
		if (goalEndKc == 0)
		{
			goalStartKc = other.goalStartKc;
			goalEndKc = other.goalEndKc;
			goalNotified = other.goalNotified;
		}
		if (lootGoalGp == 0)
		{
			lootGoalGp = other.lootGoalGp;
			lootGoalNotified = other.lootGoalNotified;
		}
		regionIds.addAll(other.regionIds);
	}
}
