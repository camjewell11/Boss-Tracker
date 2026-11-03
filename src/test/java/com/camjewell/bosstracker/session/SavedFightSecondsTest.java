package com.camjewell.bosstracker.session;

import com.camjewell.bosstracker.persistence.SessionHistoryEntry;
import com.camjewell.bosstracker.persistence.Trip;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SavedFightSecondsTest
{
	@Test
	public void usesTheRecordedFightTime()
	{
		SessionHistoryEntry entry = entry(3600, 900);
		entry.setFightSeconds(2400);

		assertEquals(2400, SessionManager.savedFightSeconds(entry));
	}

	@Test
	public void olderSessionsAddUpTheirTrips()
	{
		SessionHistoryEntry entry = entry(3600, 900);
		Trip first = new Trip(0);
		first.setFightSeconds(1000);
		Trip second = new Trip(0);
		second.setFightSeconds(1500);
		entry.setTrips(Arrays.asList(first, second));

		assertEquals(2500, SessionManager.savedFightSeconds(entry));
	}

	@Test
	public void olderSessionsWithoutTripsTakeOffTheIdleTime()
	{
		assertEquals(2700, SessionManager.savedFightSeconds(entry(3600, 900)));
	}

	private static SessionHistoryEntry entry(int durationSeconds, int idleSeconds)
	{
		SessionHistoryEntry entry = new SessionHistoryEntry();
		entry.setSessionDurationSeconds(durationSeconds);
		entry.setIdleSeconds(idleSeconds);
		return entry;
	}
}
