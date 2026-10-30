package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UniqueKillCountsTest
{
	private static final int RONDACHE = 34719;
	private static final int ORION = 34646;
	private static final Instant T = Instant.parse("2026-10-28T19:42:03Z");

	@Test
	public void raidChestLootTakesTheCompletionCount()
	{
		// The count arrives at the clear; the chest is opened later.
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 34, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, ORION, T.plusSeconds(22));

		assertEquals(Collections.singletonList(34), kcs.byItem().get(ORION));
	}

	@Test
	public void aDropJustBeforeItsKillCountTakesThatCount()
	{
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 33, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, ORION, T.plusSeconds(60));
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 34, T.plusMillis(60_600));

		assertEquals(Collections.singletonList(34), kcs.byItem().get(ORION));
	}

	@Test
	public void theNextKillDoesNotMoveAnEarlierDrop()
	{
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 33, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, RONDACHE, T.plusSeconds(20));
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 34, T.plusSeconds(1800));

		assertEquals(Collections.singletonList(33), kcs.byItem().get(RONDACHE));
	}

	@Test
	public void repeatDropsKeepEveryCountOldestFirst()
	{
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 12, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, RONDACHE, T.plusSeconds(20));
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 40, T.plusSeconds(3600));
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, RONDACHE, T.plusSeconds(3620));

		assertEquals(Arrays.asList(12, 40), kcs.byItem().get(RONDACHE));
	}

	@Test
	public void anUnknownCountIsLeftOut()
	{
		// No count seen yet for this boss: another boss's doesn't stand in for it.
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.ZULRAH, 500, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, RONDACHE, T.plusSeconds(30));

		assertTrue(kcs.byItem().isEmpty());
	}

	@Test
	public void aNewSessionKeepsTheLastCount()
	{
		UniqueKillCounts kcs = new UniqueKillCounts();
		kcs.onKillCount(Boss.FRACTURED_ARCHIVE, 34, T);
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, RONDACHE, T.plusSeconds(20));
		kcs.clearDrops();
		kcs.onUnique(Boss.FRACTURED_ARCHIVE, ORION, T.plusSeconds(21));

		assertEquals(Collections.singletonList(34), kcs.byItem().get(ORION));
		assertEquals(1, kcs.byItem().size());
	}
}
