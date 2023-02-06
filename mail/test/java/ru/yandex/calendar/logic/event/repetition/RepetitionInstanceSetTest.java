package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author dbrylev
 */
public class RepetitionInstanceSetTest {

    @Test
    public void emptyOverlap() {
        Assert.isEmpty(set().overlap(set()));
        Assert.isEmpty(set().overlap(set(interval(0, 1))));
        Assert.isEmpty(set(interval(1, 2)).overlap(set(interval(0, 1))));

        Assert.notEmpty(set(interval(0, 0)).overlap(set(interval(0, 1))));
        Assert.notEmpty(set(interval(0, 1)).overlap(set(interval(0, 0))));
        Assert.notEmpty(set(interval(0, 0)).overlap(set(interval(0, 0))));
    }

    @Test
    public void longOverlap() {
        RepetitionInstanceSet longSet = set(interval(0, 25));
        RepetitionInstanceSet shortSet = set(interval(0, 5), interval(10, 15), interval(20, 25));

        Assert.equals(shortSet.getInstances(), shortSet.overlap(longSet).map(IntersectingIntervals.getOverlapF()));
        Assert.equals(shortSet.getInstances(), longSet.overlap(shortSet).map(IntersectingIntervals.getOverlapF()));
    }

    @Test
    public void selfOverlap() {
        RepetitionInstanceSet set = set(interval(0, 1), interval(2, 3), interval(4, 5));
        Assert.equals(set.getInstances(), set.overlap(set).map(IntersectingIntervals.getOverlapF()));
    }

    @Test
    public void crossOverlap() {
        RepetitionInstanceSet set1 = set(interval(0, 2), interval(3, 6));
        RepetitionInstanceSet set2 = set(interval(1, 4), interval(5, 7));

        Assert.equals(
                Cf.list(interval(1, 2), interval(3, 4), interval(5, 6)),
                set1.overlap(set2).map(IntersectingIntervals.getOverlapF()));
    }

    private static RepetitionInstanceSet set(InstantInterval ... intervals) {
        return RepetitionInstanceSet.fromSuccessiveIntervals(Cf.list(intervals));
    }

    private static InstantInterval interval(int startMinutes, int endMinutes) {
        return new InstantInterval(new Instant(startMinutes * 60 * 1000), new Instant(endMinutes * 60 * 1000));
    }
}
