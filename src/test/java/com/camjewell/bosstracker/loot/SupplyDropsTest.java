package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SupplyDropsTest
{
	@Test
	public void suppliesAreToldApartFromLoot()
	{
		assertTrue(SupplyDrops.isSupply(Boss.PHANTOM_MUSPAH, ItemID.SHARK, 3));
		assertFalse(SupplyDrops.isSupply(Boss.VORKATH, ItemID.SHARK, 3));
		assertFalse(SupplyDrops.isSupply(null, ItemID.SHARK, 3));
		assertFalse(SupplyDrops.isSupply(Boss.PHANTOM_MUSPAH, ItemID.RUNITE_ORE, 3));
	}

	/** Callisto's other table drops 100 noted anglerfish; its supply drop's are unnoted. */
	@Test
	public void notedDropsAreNotSupplies()
	{
		assertTrue(SupplyDrops.isSupply(Boss.CALLISTO, ItemID.BLIGHTED_ANGLERFISH, 5));
		// The noted item, which has no gameval constant: it's the unnoted ID plus one.
		assertFalse(SupplyDrops.isSupply(Boss.CALLISTO, ItemID.BLIGHTED_ANGLERFISH + 1, 100));
	}

	/** Araxxor's supply drop gives one venom sac; its other tables give 6 or 12. */
	@Test
	public void araxxorsVenomSacIsOnlyASupplySingly()
	{
		assertTrue(SupplyDrops.isSupply(Boss.ARAXXOR, ItemID.ARAXYTE_VENOM_SACK, 1));
		assertFalse(SupplyDrops.isSupply(Boss.ARAXXOR, ItemID.ARAXYTE_VENOM_SACK, 6));
		assertFalse(SupplyDrops.isSupply(Boss.ARAXXOR, ItemID.ARAXYTE_VENOM_SACK, 12));
	}

	@Test
	public void bossesWithoutSuppliesHaveNoSection()
	{
		assertTrue(SupplyDrops.hasSupplies(Boss.YAMA));
		assertFalse(SupplyDrops.hasSupplies(Boss.VORKATH));
		assertFalse(SupplyDrops.hasSupplies(null));
	}

	/** Catches a mistyped item constant: every supply must be one of its boss's wiki drops. */
	@Test
	public void everySupplyIsInItsBossesDropTable()
	{
		DropTables dropTables = new DropTables(new Gson());
		for (Map.Entry<Boss, Set<Integer>> boss : SupplyDrops.supplyItemIds().entrySet())
		{
			Set<Integer> drops = new HashSet<>();
			for (List<Integer> itemIds : dropTables.drops(boss.getKey()).values())
			{
				drops.addAll(itemIds);
			}
			for (int itemId : boss.getValue())
			{
				assertTrue(boss.getKey() + " doesn't drop item " + itemId, drops.contains(itemId));
			}
		}
	}
}
