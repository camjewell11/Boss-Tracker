package com.camjewell.bosstracker.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GpFormatTest
{
	@Test
	public void compactKeepsDigitsBelowABillion()
	{
		assertEquals("0", GpFormat.compact(0));
		assertEquals("999,999,999", GpFormat.compact(999_999_999));
	}

	@Test
	public void compactAbbreviatesBillions()
	{
		assertEquals("1.00b", GpFormat.compact(1_000_000_000));
		assertEquals("2.15b", GpFormat.compact(2_147_483_647));
		assertEquals("12.50b", GpFormat.compact(12_500_000_000L));
	}

	@Test
	public void abbreviatedCoversEveryTier()
	{
		assertEquals("950", GpFormat.abbreviated(950));
		assertEquals("12.50k", GpFormat.abbreviated(12_500));
		assertEquals("3.20m", GpFormat.abbreviated(3_200_000));
		assertEquals("1.23b", GpFormat.abbreviated(1_234_567_890));
	}

	@Test
	public void shorthandWritesTiersAsTheyAreTyped()
	{
		assertEquals("3m", GpFormat.shorthand(GpValue.parse("3000000")));
		assertEquals("3m", GpFormat.shorthand(GpValue.parse("3,000,000")));
		assertEquals("1.5m", GpFormat.shorthand(GpValue.parse("1.5m")));
		assertEquals("100m", GpFormat.shorthand(100_000_000));
		assertEquals("2.25b", GpFormat.shorthand(2_250_000_000L));
		assertEquals("750k", GpFormat.shorthand(750_000));
		assertEquals("1.23m", GpFormat.shorthand(1_234_567));
		assertEquals("500", GpFormat.shorthand(500));
	}

	@Test
	public void exactKeepsEveryDigit()
	{
		assertEquals("1,234,567,890", GpFormat.exact(1_234_567_890));
	}
}
