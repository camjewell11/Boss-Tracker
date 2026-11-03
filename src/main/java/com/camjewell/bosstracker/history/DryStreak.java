package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.TeammateUnique;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import lombok.Value;

/**
 * Kills since a boss last dropped a unique, counted from its saved sessions; for a raid,
 * optionally since anyone in it did. The streak counts
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
	/** The last unique's name when it was a teammate's the player added, or null when it was their own. */
	String teammateItemName;

	public boolean hasUnique()
	{
		return lastUniqueItemId != -1;
	}

	public boolean isLastUniqueTeammates()
	{
		return teammateItemName != null;
	}

	/**
	 * @param entries one boss's saved sessions, in any order
	 * @param isUnique whether an item ID counts as a unique in the given session
	 */
	public static DryStreak compute(List<SessionHistoryEntry> entries, BiPredicate<SessionHistoryEntry, Integer> isUnique)
	{
		return compute(entries, isUnique, false);
	}

	/**
	 * @param withTeammates whether a teammate's unique the player added to a session ends the
	 * streak too, for kills since anyone in the raid got one rather than since the player did
	 */
	public static DryStreak compute(List<SessionHistoryEntry> entries, BiPredicate<SessionHistoryEntry, Integer> isUnique,
		boolean withTeammates)
	{
		List<SessionHistoryEntry> newestFirst = new ArrayList<>(entries);
		newestFirst.sort(Comparator.comparingLong(SessionHistoryEntry::getEndedAtEpochMilli).reversed());

		int kills = 0;
		for (SessionHistoryEntry entry : newestFirst)
		{
			int unique = lastUnique(entry, isUnique);
			TeammateUnique teammate = withTeammates ? lastTeammateUnique(entry) : null;
			if (teammate != null && (unique == -1 || teammate.getKillCount() > latestKillCount(entry, unique)))
			{
				return new DryStreak(kills, teammate.getItemId(), entry.getEndedAtEpochMilli(), teammate.getKillCount(),
					teammate.getItemName());
			}
			if (unique != -1)
			{
				return new DryStreak(kills, unique, entry.getEndedAtEpochMilli(), latestKillCount(entry, unique), null);
			}
			kills += entry.getKillsThisSession();
		}
		return new DryStreak(kills, -1, 0, 0, null);
	}

	/**
	 * Kills since the last unique: counted from its kill count when that and {@code currentKc} are
	 * known, which takes in kills from outside saved sessions, else as whole sessions.
	 *
	 * @param currentKc the boss's kill count now, or 0 when unknown
	 */
	public int killsSince(int currentKc)
	{
		if (lastUniqueKillCount > 0 && currentKc >= lastUniqueKillCount)
		{
			return currentKc - lastUniqueKillCount;
		}
		return kills;
	}

	/** The session's teammate unique with the highest kill count, or the last added; null when it has none. */
	private static TeammateUnique lastTeammateUnique(SessionHistoryEntry entry)
	{
		List<TeammateUnique> teammates = entry.getTeammateUniques();
		TeammateUnique found = null;
		if (teammates != null)
		{
			for (TeammateUnique teammate : teammates)
			{
				if (found == null || teammate.getKillCount() >= found.getKillCount())
				{
					found = teammate;
				}
			}
		}
		return found;
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
