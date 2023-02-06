package ru.yandex.market.mboinfo.app.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.joda.time.Interval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 28.06.19
 */
public class IntervalUtilsTest {


    public static final long MILLIS_IN_DAY = TimeUnit.DAYS.toMillis(1);

    @Test
    public void testIntervalIntersection() {
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-01/2019-01-02"), Collections.emptyList()));
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-01/2019-01-02"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-02-01/2019-02-02"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-01/2019-01-10"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-20/2019-01-21"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(MILLIS_IN_DAY,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-01/2019-01-11"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(MILLIS_IN_DAY,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-19/2019-01-22"), Collections.singletonList(
                Interval.parse("2019-01-10/2019-01-20")
            )));
        assertEquals(MILLIS_IN_DAY * 3,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-19/2019-01-22"), Collections.singletonList(
                Interval.parse("2019-01-19/2019-01-22")
            )));
        assertEquals(MILLIS_IN_DAY,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-04/2019-01-06"), Arrays.asList(
                Interval.parse("2019-01-01/2019-01-02"),
                Interval.parse("2019-01-05/2019-01-07")
            )));
        assertEquals(MILLIS_IN_DAY,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-04/2019-01-06"), Arrays.asList(
                Interval.parse("2019-01-01/2019-01-02"),
                Interval.parse("2019-01-05/2019-01-07")
            )));
        assertEquals(0,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-03/2019-01-05"), Arrays.asList(
                Interval.parse("2019-01-01/2019-01-02"),
                Interval.parse("2019-01-05/2019-01-07")
            )));
        assertEquals(MILLIS_IN_DAY * 3,
            IntervalUtils.getIntersectionMillis(Interval.parse("2019-01-04/2019-01-16"), Arrays.asList(
                Interval.parse("2019-01-01/2019-01-02"),
                Interval.parse("2019-01-05/2019-01-06"),
                Interval.parse("2019-01-10/2019-01-11"),
                Interval.parse("2019-01-15/2019-01-16"),
                Interval.parse("2019-01-20/2019-01-21")
            )));
    }

}
