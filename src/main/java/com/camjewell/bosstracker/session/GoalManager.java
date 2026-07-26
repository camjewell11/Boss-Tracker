package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

/**
 * Tracks the KC goal for whichever boss is currently being displayed, and fires a one-time chat
 * notification when it's reached. Goal values are persisted on {@link BossStats}, so no new
 * storage format was needed for this phase.
 */
@Singleton
public class GoalManager
{
	@Inject
	private BossTrackerConfig config;

	@Inject
	private BossStatsStore statsStore;

	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Getter
	private BossGoal goal;

	private Boss trackedBoss;
	private Executor asyncExecutor;

	/**
	 * Must be called once by the plugin during startUp() with an executor that runs off the
	 * client thread; persistence is a no-op until this is set.
	 */
	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/**
	 * Kicks off an async load of the persisted goal for {@code boss} if it isn't already loaded
	 * or being loaded. Cheap to call every kill/tick; a no-op once {@code boss} is the tracked
	 * boss.
	 */
	public void ensureLoaded(Boss boss)
	{
		if (boss == null || boss == trackedBoss)
		{
			return;
		}

		trackedBoss = boss;
		goal = null;

		if (asyncExecutor == null)
		{
			goal = new BossGoal(boss);
			return;
		}

		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, boss);
			BossGoal loaded = new BossGoal(boss);
			loaded.setStartKc(stats.getGoalStartKc());
			loaded.setEndKc(stats.getGoalEndKc());
			loaded.setNotified(stats.isGoalNotified());

			if (trackedBoss == boss)
			{
				goal = loaded;
			}
		});
	}

	public void setGoal(int startKc, int endKc)
	{
		if (goal == null)
		{
			return;
		}

		goal.setStartKc(startKc);
		goal.setEndKc(endKc);
		goal.setNotified(false);
		persistGoal(goal);
	}

	public void resetGoal()
	{
		if (goal == null)
		{
			return;
		}

		goal.setStartKc(0);
		goal.setEndKc(0);
		goal.setNotified(false);
		persistGoal(goal);
	}

	/**
	 * Called by {@link SessionManager} on every kill. Fires a one-time chat message when the
	 * tracked boss's live KC reaches the goal's end KC.
	 */
	public void onKillCounted(Boss boss, int totalKc)
	{
		if (goal == null || goal.getBoss() != boss || goal.isNotified() || !goal.isComplete(totalKc))
		{
			return;
		}

		goal.setNotified(true);
		persistGoal(goal);

		if (config.notifyOnGoalComplete())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("Goal reached: <col=00c000>" + boss.getBossName() + "</col> KC "
					+ totalKc + "/" + goal.getEndKc())
				.build());
		}
	}

	private void persistGoal(BossGoal g)
	{
		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		Boss boss = g.getBoss();
		int startKc = g.getStartKc();
		int endKc = g.getEndKc();
		boolean notified = g.isNotified();

		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, boss);
			stats.setGoalStartKc(startKc);
			stats.setGoalEndKc(endKc);
			stats.setGoalNotified(notified);
			statsStore.save(accountHash, boss, stats);
		});
	}
}
