package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.SessionHistoryStore;
import com.camjewell.bosstracker.util.ItemPriceCache;
import java.util.ArrayList;
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

	@Getter
	private final List<SessionHistoryEntry> entries = new ArrayList<>();

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
			List<SessionHistoryEntry> loaded = store.loadAll(accountHash);
			entries.clear();
			entries.addAll(loaded);

			// See LootTracker.ensureLoaded: warm the price cache on the client thread
			// before bumping version, since getItemPrice() falls back to a
			// client-thread-only call when an item isn't already cached.
			clientThread.invoke(() ->
			{
				for (SessionHistoryEntry entry : loaded)
				{
					for (int itemId : entry.getLootItemQuantities().keySet())
					{
						priceCache.warm(itemId);
					}
				}
				version++;
				loading = false;
			});
		});
	}

	public void delete(SessionHistoryEntry entry)
	{
		entries.remove(entry);
		version++;

		if (asyncExecutor == null)
		{
			return;
		}

		long accountHash = client.getAccountHash();
		asyncExecutor.execute(() -> store.delete(accountHash, entry));
	}
}
