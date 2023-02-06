package ru.yandex.market.tsum.clients.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 07.12.18
 */
public class HolidaysTest {

    @Test
    public void holidayMillis() {
        Holidays holidays = new Holidays();

        holidays.setHolidays(Arrays.asList(
            new Holidays.Holiday(LocalDate.of(2018, 12, 30), "weekend"),
            new Holidays.Holiday(LocalDate.of(2018, 12, 31), "weekend"),
            new Holidays.Holiday(LocalDate.of(2019, 1, 1), "holiday"),
            new Holidays.Holiday(LocalDate.of(2019, 1, 5), "holiday"),
            new Holidays.Holiday(LocalDate.of(2019, 1, 7), "holiday"),
            new Holidays.Holiday(LocalDate.of(2019, 1, 9), "holiday")
        ));

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(12),
            holidays.holidayMillis(Instant.parse("2018-12-29T09:00:00Z"), Instant.parse("2018-12-30T09:00:00Z"))
        );

        Assert.assertEquals(
            TimeUnit.MINUTES.toMillis(30),
            holidays.holidayMillis(Instant.parse("2018-12-29T23:00:00Z"), Instant.parse("2018-12-29T23:30:00Z"))
        );

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(12),
            holidays.holidayMillis(Instant.parse("2019-01-01T09:00:00Z"), Instant.parse("2019-01-02T09:00:00Z"))
        );

        Assert.assertEquals(
            0,
            holidays.holidayMillis(Instant.parse("2019-01-02T12:00:00Z"), Instant.parse("2019-01-02T23:00:00Z"))
        );

        Assert.assertEquals(
            TimeUnit.DAYS.toMillis(1),
            holidays.holidayMillis(Instant.parse("2019-01-05T12:00:00Z"), Instant.parse("2019-01-07T12:00:00Z"))
        );

        Assert.assertEquals(
            TimeUnit.DAYS.toMillis(2),
            holidays.holidayMillis(Instant.parse("2019-01-05T12:00:00Z"), Instant.parse("2019-01-09T12:00:00Z"))
        );

        Assert.assertEquals(
            TimeUnit.DAYS.toMillis(2),
            holidays.holidayMillis(Instant.parse("2019-01-06T12:00:00Z"), Instant.parse("2019-01-10T12:00:00Z"))
        );
    }

    @Test
    public void getMillisFromBeginingOfDay() {
        Instant from = Instant.parse("2018-12-22T06:15:00Z");

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(9) + TimeUnit.MINUTES.toMillis(15),
            Holidays.getMillisFromBeginingOfDay(from)
        );

        from = Instant.parse("2018-12-21T22:15:00Z");

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(15),
            Holidays.getMillisFromBeginingOfDay(from)
        );
    }

    @Test
    public void getMillisUntilEndOfDay() {
        Instant from = Instant.parse("2018-12-22T06:15:00Z");

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(14) + TimeUnit.MINUTES.toMillis(45),
            Holidays.getMillisUntilEndOfDay(from)
        );

        from = Instant.parse("2018-12-21T21:15:00Z");

        Assert.assertEquals(
            TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(45),
            Holidays.getMillisUntilEndOfDay(from)
        );
    }
}
