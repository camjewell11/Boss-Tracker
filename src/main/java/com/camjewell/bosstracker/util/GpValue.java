package com.camjewell.bosstracker.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the GP amounts typed into config text fields. These are text rather than numeric config
 * items because RuneLite renders an int item as a six-column spinner pinned to the right of its
 * label, which is too narrow to show a nine-digit value; a String item gets a full-width row of
 * its own instead.
 */
public final class GpValue
{
	/** Optional separators, then digits, then an optional k/m/b multiplier. */
	private static final Pattern GP_PATTERN = Pattern.compile("^([0-9][0-9,_ ]*(?:\\.[0-9]+)?)\\s*([kmb]?)$");

	/** Returned for text that isn't a usable amount, so callers can skip the tier. */
	public static final long INVALID = -1;

	private GpValue()
	{
	}

	/**
	 * Accepts plain digits, thousands separators, and k/m/b suffixes, so "3000000", "3,000,000",
	 * "3m" and "1.5m" are all the amounts they look like.
	 *
	 * @return the amount in gp, or {@link #INVALID} if the text can't be read as one.
	 */
	public static long parse(String text)
	{
		if (text == null)
		{
			return INVALID;
		}

		Matcher matcher = GP_PATTERN.matcher(text.trim().toLowerCase());
		if (!matcher.matches())
		{
			return INVALID;
		}

		double amount;
		try
		{
			amount = Double.parseDouble(matcher.group(1).replaceAll("[,_ ]", ""));
		}
		catch (NumberFormatException e)
		{
			return INVALID;
		}

		switch (matcher.group(2))
		{
			case "k":
				amount *= 1_000D;
				break;
			case "m":
				amount *= 1_000_000D;
				break;
			case "b":
				amount *= 1_000_000_000D;
				break;
			default:
				break;
		}

		if (amount < 0 || amount > Long.MAX_VALUE)
		{
			return INVALID;
		}
		return (long) amount;
	}
}
