package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemStack;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FloorSuppliesTest
{
	private static final ItemStack TARTARE = new ItemStack(ItemID.STYMPHIKE_TARTARE, 1);

	private final FloorSupplies floor = new FloorSupplies();

	/** KC 147: three tartare hit the floor the tick the loot event counted the one that fit. */
	@Test
	public void spawnsBeforeTheLootEventAreCountedWithIt()
	{
		assertNull(floor.onSpawn(Boss.MAGGOT_KING, 100, TARTARE));
		assertNull(floor.onSpawn(Boss.MAGGOT_KING, 100, TARTARE));
		assertNull(floor.onSpawn(Boss.MAGGOT_KING, 100, TARTARE));

		assertEquals(3, floor.onLoot(Boss.MAGGOT_KING, 100).size());
	}

	@Test
	public void spawnsAfterTheLootEventAreCountedAtOnce()
	{
		assertTrue(floor.onLoot(Boss.MAGGOT_KING, 100).isEmpty());

		assertSame(TARTARE, floor.onSpawn(Boss.MAGGOT_KING, 100, TARTARE));
	}

	/** A tartare the player drops later isn't a drop. */
	@Test
	public void spawnsOnAnotherTickAreNotCounted()
	{
		floor.onLoot(Boss.MAGGOT_KING, 100);

		assertNull(floor.onSpawn(Boss.MAGGOT_KING, 101, TARTARE));
		assertNull(floor.onSpawn(Boss.MAGGOT_KING, 105, TARTARE));
		List<ItemStack> held = floor.onLoot(Boss.MAGGOT_KING, 110);
		assertTrue(held.isEmpty());
	}

	@Test
	public void eachLootEventTakesOnlyItsOwnSpawns()
	{
		floor.onSpawn(Boss.MAGGOT_KING, 100, TARTARE);
		assertEquals(1, floor.onLoot(Boss.MAGGOT_KING, 100).size());

		assertTrue(floor.onLoot(Boss.MAGGOT_KING, 100).isEmpty());
	}
}
