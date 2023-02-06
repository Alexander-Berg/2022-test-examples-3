package ru.yandex.market.clickphite;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Range;
import junit.framework.Assert;
import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.DateTimeUtils;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.TimeRange;

public class DateTimeUtilsTest {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    @Test
    public void testToPeriodStart() throws Exception {
        test(MetricPeriod.ONE_MIN, "2015-03-05 01:39:38", "2015-03-05 01:39:00", "2015-03-05 01:40:00");
        test(MetricPeriod.FIVE_MIN, "2015-03-05 01:39:38", "2015-03-05 01:35:00", "2015-03-05 01:40:00");
        test(MetricPeriod.HOUR, "2015-03-05 01:39:38", "2015-03-05 01:00:00", "2015-03-05 02:00:00");
        test(MetricPeriod.DAY, "2015-03-05 01:39:38", "2015-03-05 00:00:00", "2015-03-06 00:00:00");
        test(MetricPeriod.WEEK, "2015-02-25 01:39:38", "2015-02-23 00:00:00", "2015-03-02 00:00:00");
        test(MetricPeriod.WEEK, "2015-03-05 01:39:38", "2015-03-02 00:00:00", "2015-03-09 00:00:00");
        test(MetricPeriod.WEEK, "2015-03-01 01:39:38", "2015-02-23 00:00:00", "2015-03-02 00:00:00");
        test(MetricPeriod.MONTH, "2015-03-05 01:39:38", "2015-03-01 00:00:00", "2015-04-01 00:00:00");
        test(MetricPeriod.QUARTER, "2015-03-05 01:39:38", "2015-01-01 00:00:00", "2015-04-01 00:00:00");
    }

    private void test(MetricPeriod period, String date, String expectedStart, String expectedEnd) throws Exception {
        Date dateToRound = dateFormat.parse(date);
        int timestampSeconds = (int) (dateToRound.getTime() / 1000);
        if (expectedStart != null) {
            Date expectedStartDate = dateFormat.parse(expectedStart);
            Assert.assertEquals(expectedStartDate, DateTimeUtils.toPeriodStart(period, dateToRound));
            Assert.assertEquals(expectedStartDate, DateTimeUtils.toPeriodStart(period, timestampSeconds));
        }

        if (expectedEnd != null) {
            Date expectedEndDate = dateFormat.parse(expectedEnd);
            Assert.assertEquals(expectedEndDate, DateTimeUtils.toPeriodEnd(period, dateToRound));
            Assert.assertEquals(expectedEndDate, DateTimeUtils.toPeriodEnd(period, timestampSeconds));
        }
    }

    @Test
    public void testSliceToTimeRanges() throws Exception {
        testSlice(
            1454878800, 1455051600, MetricPeriod.DAY, 10,
            tr(1454965200, 1455051600),
            tr(1454878800, 1454965200)
        );

        testSlice(
            1454878800, 1455051600, MetricPeriod.DAY, 1,
            tr(1454965200, 1455051600)
        );
    }

    public static TimeRange tr(int start, int end) {
        return new TimeRange(start, end);
    }

    public void testSlice(int start, int end, MetricPeriod period, int limit, TimeRange... expected) throws Exception {
        List<TimeRange> ranges = DateTimeUtils.sliceToTimeRangesWithMovingPeriods(
            Range.closedOpen(start, end), MetricPeriod.DAY, limit, false
        );
        Assert.assertEquals(Arrays.asList(expected), ranges);
    }

    @Test
    public void testTasks() {
        List<TimeRange> timeRanges = Collections.singletonList(new TimeRange(1439317680, 1439317680));
//        timeRanges = DateTimeUtils.joinTimeRanges(timeRanges);
//        List<TimeRange> result = DateTimeUtils.getTimeRangesSlicedByTimeSlots(timeRanges, MetricPeriod.ONE_MIN);
//        List<TimeRange> result2 = DateTimeUtils.getTimeRanges(timeRanges, MetricPeriod.ONE_MIN);
//        Assert.assertFalse(result.isEmpty());
//        Assert.assertFalse(result2.isEmpty());
    }


}
