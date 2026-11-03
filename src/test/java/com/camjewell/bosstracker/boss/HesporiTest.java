package com.camjewell.bosstracker.boss;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.chat.ChatKillParser;
import com.camjewell.bosstracker.loot.NotableDrops;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * Messages copied from the RuneLite log of a Hespori kill.
 */
public class HesporiTest
{
	@Test
	public void killCountMessageIsHespori()
	{
		String message = "Your Hespori kill count is: <col=ff0000>31</col>.";
		assertEquals(Boss.HESPORI, ChatKillParser.findKcBoss(message, BossTrackerConfig.DksSelector.KINGS));
		assertEquals(31, ChatKillParser.parseKillCount(message));
	}

	@Test
	public void lootIsMatchedByTheNpcName()
	{
		assertEquals(Boss.HESPORI, Boss.byNpcName("Hespori"));
	}

	@Test
	public void hasNoPet()
	{
		assertEquals(-1, Boss.HESPORI.getPetItemId());
	}

	@Test
	public void compostBucketIsTheUnique()
	{
		assertTrue(NotableDrops.isNotable(Boss.HESPORI, ItemID.BOTTOMLESS_COMPOST_BUCKET, 0));
		assertFalse(NotableDrops.isNotable(Boss.HESPORI, ItemID.IASOR_SEED, 50_000));
	}
}
