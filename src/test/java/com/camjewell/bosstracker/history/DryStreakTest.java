package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DryStreakTest
{
	private static final int UNIQUE = 1;
	private static final int JUNK = 2;

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
	public void noUniqueCountsEverySession()
	{
		DryStreak streak = DryStreak.compute(Arrays.asList(session(1000, 8, JUNK), session(2000, 9)), (entry, id) -> id == UNIQUE);

		assertFalse(streak.hasUnique());
		assertEquals(17, streak.getKills());
	}
}
