package com.camjewell.bosstracker.boss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BossTripTest
{
	@Test
	public void raidsAndSingleRunEncountersHaveNoTrips()
	{
		for (Boss boss : new Boss[]{
			Boss.CHAMBERS, Boss.CM_CHAMBERS, Boss.TOA_NORMAL, Boss.TOA_EXPERT,
			Boss.THEATRE_OF_BLOOD, Boss.THEATRE_OF_BLOOD_HM,
			Boss.GAUNTLET, Boss.COLOSSEUM, Boss.TZTOK_JAD, Boss.TZKAL_ZUK})
		{
			assertFalse(boss + " should not track trips", boss.tracksTrips());
		}
	}

	@Test
	public void barrowsTracksTripsButNotByArea()
	{
		assertTrue(Boss.BARROWS.tracksTrips());
		assertFalse(Boss.BARROWS.endsTripOnLeavingArea());
	}

	@Test
	public void areaDetectionFollowsTripTracking()
	{
		assertTrue(Boss.CERBERUS.endsTripOnLeavingArea());
		assertFalse(Boss.CHAMBERS.endsTripOnLeavingArea());
		assertFalse(Boss.COLOSSEUM.endsTripOnLeavingArea());
	}

	@Test
	public void ordinaryBossesTrackTrips()
	{
		assertTrue(Boss.CERBERUS.tracksTrips());
		assertTrue(Boss.ALCHEMICAL_HYDRA.tracksTrips());
		assertTrue(Boss.NEX.tracksTrips());
		assertTrue(Boss.NIGHTMARE.tracksTrips());
		assertTrue(Boss.DAGANNOTH_KINGS.tracksTrips());
	}
}
