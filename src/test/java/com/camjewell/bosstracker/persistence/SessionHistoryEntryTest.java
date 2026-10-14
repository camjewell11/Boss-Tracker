package com.camjewell.bosstracker.persistence;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Pins the saved history format: once trips are written to users' history files, changing their
 * shape means handling old data, so a silent format change should fail here first.
 */
public class SessionHistoryEntryTest
{
	private final Gson gson = new Gson();

	@Test
	public void entrySavedBeforeTripsLoadsWithNoTrips()
	{
		String legacy = "{\"bossName\":\"Cerberus\",\"endedAtEpochMilli\":1787447952082,\"killsThisSession\":15,"
			+ "\"onSlayerTask\":true,\"lootItemQuantities\":{\"25778\":15}}";

		SessionHistoryEntry entry = gson.fromJson(legacy, SessionHistoryEntry.class);

		assertNotNull(entry.getTrips());
		assertTrue(entry.getTrips().isEmpty());
		assertNotNull(entry.getMinionLootItemQuantities());
		assertTrue(entry.getMinionLootItemQuantities().isEmpty());
		assertEquals(0, entry.getMinionKills());
		assertEquals(15, entry.getKillsThisSession());
	}

	@Test
	public void tripsRoundTrip()
	{
		Trip banked = new Trip(1000L);
		banked.setEndedAtEpochMilli(61000L);
		banked.setKills(6);
		banked.setFightSeconds(420);
		banked.getLootItemQuantities().put(25778, 6);
		banked.setEndReason(TripEndReason.BANKED);

		Trip died = new Trip(70000L);
		died.setEndedAtEpochMilli(90000L);
		died.setKills(1);
		died.setEndReason(TripEndReason.DEATH);

		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setBossName("Cerberus");
		entry.getTrips().add(banked);
		entry.getTrips().add(died);

		String json = gson.toJson(entry);
		assertTrue("end reasons are stored by name", json.contains("\"endReason\":\"BANKED\""));

		SessionHistoryEntry loaded = gson.fromJson(json, SessionHistoryEntry.class);
		assertEquals(entry.getTrips(), loaded.getTrips());
		assertEquals(TripEndReason.DEATH, loaded.getTrips().get(1).getEndReason());
		assertEquals(Integer.valueOf(6), loaded.getTrips().get(0).getLootItemQuantities().get(25778));
	}
}
