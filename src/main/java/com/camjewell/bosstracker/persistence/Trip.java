package com.camjewell.bosstracker.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One stretch of kills between restocks: it opens on the first kill after the previous trip
 * ended, and closes on a death, a bank, leaving the boss's area, or the session ending. A
 * session is a list of these. The same class is used live and in saved history, so it holds
 * only Gson-friendly fields.
 */
@Data
@NoArgsConstructor
public class Trip
{
	private long startedAtEpochMilli;

	/** 0 while the trip is still open. */
	private long endedAtEpochMilli;

	private int kills;

	/** Sum of this trip's kill durations, excluding the time between kills. */
	private int fightSeconds;

	private Map<Integer, Integer> lootItemQuantities = new LinkedHashMap<>();

	/** Null while the trip is still open. */
	private TripEndReason endReason;

	public Trip(long startedAtEpochMilli)
	{
		this.startedAtEpochMilli = startedAtEpochMilli;
	}

	public boolean isOpen()
	{
		return endReason == null;
	}
}
