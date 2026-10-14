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
	private Map<Integer, Integer> lootItemQuantities = new LinkedHashMap<>();

	/**
	 * Oldest first. Empty for entries saved before trips were tracked (Gson leaves the default in
	 * place when the key is missing) and for raids and other single-run encounters.
	 */
	private List<Trip> trips = new ArrayList<>();

	/** The session's minion drops (God Wars bodyguards), kept apart from the boss's loot. */
	private Map<Integer, Integer> minionLootItemQuantities = new LinkedHashMap<>();

	private int minionKills;
}
