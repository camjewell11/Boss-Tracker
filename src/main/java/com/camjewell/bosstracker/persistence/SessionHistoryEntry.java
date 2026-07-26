package com.camjewell.bosstracker.persistence;

import java.util.LinkedHashMap;
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
	private Map<Integer, Integer> lootItemQuantities = new LinkedHashMap<>();
}
