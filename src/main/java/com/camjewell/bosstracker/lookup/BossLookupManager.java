package com.camjewell.bosstracker.lookup;

import com.camjewell.bosstracker.boss.Boss;
import com.camjewell.bosstracker.persistence.BossLoot;
import com.camjewell.bosstracker.persistence.BossLootStore;
import com.camjewell.bosstracker.persistence.BossStats;
import com.camjewell.bosstracker.persistence.BossStatsStore;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Backs the side panel's Search tab: loads all-time {@link BossStats}/{@link BossLoot} for any
 * boss the user looks up, independent of whatever boss the live session is currently tracking.
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

	@Getter
	private Boss boss;

	@Getter
	private BossStats stats = new BossStats();

	@Getter
	private final Map<Integer, Integer> lootItemQuantities = new LinkedHashMap<>();

	@Getter
	private int version;

	private Executor asyncExecutor;
	private Boss pendingBoss;

	/**
	 * Must be called once by the plugin during startUp() with an executor that runs off the
	 * client thread; lookup()/deleteData() are no-ops until this is set.
	 */
	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/**
	 * Kicks off an async load of {@code boss}'s persisted stats and loot. If another lookup is
	 * requested before this one finishes, the stale result is discarded when it lands.
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
			BossStats loadedStats = statsStore.load(accountHash, boss);
			BossLoot loadedLoot = lootStore.load(accountHash, boss);

			if (pendingBoss == boss)
			{
				this.boss = boss;
				this.stats = loadedStats;
				lootItemQuantities.clear();
				lootItemQuantities.putAll(loadedLoot.getItemQuantities());

				// See LootTracker.ensureLoaded: warm the price cache on the client thread
				// before bumping version, since getItemPrice() falls back to a
				// client-thread-only call when an item isn't already cached.
				clientThread.invoke(() ->
				{
					for (int itemId : loadedLoot.getItemQuantities().keySet())
					{
						priceCache.warm(itemId);
					}
					version++;
				});
			}
		});
	}

	public void clear()
	{
		pendingBoss = null;
		boss = null;
		stats = new BossStats();
		lootItemQuantities.clear();
		version++;
	}

	/**
	 * Deletes all persisted stats and loot for {@code boss}. If it's the currently displayed
	 * boss, clears the display too.
	 */
	public void deleteData(Boss boss)
	{
		if (this.boss == boss)
		{
			clear();
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
