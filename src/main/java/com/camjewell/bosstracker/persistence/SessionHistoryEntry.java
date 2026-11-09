package com.camjewell.bosstracker.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A snapshot of one completed session, recorded whenever a session ends, switches to a
 * different boss, or is cut short by logout-triggered timeout. One JSON file per entry under
 * {@code RUNELITE_DIR/boss-tracker/<accountHash>/history/}, browsed via the side panel's History
 * tab.
 */
@Data
@NoArgsConstructor
public class SessionHistoryEntry
{
	private String bossName;
	private long endedAtEpochMilli;
	private int killsThisSession;
	private int sessionDurationSeconds;
	private double killsPerHour;
	private int averageKillTimeSeconds;
	private int fastestKillSeconds;
	private int idleSeconds;

	/**
	 * The session's kill durations added up, without the time between kills. 0 for sessions
	 * saved before this was recorded.
	 */
	private int fightSeconds;

	private boolean onSlayerTask;
	/** A pet dropped during the session. False for entries saved before this was recorded. */
	private boolean petObtained;

	/**
	 * Deaths across the session's clears: the whole team's, and the player's own. Only counted
	 * for bosses that track team deaths; 0 for everything else and for older entries.
	 */
	private int teamDeaths;
	private int ownDeaths;

	private Map<Integer, Integer> lootItemQuantities = new LinkedHashMap<>();

	/**
	 * Oldest first. Empty for entries saved before trips were tracked (Gson leaves the default in
	 * place when the key is missing) and for raids and other single-run encounters.
	 */
	private List<Trip> trips = new ArrayList<>();

	/** The session's minion drops (God Wars bodyguards), kept apart from the boss's loot. */
	private Map<Integer, Integer> minionLootItemQuantities = new LinkedHashMap<>();

	private int minionKills;

	/** The extra loot from the session's perfect kills, kept apart from the normal drops. */
	private Map<Integer, Integer> perfectBonusItemQuantities = new LinkedHashMap<>();

	private int perfectKills;

	/** What was inside the containers opened during the session, kept apart from the drops. */
	private Map<Integer, Integer> cacheLootItemQuantities = new LinkedHashMap<>();

	private int cachesOpened;

	/** The food and potions from the session's supply drops, kept apart from the drops. */
	private Map<Integer, Integer> supplyItemQuantities = new LinkedHashMap<>();

	private int supplyDrops;

	/**
	 * Each item's price when it dropped, so a saved session keeps the value it had at the time
	 * instead of moving with the Grand Exchange. Empty for sessions saved before this was
	 * recorded, which fall back to current prices.
	 */
	private Map<Integer, Long> itemPricesAtDrop = new LinkedHashMap<>();

	/**
	 * The kill count each unique dropped on, oldest first. Empty for sessions saved before this
	 * was recorded, and missing a unique whose kill count wasn't seen.
	 */
	private Map<Integer, List<Integer>> uniqueKillCounts = new LinkedHashMap<>();

	/** The player's own notes, written during the session or edited later from History. */
	private String notes = "";

	/**
	 * Raids only: how the player's uniques were shared, by item ID, as the number of players
	 * each was split between (1 for free for all). Items the player left unset aren't here.
	 */
	private Map<Integer, Integer> uniqueSplits = new LinkedHashMap<>();

	/** Raids only: uniques teammates received and split with the player. */
	private List<TeammateUnique> teammateUniques = new ArrayList<>();
}
