package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.TeammateUnique;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DryStreakTest
{
	private static final int UNIQUE = 1;
	private static final int JUNK = 2;
	private static final int TEAMMATE_UNIQUE = 4;

	private static SessionHistoryEntry session(long endedAt, int kills, int... itemIds)
	{
		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setEndedAtEpochMilli(endedAt);
		entry.setKillsThisSession(kills);
		for (int itemId : itemIds)
		{
			entry.getLootItemQuantities().put(itemId, 1);
		}
		return entry;
	}

	@Test
	public void countsKillsInSessionsAfterTheLastUnique()
	{
		SessionHistoryEntry oldUnique = session(1000, 30, UNIQUE);
		SessionHistoryEntry lastUnique = session(2000, 20, JUNK, UNIQUE);
		SessionHistoryEntry dry1 = session(3000, 15, JUNK);
		SessionHistoryEntry dry2 = session(4000, 10, JUNK);

		// Passed out of order on purpose: the streak must sort by when sessions ended.
		DryStreak streak = DryStreak.compute(Arrays.asList(dry2, oldUnique, dry1, lastUnique), (entry, id) -> id == UNIQUE);

		assertEquals(25, streak.getKills());
		assertEquals(UNIQUE, streak.getLastUniqueItemId());
		assertEquals(2000, streak.getLastUniqueAtEpochMilli());
	}

	@Test
	public void minionDropsCountAsUniques()
	{
		SessionHistoryEntry withMinionUnique = session(1000, 12, JUNK);
		withMinionUnique.getMinionLootItemQuantities().put(UNIQUE, 1);

		DryStreak streak = DryStreak.compute(Collections.singletonList(withMinionUnique), (entry, id) -> id == UNIQUE);

		assertTrue(streak.hasUnique());
		assertEquals(0, streak.getKills());
	}

	@Test
	public void lastUniqueIsTheOneWithTheHighestKillCount()
	{
		int laterUnique = 3;
		SessionHistoryEntry entry = session(1000, 10, UNIQUE, laterUnique);
		entry.getUniqueKillCounts().put(UNIQUE, Arrays.asList(102, 108));
		entry.getUniqueKillCounts().put(laterUnique, Collections.singletonList(105));

		DryStreak streak = DryStreak.compute(Collections.singletonList(entry), (e, id) -> id == UNIQUE || id == laterUnique);

		assertEquals(UNIQUE, streak.getLastUniqueItemId());
		assertEquals(108, streak.getLastUniqueKillCount());
	}

	@Test
	public void olderSessionsHaveNoKillCount()
	{
		DryStreak streak = DryStreak.compute(Collections.singletonList(session(1000, 5, UNIQUE)), (entry, id) -> id == UNIQUE);

		assertEquals(0, streak.getLastUniqueKillCount());
	}

	@Test
	public void noUniqueCountsEverySession()
	{
		DryStreak streak = DryStreak.compute(Arrays.asList(session(1000, 8, JUNK), session(2000, 9)), (entry, id) -> id == UNIQUE);

		assertFalse(streak.hasUnique());
		assertEquals(17, streak.getKills());
	}

	@Test
	public void aTeammatesUniqueOnlyEndsTheTeamStreak()
	{
		SessionHistoryEntry yours = session(1000, 10, UNIQUE);
		SessionHistoryEntry theirs = session(2000, 20, JUNK);
		theirs.getTeammateUniques().add(new TeammateUnique(TEAMMATE_UNIQUE, "Obligator (uncharged)", 3, 400_000_000L));
		SessionHistoryEntry dry = session(3000, 5, JUNK);
		List<SessionHistoryEntry> sessions = Arrays.asList(yours, theirs, dry);

		DryStreak personal = DryStreak.compute(sessions, (entry, id) -> id == UNIQUE);
		DryStreak team = DryStreak.compute(sessions, (entry, id) -> id == UNIQUE, true);

		assertEquals(25, personal.getKills());
		assertFalse(personal.isLastUniqueTeammates());
		assertEquals(5, team.getKills());
		assertEquals(TEAMMATE_UNIQUE, team.getLastUniqueItemId());
		assertEquals("Obligator (uncharged)", team.getTeammateItemName());
	}

	@Test
	public void theLaterOfYoursAndATeammatesInOneSessionEndsTheTeamStreak()
	{
		SessionHistoryEntry entry = session(1000, 10, UNIQUE);
		entry.getUniqueKillCounts().put(UNIQUE, Collections.singletonList(52));
		entry.getTeammateUniques().add(new TeammateUnique(TEAMMATE_UNIQUE, "Rondache", 4, 30_000_000L, 55));

		DryStreak team = DryStreak.compute(Collections.singletonList(entry), (e, id) -> id == UNIQUE, true);

		assertTrue(team.isLastUniqueTeammates());
		assertEquals(55, team.getLastUniqueKillCount());
		assertEquals(4, team.killsSince(59));
	}

	@Test
	public void killsSinceCountsFromTheKillCountWhenKnown()
	{
		SessionHistoryEntry entry = session(1000, 10, UNIQUE);
		entry.getUniqueKillCounts().put(UNIQUE, Collections.singletonList(34));
		SessionHistoryEntry dry = session(2000, 6, JUNK);

		DryStreak streak = DryStreak.compute(Arrays.asList(entry, dry), (e, id) -> id == UNIQUE);

		// Raids done outside saved sessions count too.
		assertEquals(12, streak.killsSince(46));
		// Without a current kill count, whole sessions after it.
		assertEquals(6, streak.killsSince(0));
	}

	@Test
	public void killsSinceFallsBackToSessionsWithoutAKillCount()
	{
		DryStreak streak = DryStreak.compute(Arrays.asList(session(1000, 10, UNIQUE), session(2000, 7, JUNK)), (e, id) -> id == UNIQUE);

		assertEquals(7, streak.killsSince(80));
	}
}
