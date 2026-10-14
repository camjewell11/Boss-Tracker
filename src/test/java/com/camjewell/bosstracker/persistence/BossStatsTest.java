package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossStatsTest
{
	private final Gson gson = new Gson();

	@Test
	public void statsSavedBeforeRegionsLoadWithNone()
	{
		// Copied from a real stats file, including a leftover key from a since-removed field.
		String legacy = "{\"totalTimeActualSeconds\":0,\"totalTimeVirtualSeconds\":787,\"killsTracked\":7,"
			+ "\"fastestKillSeconds\":101,\"totalKc\":23,\"lootKillsTracked\":12}";

		BossStats stats = gson.fromJson(legacy, BossStats.class);

		assertNotNull(stats.getRegionIds());
		assertTrue(stats.getRegionIds().isEmpty());
		assertEquals(7, stats.getKillsTracked());
	}

	@Test
	public void learnedRegionsRoundTrip()
	{
		BossStats stats = new BossStats();
		stats.getRegionIds().add(4883);
		stats.getRegionIds().add(5140);

		BossStats loaded = gson.fromJson(gson.toJson(stats), BossStats.class);

		assertEquals(stats.getRegionIds(), loaded.getRegionIds());
	}

	@Test
	public void mergingAddsTotalsAndKeepsTheBest()
	{
		BossStats gauntlet = new BossStats();
		gauntlet.setKillsTracked(2);
		gauntlet.setTotalTimeVirtualSeconds(1200);
		gauntlet.setFastestKillSeconds(500);
		gauntlet.setTotalKc(40);

		BossStats corrupted = new BossStats();
		corrupted.setKillsTracked(5);
		corrupted.setTotalTimeVirtualSeconds(3000);
		corrupted.setFastestKillSeconds(420);
		corrupted.setTotalKc(150);
		corrupted.setGoalStartKc(150);
		corrupted.setGoalEndKc(200);

		gauntlet.mergeFrom(corrupted);

		assertEquals(7, gauntlet.getKillsTracked());
		assertEquals(4200, gauntlet.getTotalTimeVirtualSeconds());
		assertEquals(420, gauntlet.getFastestKillSeconds());
		assertEquals(150, gauntlet.getTotalKc());
		assertEquals(200, gauntlet.getGoalEndKc());
	}
}
