package com.camjewell.bosstracker.session;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the in-progress kill-timer start for HITSPLAT bosses, keyed so that bosses sharing a
 * {@link com.camjewell.bosstracker.boss.TimingFamily} (Barrows, Kraken) share one clock while
 * per-NPC families (Dagannoth Kings) and standalone bosses each get their own.
 */
public class FamilyKillClock
{
	private final Map<String, Instant> clocksByKey = new HashMap<>();

	/**
	 * Starts the clock for this key if it isn't already running.
	 *
	 * @return the clock's start time, whether just started or already in progress.
	 */
	public Instant start(String key)
	{
		return clocksByKey.computeIfAbsent(key, k -> Instant.now());
	}

	public Instant peek(String key)
	{
		return clocksByKey.get(key);
	}

	public void clear(String key)
	{
		clocksByKey.remove(key);
	}

	public void clearAll()
	{
		clocksByKey.clear();
	}
}
