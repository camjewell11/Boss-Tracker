package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import lombok.Value;

/**
 * Finds which saved sessions dropped an item, for the search view. Matches item names containing
 * the query, so "runite" finds runite ore and runite bolts. A session's regular, minion and
 * perfect-kill loot all count, and noted and unnoted drops of the same item are combined by name.
 */
public final class ItemDropSearch
{
	/** Shorter queries ("a", "ru") would match most of the item list. */
	public static final int MIN_QUERY_LENGTH = 3;

	@Value
	public static class Drop
	{
		SessionHistoryEntry entry;
		int quantity;
	}

	@Value
	public static class ItemMatch
	{
		String itemName;
		/** One of the matching item IDs, for the icon and price. */
		int itemId;
		/** Newest session first. */
		List<Drop> drops;

		public int getTotalQuantity()
		{
			int total = 0;
			for (Drop drop : drops)
			{
				total += drop.getQuantity();
			}
			return total;
		}
	}

	private ItemDropSearch()
	{
	}

	/**
	 * @param itemNames resolves an item ID to its name
	 * @return matching items, most-dropped first; empty when the query is too short
	 */
	public static List<ItemMatch> search(List<SessionHistoryEntry> entries, IntFunction<String> itemNames, String query)
	{
		List<ItemMatch> matches = new ArrayList<>();
		if (query == null || query.trim().length() < MIN_QUERY_LENGTH)
		{
			return matches;
		}
		String needle = query.trim().toLowerCase();

		List<SessionHistoryEntry> newestFirst = new ArrayList<>(entries);
		newestFirst.sort(Comparator.comparingLong(SessionHistoryEntry::getEndedAtEpochMilli).reversed());

		Map<String, Integer> itemIds = new LinkedHashMap<>();
		Map<String, List<Drop>> dropsByName = new LinkedHashMap<>();
		for (SessionHistoryEntry entry : newestFirst)
		{
			Map<String, Integer> quantityByName = new LinkedHashMap<>();
			collect(entry.getLootItemQuantities(), itemNames, needle, quantityByName, itemIds);
			collect(entry.getMinionLootItemQuantities(), itemNames, needle, quantityByName, itemIds);
			collect(entry.getPerfectBonusItemQuantities(), itemNames, needle, quantityByName, itemIds);
			quantityByName.forEach((name, quantity) ->
				dropsByName.computeIfAbsent(name, n -> new ArrayList<>()).add(new Drop(entry, quantity)));
		}

		dropsByName.forEach((name, drops) -> matches.add(new ItemMatch(name, itemIds.get(name), drops)));
		matches.sort(Comparator.comparingInt((ItemMatch m) -> m.getDrops().size()).reversed());
		return matches;
	}

	private static void collect(Map<Integer, Integer> loot, IntFunction<String> itemNames, String needle,
		Map<String, Integer> quantityByName, Map<String, Integer> itemIds)
	{
		if (loot == null)
		{
			return;
		}
		for (Map.Entry<Integer, Integer> item : loot.entrySet())
		{
			String name = itemNames.apply(item.getKey());
			if (name == null || !name.toLowerCase().contains(needle))
			{
				continue;
			}
			quantityByName.merge(name, item.getValue(), Integer::sum);
			itemIds.putIfAbsent(name, item.getKey());
		}
	}
}
