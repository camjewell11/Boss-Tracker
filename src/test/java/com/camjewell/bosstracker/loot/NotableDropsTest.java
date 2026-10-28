package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * The Fractured Archive's uniques, from the wiki's chest drop table.
 */
public class NotableDropsTest
{
	private static final Boss ARCHIVE = Boss.FRACTURED_ARCHIVE;

	@Test
	public void archivePetIsOrion()
	{
		// One pet, Orion; the boss-named versions are only its other looks.
		assertEquals(ItemID.OCELLUSPET, ARCHIVE.getPetItemId());
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.OCELLUSPET, 0));
	}

	@Test
	public void untradeableFragmentsAreUniques()
	{
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.TFA_MOTE_AIR, 0));
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.TFA_MOTE_WATER, 0));
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.TFA_MOTE_EARTH, 0));
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.TFA_MOTE_FIRE, 0));
	}

	@Test
	public void chestUniquesCountByPrice()
	{
		// Rondache, the cheapest of the five, sells for about 33m.
		assertTrue(NotableDrops.isNotable(ARCHIVE, ItemID.RONDACHE, 33_000_000));
		assertFalse(NotableDrops.isNotable(ARCHIVE, ItemID.ASCENSION_SHARDS, 300));
		assertFalse(NotableDrops.isNotable(ARCHIVE, ItemID.ARCHIVISTS_FOLIO_EMPTY, 0));
	}
}
