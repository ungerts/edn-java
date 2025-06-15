/*
 * Copyright (c) 2025 JabRef Authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 */
package org.jabref.edn.parser;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.edn.EdnSyntaxException;


public class InstantUtils {

    private InstantUtils() {
        // prevent instantiation
    }

    private static final Pattern INSTANT = Pattern.compile(
            "(\\d\\d\\d\\d)(?:-(\\d\\d)(?:-(\\d\\d)" +
                    "(?:[T](\\d\\d)(?::(\\d\\d)(?::(\\d\\d)(?:[.](\\d{1,9}))?)?)?)?)?)?" +
            "(?:[Z]|([-+])(\\d\\d):(\\d\\d))?");

    static ParsedInstant parse(String value) {
        Matcher m = INSTANT.matcher(value);
        if (!m.matches()) {
            throw new EdnSyntaxException("Can't parse " + "\"" + value + "\"");
        }

        final int years = Integer.parseInt(m.group(1));
        final int months = parseIntOrElse(m.group(2), 1);
        final int days = parseIntOrElse(m.group(3), 1);
        final int hours = parseIntOrElse(m.group(4), 0);
        final int minutes = parseIntOrElse(m.group(5), 0);
        final int seconds = parseIntOrElse(m.group(6), 0);
        final int nanoseconds = parseNanoseconds(m.group(7));
        final int offsetSign = parseOffsetSign(m.group(8));
        final int offsetHours = parseIntOrElse(m.group(9), 0);
        final int offsetMinutes = parseIntOrElse(m.group(10), 0);

        // extra-grammatical restrictions from RFC3339

        if (months < 1 || 12 < months) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid month in '%s'",
                            months, value));
        }
        if (days < 1 || daysInMonth(months, isLeapYear(years)) < days) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid day in '%s'",
                            days, value));
        }
        if (hours < 0 || 23 < hours) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid hour in '%s'",
                            hours, value));
        }
        if (minutes < 0 || 59 < minutes) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid minute in '%s'",
                            minutes, value));
        }
        if (seconds < 0 || (minutes == 59 ? 60 : 59) < seconds) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid second in '%s'",
                            seconds, value));
        }
        assert 0 <= nanoseconds && nanoseconds <= 999999999:
            "nanoseconds are assured to be in [0..999999999] by INSTANT Pattern";
        assert -1 <= offsetSign && offsetSign <= 1:
            "parser assures offsetSign is -1, 0 or 1.";
        if (offsetHours < 0 || 23 < offsetHours) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid offset hour in '%s'",
                            offsetHours, value));
        }
        if (offsetMinutes < 0 || 59 < offsetMinutes) {
            throw new EdnSyntaxException(
                    String.format("'%02d' is not a valid offset minute in '%s'",
                            offsetMinutes, value));
        }

        return new ParsedInstant(years, months, days, hours, minutes, seconds,
                nanoseconds, offsetSign, offsetHours, offsetMinutes);
    }

    static boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    static int daysInMonth(int month, boolean isLeapYear) {
        int i = (month - 1) + 12 * (isLeapYear ? 1 : 0);
        return DAYS_IN_MONTH[i];
    }

    private static final byte[] DAYS_IN_MONTH = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, // non-leap-year
        31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, // leap year
    };

    private static int parseOffsetSign(String s) {
        if (s == null) {
            return 0;
        } else {
            return "-".equals(s) ? -1 : 1;
        }
    }

    static int parseNanoseconds(String s) {
        if (s == null) {
            return 0;
        } else if (s.length() < 9) {
            return Integer.parseInt(s + "000000000".substring(s.length()));
        } else {
            return Integer.parseInt(s);
        }
    }

    private static int parseIntOrElse(String s, int alternative) {
        if (s == null) {
            return alternative;
        }
        return Integer.parseInt(s);
    }


    static Timestamp makeTimestamp(ParsedInstant pi) {
        GregorianCalendar c = makeCalendar(pi);
        Timestamp ts = new Timestamp((c.getTimeInMillis() / 1000L) * 1000L);
        ts.setNanos(pi.nanoseconds);
        return ts;
    }

    static Date makeDate(ParsedInstant pi) {
        return makeCalendar(pi).getTime();
    }

    static GregorianCalendar makeCalendar(ParsedInstant pi) {
        final TimeZone tz = getTimeZone(pi.offsetSign, pi.offsetHours, pi.offsetMinutes);
        final GregorianCalendar cal = new GregorianCalendar(tz);
        cal.set(Calendar.YEAR, pi.years);
        cal.set(Calendar.MONTH, pi.months - 1);
        cal.set(Calendar.DAY_OF_MONTH, pi.days);
        cal.set(Calendar.HOUR_OF_DAY, pi.hours);
        cal.set(Calendar.MINUTE, pi.minutes);
        cal.set(Calendar.SECOND, pi.seconds);
        int millis = pi.nanoseconds / NANOSECS_PER_MILLISEC;
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static final int TZ_LIMIT = 23;

    private static final TimeZone[] TZ_CACHE;
    static {
        TimeZone[] tzs = new TimeZone[TZ_LIMIT*2+1];
        for (int h = -TZ_LIMIT; h <= TZ_LIMIT; h++) {
            tzs[h+TZ_LIMIT] = TimeZone.getTimeZone(String.format("GMT%+03d:00", h));
        }
        TZ_CACHE = tzs;
    }

    private static TimeZone getTimeZone(int offsetSign, int offsetHours, int offsetMinutes) {
        if (offsetMinutes == 0 && offsetHours <= TZ_LIMIT) {
            int i = offsetHours * (offsetSign < 0 ? -1 : 1) + TZ_LIMIT;
            return TZ_CACHE[i];
        }
        final String tzID = String.format("GMT%s%02d:%02d",
                (offsetSign > 0 ? "+" : "-"),
                offsetHours, offsetMinutes);
        return TimeZone.getTimeZone(tzID);
    }

    private static final int NANOSECS_PER_MILLISEC = 1000000;

    /**
     * Return a String suitable for use as an edn {@code #inst}, given
     * a {@link GregorianCalendar}.
     * @param cal must not be null.
     * @return an RFC3339 compatible string.
     */
    public static String calendarToString(GregorianCalendar cal) {
        Instant instant = cal.toInstant();
        return RFC3339.format(instant);
    }

    /**
     * Return a String suitable for use as an edn {@code #inst}, given
     * a {@link Date}.
     * @param date must not be null.
     * @return an RFC3339 compatible string.
     */
    public static String dateToString(Date date) {
        Instant instant = date.toInstant();
        return RFC3339.format(instant);
    }

  /**
     * A DateTimeFormatter for RFC3339, which is the format used by edn
     * for {@code #inst} values.
     */
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXXXX")
        .withZone(ZoneOffset.UTC);

    public static String timestampToString(Timestamp ts) {
        Instant instant = ts.toInstant();
        return RFC3339.format(instant);
    }

    public static String instantToString(Instant instant) {
        return RFC3339.format(instant);
    }

    public static String zonedDateTimeToString(ZonedDateTime zdt) {
        // Always serialize as UTC
        return RFC3339.format(zdt.withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String localDateToString(LocalDate localDate) {
        // Use JVM default timezone, then convert to UTC
        return RFC3339.format(localDate.atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String localTimeToString(LocalTime localTime) {
        // Combine with current date, JVM default timezone, then to UTC
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return RFC3339.format(localTime.atDate(today).atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String localDateTimeToString(LocalDateTime localDateTime) {
        // Use JVM default timezone, then convert to UTC
        return RFC3339.format(localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String offsetDateTimeToString(OffsetDateTime offsetDateTime) {
        // Always serialize as UTC
        return RFC3339.format(offsetDateTime.toInstant());
    }

    public static String offsetTimeToString(OffsetTime offsetTime) {
        // Combine with current date, then to UTC
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return RFC3339.format(offsetTime.atDate(today).toInstant());
    }

    public static String yearToString(Year year) {
        // January 1st of the year, JVM default timezone, then to UTC
        return RFC3339.format(year.atDay(1).atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String yearMonthToString(YearMonth yearMonth) {
        // 1st day of the month, JVM default timezone, then to UTC
        return RFC3339.format(yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    public static String monthDayToString(MonthDay monthDay) {
        // Combine with current year, JVM default timezone, then to UTC
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        return RFC3339.format(monthDay.atYear(year).atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }
}

/*
 * Notes
 *
 * RFC3339 says to use -00:00 when the timezone is unknown (+00:00
 * implies a known GMT)
 */
