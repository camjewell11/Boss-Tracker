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
	public void hueycoatlDropsUnderItsDefiniteArticleName()
	{
		// Confirmed from a live LootReceived event: name='The Hueycoatl'
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.HUEYCOATL, "The Hueycoatl"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.HUEYCOATL, "Hueycoatl"));
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
	public void godWarsMinionsMatchTheirGeneralAsMinionsOnly()
	{
		assertTrue(LootBossMatcher.matchesMinionLootEvent(Boss.GENERAL_GRAARDOR, "Sergeant Strongstack"));
		assertTrue(LootBossMatcher.matchesMinionLootEvent(Boss.COMMANDER_ZILYANA, "Growler"));
		assertTrue(LootBossMatcher.matchesMinionLootEvent(Boss.KREEARRA, "Flight Kilisa"));
		assertTrue(LootBossMatcher.matchesMinionLootEvent(Boss.KRIL_TSUTSAROTH, "Zakl'n Gritch"));

		// A minion's drop is never the boss's own loot...
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.GENERAL_GRAARDOR, "Sergeant Strongstack"));
		// ...nor another general's minion.
		assertFalse(LootBossMatcher.matchesMinionLootEvent(Boss.GENERAL_GRAARDOR, "Growler"));
		// ...nor the general itself.
		assertFalse(LootBossMatcher.matchesMinionLootEvent(Boss.GENERAL_GRAARDOR, "General Graardor"));
	}

	@Test
	public void onlyTheGeneralsHaveMinions()
	{
		assertTrue(LootBossMatcher.hasMinions(Boss.KREEARRA));
		assertFalse(LootBossMatcher.hasMinions(Boss.CERBERUS));
		assertFalse(LootBossMatcher.hasMinions(Boss.NEX));
		assertFalse(LootBossMatcher.hasMinions(null));
	}

	@Test
	public void unrelatedNamesDoNotMatch()
	{
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.GROTESQUE_GUARDIANS, "Cerberus"));
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.CERBERUS, "Dusk"));
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.CERBERUS, null));
		assertFalse(LootBossMatcher.matchesLootEvent(null, "Dusk"));
	}

	@Test
	public void gauntletLootComesFromEitherHunllef()
	{
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.GAUNTLET, "Crystalline Hunllef"));
		assertTrue(LootBossMatcher.matchesLootEvent(Boss.GAUNTLET, "Corrupted Hunllef"));
		assertFalse(LootBossMatcher.matchesLootEvent(Boss.COLOSSEUM, "Corrupted Hunllef"));
	}
}
