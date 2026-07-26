package com.camjewell.bosstracker.boss;

/**
 * Groups of bosses that share a single kill-timer instead of each NPC name timing itself.
 */
public enum TimingFamily
{
	/**
	 * No shared timer; the boss times itself independently.
	 */
	NONE(false),

	/**
	 * Prime, Rex, and Supreme are simultaneously alive in the same fight, so each needs its
	 * own independent clock keyed by NPC name rather than one shared start time.
	 */
	DAGANNOTH_KINGS(true),

	/**
	 * All 6 Barrows brothers share one clock: the timer starts on the first brother hit and
	 * runs until the chest is opened, regardless of which brothers are fought.
	 */
	BARROWS(false),

	/**
	 * Kraken and its Enormous Tentacle sub-NPC are one continuous encounter sharing one clock.
	 */
	KRAKEN(false);

	private final boolean perNpcClock;

	TimingFamily(boolean perNpcClock)
	{
		this.perNpcClock = perNpcClock;
	}

	/**
	 * @return true if each NPC name in the family tracks its own independent start time,
	 * false if the whole family shares a single start time.
	 */
	public boolean isPerNpcClock()
	{
		return perNpcClock;
	}
}
