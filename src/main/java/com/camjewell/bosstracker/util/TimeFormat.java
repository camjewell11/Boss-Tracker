package com.camjewell.bosstracker.util;

import com.camjewell.bosstracker.BossTrackerConfig;
import java.text.DecimalFormat;

/**
 * Shared time and KPH formatting, deduplicating what the original plugin repeated across
 * three separate methods.
 */
public final class TimeFormat
{
	private TimeFormat()
	{
	}

	/**
	 * Formats seconds as "mm:ss", or "hh:mm:ss" once past an hour.
	 */
	public static String minutesSeconds(int totalSeconds)
	{
		if (totalSeconds < 0)
		{
			totalSeconds = 0;
		}
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%02d:%02d", minutes, seconds);
	}

	/**
	 * Formats a kills-per-hour value per the configured calculation method.
	 */
	public static String kph(double killsPerHour, BossTrackerConfig.KphMethod method)
	{
		switch (method)
		{
			case PRECISE:
				return new DecimalFormat("#.#").format(killsPerHour);
			case ROUNDED:
				return String.valueOf(Math.round(killsPerHour));
			case ROUND_UP:
				return String.valueOf((int) Math.ceil(killsPerHour));
			case TRADITIONAL:
				return String.valueOf((int) killsPerHour);
			default:
				return String.valueOf(killsPerHour);
		}
	}
}
