package com.camjewell.bosstracker.chat;

import com.camjewell.bosstracker.BossTrackerConfig;
import com.camjewell.bosstracker.boss.Boss;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Messages copied from the RuneLite log of a clear on 2026-10-23.
 */
public class ChatKillParserArchiveTest
{
	private static final BossTrackerConfig.DksSelector KINGS = BossTrackerConfig.DksSelector.KINGS;

	@Test
	public void completionTime()
	{
		assertEquals(Integer.valueOf(27 * 60 + 57), ChatKillParser.parseDurationSeconds(
			"The Fractured Archive: Total completion time: <col=ef1020>27:57.60</col> (new personal best)", false));
	}

	@Test
	public void roomCompletionsAreNotTheRaidsTime()
	{
		assertNull(ChatKillParser.parseDurationSeconds(
			"Challenge complete: Ocellus. Duration: <col=ef1020>13:08.40</col>. Total: <col=ef1020>27:57.60</col> (new personal best)", false));
		assertNull(ChatKillParser.parseDurationSeconds(
			"Challenge complete: Xarth. Duration: <col=ef1020>4:33.00</col>. Total: <col=ef1020>18:29.40</col> (new personal best)", false));
	}

	@Test
	public void killCountMessage()
	{
		String message = "Your completed Fractured Archive count is: <col=ff0000>1</col>.";
		assertEquals(Boss.FRACTURED_ARCHIVE, ChatKillParser.findKcBoss(message, KINGS));
		assertEquals(1, ChatKillParser.parseKillCount(message));
		assertEquals(Boss.TOA_NORMAL,
			ChatKillParser.findKcBoss("Your completed Tombs of Amascut count is: <col=ff0000>3</col>.", KINGS));
	}

	@Test
	public void archiveActivity()
	{
		assertTrue(ChatKillParser.isFracturedArchiveActivity("Axecutioner has entered the Fractured Archive. Step inside to join him..."));
		assertTrue(ChatKillParser.isFracturedArchiveActivity("You enter the Archive..."));
		assertTrue(ChatKillParser.isFracturedArchiveActivity("Challenge started: Archive Guardian."));
		assertTrue(ChatKillParser.isFracturedArchiveActivity("Scuffed Boi has proceeded to face Dreadnoth the Jellied. Join him..."));
		assertTrue(ChatKillParser.isFracturedArchiveActivity("Scuffed Boi has proceeded to the inner vault. Join him..."));
		assertTrue(ChatKillParser.isFracturedArchiveActivity("You failed to survive the Fractured Archive."));

		assertFalse(ChatKillParser.isFracturedArchiveActivity("Challenge started: Path of Het."));
		assertFalse(ChatKillParser.isFracturedArchiveActivity("You have died. Total deaths: <col=ff0000>3</col>."));
	}

	@Test
	public void durationsOverAnHour()
	{
		assertEquals(Integer.valueOf(3600 + 2 * 60 + 3), ChatKillParser.parseDurationSeconds(
			"Theatre of Blood total completion time: <col=ff0000>1:02:03.40</col>", false));
		assertEquals(Integer.valueOf(83), ChatKillParser.parseDurationSeconds(
			"Fight duration: <col=ff0000>1:23</col>. Personal best: 1:07", false));
		assertEquals(Integer.valueOf(12 * 60 + 34), ChatKillParser.parseDurationSeconds(
			"Fight duration: <col=ff0000>12:34.60</col>.", false));
	}
}
