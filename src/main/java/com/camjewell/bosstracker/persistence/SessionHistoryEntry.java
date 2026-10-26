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

	/**
	 * Each item's price when it dropped, so a saved session keeps the value it had at the time
	 * instead of moving with the Grand Exchange. Empty for sessions saved before this was
	 * recorded, which fall back to current prices.
	 */
	private Map<Integer, Long> itemPricesAtDrop = new LinkedHashMap<>();
}
