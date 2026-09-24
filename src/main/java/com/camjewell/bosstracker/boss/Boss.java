package com.camjewell.bosstracker.boss;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * The data-driven registry of every tracked boss. Replaces the original plugin's scattered
 * per-boss switch statements: timing-family membership, multi-phase handling, and alternate
 * NPC names are all declared here instead of hardcoded elsewhere.
 */
@Getter
public enum Boss
{
	// ---- SELF_REPORTED: the boss/raid prints its own completion message in chat ----
	PHANTOM_MUSPAH("Phantom Muspah", ItemID.MUSPAHPET, "Your Phantom Muspah kill count is:", "phantom", "muspah", "grumbler"),
	ZULRAH("Zulrah", ItemID.SNAKEPET_BLUE, "Your Zulrah kill count is:"),
	AMOXLIATL("Amoxliatl", ItemID.AMOXLIATLPET, "Your Amoxliatl kill count is:", "amox", "moxi"),
	HUEYCOATL("Hueycoatl", ItemID.HUEYPET, "Your Hueycoatl kill count is:", "huey"),
	ARAXXOR("Araxxor", ItemID.ARAXXORPET, "Your Araxxor kill count is:"),
	ROYAL_TITANS("Royal Titans", ItemID.RTBRANDAPET, "Your Royal Titans kill count is:"),
	YAMA("Yama", ItemID.YAMAPET, "Your Yama success count is:"),
	SHELLBANE_GRYPHON("Shellbane Gryphon", ItemID.GRYPHONBOSSPET, "Your Shellbane Gryphon kill count is:", "gull", "gryphon", "gulliver"),
	MAGGOT_KING("Maggot King", ItemID.MAGGOTKINGPET, "Your Maggot King kill count is:", "maggot"),
	BRUTUS("Brutus", ItemID.COWBOSSPET, "Your Brutus kill count is:", "cow", "cow boss"),
	DUKE_SUCELLUS("Duke Sucellus", ItemID.DUKESUCELLUSPET, "Your Duke Sucellus kill count is:"),
	VARDORVIS("Vardorvis", ItemID.VARDORVISPET, "Your Vardorvis kill count is:"),
	WHISPERER("The Whisperer", ItemID.WHISPERERPET, "Your Whisperer kill count is:"),
	LEVIATHAN("The Leviathan", ItemID.LEVIATHANPET, "Your Leviathan kill count is:"),
	CHAMBERS("Chambers", ItemID.OLMPET, "Your completed Chambers of Xeric count is:", "chambers of xeric", "cox"),
	CM_CHAMBERS("CM Chambers", ItemID.OLMPET, "Your completed Chambers of Xeric Challenge Mode count is:", "cm", "cm cox", "challange mode", "challange mode chambers of xeric"),
	TOA_NORMAL("TOA Normal", ItemID.WARDENPET_TUMEKEN, "Your completed Tombs of Amascut count is:", "toa"),
	TOA_EXPERT("TOA Expert", ItemID.WARDENPET_ELIDINIS, "Your completed Tombs of Amascut: Expert Mode count is:"),
	COLOSSEUM("Colosseum", ItemID.SOLHEREDITPET, "Your Sol Heredit kill count is:", "fortis colosseum", "colo"),
	GAUNTLET("Gauntlet", ItemID.GAUNTLETPET, "Your Gauntlet completion count is:"),
	CORRUPTED_GAUNTLET("Corrupted Gauntlet", ItemID.GAUNTLETPET_CORRUPT, "Your Corrupted Gauntlet completion count is:", "c gauntlet"),
	THEATRE_OF_BLOOD("Theatre of Blood", ItemID.VERZIKPET, "Your completed Theatre of Blood count is:", "theatre", "tob"),
	THEATRE_OF_BLOOD_HM("Theatre of Blood HM", ItemID.SOTETSEGPET, "Your completed Theatre of Blood: Hard Mode count is:", "theatre hm", "tob hm", "hm"),
	VORKATH("Vorkath", ItemID.VORKATHPET, "Your Vorkath kill count is:", "vork"),
	ALCHEMICAL_HYDRA("Alchemical Hydra", ItemID.HYDRAPET_ELECTRIC, "Your Alchemical Hydra kill count is:", "hydra"),
	GROTESQUE_GUARDIANS("Grotesque Guardians", ItemID.DUSKPET, "Your Grotesque Guardians kill count is:", "garg boss", "dusk", "dawn", "ggs"),
	NIGHTMARE("Nightmare", ItemID.NIGHTMAREPET, "Your Nightmare kill count is:"),
	PHOSANIS_NIGHTMARE("Phosani's Nightmare", ItemID.NIGHTMAREPET_PARASITE, "Your Phosani's Nightmare kill count is:", "pnm"),
	TZTOK_JAD("TzTok-Jad", ItemID.JAD_PET, "Your TzTok-Jad kill count is:", "jad"),
	TZKAL_ZUK("TzKal-Zuk", ItemID.INFERNOPET_ZUK, "Your TzKal-Zuk kill count is:", "zuk"),
	NEX("Nex", ItemID.NEXPET, "Your Nex kill count is:"),

