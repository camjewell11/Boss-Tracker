package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.ItemGoal;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class DropTablesTest
{
	private final DropTables dropTables = new DropTables(new Gson());

	@Test
	public void everyBossHasADropTable()
	{
		for (Boss boss : Boss.values())
		{
			assertFalse(boss + " has no drops", dropTables.drops(boss).isEmpty());
		}
	}

	@Test
	public void includesTradeablesAndUntradeables()
	{
		Map<String, List<Integer>> muspah = dropTables.drops(Boss.PHANTOM_MUSPAH);

		assertEquals(Arrays.asList(ItemID.VENATOR_SHARD), muspah.get("Venator shard"));
		assertTrue(muspah.get("Muphin").contains(ItemID.MUSPAHPET));
	}

	@Test
	public void everyPetIsInItsBossesTable()
	{
		for (Boss boss : Boss.values())
		{
			int petItemId = boss.getPetItemId();
			if (petItemId > 0)
			{
				assertTrue(boss + " is missing its pet",
					dropTables.drops(boss).values().stream().anyMatch(ids -> ids.contains(petItemId)));
			}
		}
	}

	@Test
	public void aPetCountsInAnyOfItsForms()
	{
		ItemGoal muphin = new ItemGoal("Muphin", dropTables.drops(Boss.PHANTOM_MUSPAH).get("Muphin"), 1);
		Map<Integer, Integer> lifetimeLoot = new HashMap<>();
		lifetimeLoot.put(ItemID.MUSPAHPET, 1);

		assertTrue(muphin.isComplete(lifetimeLoot));
	}
}
