package com.camjewell.bosstracker.loot;

import com.camjewell.bosstracker.persistence.TeammateUnique;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToLongFunction;

/**
 * What raid uniques are worth to the player once split with the group. A split is the number of
 * players an item was shared between, the player included; 1 means free for all, so the player
 * kept it, and an item with no split set is counted as kept too.
 *
 * A split item is sold to share the gold, so its Grand Exchange tax comes off before dividing.
 * Kept items stay at their full price, like the rest of the loot.
 */
public final class UniqueSplits
{
	public static final int FREE_FOR_ALL = 1;

	private static final double GE_TAX_RATE = 0.02;
	private static final long GE_TAX_CAP = 5_000_000;

	private UniqueSplits()
	{
	}

	/** What one sells for once the Grand Exchange takes its 2%, capped at 5m per item. */
	public static long afterTax(long unitPrice)
	{
		return unitPrice - Math.min((long) (unitPrice * GE_TAX_RATE), GE_TAX_CAP);
	}

	/** The player's share of {@code quantity} of an item split {@code splitSize} ways. */
	public static double share(long unitPrice, int quantity, int splitSize)
	{
		if (splitSize <= FREE_FOR_ALL)
		{
			return (double) unitPrice * quantity;
		}
		return (double) afterTax(unitPrice) * quantity / splitSize;
	}

	/** The part of the player's own loot that went to the rest of the group, or to tax. */
	public static double givenAway(Map<Integer, Integer> loot, Map<Integer, Integer> splits, IntToLongFunction price)
	{
		double givenAway = 0;
		for (double itemGivenAway : givenAwayByItem(loot, splits, price).values())
		{
			givenAway += itemGivenAway;
		}
		return givenAway;
	}

	/**
	 * {@link #givenAway}, item by item. Has an entry for every split item in the loot, so a free
	 * for all one is here at 0.
	 */
	public static Map<Integer, Double> givenAwayByItem(Map<Integer, Integer> loot, Map<Integer, Integer> splits, IntToLongFunction price)
	{
		Map<Integer, Double> givenAway = new LinkedHashMap<>();
		for (Map.Entry<Integer, Integer> split : splits.entrySet())
		{
			Integer quantity = loot.get(split.getKey());
			if (quantity != null)
			{
				long unitPrice = price.applyAsLong(split.getKey());
				givenAway.put(split.getKey(), (double) unitPrice * quantity - share(unitPrice, quantity, split.getValue()));
			}
		}
		return givenAway;
	}

	/** The player's shares of uniques their teammates received. */
	public static double teammateShares(List<TeammateUnique> uniques)
	{
		double shares = 0;
		for (TeammateUnique unique : uniques)
		{
			shares += share(unique.getPrice(), 1, unique.getSplitSize());
		}
		return shares;
	}

	/** Loot worth {@code rawValue} once the player's own splits and teammates' shares are counted. */
	public static double afterSplits(double rawValue, Map<Integer, Integer> loot, Map<Integer, Integer> splits,
		List<TeammateUnique> teammateUniques, IntToLongFunction price)
	{
		return rawValue - givenAway(loot, splits, price) + teammateShares(teammateUniques);
	}
}
