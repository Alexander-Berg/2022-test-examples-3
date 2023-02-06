package ru.yandex.calendar.util.dates;

import org.joda.time.DateTime;
import org.junit.Test;

import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class TimeFieldTest {

    @Test
    public void expand() {
        DateTime today = dateTime(2018, 2, 1, 16, 35);

        Assert.some(new InstantInterval(dateTime(2018, 2, 1), dateTime(2018, 2, 2)), TimeField.DAY.expand(today));

        Assert.some(new InstantInterval(dateTime(2018, 1, 29), dateTime(2018, 2, 3)), TimeField.WEEKDAYS.expand(today));

        Assert.some(new InstantInterval(dateTime(2018, 1, 29), dateTime(2018, 2, 5)), TimeField.WEEK.expand(today));

        Assert.some(new InstantInterval(dateTime(2018, 2, 1), dateTime(2018, 3, 1)), TimeField.MONTH.expand(today));

        Assert.none(TimeField.WEEKDAYS.expand(dateTime(2018, 1, 13, 12, 0)));
    }

    private static DateTime dateTime(int year, int month, int day) {
        return dateTime(year, month, day, 0, 0);
    }

    private static DateTime dateTime(int year, int month, int day, int hour, int minutes) {
        return MoscowTime.dateTime(year, month, day, hour, minutes);
    }
}
