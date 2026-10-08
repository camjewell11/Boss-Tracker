package com.camjewell.bosstracker.persistence;

/**
 * Why a trip ended. Stored with each trip rather than reduced to a count, so a session's history
 * can show, for example, how many of its trips ended in a death.
 */
public enum TripEndReason
{
	DEATH,
	BANKED,
	LEFT_AREA,
	SESSION_ENDED
}
