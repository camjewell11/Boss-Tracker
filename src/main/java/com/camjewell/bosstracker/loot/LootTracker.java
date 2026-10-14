package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossLoot;
import com.camjewell.bosstracker.persistence.BossLootStore;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import com.camjewell.bosstracker.session.GoalManager;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemStack;

/**
 * Tracks loot for whichever boss the current session is tracking: this session's drops (reset
 * whenever a new session starts) and the all-time totals for that boss (loaded async, mirroring
 * {@link com.camjewell.bosstracker.session.GoalManager}). Boss-name resolution for incoming
 * {@code LootReceived} events is delegated to {@link LootBossMatcher}.
 */
@Singleton
public class LootTracker
{
	@Inject
	private BossLootStore lootStore;

	@Inject
	private BossStatsStore statsStore;

	@Inject
	private GoalManager goalManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemPriceCache priceCache;

	@Getter
	private final Map<Integer, Integer> sessionLoot = new LinkedHashMap<>();

	@Getter
	private final Map<Integer, Integer> lifetimeLoot = new LinkedHashMap<>();

	@Getter
	private final Set<Integer> ignoredItemIds = new HashSet<>();

	/** Minion drops for the tracked boss, kept apart from its own; see LootBossMatcher. */
	@Getter
	private final Map<Integer, Integer> sessionMinionLoot = new LinkedHashMap<>();

	@Getter
	private final Map<Integer, Integer> lifetimeMinionLoot = new LinkedHashMap<>();

	@Getter
	private int sessionMinionKills;

	@Getter
	private int lifetimeMinionKills;

	/**
	 * The boss's all-time tracked-kill count, used as the denominator for all-time GP/kill. Loot
	 * is only recorded while a session is tracking that boss, which is exactly when kills are
	 * timed, so this covers the same kills that produced the loot.
	 */
	@Getter
	private int lifetimeKillsTracked;

	@Getter
	private long lifetimeTimeActualSeconds;

	/** Saved total of timed fight durations for the tracked boss, excluding idle time. */
	private long lifetimeKillTimeSeconds;

	/**
	 * Bumped whenever session/lifetime loot or the ignore list changes, so the panel can skip
	 * rebuilding the loot grid (and re-registering item image listeners) on every tick.
	 */
	@Getter
	private int version;

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
	 * Called by {@link com.camjewell.bosstracker.session.SessionManager} whenever a new session
	 * starts (including switching bosses), clearing this session's loot and (re)loading the
	 * new boss's all-time totals.
	 */
	public void startNewSession(Boss boss)
	{
		sessionLoot.clear();
		sessionMinionLoot.clear();
		sessionMinionKills = 0;
		trackedBoss = null;
		ensureLoaded(boss);
	}

