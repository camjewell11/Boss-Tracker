package com.camjewell.bosstracker.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
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
	private final Map<Integer, Long> haPrices = new ConcurrentHashMap<>();
	private final Map<Integer, String> names = new ConcurrentHashMap<>();

	/**
	 * Must be called from the client thread.
	 */
	public void warm(int itemId)
	{
		prices.put(itemId, (long) itemManager.getItemPrice(itemId));

		// One composition lookup covers both; it is the expensive, client-thread-only part.
		ItemComposition composition = itemManager.getItemComposition(itemId);
		names.put(itemId, composition.getName());
		haPrices.put(itemId, (long) composition.getHaPrice());
	}

	public long getPrice(int itemId)
	{
		return prices.getOrDefault(itemId, 0L);
	}

	public String getName(int itemId)
	{
		return names.getOrDefault(itemId, "Unknown item");
	}

	/**
	 * @return the item's high alchemy value, or 0 if unknown. Every item carries one, but not
	 * every item can actually be alched, so callers should treat 0 as "don't show it".
	 */
	public long getHaPrice(int itemId)
	{
		return haPrices.getOrDefault(itemId, 0L);
	}
}
