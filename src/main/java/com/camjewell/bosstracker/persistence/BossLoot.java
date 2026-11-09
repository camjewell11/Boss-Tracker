package com.camjewell.bosstracker.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All-time persisted loot for a single boss: item ID to total quantity received, plus the set of
 * item IDs the user has chosen to hide from the loot grid. One JSON file per boss per account,
 * separate from {@link BossStats} since loot data grows unbounded while stats stay fixed-size.
 */
@Data
@NoArgsConstructor
public class BossLoot
{
	private Map<Integer, Integer> itemQuantities = new HashMap<>();
	private Set<Integer> ignoredItemIds = new HashSet<>();

	/**
	 * Loot from this boss's minions (the God Wars generals' bodyguards), kept apart from the
	 * boss's own drops. Empty for bosses without minions and for files saved before it existed.
	 */
	private Map<Integer, Integer> minionItemQuantities = new HashMap<>();

	/** One per minion drop received. */
	private int minionKills;

	/**
	 * The extra loot from perfect kills (Desert Treasure II bosses), kept apart from the boss's
	 * normal drops; see PerfectKillLoot. Empty for other bosses and older files.
	 */
	private Map<Integer, Integer> perfectBonusItemQuantities = new HashMap<>();
	private int perfectKills;

	/**
	 * What was inside the boss's containers once opened (a Frozen cache, a Cache of runes), kept
	 * apart from its drops; see LootBossMatcher#containerOwners. Empty for older files.
	 */
	private Map<Integer, Integer> cacheItemQuantities = new HashMap<>();

	/** One per container opened. */
	private int cachesOpened;

	/** Folds another boss's loot into this one, for bosses that were merged. */
	public void mergeFrom(BossLoot other)
	{
		other.itemQuantities.forEach((id, qty) -> itemQuantities.merge(id, qty, Integer::sum));
		ignoredItemIds.addAll(other.ignoredItemIds);
		other.minionItemQuantities.forEach((id, qty) -> minionItemQuantities.merge(id, qty, Integer::sum));
		minionKills += other.minionKills;
		other.perfectBonusItemQuantities.forEach((id, qty) -> perfectBonusItemQuantities.merge(id, qty, Integer::sum));
		perfectKills += other.perfectKills;
		other.cacheItemQuantities.forEach((id, qty) -> cacheItemQuantities.merge(id, qty, Integer::sum));
		cachesOpened += other.cachesOpened;
	}
}
