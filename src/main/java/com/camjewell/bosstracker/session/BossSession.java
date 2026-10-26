package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.Trip;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Mutable state for the currently-tracked boss session. One instance lives for as long as the
 * player keeps killing the same {@link Boss}; switching bosses replaces it with a new one.
 */
@Getter
@Setter
public class BossSession
{
	private final Boss boss;
	private int killsThisSession;
	private int killCount;
	private boolean paused;
	private Instant sessionStart;
	private Instant lastKillAt;
	private Instant pauseStart;
	private int pausedSeconds;

	/**
	 * Duration of the first kill of the session (or a kill immediately following an
	 * auto-resume), added on top of elapsed wall-clock time so the ACTUAL calc mode accounts
	 * for the fight that was already in progress when the session/clock started.
	 */
	private int timerOffsetSeconds;

	/**
	 * Sum of every individual kill's own duration; the basis for VIRTUAL calc mode.
	 */
	private int cumulativeKillTimeSeconds;
	private int fastestKillSeconds = Integer.MAX_VALUE;
	private int idleSeconds;
	private double killsPerHour;
	private int averageKillTimeSeconds;
	private CalcMode calcMode = CalcMode.ACTUAL;

	/**
	 * Sticky for the life of the session: set once any kill is confirmed to have counted toward
	 * an active Slayer task for this boss, and never cleared back to false even if the task
	 * later completes or changes mid-session.
	 */
	private boolean onSlayerTask;

	/** Set when the player's own pet-drop message arrives during the session. */
	private boolean petObtained;

	/**
	 * Deaths across the session's clears, for bosses where {@link Boss#tracksTeamDeaths()}: the
	 * whole team's, and the player's own. Runs that wipe add nothing.
	 */
	private int teamDeaths;
	private int ownDeaths;

	/** Oldest first; the last one is still open unless a death, bank or the session ended it. */
	private final List<Trip> trips = new ArrayList<>();

	/**
	 * @return the trip currently accumulating kills, or null if the last one has ended (or none
	 * has started yet).
	 */
	public Trip getOpenTrip()
	{
		if (trips.isEmpty())
		{
			return null;
		}
		Trip last = trips.get(trips.size() - 1);
		return last.isOpen() ? last : null;
	}

	public BossSession(Boss boss)
	{
		this.boss = boss;
		this.sessionStart = Instant.now();
	}
}
