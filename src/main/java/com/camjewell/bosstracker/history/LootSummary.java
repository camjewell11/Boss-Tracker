package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToLongBiFunction;
import lombok.Getter;

/**
 * The combined loot of a set of saved sessions, for History's loot summary: every item's quantity
 * and value, kept apart by section as each session keeps them. Each item is valued at the price
 * its own session gives it, so a summary agrees with the sessions it's made from.
 */
@Getter
public final class LootSummary
{
	/** One kind of loot across the sessions: its items, their values, and how many times it came. */
	@Getter
	public static final class Section
	{
		private final Map<Integer, Integer> quantities = new LinkedHashMap<>();
		private final Map<Integer, Double> values = new LinkedHashMap<>();
		/** Kills, minion kills, perfect kills, caches opened or supply drops. */
		private int count;
		private double totalValue;

		public boolean isEmpty()
		{
			return quantities.isEmpty();
		}

		/** What one of an item was worth on average across the sessions that got it. */
		public long averagePrice(int itemId)
		{
			Integer quantity = quantities.get(itemId);
			return quantity == null || quantity == 0 ? 0 : Math.round(values.get(itemId) / quantity);
		}

		private void add(SessionHistoryEntry entry, Map<Integer, Integer> loot, int times,
			ToLongBiFunction<SessionHistoryEntry, Integer> price)
		{
			count += times;
			if (loot == null)
			{
				return;
			}
			for (Map.Entry<Integer, Integer> item : loot.entrySet())
			{
				double value = (double) price.applyAsLong(entry, item.getKey()) * item.getValue();
				quantities.merge(item.getKey(), item.getValue(), Integer::sum);
				values.merge(item.getKey(), value, Double::sum);
				totalValue += value;
			}
		}
	}

	private int sessions;
	private long seconds;
	private final Section loot = new Section();
	private final Section minionLoot = new Section();
	private final Section perfectBonus = new Section();
	private final Section cacheLoot = new Section();
	private final Section supplies = new Section();

	private LootSummary()
	{
	}

	/**
	 * @param price what one of an item was worth in a session: its price when it dropped, or
	 * today's for sessions saved before drop prices were
	 */
	public static LootSummary of(Collection<SessionHistoryEntry> entries, ToLongBiFunction<SessionHistoryEntry, Integer> price)
	{
		LootSummary summary = new LootSummary();
		for (SessionHistoryEntry entry : entries)
		{
			summary.sessions++;
			summary.seconds += entry.getSessionDurationSeconds();
			summary.loot.add(entry, entry.getLootItemQuantities(), entry.getKillsThisSession(), price);
			summary.minionLoot.add(entry, entry.getMinionLootItemQuantities(), entry.getMinionKills(), price);
			summary.perfectBonus.add(entry, entry.getPerfectBonusItemQuantities(), entry.getPerfectKills(), price);
			summary.cacheLoot.add(entry, entry.getCacheLootItemQuantities(), entry.getCachesOpened(), price);
			summary.supplies.add(entry, entry.getSupplyItemQuantities(), entry.getSupplyDrops(), price);
		}
		return summary;
	}

	public int getKills()
	{
		return loot.getCount();
	}
}