	// ---- HITSPLAT: no completion message; timed from first hitsplat (or family clock) to death ----
	GIANT_MOLE("Giant Mole", ItemID.MOLEPET, 220, "Your Giant Mole kill count is:", "mole"),
	SARACHNIS("Sarachnis", ItemID.SARACHNISPET, 220, "Your Sarachnis kill count is:"),
	ABYSSAL_SIRE("Abyssal Sire", ItemID.ABYSSALSIRE_PET, KillTiming.HITSPLAT, 300,
		Collections.singletonList("Your Abyssal Sire kill count is:"), TimingFamily.NONE, false,
		"The Sire has been disorientated temporarily.", Collections.singletonList("Abyssal Sire"),
		Arrays.asList("sire")),
	COMMANDER_ZILYANA("Commander Zilyana", ItemID.SARADOMINPET, 120, "Your Commander Zilyana kill count is:", "sara", "zilly", "zilyana"),
	GENERAL_GRAARDOR("General Graardor", ItemID.BANDOSPET, 120, "Your General Graardor kill count is:", "bandos", "graardor"),
	KREEARRA("Kree'arra", ItemID.ARMADYLPET, 120, "Your Kree'arra kill count is:", "arma", "kree", "kreearra"),
	KRIL_TSUTSAROTH("K'ril Tsutsaroth", ItemID.ZAMORAKPET, 120, "Your K'ril Tsutsaroth kill count is:", "zammy", "kril", "kril tsutsaroth"),
	KRAKEN("Kraken", ItemID.KRAKENPET, 220, "Your Kraken kill count is:", TimingFamily.KRAKEN,
		Arrays.asList("Kraken", "Enormous Tentacle")),
	THERMY("Thermy", ItemID.SMOKEPET, 220, "Your Thermonuclear Smoke Devil kill count is:", TimingFamily.NONE,
		Arrays.asList("Thermy", "Thermonuclear smoke devil"), "thermonuclear smoke devil"),
	CERBERUS("Cerberus", ItemID.HELL_PET, 150, "Your Cerberus kill count is:", "cerb"),
	KING_BLACK_DRAGON("King Black Dragon", ItemID.KBDPET, 120, "Your King Black Dragon kill count is:", "kbd"),
	SCORPIA("Scorpia", ItemID.SCORPIA_PET, 250, "Your Scorpia kill count is:"),
	CHAOS_FANATIC("Chaos Fanatic", ItemID.CABBAGE, 250, "Your Chaos Fanatic kill count is:"),
	CRAZY_ARCHAEOLOGIST("Crazy archaeologist", ItemID.FEDORA, 250, "Your Crazy Archaeologist kill count is:"),
	CHAOS_ELEMENTAL("Chaos Elemental", ItemID.CHAOSELEPET, 250, "Your Chaos Elemental kill count is:", "chaos elly"),
	CALLISTO("Callisto", ItemID.CALLISTO_PET, 250, "Your Callisto kill count is:"),
	ARTIO("Artio", ItemID.CALLISTO_PET_LEGACY, 250, "Your Artio kill count is:"),
	VETION("Vet'ion", ItemID.VETION_PET2, 350, "Your Vet'ion kill count is:", TimingFamily.NONE,
		true, null, Arrays.asList("Vet'ion", "Vet'ion Reborn"), "vetion"),
	CALVARION("Calvar'ion", ItemID.VETION_PET_LEGACY, 350, "Your Calvar'ion kill count is:"),
	VENENATIS("Venenatis", ItemID.VENENATIS_PET, 250, "Your Venenatis kill count is:"),
	SPINDEL("Spindel", ItemID.VENENATIS_PET_LEGACY, 250, "Your Spindel kill count is:"),
	SCURRIUS("Scurrius", ItemID.SCURRIUSPET, 250, "Your Scurrius kill count is:"),
	BARROWS("Barrows", ItemID.BARROWS_VERAC_HEAD, 1500, "Your Barrows chest count is:", TimingFamily.BARROWS,
		Arrays.asList("Barrows", "Verac the Defiled", "Torag the Corrupted", "Karil the Tainted",
			"Guthan the Infested", "Dharok the Wretched", "Ahrim the Blighted")),
	DERANGED_ARCHAEOLOGIST("Deranged archaeologist", ItemID.FOSSIL_RARE_UNID, 120, "Your Deranged Archaeologist kill count is:"),
	KALPHITE_QUEEN("Kalphite Queen", ItemID.KQPET_WALKING, 400, "Your Kalphite Queen kill count is:", TimingFamily.NONE,
		true, null, Collections.singletonList("Kalphite Queen"), "kq"),
	CORPOREAL_BEAST("Corporeal Beast", ItemID.CORPPET, 400, "Your Corporeal Beast kill count is:", "corp"),
	DAGANNOTH_PRIME("Dagannoth Prime", ItemID.PRIMEPET, 15, "Your Dagannoth Prime kill count is:", TimingFamily.DAGANNOTH_KINGS),
	DAGANNOTH_REX("Dagannoth Rex", ItemID.REXPET, 15, "Your Dagannoth Rex kill count is:", TimingFamily.DAGANNOTH_KINGS),
	DAGANNOTH_SUPREME("Dagannoth Supreme", ItemID.SUPREMEPET, 15, "Your Dagannoth Supreme kill count is:", TimingFamily.DAGANNOTH_KINGS),
	DAGANNOTH_KINGS("Dagannoth Kings", ItemID.REXPET, KillTiming.HITSPLAT, 15,
		Arrays.asList("Your Dagannoth Prime kill count is:", "Your Dagannoth Rex kill count is:", "Your Dagannoth Supreme kill count is:"),
		TimingFamily.DAGANNOTH_KINGS, false, null,
		Arrays.asList("Dagannoth Prime", "Dagannoth Rex", "Dagannoth Supreme"), Collections.emptyList());

