package com.camjewell.bosstracker.boss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Guards the Slayer task name to boss mapping. The names asserted here are the ones the Slayer
 * plugin reports from the game's task table, which frequently differ from the boss's own name.
 */
public class BossSlayerTaskTest
{
	@Test
	public void regularTaskCreditsItsBoss()
	{
		assertTrue(Boss.CERBERUS.countsForSlayerTask("Hellhounds"));
		assertTrue(Boss.ALCHEMICAL_HYDRA.countsForSlayerTask("Hydras"));
		assertTrue(Boss.GROTESQUE_GUARDIANS.countsForSlayerTask("Gargoyles"));
		assertTrue(Boss.KRAKEN.countsForSlayerTask("Cave kraken"));
		assertTrue(Boss.THERMY.countsForSlayerTask("Smoke devils"));
		assertTrue(Boss.SCURRIUS.countsForSlayerTask("Rats"));
	}

	@Test
	public void bossTaskCreditsItsBoss()
	{
		assertTrue(Boss.CERBERUS.countsForSlayerTask("Cerberus"));
		assertTrue(Boss.ALCHEMICAL_HYDRA.countsForSlayerTask("The Alchemical Hydra"));
		assertTrue(Boss.ABYSSAL_SIRE.countsForSlayerTask("The Abyssal Sire"));
		assertTrue(Boss.KALPHITE_QUEEN.countsForSlayerTask("The Kalphite Queen"));
	}

	@Test
	public void oneTaskCanCreditSeveralBosses()
	{
		assertTrue(Boss.SARACHNIS.countsForSlayerTask("Spiders"));
		assertTrue(Boss.VENENATIS.countsForSlayerTask("Spiders"));
		assertTrue(Boss.SPINDEL.countsForSlayerTask("Spiders"));
		assertTrue(Boss.ARAXXOR.countsForSlayerTask("Spiders"));
		assertTrue(Boss.DAGANNOTH_REX.countsForSlayerTask("Dagannoth Kings"));
		assertTrue(Boss.DAGANNOTH_KINGS.countsForSlayerTask("Dagannoth Kings"));
	}

	@Test
	public void matchingIsCaseInsensitive()
	{
		assertTrue(Boss.CERBERUS.countsForSlayerTask("hellhounds"));
		assertTrue(Boss.CERBERUS.countsForSlayerTask("HELLHOUNDS"));
	}

	@Test
	public void unrelatedTaskDoesNotCreditBoss()
	{
		assertFalse(Boss.CERBERUS.countsForSlayerTask("Hydras"));
		assertFalse(Boss.ALCHEMICAL_HYDRA.countsForSlayerTask("Hellhounds"));
		assertFalse(Boss.VORKATH.countsForSlayerTask("Bats"));
		assertFalse(Boss.ZULRAH.countsForSlayerTask(""));
		assertFalse(Boss.ZULRAH.countsForSlayerTask(null));
	}

	@Test
	public void bossNameAloneIsNotEnoughToCountAsATask()
	{
		// "Chambers" is not a Slayer assignment, and its alias must not resolve one either.
		assertFalse(Boss.CHAMBERS.countsForSlayerTask("Chambers"));
		assertFalse(Boss.CHAMBERS.countsForSlayerTask("cox"));
	}
}
