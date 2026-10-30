package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import lombok.Value;

/**
 * Kills since a boss last dropped a unique, counted from its saved sessions. The streak counts
 * whole sessions: the session holding the last unique ends it and only later sessions' kills count
 * toward it. Older sessions don't record which kill dropped what.
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
	/** The kill count the last unique dropped on, or 0 when it wasn't recorded. */
	int lastUniqueKillCount;

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
			int unique = lastUnique(entry, isUnique);
			if (unique != -1)
			{
				return new DryStreak(kills, unique, entry.getEndedAtEpochMilli(), latestKillCount(entry, unique));
			}
			kills += entry.getKillsThisSession();
		}
		return new DryStreak(kills, -1, 0, 0);
	}

	/**
	 * The session's unique with the highest recorded kill count, or the first found when none was
	 * recorded, from its loot or minion loot. -1 when it has no unique.
	 */
	private static int lastUnique(SessionHistoryEntry entry, BiPredicate<SessionHistoryEntry, Integer> isUnique)
	{
		int found = -1;
		for (Map<Integer, Integer> loot : Arrays.asList(entry.getLootItemQuantities(), entry.getMinionLootItemQuantities()))
		{
			if (loot == null)
			{
				continue;
			}
			for (int itemId : loot.keySet())
			{
				if (isUnique.test(entry, itemId)
					&& (found == -1 || latestKillCount(entry, itemId) > latestKillCount(entry, found)))
				{
					found = itemId;
				}
			}
		}
		return found;
	}

	/** The highest kill count the item dropped on in this session, or 0 when none was recorded. */
	static int latestKillCount(SessionHistoryEntry entry, int itemId)
	{
		List<Integer> killCounts = entry.getUniqueKillCounts() != null ? entry.getUniqueKillCounts().get(itemId) : null;
		return killCounts == null || killCounts.isEmpty() ? 0 : Collections.max(killCounts);
	}
}
