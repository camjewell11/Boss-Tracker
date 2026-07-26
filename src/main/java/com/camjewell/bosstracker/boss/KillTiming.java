package com.camjewell.bosstracker.boss;

/**
 * How a boss's kill time is determined.
 */
public enum KillTiming
{
	/**
	 * The boss reports its own duration in a chat message (raids completion messages,
	 * "Fight duration:", "Challenge duration:", etc). The kill time is parsed from chat.
	 */
	SELF_REPORTED,

	/**
	 * The boss has no duration chat message. The kill time is measured from the first
	 * hitsplat dealt to the NPC until it dies.
	 */
	HITSPLAT
}