	private final String bossName;
	private final int iconItemId;
	private final KillTiming timing;
	private final int attackTimeoutTicks;
	private final List<String> kcChatIdentifiers;
	private final TimingFamily family;
	private final boolean multiPhase;
	private final String earlyStartTrigger;
	private final List<String> npcNames;
	private final List<String> aliases;

	/**
	 * Full constructor; every other constructor below delegates here.
	 */
	Boss(String bossName, int iconItemId, KillTiming timing, int attackTimeoutTicks,
		List<String> kcChatIdentifiers, TimingFamily family, boolean multiPhase,
		String earlyStartTrigger, List<String> npcNames, List<String> aliases)
	{
		this.bossName = bossName;
		this.iconItemId = iconItemId;
		this.timing = timing;
		this.attackTimeoutTicks = attackTimeoutTicks;
		this.kcChatIdentifiers = kcChatIdentifiers;
		this.family = family;
		this.multiPhase = multiPhase;
		this.earlyStartTrigger = earlyStartTrigger;
		this.npcNames = npcNames;
		this.aliases = aliases;
	}

	/**
	 * SELF_REPORTED boss: the encounter announces its own completion message.
	 */
	Boss(String bossName, int iconItemId, String kcMessage, String... aliases)
	{
		this(bossName, iconItemId, KillTiming.SELF_REPORTED, 0, Collections.singletonList(kcMessage),
			TimingFamily.NONE, false, null, Collections.singletonList(bossName), Arrays.asList(aliases));
	}

	/**
	 * HITSPLAT boss with no timing family, single NPC name.
	 */
	Boss(String bossName, int iconItemId, int attackTimeoutTicks, String kcMessage, String... aliases)
	{
		this(bossName, iconItemId, KillTiming.HITSPLAT, attackTimeoutTicks, Collections.singletonList(kcMessage),
			TimingFamily.NONE, false, null, Collections.singletonList(bossName), Arrays.asList(aliases));
	}

