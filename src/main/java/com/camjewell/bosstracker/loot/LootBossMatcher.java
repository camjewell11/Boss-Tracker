package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves whether a {@code LootReceived} event's name belongs to a given tracked {@link Boss}.
 * Group-content encounters (raids, Gauntlet, Nightmare) report loot under an "event name" that
 * differs from any {@code Boss}'s own display/NPC name, so those need an explicit alias here.
 *
 * <p>Multi-NPC encounters need one too whenever the NPC that actually dies is named differently
 * from the boss: RuneLite posts NPC loot under {@code Text.removeTags(npc.getName())}, so the
 * Grotesque Guardians drop as "Dusk", not as "Grotesque Guardians". Kill tracking is unaffected
 * (it reads the kill-count chat message), which makes this fail quietly - kills accrue while the
 * loot silently goes nowhere.
 * Ambiguous cases (e.g. normal vs. Challenge Mode Chambers, both reported as
 * "Chambers of Xeric") are resolved by checking against whichever {@code Boss} the current
 * session is already tracking, rather than by picking one canonical owner up front.
 *
 * <p>The Gauntlet's reward arrives as NPC loot from the Hunllef that died ("Crystalline Hunllef"
 * or "Corrupted Hunllef"), confirmed in-game. Older RuneLite versions reported both variants as
 * the event "The Gauntlet". All three belong to the single {@link Boss#GAUNTLET}.
 *
 * <p>{@link Boss#COLOSSEUM}'s alias ("Fortis Colosseum") and the assumption that
 * {@link Boss#ARAXXOR}/{@link Boss#MAGGOT_KING}/{@link Boss#BARROWS} need no alias at all were
 * confirmed by decompiling RuneLite's own {@code loottracker} plugin class and reading its
 * literal event-name table directly ("Barrows", "Maggot King", "Fortis Colosseum", etc. all
 * appear there verbatim) rather than guessed — the same class of bug as the Corrupted Gauntlet
 * fix above, since a boss enum's own display name matching its NPC name doesn't guarantee it
 * matches its loot *event* name for chest/corpse-interaction encounters.
 */
public final class LootBossMatcher
{
	private static final Map<Boss, List<String>> LOOT_ALIASES = new EnumMap<>(Boss.class);

	static
	{
		LOOT_ALIASES.put(Boss.CHAMBERS, Collections.singletonList("Chambers of Xeric"));
		LOOT_ALIASES.put(Boss.CM_CHAMBERS, Collections.singletonList("Chambers of Xeric"));
		LOOT_ALIASES.put(Boss.TOA_NORMAL, Collections.singletonList("Tombs of Amascut"));
		LOOT_ALIASES.put(Boss.TOA_EXPERT, Collections.singletonList("Tombs of Amascut"));
		LOOT_ALIASES.put(Boss.THEATRE_OF_BLOOD, Collections.singletonList("Theatre of Blood"));
		LOOT_ALIASES.put(Boss.THEATRE_OF_BLOOD_HM, Collections.singletonList("Theatre of Blood"));
		LOOT_ALIASES.put(Boss.GAUNTLET, Arrays.asList("Crystalline Hunllef", "Corrupted Hunllef", "The Gauntlet"));
		LOOT_ALIASES.put(Boss.NIGHTMARE, Collections.singletonList("The Nightmare"));
		LOOT_ALIASES.put(Boss.PHOSANIS_NIGHTMARE, Collections.singletonList("The Nightmare"));
		LOOT_ALIASES.put(Boss.ROYAL_TITANS, Arrays.asList("Branda the Fire Queen", "Eldric the Ice King"));
		LOOT_ALIASES.put(Boss.COLOSSEUM, Collections.singletonList("Fortis Colosseum"));
		LOOT_ALIASES.put(Boss.GROTESQUE_GUARDIANS, Arrays.asList("Dusk", "Dawn"));
		LOOT_ALIASES.put(Boss.HUEYCOATL, Collections.singletonList("The Hueycoatl"));
	}

	/**
	 * The God Wars generals' bodyguards, which share their general's drop table and are killed
	 * alongside it. Names match the loot event's NPC name: all but Zilyana's appear verbatim in
	 * RuneLite's slayer task targets, and hers (which count toward no task) match the OSRS wiki.
	 */
	private static final Map<Boss, List<String>> MINIONS = new EnumMap<>(Boss.class);

	static
	{
		MINIONS.put(Boss.GENERAL_GRAARDOR, Arrays.asList("Sergeant Strongstack", "Sergeant Steelwill", "Sergeant Grimspike"));
		MINIONS.put(Boss.COMMANDER_ZILYANA, Arrays.asList("Starlight", "Growler", "Bree"));
		MINIONS.put(Boss.KREEARRA, Arrays.asList("Wingman Skree", "Flockleader Geerin", "Flight Kilisa"));
		MINIONS.put(Boss.KRIL_TSUTSAROTH, Arrays.asList("Tstanon Karlak", "Zakl'n Gritch", "Balfrug Kreeyath"));
	}

	public static boolean hasMinions(Boss boss)
	{
		return boss != null && MINIONS.containsKey(boss);
	}

	/**
	 * @return true if the loot event came from one of {@code boss}'s minions rather than the boss.
	 */
	public static boolean matchesMinionLootEvent(Boss boss, String lootEventName)
	{
		if (boss == null || lootEventName == null)
		{
			return false;
		}
		for (String minion : MINIONS.getOrDefault(boss, Collections.emptyList()))
		{
			if (minion.equalsIgnoreCase(lootEventName))
			{
				return true;
			}
		}
		return false;
	}

	private LootBossMatcher()
	{
	}

	public static boolean matchesLootEvent(Boss boss, String lootEventName)
	{
		if (boss == null || lootEventName == null)
		{
			return false;
		}

		if (boss.getBossName().equalsIgnoreCase(lootEventName))
		{
			return true;
		}

		for (String npcName : boss.getNpcNames())
		{
			if (npcName.equalsIgnoreCase(lootEventName))
			{
				return true;
			}
		}

		for (String alias : LOOT_ALIASES.getOrDefault(boss, Collections.emptyList()))
		{
			if (alias.equalsIgnoreCase(lootEventName))
			{
				return true;
			}
		}

		return false;
	}
}
