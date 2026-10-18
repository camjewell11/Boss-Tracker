package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemDropSearchTest
{
	private static final int RUNITE_ORE = 451;
	private static final int RUNITE_ORE_NOTED = 452;
	private static final int RUNITE_BOLTS = 9144;
	private static final int SOUL_RUNE = 566;

	private static final Map<Integer, String> NAMES = new HashMap<>();

	static
	{
		NAMES.put(RUNITE_ORE, "Runite ore");
		NAMES.put(RUNITE_ORE_NOTED, "Runite ore");
		NAMES.put(RUNITE_BOLTS, "Runite bolts");
		NAMES.put(SOUL_RUNE, "Soul rune");
	}

	private static SessionHistoryEntry entry(String boss, long endedAt, Map<Integer, Integer> loot)
	{
		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setBossName(boss);
		entry.setEndedAtEpochMilli(endedAt);
		entry.getLootItemQuantities().putAll(loot);
		return entry;
	}

	private static Map<Integer, Integer> loot(int... idQuantityPairs)
	{
		Map<Integer, Integer> loot = new HashMap<>();
		for (int i = 0; i < idQuantityPairs.length; i += 2)
		{
			loot.put(idQuantityPairs[i], idQuantityPairs[i + 1]);
		}
		return loot;
	}

	@Test
	public void findsSessionsNewestFirstAndCombinesNotedDrops()
	{
		SessionHistoryEntry older = entry("Vardorvis", 1000, loot(RUNITE_ORE_NOTED, 18));
		SessionHistoryEntry newer = entry("Duke Sucellus", 2000, loot(RUNITE_ORE, 1, RUNITE_ORE_NOTED, 30));
		SessionHistoryEntry unrelated = entry("Zulrah", 3000, loot(SOUL_RUNE, 100));

		List<ItemDropSearch.ItemMatch> matches =
			ItemDropSearch.search(Arrays.asList(older, newer, unrelated), NAMES::get, "runite ore");

		assertEquals(1, matches.size());
		ItemDropSearch.ItemMatch ore = matches.get(0);
		assertEquals("Runite ore", ore.getItemName());
		assertEquals(2, ore.getDrops().size());
		assertEquals("Duke Sucellus", ore.getDrops().get(0).getEntry().getBossName());
		assertEquals(31, ore.getDrops().get(0).getQuantity());
		assertEquals(49, ore.getTotalQuantity());
	}

	@Test
	public void partialNamesMatchSeveralItems()
	{
		SessionHistoryEntry session = entry("Vardorvis", 1000, loot(RUNITE_ORE, 18, RUNITE_BOLTS, 50));

		List<ItemDropSearch.ItemMatch> matches = ItemDropSearch.search(Arrays.asList(session), NAMES::get, "RUNITE");

		assertEquals(2, matches.size());
	}

	@Test
	public void minionAndPerfectBonusLootCount()
	{
		SessionHistoryEntry session = entry("Vardorvis", 1000, loot(RUNITE_ORE_NOTED, 18));
		session.getPerfectBonusItemQuantities().put(RUNITE_ORE_NOTED, 9);

		List<ItemDropSearch.ItemMatch> matches = ItemDropSearch.search(Arrays.asList(session), NAMES::get, "runite ore");

		assertEquals(27, matches.get(0).getTotalQuantity());
	}

	@Test
	public void shortQueriesMatchNothing()
	{
		SessionHistoryEntry session = entry("Vardorvis", 1000, loot(RUNITE_ORE, 18));

		assertTrue(ItemDropSearch.search(Arrays.asList(session), NAMES::get, "ru").isEmpty());
	}
}
