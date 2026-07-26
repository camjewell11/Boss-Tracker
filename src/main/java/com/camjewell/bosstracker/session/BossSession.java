package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.boss.Boss;
import java.time.Instant;
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

	public BossSession(Boss boss)
	{
		this.boss = boss;
		this.sessionStart = Instant.now();
	}
}
