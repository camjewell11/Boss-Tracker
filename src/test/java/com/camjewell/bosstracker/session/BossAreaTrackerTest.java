package com.camjewell.bosstracker.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Region IDs pack x in the high byte and y in the low byte. The Cerberus IDs are from live play:
 * kills in 5140 and 5395, and 5139 reached on foot from both lairs.
 */
public class BossAreaTrackerTest
{
	@Test
	public void anyCerberusLairCoversTheWholeComplex()
	{
		int[] complex = {4883, 5140, 5395, 5139};
		for (int a : complex)
		{
			for (int b : complex)
			{
				assertTrue(a + " and " + b, BossAreaTracker.isNearby(a, b));
			}
		}
	}

	@Test
	public void westAndEastLairsAreTwoSquaresApart()
	{
		// The case one-square tolerance got wrong: first kill in the west lair, then the east.
		assertTrue(BossAreaTracker.isNearby(4883, 5395));
	}

	@Test
	public void distantRegionsAreNot()
	{
		// Three squares east of the north lair.
		assertFalse(BossAreaTracker.isNearby(5140, 5908));
		// Lumbridge, where a home teleport lands, against the north lair.
		assertFalse(BossAreaTracker.isNearby(5140, 12850));
	}
}
