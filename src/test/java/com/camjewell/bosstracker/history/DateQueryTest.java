package com.camjewell.bosstracker.history;

import java.time.LocalDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DateQueryTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

	private static void assertDay(LocalDate expected, String query)
	{
		DateQuery.Range range = DateQuery.parse(query, TODAY);
		assertEquals(query, expected, range.getStart());
		assertTrue(query, range.isSingleDay());
	}

	private static void assertRange(LocalDate start, LocalDate end, String query)
	{
		DateQuery.Range range = DateQuery.parse(query, TODAY);
		assertEquals(query, start, range.getStart());
		assertEquals(query, end, range.getEnd());
	}

	@Test
	public void relativeDays()
	{
		assertDay(TODAY, "Today");
		assertDay(LocalDate.of(2026, 10, 18), "yesterday");
	}

	@Test
	public void namedDays()
	{
		LocalDate oct17 = LocalDate.of(2026, 10, 17);
		assertDay(oct17, "Oct 17");
		assertDay(oct17, "october 17");
		assertDay(oct17, "17 Oct");
		assertDay(oct17, "Oct. 17, 2026");
		assertDay(LocalDate.of(2026, 9, 5), "Sept 5");
		assertEquals("Sat, Oct 17, 2026", DateQuery.parse("Oct 17", TODAY).getLabel());
	}

	@Test
	public void numericDays()
	{
		LocalDate oct17 = LocalDate.of(2026, 10, 17);
		assertDay(oct17, "10/17");
		assertDay(oct17, "10-17");
		assertDay(oct17, "10/17/2026");
		assertDay(oct17, "10/17/26");
		assertDay(oct17, "2026-10-17");
	}

	@Test
	public void wholeMonths()
	{
		LocalDate start = LocalDate.of(2026, 10, 1);
		LocalDate end = LocalDate.of(2026, 10, 31);
		assertRange(start, end, "October");
		assertRange(start, end, "oct 2026");
		assertRange(start, end, "10/2026");
		assertRange(start, end, "2026-10");
		assertEquals("October 2026", DateQuery.parse("oct", TODAY).getLabel());
		assertRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), "feb");
	}

	@Test
	public void wholeYears()
	{
		assertRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "2026");
		assertEquals("2026", DateQuery.parse("2026", TODAY).getLabel());
	}

	@Test
	public void withoutAYearMeansTheMostRecentOccurrence()
	{
		assertDay(LocalDate.of(2025, 12, 25), "Dec 25");
		assertRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31), "December");
	}

	@Test
	public void nonDatesAreRejected()
	{
		assertNull(DateQuery.parse("runite ore", TODAY));
		assertNull(DateQuery.parse("vorkath", TODAY));
		assertNull(DateQuery.parse("cox", TODAY));
		assertNull(DateQuery.parse("Feb 30", TODAY));
		assertNull(DateQuery.parse("13/40", TODAY));
		assertNull(DateQuery.parse("1500", TODAY));
	}
}
