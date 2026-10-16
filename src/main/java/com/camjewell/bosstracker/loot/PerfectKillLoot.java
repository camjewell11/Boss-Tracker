package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The Desert Treasure II bosses drop 50% more from their standard table when killed without
 * taking avoidable damage. The game never announces a perfect kill, but every standard drop has
 * a fixed quantity with a fixed perfect-kill quantity, so a drop at the perfect quantity marks
 * one, and the difference is the bonus.
 *
 * <p>Both quantities are stored as the OSRS wiki lists them rather than derived from the base,
 * because the game doesn't round consistently (45 becomes 67, but 67 becomes 101). Supply drops
 * have no perfect quantity and are left out. Keyed by item name, which a noted drop shares with
 * the item it represents.
 */
public final class PerfectKillLoot
{
	private static final Map<Boss, Map<String, int[]>> TABLES = new EnumMap<>(Boss.class);

	static
	{
		Map<String, int[]> vardorvis = table(Boss.VARDORVIS);
		drop(vardorvis, "Pure essence", 120, 180);
		drop(vardorvis, "Iron ore", 38, 57);
		drop(vardorvis, "Silver ore", 38, 57);
		drop(vardorvis, "Coal", 130, 195);
		drop(vardorvis, "Mithril ore", 38, 57);
		drop(vardorvis, "Adamantite ore", 45, 67);
		drop(vardorvis, "Runite ore", 18, 27);
		drop(vardorvis, "Sapphire", 17, 25);
		drop(vardorvis, "Emerald", 17, 25);
		drop(vardorvis, "Ruby", 17, 25);
		drop(vardorvis, "Uncut ruby", 25, 37);
		drop(vardorvis, "Uncut diamond", 25, 37);
		drop(vardorvis, "Rune javelin tips", 36, 54);
		drop(vardorvis, "Dragon javelin tips", 36, 54);
		drop(vardorvis, "Dragon dart tip", 100, 150);
		drop(vardorvis, "Raw shark", 200, 300);
		drop(vardorvis, "Bronze javelin", 42, 63);
		drop(vardorvis, "Mithril javelin", 42, 63);
		drop(vardorvis, "Adamant javelin", 42, 63);
		drop(vardorvis, "Onyx bolts (e)", 60, 90);
		drop(vardorvis, "Mind rune", 120, 180);
		drop(vardorvis, "Fire rune", 120, 180);
		drop(vardorvis, "Lava rune", 200, 300);
		drop(vardorvis, "Blood rune", 200, 300);
		drop(vardorvis, "Soul rune", 400, 600);

		Map<String, int[]> leviathan = table(Boss.LEVIATHAN);
		drop(leviathan, "Aether catalyst", 1500, 2250);
		drop(leviathan, "Iron ore", 57, 85);
		drop(leviathan, "Silver ore", 57, 85);
		drop(leviathan, "Coal", 195, 292);
		drop(leviathan, "Gold ore", 67, 101);
		drop(leviathan, "Adamantite ore", 57, 85);
		drop(leviathan, "Runite ore", 27, 40);
		drop(leviathan, "Sapphire", 25, 38);
		drop(leviathan, "Emerald", 25, 38);
		drop(leviathan, "Ruby", 25, 38);
		drop(leviathan, "Uncut ruby", 37, 56);
		drop(leviathan, "Uncut diamond", 37, 56);
		drop(leviathan, "Dragon javelin tips", 54, 81);
		drop(leviathan, "Dragon bolts (unf)", 150, 225);
		drop(leviathan, "Onyx bolt tips", 90, 135);
		drop(leviathan, "Raw manta ray", 180, 270);
		drop(leviathan, "Anglerfish", 4, 6);
		drop(leviathan, "Bronze arrow", 63, 94);
		drop(leviathan, "Mithril arrow", 63, 94);
		drop(leviathan, "Adamant arrow", 63, 94);
		drop(leviathan, "Rune arrow", 54, 81);
		drop(leviathan, "Body rune", 180, 270);
		drop(leviathan, "Earth rune", 180, 270);
		drop(leviathan, "Smoke rune", 300, 450);
		drop(leviathan, "Soul rune", 600, 900);

		Map<String, int[]> whisperer = table(Boss.WHISPERER);
		drop(whisperer, "Bronze longsword", 16, 24);
		drop(whisperer, "Mithril longsword", 7, 10);
		drop(whisperer, "Adamant longsword", 9, 14);
		drop(whisperer, "Battlestaff", 70, 105);
		drop(whisperer, "Dragon plateskirt", 7, 10);
		drop(whisperer, "Pure essence", 280, 420);
		drop(whisperer, "Iron ore", 88, 133);
		drop(whisperer, "Coal", 303, 455);
		drop(whisperer, "Gold ore", 88, 133);
		drop(whisperer, "Mithril ore", 88, 133);
		drop(whisperer, "Adamantite ore", 105, 157);
		drop(whisperer, "Runite ore", 42, 63);
		drop(whisperer, "Sapphire", 39, 59);
		drop(whisperer, "Emerald", 39, 59);
		drop(whisperer, "Ruby", 39, 59);
		drop(whisperer, "Uncut ruby", 58, 87);
		drop(whisperer, "Uncut diamond", 58, 87);
		drop(whisperer, "Dragon javelin tips", 84, 126);
		drop(whisperer, "Runite bolts (unf)", 84, 126);
		drop(whisperer, "Raw monkfish", 700, 1050);
		drop(whisperer, "Water rune", 280, 420);
		drop(whisperer, "Steam rune", 466, 700);
		drop(whisperer, "Chaos rune", 140, 210);
		drop(whisperer, "Death rune", 466, 700);
		drop(whisperer, "Soul rune", 933, 1400);

		Map<String, int[]> duke = table(Boss.DUKE_SUCELLUS);
		drop(duke, "Bronze chainbody", 11, 17);
		drop(duke, "Mithril chainbody", 5, 7);
		drop(duke, "Adamant chainbody", 6, 10);
		drop(duke, "Dragon platelegs", 5, 7);
		drop(duke, "Pure essence", 200, 300);
		drop(duke, "Iron ore", 63, 95);
		drop(duke, "Coal", 216, 325);
		drop(duke, "Mithril ore", 33, 50);
		drop(duke, "Adamantite ore", 75, 112);
		drop(duke, "Runite ore", 30, 45);
		drop(duke, "Sapphire", 28, 42);
		drop(duke, "Emerald", 28, 42);
		drop(duke, "Ruby", 28, 42);
		drop(duke, "Uncut ruby", 41, 62);
		drop(duke, "Uncut diamond", 41, 62);
		drop(duke, "Bronze bar", 63, 95);
		drop(duke, "Dragon arrowtips", 166, 250);
		drop(duke, "Rune javelin tips", 60, 90);
		drop(duke, "Dragon javelin tips", 60, 90);
		drop(duke, "Raw sea turtle", 200, 300);
		drop(duke, "Air rune", 200, 300);
		drop(duke, "Mist rune", 100, 150);
		drop(duke, "Chaos rune", 100, 150);
		drop(duke, "Soul rune", 666, 1000);
	}

	private PerfectKillLoot()
	{
	}

	private static Map<String, int[]> table(Boss boss)
	{
		Map<String, int[]> table = new HashMap<>();
		TABLES.put(boss, table);
		return table;
	}

	private static void drop(Map<String, int[]> table, String itemName, int base, int perfect)
	{
		table.put(itemName.toLowerCase(), new int[]{base, perfect});
	}

	public static boolean hasPerfectKillBonus(Boss boss)
	{
		return boss != null && TABLES.containsKey(boss);
	}

	/**
	 * @return how much of this drop is perfect-kill bonus: the difference from the normal quantity
	 * when {@code quantity} is the item's perfect-kill quantity, otherwise 0.
	 */
	public static int bonusQuantity(Boss boss, String itemName, int quantity)
	{
		if (itemName == null)
		{
			return 0;
		}
		int[] quantities = TABLES.getOrDefault(boss, Collections.emptyMap()).get(itemName.toLowerCase());
		if (quantities == null || quantity != quantities[1])
		{
			return 0;
		}
		return quantities[1] - quantities[0];
	}
}
