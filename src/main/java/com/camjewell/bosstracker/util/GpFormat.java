package com.camjewell.bosstracker.util;

/**
 * Formats GP amounts for display. The sidebar is narrow, so a full ten-digit amount (a mega-rare
 * raid drop) doesn't fit beside its neighbours; {@link #compact} keeps exact digits up to a
 * billion and switches to a "b" suffix from there.
 */
public final class GpFormat
{
	private static final double THOUSAND = 1_000D;
	private static final double MILLION = 1_000_000D;
	private static final double BILLION = 1_000_000_000D;

	private GpFormat()
	{
	}

	/** Every digit, with separators: "1,234,567,890". For tooltips, where width doesn't matter. */
	public static String exact(double value)
	{
		return String.format("%,.0f", value);
	}

	/** Exact digits below a billion, "1.23b" from there. */
	public static String compact(double value)
	{
		if (Math.abs(value) >= BILLION)
		{
			return String.format("%,.2fb", value / BILLION);
		}
		return exact(value);
	}

	/** Always abbreviated past a thousand: "950", "12.50k", "3.20m", "1.23b". */
	public static String abbreviated(double value)
	{
		double abs = Math.abs(value);
		if (abs >= BILLION)
		{
			return String.format("%,.2fb", value / BILLION);
		}
		if (abs >= MILLION)
		{
			return String.format("%,.2fm", value / MILLION);
		}
		if (abs >= THOUSAND)
		{
			return String.format("%,.2fk", value / THOUSAND);
		}
		return exact(value);
	}
}
