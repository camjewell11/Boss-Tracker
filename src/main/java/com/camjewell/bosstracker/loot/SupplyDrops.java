package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * The supplies some bosses drop beside their loot: food and potions from a roll of their own,
 * which the OSRS wiki lists under each boss's "Supplies" heading. They arrive in the same loot
 * event as the boss's other drops, so they're told apart by item: supplies always drop unnoted,
 * and none of these items drop unnoted from the same boss's other tables, with one exception.
 * Araxxor's other tables drop its venom sacs 6 or 12 at a time, but its supply drop gives one.
 */
public final class SupplyDrops
{
	/** Matches a supply item dropped in any quantity. */
	private static final int ANY = 0;

	/** Each boss's supply items, mapped to the only quantity that's a supply drop, or ANY. */
	private static final Map<Boss, Map<Integer, Integer>> TABLES = new EnumMap<>(Boss.class);

	static
	{
		supplies(Boss.ARAXXOR, ItemID._1DOSE2COMBAT,ItemID._4DOSEPRAYERRESTORE, ItemID.SHARK, ItemID.WILD_PIE);
		TABLES.get(Boss.ARAXXOR).put(ItemID.ARAXYTE_VENOM_SACK, 1);

		for (Boss wildernessBoss : new Boss[]{Boss.ARTIO, Boss.CALLISTO, Boss.CALVARION, Boss.SPINDEL, Boss.VENENATIS, Boss.VETION})
		{
			supplies(wildernessBoss, ItemID.BLIGHTED_ANGLERFISH, ItemID.BLIGHTED_KARAMBWAN, ItemID.BLIGHTED_3DOSE2RESTORE,
				ItemID.BLIGHTED_4DOSE2RESTORE, ItemID._2DOSERANGERSPOTION, ItemID._2DOSE2COMBAT);
		}

		supplies(Boss.DUKE_SUCELLUS, ItemID.PINEAPPLE_PIZZA, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSE2COMBAT);
		supplies(Boss.VARDORVIS, ItemID.POTATO_TUNA_SWEETCORN, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSE2COMBAT);
		supplies(Boss.LEVIATHAN, ItemID.SEATURTLE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSERANGERSPOTION);
		supplies(Boss.WHISPERER, ItemID.MANTARAY, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEANCIENTBREW);
		supplies(Boss.GROTESQUE_GUARDIANS, ItemID.POTATO_MUSHROOM_ONION, ItemID._4DOSEPOTIONOFSARADOMIN,
			ItemID._2DOSE1MAGIC, ItemID._2DOSERANGERSPOTION, ItemID._2DOSE2COMBAT, ItemID._4DOSEPRAYERRESTORE);
		supplies(Boss.MAD_ANGEL, ItemID.SHARK, ItemID.YELLOWFIN, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSE2COMBAT);
		supplies(Boss.MAGGOT_KING, ItemID.STYMPHIKE_TARTARE, ItemID.DULL_ZAROSIAN_MEDAL);
		supplies(Boss.PHANTOM_MUSPAH, ItemID.SHARK, ItemID.SUMMER_PIE, ItemID._3DOSEANCIENTBREW,
			ItemID._3DOSERANGERSPOTION, ItemID._3DOSE2RESTORE, ItemID._3DOSEPRAYERRESTORE);
		supplies(Boss.YAMA, ItemID.PINEAPPLE_PIZZA, ItemID.WILD_PIE, ItemID._3DOSEPRAYERRESTORE,
			ItemID.BRUTAL_2DOSE2RESTORE, ItemID._1DOSE2COMBAT, ItemID.BRUTAL_2DOSEPOTIONOFZAMORAK);
	}

	private SupplyDrops()
	{
	}

	private static void supplies(Boss boss, int... itemIds)
	{
		Map<Integer, Integer> table = new HashMap<>();
		for (int itemId : itemIds)
		{
			table.put(itemId, ANY);
		}
		TABLES.put(boss, table);
	}

	/** The bosses with supply drops, mapped to their supply items' IDs, for the tests. */
	static Map<Boss, Set<Integer>> supplyItemIds()
	{
		Map<Boss, Set<Integer>> itemIds = new EnumMap<>(Boss.class);
		TABLES.forEach((boss, table) -> itemIds.put(boss, table.keySet()));
		return itemIds;
	}

	public static boolean hasSupplies(Boss boss)
	{
		return boss != null && TABLES.containsKey(boss);
	}

	/** Whether this drop of {@code boss}'s came from its supply roll rather than its loot. */
	public static boolean isSupply(Boss boss, int itemId, int quantity)
	{
		if (boss == null)
		{
			return false;
		}
		Integer supplyQuantity = TABLES.getOrDefault(boss, Collections.emptyMap()).get(itemId);
		return supplyQuantity != null && (supplyQuantity == ANY || supplyQuantity == quantity);
	}
}
