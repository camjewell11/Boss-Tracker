package com.camjewell.bosstracker.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GpValueTest
{
	@Test
	public void parsesPlainDigits()
	{
		assertEquals(3_000_000L, GpValue.parse("3000000"));
		assertEquals(0L, GpValue.parse("0"));
	}

	@Test
	public void parsesSuffixes()
	{
		assertEquals(3_000L, GpValue.parse("3k"));
		assertEquals(3_000_000L, GpValue.parse("3m"));
		assertEquals(1_500_000L, GpValue.parse("1.5m"));
		assertEquals(2_000_000_000L, GpValue.parse("2b"));
	}

	@Test
	public void parsesSeparatorsAndSpacingAndCase()
	{
		assertEquals(3_000_000L, GpValue.parse("3,000,000"));
		assertEquals(3_000_000L, GpValue.parse("3_000_000"));
		assertEquals(3_000_000L, GpValue.parse("  3M  "));
		assertEquals(100_000_000L, GpValue.parse("100m"));
	}

	@Test
	public void rejectsUnusableText()
	{
		assertEquals(GpValue.INVALID, GpValue.parse(null));
		assertEquals(GpValue.INVALID, GpValue.parse(""));
		assertEquals(GpValue.INVALID, GpValue.parse("   "));
		assertEquals(GpValue.INVALID, GpValue.parse("abc"));
		assertEquals(GpValue.INVALID, GpValue.parse("3x"));
		assertEquals(GpValue.INVALID, GpValue.parse("-3m"));
		assertEquals(GpValue.INVALID, GpValue.parse("3mm"));
		assertEquals(GpValue.INVALID, GpValue.parse("m3"));
	}
}
