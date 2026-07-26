package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.boss.KillTiming;
import com.camjewell.bosstracker.boss.TimingFamily;
import com.camjewell.bosstracker.chat.ChatKillParser;
import com.camjewell.bosstracker.loot.LootTracker;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.SessionHistoryStore;
import com.camjewell.bosstracker.util.TimeFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

/**
 * Owns the live {@link BossSession} and drives its lifecycle from game events. Replaces the
 * original plugin's ~1600-line god-object plugin class: chat parsing lives in
 * {@link ChatKillParser}, per-boss data lives in {@link Boss}, and this class only orchestrates
 * session state transitions.
 */
@Singleton
public class SessionManager
{
	@Inject
	private BossTrackerConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Client client;

	@Inject
	private BossStatsStore statsStore;

	@Inject
	private GoalManager goalManager;

	@Inject
	private LootTracker lootTracker;

	@Inject
	private SessionHistoryStore historyStore;

	@Getter
	private BossSession session;

	/**
	 * Retained after {@link #end()} so the "!Info" chat command and side panel can still show
	 * the last completed session's stats.
	 */
	@Getter
	private BossSession lastCompletedSession;

	private final FamilyKillClock familyClock = new FamilyKillClock();

	private NPC currentAttackedNpc;
	private Boss currentAttackedBoss;
	private String currentAttackedNpcName;
	private int attackCount;
	private int ticksSinceLastAttack;
	private int currentAttackTimeoutTicks;
	private boolean earlyStartPending;
	private boolean deathHandled;
	private boolean autoResumed;
	private int ticksSinceTimeoutCheck;

	private Boss pendingSelfReportedBoss;
	private Integer pendingKillCount;
	private Integer pendingDurationSeconds;

	private Executor asyncExecutor;

	/**
	 * Must be called once by the plugin during startUp() with an executor that runs off the
	 * client thread; persistence is a no-op until this is set.
	 */
	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	// ---- chat ----

	public void onChatMessage(String message, boolean inFightCavesOrInferno)
	{
		if (session != null && session.isPaused()
			&& ChatKillParser.isRecognizedBossMessage(message, config.dksSelector(), inFightCavesOrInferno))
		{
			resume();
		}

		Boss earlyStartBoss = ChatKillParser.findEarlyStartBoss(message);
		if (earlyStartBoss != null)
		{
			triggerEarlyStart(earlyStartBoss);
		}

		Boss kcBoss = ChatKillParser.findKcBoss(message, config.dksSelector());
		if (kcBoss != null)
		{
			int killCount = ChatKillParser.parseKillCount(message);
			if (kcBoss.getTiming() == KillTiming.HITSPLAT)
			{
				finalizeHitsplatKill(kcBoss, killCount);
			}
			else
			{
				pendingSelfReportedBoss = kcBoss;
				pendingKillCount = killCount;
				tryFinalizePendingSelfReported();
			}
			return;
		}

		if (pendingSelfReportedBoss != null)
		{
			Integer seconds = ChatKillParser.parseDurationSeconds(message, inFightCavesOrInferno);
			if (seconds != null)
			{
				pendingDurationSeconds = seconds;
				tryFinalizePendingSelfReported();
			}
		}
	}

	private void tryFinalizePendingSelfReported()
	{
		if (pendingSelfReportedBoss != null && pendingKillCount != null && pendingDurationSeconds != null)
		{
			finalizeKill(pendingSelfReportedBoss, pendingKillCount, pendingDurationSeconds);
			pendingSelfReportedBoss = null;
			pendingKillCount = null;
			pendingDurationSeconds = null;
		}
	}

	private void finalizeHitsplatKill(Boss boss, int killCount)
	{
		String key = clockKey(boss, currentAttackedNpcName);
		Instant start = familyClock.peek(key);
		int durationSeconds = start != null ? (int) Duration.between(start, Instant.now()).getSeconds() : 0;
		familyClock.clear(key);
		attackCount = 0;
		finalizeKill(boss, killCount, durationSeconds);
	}

