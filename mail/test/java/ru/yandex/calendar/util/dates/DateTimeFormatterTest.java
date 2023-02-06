package ru.yandex.calendar.util.dates;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import ru.yandex.calendar.logic.event.grid.ViewType;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

public class DateTimeFormatterTest extends CalendarTestBase {

    @Test
    public void getViewTypeBoundWeekLeft() {
        Instant expected = new LocalDate(2010, 5, 10).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.WEEK, DayOfWeek.MONDAY, true).toInstant();
        Assert.A.equals(expected, got);
    }


    @Test
    public void getViewTypeBoundWeekRight() {
        Instant expected = new LocalDate(2010, 5, 17).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.WEEK, DayOfWeek.MONDAY, false).toInstant();
        Assert.A.equals(expected, got);
    }

    @Test
    public void getViewTypeBoundMonthLeft() {
        Instant expected = new LocalDate(2010, 4, 26).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.MONTH, DayOfWeek.MONDAY, true).toInstant();
        Assert.A.equals(expected, got);
    }

    @Test
    public void getViewTypeBoundMonthRight() {
        Instant expected = new LocalDate(2010, 6, 7).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.MONTH, DayOfWeek.MONDAY, false).toInstant();
        Assert.A.equals(expected, got);
    }

    @Test
    public void getViewTypeBoundDayLeft() {
        Instant expected = new LocalDate(2010, 5, 13).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.DAY, DayOfWeek.MONDAY, true).toInstant();
        Assert.A.equals(expected, got);
    }

    @Test
    public void getViewTypeBoundDayRight() {
        Instant expected = new LocalDate(2010, 5, 14).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 5, 13), ViewType.DAY, DayOfWeek.MONDAY, false).toInstant();
        Assert.A.equals(expected, got);
    }


    @Test
    public void getViewTypeBoundMonthLeftForSaturday() {
        Instant expected = new LocalDate(2010, 11, 27).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 12, 20), ViewType.MONTH, DayOfWeek.SATURDAY, true).toInstant();
        Assert.A.equals(expected, got);
    }

    @Test
    public void getViewTypeBoundMonthRightForSaturday() {
        Instant expected = new LocalDate(2011, 1, 1).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant got = DateTimeFormatter.getViewTypeBound(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalDate(2010, 12, 20), ViewType.MONTH, DayOfWeek.SATURDAY, false).toInstant();
        Assert.A.equals(expected, got);
    }


    @Test
    public void getViewTypeBoundAuto() {
        for (LocalDate localDate = new LocalDate(2010, 1, 1); localDate.isBefore(new LocalDate(2012, 1, 1)); localDate = localDate.plusDays(1)) {
            for (ViewType vt : ViewType.values()) {
                Instant left = DateTimeFormatter.getViewTypeBound(
                        TimeUtils.EUROPE_MOSCOW_TIME_ZONE, localDate, vt, DayOfWeek.MONDAY, true).toInstant();
                Instant right = DateTimeFormatter.getViewTypeBound(
                        TimeUtils.EUROPE_MOSCOW_TIME_ZONE, localDate, vt, DayOfWeek.MONDAY, false).toInstant();

                Assert.A.equals(LocalTime.MIDNIGHT, left.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toLocalTime());
                Assert.A.equals(LocalTime.MIDNIGHT, right.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toLocalTime());

                LocalDate leftLocalDate = left.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toLocalDate();
                LocalDate rightLocalDate = right.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toLocalDate();

                Assert.assertTrue(!leftLocalDate.isAfter(localDate));
                Assert.assertTrue(rightLocalDate.isAfter(localDate));

                if (vt == ViewType.DAY || vt == ViewType.GREY_DAY) {
                    Assert.A.equals(localDate.toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant(), left);
                    Assert.A.equals(localDate.plusDays(1).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant(), right);
                }

                if (vt != ViewType.DAY && vt != ViewType.GREY_DAY) {
                    Assert.A.equals(leftLocalDate.getDayOfWeek(), DayOfWeek.MONDAY.getJoda());
                    Assert.A.equals(rightLocalDate.getDayOfWeek(), DayOfWeek.MONDAY.getJoda());
                }

                if (vt == ViewType.WEEK || vt == ViewType.GREY_WEEK) {
                    Assert.A.equals(rightLocalDate, leftLocalDate.plusWeeks(1));
                }

                if (vt == ViewType.MONTH || vt == ViewType.GREY_MONTH) {
                    Assert.assertTrue(rightLocalDate.getDayOfMonth() <= 7);
                    Assert.A.equals(1, (12 + rightLocalDate.getMonthOfYear() - localDate.getMonthOfYear()) % 12);

                    LocalDate leftPlusWeekLocalDate = leftLocalDate.plusWeeks(1);
                    Assert.assertTrue(leftPlusWeekLocalDate.getDayOfMonth() >= 2);
                    Assert.assertTrue(leftPlusWeekLocalDate.getDayOfMonth() <= 8);
                    Assert.A.equals(localDate.getMonthOfYear(), leftPlusWeekLocalDate.getMonthOfYear());
                }
            }
        }
    }

    @Test
    public void isAtLeastOneDayDiff() {
        DateTime dt = new DateTime(2010, 9, 8, 14, 15, 16, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        Assert.assertFalse(DateTimeFormatter.isAtLeastOneDayDiff(dt.toInstant(), dt.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        Assert.assertFalse(DateTimeFormatter.isAtLeastOneDayDiff(dt.minusHours(1).toInstant(), dt.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        Assert.assertFalse(DateTimeFormatter.isAtLeastOneDayDiff(dt.minusHours(17).toInstant(), dt.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        // I don't understand it // stepancheg@
        Assert.assertTrue(DateTimeFormatter.isAtLeastOneDayDiff(dt.minusHours(25).toInstant(), dt.toInstant(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
    }

    @Test
    public void parseDateForMachines() {
        LocalDate localDate = DateTimeFormatter.parseLocalDateFromMachines("2010-12-14");
        Assert.A.equals(new LocalDate(2010, 12, 14), localDate);
    }

} //~