	/**
	 * Kicks off an async load of the persisted loot/stats for {@code boss} if it isn't already
	 * loaded or being loaded. Cheap to call repeatedly; a no-op once {@code boss} is tracked.
	 */
	public void ensureLoaded(Boss boss)
	{
		if (boss == null || boss == trackedBoss)
		{
			return;
		}

		trackedBoss = boss;
		lifetimeLoot.clear();
		lifetimeMinionLoot.clear();
		lifetimeMinionKills = 0;
		ignoredItemIds.clear();
		lifetimeKillsTracked = 0;
		lifetimeTimeActualSeconds = 0;
		lifetimeKillTimeSeconds = 0;
		version++;

		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			BossLoot loot = lootStore.load(accountHash, boss);
			BossStats stats = statsStore.load(accountHash, boss);

			if (trackedBoss == boss)
			{
				lifetimeLoot.putAll(loot.getItemQuantities());
				lifetimeMinionLoot.putAll(loot.getMinionItemQuantities());
				lifetimeMinionKills = loot.getMinionKills();
				ignoredItemIds.addAll(loot.getIgnoredItemIds());
				lifetimeKillsTracked = stats.getKillsTracked();
				lifetimeTimeActualSeconds = stats.getTotalTimeActualSeconds();
				lifetimeKillTimeSeconds = stats.getTotalTimeVirtualSeconds();

				// ItemManager.getItemPrice()/getItemComposition() always call
				// client.getItemDefinition() with no caching of their own, so they can only be
				// called from the client thread. We're on a background executor here, so warm
				// our own ItemPriceCache before bumping version, since that triggers a Swing
				// refresh that reads prices on the EDT.
				clientThread.invoke(() ->
				{
					for (int itemId : loot.getItemQuantities().keySet())
					{
						priceCache.warm(itemId);
					}
					for (int itemId : loot.getMinionItemQuantities().keySet())
					{
						priceCache.warm(itemId);
					}
					version++;
				});
			}
		});
	}

	/**
	 * Called by the plugin's {@code LootReceived} handler. {@code sessionBoss} is whichever boss
	 * the active session is tracking (loot for any other boss/event is ignored, matching the
	 * original plugin's behavior of only recording loot while actively tracking that boss).
	 *
	 * @return true if the loot was {@code sessionBoss}'s own drop and was recorded. Minion drops are
	 * recorded too but return false: they are not the boss's loot and belong to no trip.
	 */
	public boolean onLootReceived(Boss sessionBoss, String lootEventName, Collection<ItemStack> items)
	{
		if (sessionBoss == null || items == null || items.isEmpty())
		{
			return false;
		}
		if (LootBossMatcher.matchesMinionLootEvent(sessionBoss, lootEventName))
		{
			recordMinionLoot(sessionBoss, items);
			return false;
		}
		if (!LootBossMatcher.matchesLootEvent(sessionBoss, lootEventName))
		{
			return false;
		}

		ensureLoaded(sessionBoss);

		for (ItemStack stack : items)
		{
			sessionLoot.merge(stack.getId(), stack.getQuantity(), Integer::sum);
			lifetimeLoot.merge(stack.getId(), stack.getQuantity(), Integer::sum);

			// Warm the price cache while we're on the client thread (LootReceived is a game
			// event) so the panel's later EDT-side reads don't need ItemManager at all.
			priceCache.warm(stack.getId());
		}

		version++;

		goalManager.onLootValueChanged(sessionBoss, computeLifetimeGp());
		persistLoot(sessionBoss, items);
		return true;
	}

	private long computeLifetimeGp()
	{
		long total = 0;
		for (Map.Entry<Integer, Integer> entry : lifetimeLoot.entrySet())
		{
			total += priceCache.getPrice(entry.getKey()) * entry.getValue();
		}
		return total;
	}

	/**
	 * @return the average saved fight duration for {@code boss}, or -1 if its stats aren't loaded
	 * (it isn't the boss being tracked, or the async load hasn't landed) or it has no timed kills.
	 */
	public int historicalAverageKillSeconds(Boss boss)
	{
		if (boss != trackedBoss || lifetimeKillsTracked <= 0 || lifetimeKillTimeSeconds <= 0)
		{
			return -1;
		}
		return (int) (lifetimeKillTimeSeconds / lifetimeKillsTracked);
	}

	public void toggleIgnored(int itemId)
	{
		if (trackedBoss == null)
		{
			return;
		}

		if (!ignoredItemIds.remove(itemId))
		{
			ignoredItemIds.add(itemId);
		}
		version++;

		persistIgnoreList(trackedBoss, new HashSet<>(ignoredItemIds));
	}

	/**
	 * Records a minion drop: never a boss kill, and kept out of the boss's loot totals and goal.
	 */
	private void recordMinionLoot(Boss boss, Collection<ItemStack> items)
	{
		ensureLoaded(boss);
		for (ItemStack stack : items)
		{
			sessionMinionLoot.merge(stack.getId(), stack.getQuantity(), Integer::sum);
			lifetimeMinionLoot.merge(stack.getId(), stack.getQuantity(), Integer::sum);
			priceCache.warm(stack.getId());
		}
		sessionMinionKills++;
		lifetimeMinionKills++;
		version++;

		if (asyncExecutor == null)
		{
			return;
		}
		long accountHash = client.getAccountHash();
		List<ItemStack> snapshot = new ArrayList<>(items);
		asyncExecutor.execute(() ->
		{
			BossLoot loot = lootStore.load(accountHash, boss);
			for (ItemStack stack : snapshot)
			{
				loot.getMinionItemQuantities().merge(stack.getId(), stack.getQuantity(), Integer::sum);
			}
			loot.setMinionKills(loot.getMinionKills() + 1);
			lootStore.save(accountHash, boss, loot);
		});
	}

	private void persistLoot(Boss boss, Collection<ItemStack> items)
	{
		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		List<ItemStack> snapshot = new ArrayList<>(items);

		asyncExecutor.execute(() ->
		{
			BossLoot loot = lootStore.load(accountHash, boss);
			for (ItemStack stack : snapshot)
			{
				loot.getItemQuantities().merge(stack.getId(), stack.getQuantity(), Integer::sum);
			}
			lootStore.save(accountHash, boss, loot);
		});
	}

	private void persistIgnoreList(Boss boss, Set<Integer> ignored)
	{
		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();

		asyncExecutor.execute(() ->
		{
			BossLoot loot = lootStore.load(accountHash, boss);
			loot.setIgnoredItemIds(ignored);
			lootStore.save(accountHash, boss, loot);
		});
	}
}