	private void finalizeKill(Boss boss, int killCount, int durationSeconds)
	{
		boolean bossChanged = session == null || session.getBoss() != boss;
		if (bossChanged)
		{
			if (session != null)
			{
				maybeAnnounceOnChange(session);
				persistSession(session);
				persistHistory(session);
			}
			session = new BossSession(boss);
			lootTracker.startNewSession(boss);
		}

		session.setKillsThisSession(session.getKillsThisSession() + 1);
		session.setKillCount(killCount);

		goalManager.ensureLoaded(boss);
		goalManager.onKillCounted(boss, killCount);

		boolean isFirstKillOfSession = session.getKillsThisSession() == 1;
		if (isFirstKillOfSession)
		{
			session.setSessionStart(Instant.now());
		}
		if (isFirstKillOfSession || autoResumed)
		{
			session.setTimerOffsetSeconds(session.getTimerOffsetSeconds() + durationSeconds);
			autoResumed = false;
		}

		session.setCumulativeKillTimeSeconds(session.getCumulativeKillTimeSeconds() + durationSeconds);
		session.setFastestKillSeconds(Math.min(session.getFastestKillSeconds(), durationSeconds));
		session.setLastKillAt(Instant.now());

		recalculateKph();
		maybeAnnounceKillDuration(boss, durationSeconds);
		maybePrintKphInChat();
		persistKill(boss, killCount, durationSeconds);
	}

	private void recalculateKph()
	{
		int totalTimeSeconds = session.getCalcMode() == CalcMode.VIRTUAL
			? session.getCumulativeKillTimeSeconds()
			: computeActualElapsedSeconds();

		int killsThisSession = session.getKillsThisSession();
		int averageKillTime = killsThisSession > 0 ? totalTimeSeconds / killsThisSession : 0;
		session.setAverageKillTimeSeconds(averageKillTime);
		session.setKillsPerHour(averageKillTime == 0 ? 0 : 3600.0 / averageKillTime);
		session.setIdleSeconds(Math.max(0, totalTimeSeconds - session.getCumulativeKillTimeSeconds()));
	}

	/**
	 * Real wall-clock time elapsed since the session's first kill, plus the offset accounting
	 * for the fight already in progress when tracking started, minus any paused time. Used for
	 * ACTUAL calc mode and always used for the "session time" display regardless of calc mode.
	 */
	public int computeActualElapsedSeconds()
	{
		if (session == null)
		{
			return 0;
		}
		long elapsed = Duration.between(session.getSessionStart(), Instant.now()).getSeconds();
		return (int) elapsed + session.getTimerOffsetSeconds() - session.getPausedSeconds();
	}

	public void toggleCalcMode()
	{
		if (session != null)
		{
			session.setCalcMode(session.getCalcMode() == CalcMode.ACTUAL ? CalcMode.VIRTUAL : CalcMode.ACTUAL);
			recalculateKph();
		}
	}

	// ---- hitsplat / attack tracking ----

	public void onHitsplatApplied(NPC npc)
	{
		Boss boss = Boss.byNpcName(npc.getName());
		if (boss == null || boss.getTiming() != KillTiming.HITSPLAT)
		{
			return;
		}

		currentAttackedNpc = npc;
		ticksSinceLastAttack = 0;
		earlyStartPending = false;

		attackCount++;
		if (attackCount == 1)
		{
			startNewAttackSequence(boss, npc.getName());
		}
		else if (!npc.getName().equals(currentAttackedNpcName) && !boss.isMultiPhase())
		{
			attackCount = 1;
			startNewAttackSequence(boss, npc.getName());
		}
	}

	private void startNewAttackSequence(Boss boss, String npcName)
	{
		currentAttackedBoss = boss;
		currentAttackedNpcName = npcName;
		currentAttackTimeoutTicks = boss.getAttackTimeoutTicks();
		familyClock.start(clockKey(boss, npcName));
	}