	/**
	 * HITSPLAT boss belonging to a timing family, with a default single NPC name.
	 */
	Boss(String bossName, int iconItemId, int attackTimeoutTicks, String kcMessage, TimingFamily family, String... aliases)
	{
		this(bossName, iconItemId, KillTiming.HITSPLAT, attackTimeoutTicks, Collections.singletonList(kcMessage),
			family, false, null, Collections.singletonList(bossName), Arrays.asList(aliases));
	}

	/**
	 * HITSPLAT boss belonging to a timing family, with multiple recognized NPC names
	 * (e.g. Kraken/Enormous Tentacle, all 6 Barrows brothers).
	 */
	Boss(String bossName, int iconItemId, int attackTimeoutTicks, String kcMessage, TimingFamily family,
		List<String> npcNames, String... aliases)
	{
		this(bossName, iconItemId, KillTiming.HITSPLAT, attackTimeoutTicks, Collections.singletonList(kcMessage),
			family, false, null, npcNames, Arrays.asList(aliases));
	}

	/**
	 * HITSPLAT boss that can visually "die" mid-fight without the encounter actually ending
	 * (Kalphite Queen's phase change, Vet'ion's Reborn phase), optionally under multiple NPC names.
	 */
	Boss(String bossName, int iconItemId, int attackTimeoutTicks, String kcMessage, TimingFamily family,
		boolean multiPhase, String earlyStartTrigger, List<String> npcNames, String... aliases)
	{
		this(bossName, iconItemId, KillTiming.HITSPLAT, attackTimeoutTicks, Collections.singletonList(kcMessage),
			family, multiPhase, earlyStartTrigger, npcNames, Arrays.asList(aliases));
	}

	private static final Map<String, Boss> BY_NAME = new HashMap<>();
	private static final Map<String, Boss> BY_NPC_NAME = new HashMap<>();
	private static final Map<String, Boss> BY_ALIAS = new HashMap<>();

	/**
	 * Slayer task name (as the Slayer plugin reports it) to every tracked boss whose kills count
	 * toward that task. Deliberately not derived from {@link #bossName}/{@link #aliases}: a task's
	 * name frequently differs from the boss it credits ("Hellhounds" credits Cerberus, "Hydras"
	 * credits the Alchemical Hydra), and one task can credit several bosses ("Spiders" credits
	 * Sarachnis, Venenatis, Spindel and Araxxor).
	 */
	private static final Map<String, Set<Boss>> BY_SLAYER_TASK = new HashMap<>();

