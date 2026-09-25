package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The loot event name is the dying NPC's name, which is not always the boss's name.
 */
public class LootBossMatcherTest
{
	@Test
	public void grotesqueGuardiansDropAsDusk()
	{
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.GROTESQUE_GUARDIANS, "Dusk"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.GROTESQUE_GUARDIANS, "Dawn"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.GROTESQUE_GUARDIANS, "Grotesque Guardians"));
	}

	@Test
	public void alternateNpcNamesStillMatch()
	{
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.KRAKEN, "Kraken"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.THERMY, "Thermonuclear smoke devil"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.VETION, "Vet'ion Reborn"));
	}

	@Test
	public void groupContentAliasesStillMatch()
	{
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.CHAMBERS, "Chambers of Xeric"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.CM_CHAMBERS, "Chambers of Xeric"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.COLOSSEUM, "Fortis Colosseum"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.ROYAL_TITANS, "Branda the Fire Queen"));
	}

	@Test
	public void unrelatedNamesDoNotMatch()
	{
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.GROTESQUE_GUARDIANS, "Cerberus"));
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.CERBERUS, "Dusk"));
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.CERBERUS, null));
		assertFalse(LootBossMatcher.matchesLootEvent(null, "Dusk"));
	}
}
