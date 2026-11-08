package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import com.camjewell.bosstracker.persistence.ItemGoal;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	@Inject
	private ItemPriceCache priceCache;

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
			loaded.setLootGoalGp(stats.getLootGoalGp());
			loaded.setLootGoalNotified(stats.isLootGoalNotified());
			loaded.setLootGoalBaseline(stats.getLootGoalBaseline());
			if (stats.getItemGoals() != null)
			{
				loaded.setItemGoals(new ArrayList<>(stats.getItemGoals()));
			}

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
		goal.setLootGoalGp(0);
		goal.setLootGoalNotified(false);
		goal.setLootGoalBaseline(null);
		goal.setItemGoals(new ArrayList<>());
		persistGoal(goal);
	}

	/**
	 * Sets the loot and item goals, counting either all of {@code lifetimeLoot} or only what drops
	 * from now. Goals already counting from now keep their starting points, so changing an amount
	 * or the KC range doesn't throw away the progress made since; an item added later counts from
	 * when it was added.
	 */
	public void setDropGoals(long lootGoalGp, List<ItemGoal> itemGoals, boolean fromNow, Map<Integer, Integer> lifetimeLoot)
	{
		if (goal == null)
		{
			return;
		}

		boolean keepStart = fromNow == goal.isLootGoalFromNow();
		Map<Integer, Integer> baseline = null;
		if (fromNow && (lootGoalGp > 0 || !itemGoals.isEmpty()))
		{
			baseline = keepStart && goal.isLootGoalSet() ? goal.getLootGoalBaseline() : new HashMap<>(lifetimeLoot);
		}

		List<ItemGoal> items = new ArrayList<>();
		for (ItemGoal wanted : itemGoals)
		{
			ItemGoal existing = keepStart ? findItemGoal(wanted.getItemName()) : null;
			int startQuantity = existing != null ? existing.getStartQuantity()
				: fromNow ? wanted.lifetimeQuantity(lifetimeLoot) : 0;
			boolean notified = existing != null && existing.getQuantity() == wanted.getQuantity() && existing.isNotified();
			items.add(new ItemGoal(wanted.getItemName(), new ArrayList<>(wanted.getItemIds()), wanted.getQuantity(), startQuantity, notified));
		}

		if (lootGoalGp != goal.getLootGoalGp() || !keepStart)
		{
			goal.setLootGoalNotified(false);
		}
		goal.setLootGoalGp(lootGoalGp);
		goal.setLootGoalBaseline(baseline);
		goal.setItemGoals(items);
		persistGoal(goal);
	}

	private ItemGoal findItemGoal(String itemName)
	{
		for (ItemGoal item : goal.getItemGoals())
		{
			if (item.getItemName().equals(itemName))
			{
				return item;
			}
		}
		return null;
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

	/**
	 * Called by {@link com.camjewell.bosstracker.loot.LootTracker} whenever this boss's all-time
	 * loot changes. Fires a one-time chat message when the loot-value goal or an item goal is
	 * reached.
	 */
	public void onLootChanged(Boss boss, Map<Integer, Integer> lifetimeLoot)
	{
		if (goal == null || goal.getBoss() != boss)
		{
			return;
		}

		boolean reached = false;
		if (goal.isLootGoalSet() && !goal.isLootGoalNotified())
		{
			long currentGp = goal.lootProgressGp(lifetimeLoot, priceCache::getPrice);
			if (goal.isLootGoalComplete(currentGp))
			{
				goal.setLootGoalNotified(true);
				reached = true;
				notifyGoalReached("Loot goal reached: <col=00c000>" + boss.getBossName() + "</col> "
					+ currentGp + "/" + goal.getLootGoalGp() + " gp");
			}
		}

		for (ItemGoal item : goal.getItemGoals())
		{
			if (!item.isNotified() && item.isComplete(lifetimeLoot))
			{
				item.setNotified(true);
				reached = true;
				notifyGoalReached("Item goal reached: <col=00c000>" + boss.getBossName() + "</col> "
					+ item.getItemName() + " " + item.obtained(lifetimeLoot) + "/" + item.getQuantity());
			}
		}

		if (reached)
		{
			persistGoal(goal);
		}
	}

	private void notifyGoalReached(String message)
	{
		if (config.notifyOnGoalComplete())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(message)
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
		long lootGoalGp = g.getLootGoalGp();
		boolean lootGoalNotified = g.isLootGoalNotified();
		Map<Integer, Integer> lootGoalBaseline = g.getLootGoalBaseline();
		List<ItemGoal> itemGoals = new ArrayList<>(g.getItemGoals());

		asyncExecutor.execute(() ->
		{
			BossStats stats = statsStore.load(accountHash, boss);
			stats.setGoalStartKc(startKc);
			stats.setGoalEndKc(endKc);
			stats.setGoalNotified(notified);
			stats.setLootGoalGp(lootGoalGp);
			stats.setLootGoalNotified(lootGoalNotified);
			stats.setLootGoalBaseline(lootGoalBaseline);
			stats.setItemGoals(itemGoals);
			statsStore.save(accountHash, boss, stats);
		});
	}
}