	static
	{
		for (Boss boss : values())
		{
			BY_NAME.put(boss.bossName.toLowerCase(), boss);
			for (String npcName : boss.npcNames)
			{
				BY_NPC_NAME.put(npcName.toLowerCase(), boss);
			}
			for (String alias : boss.aliases)
			{
				BY_ALIAS.put(alias.toLowerCase(), boss);
			}
		}

		// Boss tasks: the assignment names the boss directly.
		slayerTask("The Phantom Muspah", PHANTOM_MUSPAH);
		slayerTask("Zulrah", ZULRAH);
		slayerTask("Araxxor", ARAXXOR);
		slayerTask("The Shellbane Gryphon", SHELLBANE_GRYPHON);
		slayerTask("The Maggot King", MAGGOT_KING);
		slayerTask("Duke Sucellus", DUKE_SUCELLUS);
		slayerTask("Vardorvis", VARDORVIS);
		slayerTask("The Whisperer", WHISPERER);
		slayerTask("The Leviathan", LEVIATHAN);
		slayerTask("Vorkath", VORKATH);
		slayerTask("The Alchemical Hydra", ALCHEMICAL_HYDRA);
		slayerTask("The Grotesque Guardians", GROTESQUE_GUARDIANS);
		slayerTask("TzTok-Jad", TZTOK_JAD);
		slayerTask("TzKal-Zuk", TZKAL_ZUK);
		slayerTask("The Giant Mole", GIANT_MOLE);
		slayerTask("Sarachnis", SARACHNIS);
		slayerTask("The Abyssal Sire", ABYSSAL_SIRE);
		slayerTask("Commander Zilyana", COMMANDER_ZILYANA);
		slayerTask("General Graardor", GENERAL_GRAARDOR);
		slayerTask("Kree'arra", KREEARRA);
		slayerTask("K'ril Tsutsaroth", KRIL_TSUTSAROTH);
		slayerTask("The Cave Kraken Boss", KRAKEN);
		slayerTask("The Thermonuclear Smoke Devil", THERMY);
		slayerTask("Cerberus", CERBERUS);
		slayerTask("The King Black Dragon", KING_BLACK_DRAGON);
		slayerTask("Scorpia", SCORPIA);
		slayerTask("The Chaos Fanatic", CHAOS_FANATIC);
		slayerTask("Crazy Archaeologists", CRAZY_ARCHAEOLOGIST);
		slayerTask("The Chaos Elemental", CHAOS_ELEMENTAL);
		slayerTask("Callisto", CALLISTO, ARTIO);
		slayerTask("Vet'ion", VETION, CALVARION);
		slayerTask("Venenatis", VENENATIS, SPINDEL);
		slayerTask("Barrows Brothers", BARROWS);
		slayerTask("Deranged Archaeologist", DERANGED_ARCHAEOLOGIST);
		slayerTask("The Kalphite Queen", KALPHITE_QUEEN);
		slayerTask("Dagannoth Kings", DAGANNOTH_KINGS, DAGANNOTH_PRIME, DAGANNOTH_REX, DAGANNOTH_SUPREME);

		// Regular tasks that the boss also counts toward.
		slayerTask("Hellhounds", CERBERUS);
		slayerTask("Hydras", ALCHEMICAL_HYDRA);
		slayerTask("Gargoyles", GROTESQUE_GUARDIANS);
		slayerTask("Abyssal demons", ABYSSAL_SIRE);
		slayerTask("Cave kraken", KRAKEN);
		slayerTask("Smoke devils", THERMY);
		slayerTask("Black dragons", KING_BLACK_DRAGON);
		slayerTask("Blue dragons", VORKATH);
		slayerTask("Zombies", VORKATH);
		slayerTask("Aviansies", KREEARRA);
		slayerTask("Greater demons", KRIL_TSUTSAROTH);
		slayerTask("Kalphites", KALPHITE_QUEEN);
		slayerTask("Dagannoth", DAGANNOTH_KINGS, DAGANNOTH_PRIME, DAGANNOTH_REX, DAGANNOTH_SUPREME);
		slayerTask("Scorpions", SCORPIA);
		slayerTask("Bears", CALLISTO, ARTIO);
		slayerTask("Skeletons", VETION, CALVARION);
		slayerTask("Spiders", SARACHNIS, VENENATIS, SPINDEL, ARAXXOR);
		slayerTask("Araxytes", ARAXXOR);
		slayerTask("Rats", SCURRIUS);
		slayerTask("Cows", BRUTUS);
		slayerTask("Gryphons", SHELLBANE_GRYPHON);
		slayerTask("Lesser Nagua", AMOXLIATL);
		slayerTask("Tzhaar", TZTOK_JAD, TZKAL_ZUK);
		slayerTask("Fire giants", ROYAL_TITANS);
		slayerTask("Ice giants", ROYAL_TITANS);
	}

	private static void slayerTask(String taskName, Boss... bosses)
	{
		BY_SLAYER_TASK.computeIfAbsent(taskName.toLowerCase(), k -> EnumSet.noneOf(Boss.class))
			.addAll(Arrays.asList(bosses));
	}

	/**
	 * @return true if killing this boss makes progress on a Slayer task with this name. Matching is
	 * case-insensitive; an unrecognized or null task name returns false.
	 */
	public boolean countsForSlayerTask(String taskName)
	{
		if (taskName == null || taskName.isEmpty())
		{
			return false;
		}
		Set<Boss> bosses = BY_SLAYER_TASK.get(taskName.toLowerCase());
		return bosses != null && bosses.contains(this);
	}

	/**
	 * @return the Boss with this exact display name, or null if none match.
	 */
	public static Boss byName(String name)
	{
		return BY_NAME.get(name.toLowerCase());
	}

	/**
	 * @return the Boss whose npcNames contains this in-game NPC name, or null if none match.
	 */
	public static Boss byNpcName(String npcName)
	{
		return BY_NPC_NAME.get(npcName.toLowerCase());
	}

	/**
	 * Resolves a search term to a Boss by exact display name first, then by alias.
	 *
	 * @return the matching Boss, or null if none match.
	 */
	public static Boss byNameOrAlias(String query)
	{
		Boss byName = byName(query);
		if (byName != null)
		{
			return byName;
		}
		return BY_ALIAS.get(query.toLowerCase());
	}
}