	private void triggerEarlyStart(Boss boss)
	{
		String key = clockKey(boss, boss.getBossName());
		if (familyClock.peek(key) == null)
		{
			familyClock.start(key);
			earlyStartPending = true;
			currentAttackedBoss = boss;
			currentAttackedNpcName = boss.getBossName();
			currentAttackTimeoutTicks = boss.getAttackTimeoutTicks();
			ticksSinceLastAttack = 0;
		}
	}

	private String clockKey(Boss boss, String npcName)
	{
		if (boss.getFamily().isPerNpcClock())
		{
			return npcName != null ? npcName : boss.getBossName();
		}
		if (boss.getFamily() != TimingFamily.NONE)
		{
			return boss.getFamily().name();
		}
		return boss.getBossName();
	}

	// ---- ticks ----

	public void onGameTick()
	{
		if (attackCount > 0 || earlyStartPending)
		{
			ticksSinceLastAttack++;
			if (ticksSinceLastAttack == currentAttackTimeoutTicks)
			{
				attackCount = 0;
				earlyStartPending = false;
				if (currentAttackedBoss != null)
				{
					familyClock.clear(clockKey(currentAttackedBoss, currentAttackedNpcName));
				}
			}
		}

		pollForBossDeath();

		ticksSinceTimeoutCheck++;
		if (ticksSinceTimeoutCheck >= 2)
		{
			ticksSinceTimeoutCheck = 0;
			if (session != null && session.getKillsThisSession() > 0 && !session.isPaused()
				&& config.sessionTimeoutMinutes() != 0)
			{
				checkSessionTimeout();
			}
		}
	}

	private void pollForBossDeath()
	{
		if (currentAttackedNpc == null)
		{
			return;
		}
		if (currentAttackedNpc.isDead() && !deathHandled)
		{
			if (currentAttackedBoss != null && !currentAttackedBoss.isMultiPhase())
			{
				familyClock.clear(clockKey(currentAttackedBoss, currentAttackedNpcName));
				attackCount = 0;
			}
			deathHandled = true;
		}
		else if (!currentAttackedNpc.isDead())
		{
			deathHandled = false;
		}
	}

	private void checkSessionTimeout()
	{
		long minutesSinceLastKill = Duration.between(session.getLastKillAt(), Instant.now()).toMinutes();
		if (minutesSinceLastKill >= config.sessionTimeoutMinutes())
		{
			end();
		}
	}

	// ---- lifecycle ----

	/**
	 * Called when hopping worlds or logging back in: cancels any in-progress attack tracking
	 * without touching the session itself (a world hop mid-fight shouldn't end the session).
	 */
	public void onWorldHopOrLogin()
	{
		attackCount = 0;
		ticksSinceLastAttack = 0;
		earlyStartPending = false;
		familyClock.clearAll();
	}

	public void pause()
	{
		if (session != null && !session.isPaused())
		{
			session.setPaused(true);
			session.setPauseStart(Instant.now());
			queueGameMessage("Session Paused");
		}
	}

	public void resume()
	{
		if (session != null && session.isPaused())
		{
			session.setPaused(false);
			int pausedSeconds = (int) Duration.between(session.getPauseStart(), Instant.now()).getSeconds();
			session.setPausedSeconds(session.getPausedSeconds() + pausedSeconds);
			session.setLastKillAt(Instant.now());
			familyClock.clearAll();
			attackCount = 0;
			autoResumed = true;
			queueGameMessage("Session Resumed");
		}
	}

	public void end()
	{
		if (session != null)
		{
			maybeAnnounceOnChange(session);
			persistSession(session);
			persistHistory(session);
			lastCompletedSession = session;
			session = null;
			familyClock.clearAll();
			attackCount = 0;
			currentAttackedNpc = null;
			currentAttackedBoss = null;
			currentAttackedNpcName = null;
		}
	}

	public void announceCurrentInfo()
	{
		BossSession toAnnounce = session != null ? session : lastCompletedSession;
		if (toAnnounce != null)
		{
			announceSessionInfo(toAnnounce);
		}
	}

	// ---- chat announcements ----

