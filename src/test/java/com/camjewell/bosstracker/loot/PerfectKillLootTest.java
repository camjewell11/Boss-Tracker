package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PerfectKillLootTest
{
	@Test
	public void perfectQuantityYieldsTheBonus()
	{
		// The in-game example: a perfect Vardorvis kill dropped 27 runite ore instead of 18.
		assertEquals(9, PerfectKillLoot.bonusQuantity(Boss.VARDORVIS, "Runite ore", 27));
		assertEquals(22, PerfectKillLoot.bonusQuantity(Boss.VARDORVIS, "Adamantite ore", 67));
		assertEquals(34, PerfectKillLoot.bonusQuantity(Boss.LEVIATHAN, "Gold ore", 101));
	}

	@Test
	public void normalQuantityHasNoBonus()
	{
		assertEquals(0, PerfectKillLoot.bonusQuantity(Boss.VARDORVIS, "Runite ore", 18));
		// 27 is the Leviathan's normal runite drop, not a perfect one.
		assertEquals(0, PerfectKillLoot.bonusQuantity(Boss.LEVIATHAN, "Runite ore", 27));
	}

	@Test
	public void itemNamesMatchIgnoringCase()
	{
		assertEquals(467, PerfectKillLoot.bonusQuantity(Boss.WHISPERER, "SOUL RUNE", 1400));
		assertEquals(30, PerfectKillLoot.bonusQuantity(Boss.DUKE_SUCELLUS, "dragon javelin tips", 90));
	}

	@Test
	public void onlyDesertTreasureBossesHaveTheBonus()
	{
		assertTrue(PerfectKillLoot.hasPerfectKillBonus(Boss.VARDORVIS));
		assertTrue(PerfectKillLoot.hasPerfectKillBonus(Boss.DUKE_SUCELLUS));
		assertFalse(PerfectKillLoot.hasPerfectKillBonus(Boss.VORKATH));
		assertEquals(0, PerfectKillLoot.bonusQuantity(Boss.VORKATH, "Runite ore", 27));
		assertEquals(0, PerfectKillLoot.bonusQuantity(Boss.VARDORVIS, null, 27));
	}
}
