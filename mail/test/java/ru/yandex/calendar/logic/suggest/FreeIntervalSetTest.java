package ru.yandex.calendar.logic.suggest;

import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class FreeIntervalSetTest {

    @Test
    public void isFreeIn() {
        FreeIntervalSet emptySet = FreeIntervalSet.empty();
        Assert.isFalse(emptySet.isFreeIn(interval(1, 1)));
        Assert.isFalse(emptySet.isFreeIn(interval(1, 2)));

        FreeIntervalSet continuousSet = new FreeIntervalSet(Cf.list(
                freeInterval(1, 3),
                freeIntervalWithDue(3, 5, 2),
                freeIntervalWithDue(5, 7, 1)));

        Assert.isFalse(continuousSet.isFreeIn(interval(0, 2)));
        Assert.isTrue(continuousSet.isFreeIn(interval(1, 2)));
        Assert.isTrue(continuousSet.isFreeIn(interval(2, 4)));
        Assert.isTrue(continuousSet.isFreeIn(interval(6, 6)));
        Assert.isTrue(continuousSet.isFreeIn(interval(1, 7)));
        Assert.isFalse(continuousSet.isFreeIn(interval(0, 8)));

        FreeIntervalSet gapsSet = new FreeIntervalSet(Cf.list(
                freeInterval(1, 3),
                freeInterval(5, 7)));

        Assert.isTrue(gapsSet.isFreeIn(interval(1, 3)));
        Assert.isFalse(gapsSet.isFreeIn(interval(2, 4)));
        Assert.isFalse(gapsSet.isFreeIn(interval(4, 4)));
        Assert.isTrue(gapsSet.isFreeIn(interval(5, 6)));
        Assert.isFalse(gapsSet.isFreeIn(interval(0, 8)));
    }

    private static FreeInterval freeIntervalWithDue(int startHour, int endHour, int dueDays) {
        return freeInterval(startHour, endHour, Option.of(dueDays));
    }

    private static FreeInterval freeInterval(int startHour, int endHour) {
        return freeInterval(startHour, endHour, Option.<Integer>empty());
    }

    private static FreeInterval freeInterval(int startHour, int endHour, Option<Integer> dueDays) {
        InstantInterval interval = interval(startHour, endHour);
        Option<LocalDate> dueDate = Option.empty();

        if (dueDays.isPresent()) {
            dueDate = Option.of(new LocalDate(interval.getStart(), MoscowTime.TZ).plusDays(dueDays.get()));
        }
        return new FreeInterval(interval, dueDate);
    }

    private static InstantInterval interval(int startHour, int endHour) {
        return new InstantInterval(
                MoscowTime.instant(2014, 7, 18, startHour, 0),
                MoscowTime.instant(2014, 7, 18, endHour, 0));
    }
}
