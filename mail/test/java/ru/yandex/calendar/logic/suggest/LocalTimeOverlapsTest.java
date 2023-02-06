package ru.yandex.calendar.logic.suggest;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class LocalTimeOverlapsTest {

    @Test
    public void mergeLapping() {
        // 1-st overlaps 2-nd
        Assert.equals(Cf.list(overlap(0, 2, 0), overlap(2, 3, 33)),
                mergeOverlaps(overlap(0, 2, 0), overlap(1, 3, 33)));

        Assert.equals(Cf.list(overlap(0, 1, 33), overlap(1, 3, 0)),
                mergeOverlaps(overlap(0, 2, 33), overlap(1, 3, 0)));

        // 1-st overlaps 2-nd and 3-rd, and 2-nd contains 3-rd
        Assert.equals(Cf.list(overlap(0, 5, 15), overlap(5, 6, 25), overlap(6, 10, 35)),
                mergeOverlaps(overlap(0, 5, 15), overlap(2, 10, 35), overlap(4, 6, 25)));

        Assert.equals(Cf.list(overlap(0, 4, 25), overlap(4, 6, 15), overlap(6, 10, 35)),
                mergeOverlaps(overlap(0, 5, 25), overlap(2, 10, 35), overlap(4, 6, 15)));

        Assert.equals(Cf.list(overlap(0, 2, 35), overlap(2, 10, 15)),
                mergeOverlaps(overlap(0, 5, 35), overlap(2, 10, 15), overlap(4, 6, 25)));
    }

    @Test
    public void mergeContaining() {
        // 1-st contains 2-nd
        Assert.equals(Cf.list(overlap(0, 1, 17), overlap(1, 2, 7), overlap(2, 3, 17)),
                mergeOverlaps(overlap(0, 3, 17), overlap(1, 2, 7)));

        Assert.equals(Cf.list(overlap(0, 3, 7)),
                mergeOverlaps(overlap(0, 3, 7), overlap(1, 2, 17)));

        // 1-st contains 2-nd contains 3-rd
        Assert.equals(Cf.list(overlap(0, 10, 16)),
                mergeOverlaps(overlap(0, 10, 16), overlap(3, 8, 26), overlap(5, 6, 36)));

        Assert.equals(Cf.list(overlap(0, 5, 26), overlap(5, 6, 16), overlap(6, 10, 26)),
                mergeOverlaps(overlap(0, 10, 26), overlap(3, 8, 36), overlap(5, 6, 16)));

        Assert.hasSize(5,
                mergeOverlaps(overlap(0, 10, 36), overlap(3, 8, 26), overlap(5, 6, 16)));
    }

    private static ListF<LocalTimeOverlap> mergeOverlaps(LocalTimeOverlap ... overlaps) {
        return LocalTimeOverlaps.merge(Cf.x(overlaps));
    }

    private static LocalTimeOverlap overlap(int from, int to, int since) {
        return new LocalTimeOverlap(
                new DayTime(0, LocalTime.MIDNIGHT.plusMinutes(from)),
                new DayTime(0, LocalTime.MIDNIGHT.plusMinutes(to)),
                new LocalDate(2013, 1, 1).plusDays(since));
    }
}
