package com.camjewell.bosstracker.history;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Reads a search query as a day, month or year, so History can be searched by date.
 *
 * <ul>
 * <li>Days: "today", "yesterday", "Oct 17", "October 17", "17 Oct", "10/17", "10-17",
 * "10/17/2026", "2026-10-17"</li>
 * <li>Months: "October", "Oct 2026", "10/2026", "2026-10"</li>
 * <li>Years: "2026"</li>
 * </ul>
 *
 * Without a year, a day or month means its most recent occurrence, so in October "Dec 25" is
 * last Christmas and "December" is last December.
 */
public final class DateQuery
{
	@Value
	public static class Range
	{
		/** First and last day, inclusive. */
		LocalDate start;
		LocalDate end;
		/** How to name the range: "Sat, Oct 17", "October 2026" or "2026". */
		String label;

		public boolean contains(LocalDate day)
		{
			return !day.isBefore(start) && !day.isAfter(end);
		}

		public boolean isSingleDay()
		{
			return start.equals(end);
		}
	}

	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	private static final Pattern ISO_DAY = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
	private static final Pattern ISO_MONTH = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
	private static final Pattern NUMERIC_DAY = Pattern.compile("^(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2}|\\d{4}))?$");
	private static final Pattern NUMERIC_MONTH = Pattern.compile("^(\\d{1,2})[/-](\\d{4})$");
	private static final Pattern MONTH_DAY = Pattern.compile("^([a-z]{3,9})\\.?\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?$");
	private static final Pattern DAY_MONTH = Pattern.compile("^(\\d{1,2})\\s+([a-z]{3,9})\\.?(?:\\s+(\\d{4}))?$");
	private static final Pattern MONTH_ONLY = Pattern.compile("^([a-z]{3,9})\\.?(?:,?\\s+(\\d{4}))?$");
	private static final Pattern YEAR_ONLY = Pattern.compile("^(20\\d{2})$");

	private DateQuery()
	{
	}

	/**
	 * @return the days the query names, or null if it isn't a date
	 */
	public static Range parse(String query, LocalDate today)
	{
		if (query == null)
		{
			return null;
		}
		String q = query.trim().toLowerCase(Locale.ROOT);
		try
		{
			return parseLowercase(q, today);
		}
		catch (DateTimeException e)
		{
			// "Feb 30", "13/40" and the like aren't dates.
			return null;
		}
	}

	private static Range parseLowercase(String q, LocalDate today)
	{
		if (q.equals("today"))
		{
			return day(today);
		}
		if (q.equals("yesterday"))
		{
			return day(today.minusDays(1));
		}

		Matcher m = ISO_DAY.matcher(q);
		if (m.matches())
		{
			return day(LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
		}
		m = ISO_MONTH.matcher(q);
		if (m.matches())
		{
			return month(YearMonth.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
		}
		m = NUMERIC_MONTH.matcher(q);
		if (m.matches())
		{
			return month(YearMonth.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))));
		}
		m = NUMERIC_DAY.matcher(q);
		if (m.matches())
		{
			return day(resolveDay(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), m.group(3), today));
		}
		m = MONTH_DAY.matcher(q);
		if (m.matches())
		{
			Month month = monthNamed(m.group(1));
			return month == null ? null : day(resolveDay(month.getValue(), Integer.parseInt(m.group(2)), m.group(3), today));
		}
		m = DAY_MONTH.matcher(q);
		if (m.matches())
		{
			Month month = monthNamed(m.group(2));
			return month == null ? null : day(resolveDay(month.getValue(), Integer.parseInt(m.group(1)), m.group(3), today));
		}
		m = MONTH_ONLY.matcher(q);
		if (m.matches())
		{
			Month month = monthNamed(m.group(1));
			if (month == null)
			{
				return null;
			}
			if (m.group(2) != null)
			{
				return month(YearMonth.of(Integer.parseInt(m.group(2)), month));
			}
			YearMonth thisYear = YearMonth.of(today.getYear(), month);
			return month(thisYear.isAfter(YearMonth.from(today)) ? thisYear.minusYears(1) : thisYear);
		}
		m = YEAR_ONLY.matcher(q);
		if (m.matches())
		{
			int year = Integer.parseInt(m.group(1));
			return new Range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), String.valueOf(year));
		}
		return null;
	}

	private static Range day(LocalDate day)
	{
		return new Range(day, day, DAY_LABEL.format(day));
	}

	private static Range month(YearMonth month)
	{
		return new Range(month.atDay(1), month.atEndOfMonth(), MONTH_LABEL.format(month));
	}

	private static LocalDate resolveDay(int month, int day, String year, LocalDate today)
	{
		if (year != null)
		{
			int y = Integer.parseInt(year);
			return LocalDate.of(year.length() == 2 ? 2000 + y : y, month, day);
		}
		LocalDate thisYear = LocalDate.of(today.getYear(), month, day);
		return thisYear.isAfter(today) ? thisYear.minusYears(1) : thisYear;
	}

	/** "oct", "october" and "sept" all work; anything else returns null. */
	private static Month monthNamed(String name)
	{
		for (Month month : Month.values())
		{
			String full = month.name().toLowerCase(Locale.ROOT);
			if ((name.length() >= 3 && full.startsWith(name)) || (name.equals("sept") && month == Month.SEPTEMBER))
			{
				return month;
			}
		}
		return null;
	}
}
