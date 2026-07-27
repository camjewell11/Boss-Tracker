package com.camjewell.bosstracker.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

/**
 * {@link ItemManager#getItemComposition(int)} always calls {@code client.getItemDefinition()}
 * with no caching of its own, so it (and anything built on it, like
 * {@link ItemManager#getItemPrice(int)}) can only ever be called from the client thread. Swing
 * refresh code runs on the EDT, so this caches price/name lookups performed on the client thread
 * (once loot is received or loaded) for the panel to read without touching {@code ItemManager}
 * directly.
 */
@Singleton
public class ItemPriceCache
{
	@Inject
	private ItemManager itemManager;

	private final Map<Integer, Long> prices = new ConcurrentHashMap<>();
	private final Map<Integer, String> names = new ConcurrentHashMap<>();

	/**
	 * Must be called from the client thread.
	 */
	public void warm(int itemId)
	{
		prices.put(itemId, (long) itemManager.getItemPrice(itemId));
		names.put(itemId, itemManager.getItemComposition(itemId).getName());
	}

	public long getPrice(int itemId)
	{
		return prices.getOrDefault(itemId, 0L);
	}

	public String getName(int itemId)
	{
		return names.getOrDefault(itemId, "Unknown item");
	}
}
