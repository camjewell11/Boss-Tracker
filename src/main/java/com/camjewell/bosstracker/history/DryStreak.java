package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import lombok.Value;

/**
 * Kills since a boss last dropped a unique, counted from its saved sessions. History only keeps
 * each session's loot totals, not which kill dropped what, so the session holding the last unique
 * counts as ending the streak and only later sessions' kills count toward it.
 */
@Value
public class DryStreak
{
	/** Kills in sessions after the one with the last unique, or in every session if none had one. */
	int kills;
	/** The last unique's item ID, or -1 when no saved session has one. */
	int lastUniqueItemId;
	/** When the session with the last unique ended, or 0 when there was none. */
	long lastUniqueAtEpochMilli;

	public boolean hasUnique()
	{
		return lastUniqueItemId != -1;
	}

	/**
	 * @param entries one boss's saved sessions, in any order
	 * @param isUnique whether an item ID counts as a unique in the given session
	 */
	public static DryStreak compute(List<SessionHistoryEntry> entries, BiPredicate<SessionHistoryEntry, Integer> isUnique)
	{
		List<SessionHistoryEntry> newestFirst = new ArrayList<>(entries);
		newestFirst.sort(Comparator.comparingLong(SessionHistoryEntry::getEndedAtEpochMilli).reversed());

		int kills = 0;
		for (SessionHistoryEntry entry : newestFirst)
		{
			int unique = firstUnique(entry, isUnique);
			if (unique != -1)
			{
				return new DryStreak(kills, unique, entry.getEndedAtEpochMilli());
			}
			kills += entry.getKillsThisSession();
		}
		return new DryStreak(kills, -1, 0);
	}

	/** The first unique found in the session's loot or minion loot, or -1. */
	private static int firstUnique(SessionHistoryEntry entry, BiPredicate<SessionHistoryEntry, Integer> isUnique)
	{
		for (Map<Integer, Integer> loot : Arrays.asList(entry.getLootItemQuantities(), entry.getMinionLootItemQuantities()))
		{
			if (loot == null)
			{
				continue;
			}
			for (int itemId : loot.keySet())
			{
				if (isUnique.test(entry, itemId))
				{
					return itemId;
				}
			}
		}
		return -1;
	}
}
