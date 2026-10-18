package com.camjewell.bosstracker.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChatKillParserPetTest
{
	@Test
	public void recognisesThePlayersOwnPetMessages()
	{
		assertTrue(ChatKillParser.isOwnPetMessage("You have a funny feeling like you're being followed."));
		assertTrue(ChatKillParser.isOwnPetMessage("You feel something weird sneaking into your backpack."));
		assertTrue(ChatKillParser.isOwnPetMessage("You have a funny feeling like you would have been followed..."));
	}

	@Test
	public void ignoresOtherPlayersBroadcasts()
	{
		// Copied from clan broadcasts in the RuneLite log.
		assertFalse(ChatKillParser.isOwnPetMessage(
			"<img=2> hemi has a funny feeling like she's being followed: Pet Kraken at 2,715 killcount from Kraken."));
		assertFalse(ChatKillParser.isOwnPetMessage(
			"koiijin feels something weird sneaking into his backpack: Callisto cub at 9 killcount (Callisto)"));
	}
}