	private void maybeAnnounceOnChange(BossSession s)
	{
		if (config.outputOnChange())
		{
			announceSessionInfo(s);
		}
	}

	private void announceSessionInfo(BossSession s)
	{
		queueGameMessage("Session Info");
		queueGameMessage("-------------------------");
		queueGameMessage("KPH: " + TimeFormat.kph(s.getKillsPerHour(), config.kphMethod()));
		queueGameMessage("Kills: " + s.getKillsThisSession());
		queueGameMessage("Avg Kill: " + TimeFormat.minutesSeconds(s.getAverageKillTimeSeconds()));
		queueGameMessage("Idle Time: " + TimeFormat.minutesSeconds(s.getIdleSeconds()));
		queueGameMessage("Session Time: " + TimeFormat.minutesSeconds(computeActualElapsedSeconds()));
		queueGameMessage("-------------------------");
	}

	private void maybeAnnounceKillDuration(Boss boss, int durationSeconds)
	{
		if (config.displayKillTimes())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				boss.getBossName() + " Fight Duration: <col=ff0000>" + TimeFormat.minutesSeconds(durationSeconds), "");
		}
	}

	private void maybePrintKphInChat()
	{
		if (config.printKphInChat())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"KPH: <col=ff0000>" + TimeFormat.kph(session.getKillsPerHour(), config.kphMethod()) + "</col>", "");
		}
	}

	private void queueGameMessage(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.GAMEMESSAGE).runeLiteFormattedMessage(text).build());
	}

	// ---- persistence ----

	private void persistKill(Boss boss, int killCount, int durationSeconds)
	{
		if (asyncExecutor == null)
		{
			return;
		}
		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, boss);
			stats.setKillsTracked(stats.getKillsTracked() + 1);
			stats.setTotalKc(killCount);
			stats.setFastestKillSeconds(Math.min(stats.getFastestKillSeconds(), durationSeconds));
			stats.setTotalTimeVirtualSeconds(stats.getTotalTimeVirtualSeconds() + durationSeconds);
			statsStore.save(accountHash, boss, stats);
		});
	}

	private void persistSession(BossSession endedSession)
	{
		if (asyncExecutor == null || endedSession == null)
		{
			return;
		}
		long accountHash = client.getAccountHash();
		long elapsed = Duration.between(endedSession.getSessionStart(), Instant.now()).getSeconds();
		long actualSeconds = elapsed + endedSession.getTimerOffsetSeconds() - endedSession.getPausedSeconds();
		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, endedSession.getBoss());
			stats.setTotalTimeActualSeconds(stats.getTotalTimeActualSeconds() + actualSeconds);
			statsStore.save(accountHash, endedSession.getBoss(), stats);
		});
	}

	/**
	 * Records a history-log entry for a just-ended session (via {@link #end()} or a boss switch
	 * in {@link #finalizeKill}). Both call sites invoke this while {@code this.session} still
	 * refers to {@code endedSession}, so {@link #computeActualElapsedSeconds()} correctly
	 * reflects its duration. Skips sessions with no kills to avoid empty log noise.
	 */
	private void persistHistory(BossSession endedSession)
	{
		if (asyncExecutor == null || endedSession == null || endedSession.getKillsThisSession() == 0)
		{
			return;
		}

		long accountHash = client.getAccountHash();

		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setBossName(endedSession.getBoss().getBossName());
		entry.setEndedAtEpochMilli(System.currentTimeMillis());
		entry.setKillsThisSession(endedSession.getKillsThisSession());
		entry.setSessionDurationSeconds(computeActualElapsedSeconds());
		entry.setKillsPerHour(endedSession.getKillsPerHour());
		entry.setAverageKillTimeSeconds(endedSession.getAverageKillTimeSeconds());
		entry.setFastestKillSeconds(endedSession.getFastestKillSeconds());
		entry.setIdleSeconds(endedSession.getIdleSeconds());
		entry.setLootItemQuantities(new LinkedHashMap<>(lootTracker.getSessionLoot()));

		asyncExecutor.execute(() -> historyStore.save(accountHash, entry));
	}
}
