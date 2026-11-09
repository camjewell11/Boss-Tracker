package com.camjewell.bosstracker.lookup;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossLoot;
import com.camjewell.bosstracker.persistence.BossLootStore;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Backs the side panel's Search tab: loads all-time {@link BossStats}/{@link BossLoot} for any
 * boss the user looks up, and for a raid every mode of it, independent of whatever boss the live
 * session is currently tracking.
 * {@link #getVersion()} lets the panel detect when a lookup/delete actually changed the
 * displayed data, mirroring {@link com.camjewell.bosstracker.history.SessionHistoryManager}.
 */
@Singleton
public class BossLookupManager
{
	@Inject
	private BossStatsStore statsStore;

	@Inject
	private BossLootStore lootStore;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemPriceCache priceCache;

	/** One boss's saved stats and loot, every section of it. */
	@Value
	public static class Result
	{
		Boss boss;
		BossStats stats;
		BossLoot loot;

		public boolean hasData()
		{
			return stats.getKillsTracked() > 0 || !loot.getItemQuantities().isEmpty()
				|| !loot.getMinionItemQuantities().isEmpty() || !loot.getPerfectBonusItemQuantities().isEmpty()
				|| !loot.getCacheItemQuantities().isEmpty() || !loot.getSupplyItemQuantities().isEmpty();
		}
	}

	/** The boss that was searched for. */
	@Getter
	private volatile Boss boss;

	/**
	 * The searched boss's results, one per mode of its raid in {@link Boss#getModes()} order.
	 * Replaced whole when a lookup lands, never changed in place, as the panel reads it on the
	 * Swing thread.
	 */
	@Getter
	private volatile List<Result> results = Collections.emptyList();

	@Getter
	private volatile int version;

	private Executor asyncExecutor;
	private volatile Boss pendingBoss;

	/**
	 * Must be called once by the plugin during startUp() with an executor that runs off the
	 * client thread; lookup()/deleteData() are no-ops until this is set.
	 */
	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/**
	 * Kicks off an async load of the persisted stats and loot of {@code boss} and its raid's other
	 * modes. If another lookup is requested before this one finishes, the stale result is
	 * discarded when it lands.
	 */
	public void lookup(Boss boss)
	{
		if (asyncExecutor == null || boss == null)
		{
			return;
		}

		pendingBoss = boss;
		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			List<Result> loaded = new ArrayList<>();
			for (Boss mode : boss.getModes())
			{
				loaded.add(new Result(mode, statsStore.load(accountHash, mode), lootStore.load(accountHash, mode)));
			}

			if (pendingBoss == boss)
			{
				// See LootTracker.ensureLoaded: warm the price cache on the client thread
				// before bumping version, since getItemPrice() falls back to a
				// client-thread-only call when an item isn't already cached.
				clientThread.invoke(() ->
				{
					for (Result result : loaded)
					{
						BossLoot loot = result.getLoot();
						for (Map<Integer, Integer> section : Arrays.asList(loot.getItemQuantities(),
							loot.getMinionItemQuantities(), loot.getPerfectBonusItemQuantities(),
							loot.getCacheItemQuantities(), loot.getSupplyItemQuantities()))
						{
							for (int itemId : section.keySet())
							{
								priceCache.warm(itemId);
							}
						}
					}
					// Published together, so the panel never shows one boss's stats with another's loot.
					if (pendingBoss == boss)
					{
						this.boss = boss;
						results = Collections.unmodifiableList(loaded);
						version++;
					}
				});
			}
		});
	}

	public void clear()
	{
		pendingBoss = null;
		boss = null;
		results = Collections.emptyList();
		version++;
	}

	/**
	 * Deletes all persisted stats and loot for {@code boss}. If it's on display, it's shown empty.
	 */
	public void deleteData(Boss boss)
	{
		List<Result> emptied = new ArrayList<>(results);
		boolean shown = false;
		for (int i = 0; i < emptied.size(); i++)
		{
			if (emptied.get(i).getBoss() == boss)
			{
				emptied.set(i, new Result(boss, new BossStats(), new BossLoot()));
				shown = true;
			}
		}
		if (shown)
		{
			results = Collections.unmodifiableList(emptied);
			version++;
		}

		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			statsStore.delete(accountHash, boss);
			lootStore.delete(accountHash, boss);
		});
	}
}
