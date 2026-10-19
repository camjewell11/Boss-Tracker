package com.camjewell.bosstracker.capture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UnrecognisedActivityCaptureTest
{
	@Test
	public void knownBossesAreNotCaptured()
	{
		// Copied from the RuneLite log.
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your Vardorvis kill count is: <col=ff0000>52</col>."));
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your Corrupted Gauntlet completion count is: <col=ff0000>687</col>."));
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your Mad Angel kill count is: <col=ff0000>80</col>."));
		// Known even when the Dagannoth Kings selector is set to another king.
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your Dagannoth Rex kill count is: <col=ff0000>10</col>."));
	}

	@Test
	public void newEncountersAreCaptured()
	{
		assertTrue(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your completed New Raid count is: <col=ff0000>1</col>."));
		assertTrue(UnrecognisedActivityCapture.isUnrecognisedKillCount("Your New Boss kill count is: <col=ff0000>1,234</col>."));
	}

	@Test
	public void otherMessagesAreIgnored()
	{
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("Fight duration: <col=ff0000>1:52</col>. Personal best: 1:07"));
		assertFalse(UnrecognisedActivityCapture.isUnrecognisedKillCount("You have a funny feeling like you're being followed."));
	}
}
