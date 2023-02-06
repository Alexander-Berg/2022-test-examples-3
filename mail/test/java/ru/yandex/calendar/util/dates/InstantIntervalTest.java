package ru.yandex.calendar.util.dates;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author gutman
 */
public class InstantIntervalTest {
    @Test
    public void minus() {
        for (int i = 0; i < 1000; ++i) {
            InstantInterval a = randomInstantInterval();
            InstantInterval b = randomInstantInterval();
            InstantIntervalSet diff = InstantIntervalSet.minus(a, b);
            Assert.A.equals(InstantIntervalSet.union(a, b), diff.union(b));
        }
    }

    public static InstantInterval randomInstantInterval() {
        Instant a = Random2.R.nextLocalDateTimeInYear(1995).toDateTime(DateTimeZone.UTC).toInstant();
        Instant b = Random2.R.nextLocalDateTimeInYear(1995).toDateTime(DateTimeZone.UTC).toInstant();
        return a.isBefore(b) ? new InstantInterval(a, b) : new InstantInterval(b, a);
    }

    @Test
    public void mergeNeighborsCloserThan() {
        InstantIntervalSet set = InstantIntervalSet.union(
                intervalSomeDay(new LocalTime(17, 0), new LocalTime(18, 0)),
                intervalSomeDay(new LocalTime(18, 4), new LocalTime(19, 0)),
                intervalSomeDay(new LocalTime(19, 9), new LocalTime(20, 0)));

        ListF<InstantInterval> merged = set.mergeNeighborsCloserThan(Duration.standardMinutes(1)).getIntervals();
        ListF<InstantInterval> expected = set.getIntervals();
        Assert.equals(expected, merged);

        merged = set.mergeNeighborsCloserThan(Duration.standardMinutes(5)).getIntervals();
        expected = Cf.list(
                intervalSomeDay(new LocalTime(17, 0), new LocalTime(19, 0)),
                intervalSomeDay(new LocalTime(19, 9), new LocalTime(20, 0)));
        Assert.equals(expected, merged);

        merged = set.mergeNeighborsCloserThan(Duration.standardMinutes(10)).getIntervals();
        expected = Cf.list(
                intervalSomeDay(new LocalTime(17, 0), new LocalTime(20, 0)));
        Assert.equals(expected, merged);
    }

    private static InstantInterval intervalSomeDay(LocalTime a, LocalTime b) {
        DateTime someDay = new DateTime(2012, 8, 31, 17, 32, DateTimeZone.UTC);
        return new InstantInterval(a.toDateTime(someDay), b.toDateTime(someDay));
    }
}
