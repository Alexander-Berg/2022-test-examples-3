package ru.yandex.market.crm.core.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 14.12.18.
 */
public class TimeSpanTest {
    private static final long YEAR_MONTH_INTERVAL_EXPECTED_IN_SEC = 36741600L;
    private static final long DAY_TIME_INTERVAL_EXPECTED_IN_SEC = 273906L;
    private static final long MIXED_INTERVAL_EXPECTED_IN_SEC = -36497106L;
    private static final long ONE_DAY_ONE_MONTH_ETC = 34239661;
    private static final String PG_VERBOSE = "@ 1 year 1 mon 1 day 1 hour 1 min 1 sec";
    private static final String PG_VERBOSE_X2 = "@ 2 years 2 mons 2 days 2 hours 2 mins 2 secs";
    private static final String ISO_8601 = "P1Y1M1DT1H1M1S";


    @Test
    public void alpha() {
        TimeSpan a = new TimeSpan("1 day 05:20:00");
        System.out.println(a.toPgVerboseFormat());
    }

    @Test
    public void parseNullValue() {
        TimeSpan actual = new TimeSpan(null);
        assertEquals(0, actual.getSeconds());
    }

    @Test
    public void parseEmptyValue() {
        TimeSpan actual = new TimeSpan("");
        assertEquals(0, actual.getSeconds());
    }

    @Test
    public void parseYearMonthIntervalPostgres() {
        TimeSpan actual = new TimeSpan("1 year 2 mons");
        assertEquals(YEAR_MONTH_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseYearMonthIntervalPostgresVerbose() {
        TimeSpan actual = new TimeSpan("@ 1 year 2 mons");
        assertEquals(YEAR_MONTH_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseYearMonthIntervalIso8601() {
        TimeSpan actual = new TimeSpan("P1Y2M");
        assertEquals(YEAR_MONTH_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseDayTimeIntervalPostgres() {
        TimeSpan actual = new TimeSpan("3 days 04:05:06");
        assertEquals(DAY_TIME_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseDayTimeIntervalPostgresVerbose() {
        TimeSpan actual = new TimeSpan("@ 3 days 4 hours 5 mins 6 secs");
        assertEquals(DAY_TIME_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseDayTimeIntervalIso8601() {
        TimeSpan actual = new TimeSpan("P3DT4H5M6S");
        assertEquals(DAY_TIME_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseMixedIntervalPostgres() {
        TimeSpan actual = new TimeSpan("-1 year -2 mons +3 days -04:05:06");
        assertEquals(MIXED_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseMixedIntervalPostgresVerbose() {
        TimeSpan actual = new TimeSpan("@ 1 year 2 mons -3 days 4 hours 5 mins 6 secs ago");
        assertEquals(MIXED_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void parseMixedTimeIntervalIso8601() {
        TimeSpan actual = new TimeSpan("P-1Y-2M3DT-4H-5M-6S");
        assertEquals(MIXED_INTERVAL_EXPECTED_IN_SEC, actual.getSeconds());
    }

    @Test
    public void toPgVerboseFormatOneDayOneMonthEtc() {
        TimeSpan actual = new TimeSpan(ONE_DAY_ONE_MONTH_ETC);
        assertEquals(PG_VERBOSE, actual.toPgVerboseFormat());
    }

    @Test
    public void toPgVerboseFormatTwoDayTwoMonthEtc() {
        TimeSpan actual = new TimeSpan(ONE_DAY_ONE_MONTH_ETC * 2);
        assertEquals(PG_VERBOSE_X2, actual.toPgVerboseFormat());
    }

    @Test
    public void iso8601FormatOneDayOneMonthEtc() {
        TimeSpan actual = new TimeSpan(ONE_DAY_ONE_MONTH_ETC);
        assertEquals(ISO_8601, actual.toIso8601Format());
    }
}
