package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.SessionHistoryStore;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Holds the in-memory list of past session entries shown on the side panel's History tab.
 * {@link #version()} lets the panel detect when a reload or delete actually changed the list,
 * so it only rebuilds its Swing components (which would otherwise collapse any expanded entries)
 * when something changed, rather than every tick.
 */
@Singleton
public class SessionHistoryManager
{
	@Inject
	private SessionHistoryStore store;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemPriceCache priceCache;

	/**
	 * Replaced whole, never modified in place: the side panel reads it on the Swing thread while
	 * loads finish on other threads, so it must never be seen half-filled.
	 */
	@Getter
	private volatile List<SessionHistoryEntry> entries = Collections.emptyList();

	@Getter
	private int version;

	private Executor asyncExecutor;
	private boolean loading;

	/**
	 * Must be called once by the plugin during startUp() with an executor that runs off the
	 * client thread; reload()/delete() are no-ops until this is set.
	 */
	public void setAsyncExecutor(Executor executor)
	{
		this.asyncExecutor = executor;
	}

	/**
	 * Kicks off an async (re)load of every stored history entry. Call when the History tab is
	 * opened; the panel's regular per-tick refresh picks up the result once it lands.
	 */
	public void reload()
	{
		if (asyncExecutor == null || loading)
		{
			return;
		}

		loading = true;
		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() ->
		{
			List<SessionHistoryEntry> loaded = Collections.unmodifiableList(store.loadAll(accountHash));

			// See LootTracker.ensureLoaded: warm the price cache on the client thread before
			// publishing, since prices are only read from the cache. Only uncached items, so
			// reopening History doesn't re-read hundreds of items in a single tick.
			clientThread.invoke(() ->
			{
				for (SessionHistoryEntry entry : loaded)
				{
					for (int itemId : entry.getLootItemQuantities().keySet())
					{
						priceCache.warmIfAbsent(itemId);
					}
					for (int itemId : entry.getMinionLootItemQuantities().keySet())
					{
						priceCache.warmIfAbsent(itemId);
					}
					for (int itemId : entry.getPerfectBonusItemQuantities().keySet())
					{
						priceCache.warmIfAbsent(itemId);
					}
				}
				// Reopening History usually finds nothing new; leaving the version alone then
				// spares the panel rebuilding every row.
				if (!loaded.equals(entries))
				{
					entries = loaded;
					version++;
				}
				loading = false;
			});
		});
	}

	public void delete(SessionHistoryEntry entry)
	{
		List<SessionHistoryEntry> remaining = new ArrayList<>(entries);
		remaining.remove(entry);
		entries = Collections.unmodifiableList(remaining);
		version++;

		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() -> store.delete(accountHash, entry));
	}
}
