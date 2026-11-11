package com.camjewell.bosstracker.history;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LootSummaryTest
{
	private static final int RUNITE_ORE = 451;
	private static final int FIRE_RUNE = 554;

	private static SessionHistoryEntry session(int kills, int seconds, Map<Integer, Integer> loot, Map<Integer, Long> prices)
	{
		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setKillsThisSession(kills);
		entry.setSessionDurationSeconds(seconds);
		entry.setLootItemQuantities(loot);
		entry.setItemPricesAtDrop(prices);
		return entry;
	}

	private static long priceAtDrop(SessionHistoryEntry entry, int itemId)
	{
		return entry.getItemPricesAtDrop().getOrDefault(itemId, 0L);
	}

	/** Each session's items are valued at that session's own prices. */
	@Test
	public void addsUpSessionsAtTheirOwnPrices()
	{
		SessionHistoryEntry cheap = session(3, 600, Collections.singletonMap(RUNITE_ORE, 10),
			Collections.singletonMap(RUNITE_ORE, 10_000L));
		SessionHistoryEntry dear = session(2, 300, Collections.singletonMap(RUNITE_ORE, 10),
			Collections.singletonMap(RUNITE_ORE, 12_000L));

		LootSummary summary = LootSummary.of(Arrays.asList(cheap, dear), LootSummaryTest::priceAtDrop);

		assertEquals(2, summary.getSessions());
		assertEquals(5, summary.getKills());
		assertEquals(900, summary.getSeconds());
		assertEquals(Integer.valueOf(20), summary.getLoot().getQuantities().get(RUNITE_ORE));
		assertEquals(220_000, summary.getLoot().getTotalValue(), 0.001);
		assertEquals(11_000, summary.getLoot().averagePrice(RUNITE_ORE));
	}

	/** Cache loot stays out of the main loot and its total, as each session keeps it. */
	@Test
	public void keepsSectionsApart()
	{
		Map<Integer, Long> prices = new HashMap<>();
		prices.put(RUNITE_ORE, 10_000L);
		prices.put(FIRE_RUNE, 5L);
		SessionHistoryEntry entry = session(1, 60, Collections.singletonMap(RUNITE_ORE, 1), prices);
		entry.setCacheLootItemQuantities(Collections.singletonMap(FIRE_RUNE, 400));
		entry.setCachesOpened(2);

		LootSummary summary = LootSummary.of(Collections.singletonList(entry), LootSummaryTest::priceAtDrop);

		assertEquals(10_000, summary.getLoot().getTotalValue(), 0.001);
		assertEquals(Integer.valueOf(400), summary.getCacheLoot().getQuantities().get(FIRE_RUNE));
		assertEquals(2, summary.getCacheLoot().getCount());
		assertEquals(2_000, summary.getCacheLoot().getTotalValue(), 0.001);
		assertTrue(summary.getMinionLoot().isEmpty());
		assertTrue(summary.getSupplies().isEmpty());
	}
}
