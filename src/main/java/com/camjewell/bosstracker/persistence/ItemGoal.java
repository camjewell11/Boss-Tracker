package com.camjewell.bosstracker.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Part of a boss goal: get {@code quantity} of one item from the boss, such as three Venator
 * shards from the Phantom Muspah.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemGoal
{
	private String itemName;
	/**
	 * Every ID the item drops as, all counting toward the goal: a pet's forms, or the noted
	 * version of a drop.
	 */
	private List<Integer> itemIds = new ArrayList<>();
	private int quantity;
	/**
	 * How many of the item the boss's lifetime loot held when the goal was set to count from then,
	 * so only later drops count; 0 when it counts all-time drops.
	 */
	private int startQuantity;
	/** Whether the goal-reached chat message has been sent, so it only fires once. */
	private boolean notified;

	public ItemGoal(String itemName, List<Integer> itemIds, int quantity)
	{
		this(itemName, new ArrayList<>(itemIds), quantity, 0, false);
	}

	/** How many of the item the boss's lifetime loot holds, across all its IDs. */
	public int lifetimeQuantity(Map<Integer, Integer> lifetimeLoot)
	{
		int total = 0;
		for (int itemId : itemIds)
		{
			total += lifetimeLoot.getOrDefault(itemId, 0);
		}
		return total;
	}

	/** How many count toward the goal so far, which can pass {@link #quantity}. */
	public int obtained(Map<Integer, Integer> lifetimeLoot)
	{
		return Math.max(0, lifetimeQuantity(lifetimeLoot) - startQuantity);
	}

	public boolean isComplete(Map<Integer, Integer> lifetimeLoot)
	{
		return obtained(lifetimeLoot) >= quantity;
	}
}
