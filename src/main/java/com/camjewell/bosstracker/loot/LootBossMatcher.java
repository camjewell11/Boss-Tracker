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
 * Ambiguous cases (e.g. normal vs. Challenge Mode Chambers, both reported as
 * "Chambers of Xeric") are resolved by checking against whichever {@code Boss} the current
 * session is already tracking, rather than by picking one canonical owner up front.
 *
 * <p>The original plugin's equivalent check ({@code FileReadWriter.bossNameMatcher()}) hardcoded
 * "The Gauntlet" as the loot event name for <em>both</em> the regular and Corrupted Gauntlet,
 * which meant Corrupted Gauntlet loot could never match (its reward chest reports a distinct
 * event name). {@link Boss#CORRUPTED_GAUNTLET} is mapped to "The Corrupted Gauntlet" here
 * instead; verify this string in-game since it can't be confirmed without a live client.
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
		LOOT_ALIASES.put(Boss.GAUNTLET, Collections.singletonList("The Gauntlet"));
		LOOT_ALIASES.put(Boss.CORRUPTED_GAUNTLET, Collections.singletonList("The Corrupted Gauntlet"));
		LOOT_ALIASES.put(Boss.NIGHTMARE, Collections.singletonList("The Nightmare"));
		LOOT_ALIASES.put(Boss.PHOSANIS_NIGHTMARE, Collections.singletonList("The Nightmare"));
		LOOT_ALIASES.put(Boss.ROYAL_TITANS, Arrays.asList("Branda the Fire Queen", "Eldric the Ice King"));
		LOOT_ALIASES.put(Boss.COLOSSEUM, Collections.singletonList("Fortis Colosseum"));
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
