package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Which drops count as uniques: the boss's pet, anything worth at least {@link #MIN_UNIT_GP} each,
 * and raid cosmetics whatever their price. Shared by the History hover text and dry streaks so
 * the two always agree.
 */
public final class NotableDrops
{
	/** An item worth at least this much each is notable, so a big stack of cheap items isn't. */
	public static final long MIN_UNIT_GP = 1_000_000;

	/**
	 * Raid cosmetics: several are worth well under the threshold or can't be traded at all, but
	 * are still rare uniques. In-game names noted, since several gameval names don't resemble them.
	 */
	private static final Set<Integer> RAID_COSMETICS = new HashSet<>(Arrays.asList(
		ItemID.ANCESTRAL_ROBES_TWISTED_KIT,     // Twisted ancestral colour kit (CoX)
		ItemID.RAIDS_CHALLENGE_MORPH,           // Metamorphic dust (CoX)
		ItemID.TOB_HARDMODE_KIT,                // Holy ornament kit (ToB)
		ItemID.TOB_HARDMODE_KIT_BLOOD,          // Sanguine ornament kit (ToB)
		ItemID.TOB_HARDMODE_DUST,               // Sanguine dust (ToB)
		ItemID.OSMUMTENS_FANG_ORNAMENT_KIT,     // Cursed phalanx (ToA)
		ItemID.ELIDINIS_WARD_ORNAMENT_KIT,      // Menaphite ornament kit (ToA)
		ItemID.THREAD_OF_ELIDINIS,              // Thread of Elidinis (ToA)
		ItemID.EYE_OF_THE_CORRUPTOR,            // Eye of the corruptor (ToA)
		ItemID.JEWEL_OF_THE_SUN,                // Jewel of the sun (ToA)
		ItemID.JEWEL_OF_AMASCUT,                // Jewel of amascut (ToA)
		ItemID.TOA_PET_MORPH_AKKHA,             // Remnant of Akkha (ToA)
		ItemID.TOA_PET_MORPH_BABA,              // Remnant of Ba-Ba (ToA)
		ItemID.TOA_PET_MORPH_KEPHRI,            // Remnant of Kephri (ToA)
		ItemID.TOA_PET_MORPH_ZEBAK,             // Remnant of Zebak (ToA)
		ItemID.TOA_PET_MORPH_WARDENS));         // Ancient remnant (ToA)

	private NotableDrops()
	{
	}

	public static boolean isPet(Boss boss, int itemId)
	{
		return boss != null && boss.getPetItemId() == itemId;
	}

	/**
	 * @param unitPrice the item's price each, as it was valued for this drop
	 */
	public static boolean isNotable(Boss boss, int itemId, long unitPrice)
	{
		return isPet(boss, itemId) || unitPrice >= MIN_UNIT_GP || RAID_COSMETICS.contains(itemId);
	}
}
