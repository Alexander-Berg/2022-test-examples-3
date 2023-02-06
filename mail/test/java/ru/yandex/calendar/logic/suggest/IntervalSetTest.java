package ru.yandex.calendar.logic.suggest;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class IntervalSetTest {

    @Test
    public void consWithGap() {
        DateTime start = MoscowTime.dateTime(2012, 12, 10, 17, 0);

        InstantInterval i1 = new InstantInterval(start.plusHours(0), start.plusHours(1));
        InstantInterval i2 = new InstantInterval(start.plusHours(2), start.plusHours(3));

        Duration minGap = Duration.standardHours(1);
        Assert.hasSize(2, IntervalSet.cons(Cf.list(i1, i2), minGap).getIntervals());

        Assert.hasSize(1, IntervalSet.cons(Cf.list(i1, i2), minGap.plus(1)).getIntervals());
    }

    @Test
    public void splitLongInterval() {
        DateTime start = MoscowTime.dateTime(2012, 12, 10, 22, 0);

        IntervalSet set = IntervalSet.cons(Cf.list(new InstantInterval(start, start.plusDays(2))));
        ListF<IntervalSet> splits = set.splitByDaysSequences(1, MoscowTime.TZ);

        Assert.hasSize(3, splits);
        Assert.equals(start.plusDays(1).withTimeAtStartOfDay().toInstant(), splits.get(0).getEnd());
        Assert.equals(splits.get(0).getEnd(), splits.get(1).getStart());

        Assert.equals(start.plusDays(2).withTimeAtStartOfDay().toInstant(), splits.get(1).getEnd());
        Assert.equals(splits.get(1).getEnd(), splits.get(2).getStart());

        Assert.equals(splits.get(2).getEnd(), start.plusDays(2).toInstant());
    }

    @Test
    public void splitFarIntervals() {
        DateTime hereStart = MoscowTime.dateTime(2012, 12, 10, 6, 0);
        DateTime farStart = hereStart.plusDays(27);

        InstantInterval h1 = new InstantInterval(hereStart.plusHours(1), hereStart.plusHours(2));
        InstantInterval h2 = new InstantInterval(hereStart.plusHours(3), hereStart.plusHours(4));

        InstantInterval f1 = new InstantInterval(farStart.plusHours(1), farStart.plusHours(2));
        InstantInterval f2 = new InstantInterval(farStart.plusHours(3), farStart.plusHours(4));

        IntervalSet set = IntervalSet.cons(Cf.list(h1, h2, f1, f2));
        ListF<IntervalSet> splits = set.splitByDaysSequences(5, MoscowTime.TZ);

        Assert.hasSize(2, splits);
        Assert.equals(Cf.list(h1, h2), splits.first().getIntervals());
        Assert.equals(Cf.list(f1, f2), splits.last().getIntervals());
    }

    @Test
    public void splitMaxDays() {
        DateTime start = MoscowTime.dateTime(2012, 12, 10, 12, 0);

        InstantInterval i1 = new InstantInterval(start.plusDays(0), start.plusDays(0).plusHours(1));
        InstantInterval i2 = new InstantInterval(start.plusDays(1), start.plusDays(1).plusHours(1));
        InstantInterval i3 = new InstantInterval(start.plusDays(2), start.plusDays(2).plusHours(1));
        InstantInterval i4 = new InstantInterval(start.plusDays(3), start.plusDays(3).plusHours(1));

        IntervalSet set = IntervalSet.cons(Cf.list(i1, i2, i3, i4));
        ListF<IntervalSet> splits = set.splitByDaysSequences(3, MoscowTime.TZ);

        Assert.hasSize(2, splits);
        Assert.equals(Cf.list(i1, i2, i3), splits.first().getIntervals());
        Assert.equals(Cf.list(i4), splits.last().getIntervals());
    }

    @Test
    public void splitEmpty() {
        IntervalSet emptySet = IntervalSet.cons(Cf.<InstantInterval>list());
        ListF<IntervalSet> splits = emptySet.splitByDaysSequences(1, MoscowTime.TZ);

        Assert.isEmpty(splits);
    }

    @Test
    public void contains() {
        Assert.isFalse(IntervalSet.empty().contains(hoursInterval(12, 13)));

        IntervalSet set = IntervalSet.cons(Cf.list(hoursInterval(1, 3), hoursInterval(4, 6)));
        Assert.isTrue(set.contains(hoursInterval(1, 3)));
        Assert.isTrue(set.contains(hoursInterval(4, 6)));

        Assert.isFalse(set.contains(hoursInterval(0, 2)));
        Assert.isFalse(set.contains(hoursInterval(2, 5)));
        Assert.isFalse(set.contains(hoursInterval(5, 7)));

        Assert.isFalse(set.contains(hoursInterval(0, 1)));
        Assert.isFalse(set.contains(hoursInterval(3, 4)));
        Assert.isFalse(set.contains(hoursInterval(6, 7)));

        set = IntervalSet.cons(Cf.list(hoursInterval(8, 9), hoursInterval(9, 9), hoursInterval(9, 10)));
        Assert.isTrue(set.contains(hoursInterval(8, 9)));
        Assert.isTrue(set.contains(hoursInterval(9, 10)));
    }

    @Test
    public void overlaps() {
        Assert.isFalse(IntervalSet.empty().overlaps(hoursInterval(12, 13)));

        IntervalSet set = IntervalSet.cons(Cf.list(hoursInterval(1, 3), hoursInterval(4, 6)));
        Assert.isTrue(set.overlaps(hoursInterval(1, 6)));
        Assert.isTrue(set.overlaps(hoursInterval(0, 4)));
        Assert.isTrue(set.overlaps(hoursInterval(3, 7)));

        Assert.isFalse(set.contains(hoursInterval(0, 1)));
        Assert.isFalse(set.contains(hoursInterval(3, 4)));
        Assert.isFalse(set.contains(hoursInterval(6, 7)));
    }

    @Test
    public void minGapDuration() {
        IntervalSet set = IntervalSet.cons(Cf.list(hoursInterval(1, 2), hoursInterval(4, 5)), Duration.standardHours(2));
        Assert.hasSize(2, set.getIntervals());

        set = IntervalSet.cons(Cf.list(hoursInterval(1, 2), hoursInterval(3, 4)), Duration.standardHours(2));
        Assert.hasSize(1, set.getIntervals());

        set = IntervalSet.cons(Cf.list(hoursInterval(1, 2), hoursInterval(2, 3)));
        Assert.hasSize(1, set.getIntervals());
    }

    @Test
    public void cut() {
        IntervalSet set = IntervalSet.cons(Cf.list(hoursInterval(1, 6)));
        Assert.equals(Cf.list(hoursInterval(1, 3), hoursInterval(4, 6)), set.cut(hoursInterval(3, 4)).getIntervals());

        set = IntervalSet.cons(Cf.list(hoursInterval(1, 3), hoursInterval(4, 6)));
        Assert.equals(Cf.list(hoursInterval(1, 2), hoursInterval(5, 6)), set.cut(hoursInterval(2, 5)).getIntervals());

        set = IntervalSet.cons(Cf.list(hoursInterval(2, 4)));
        Assert.equals(set.getIntervals(), set.cut(hoursInterval(0, 1)).getIntervals());
        Assert.equals(set.getIntervals(), set.cut(hoursInterval(0, 2)).getIntervals());
        Assert.equals(set.getIntervals(), set.cut(hoursInterval(4, 6)).getIntervals());
        Assert.equals(set.getIntervals(), set.cut(hoursInterval(5, 6)).getIntervals());

        Assert.equals(set.getIntervals(), set.cut(hoursInterval(2, 2)).getIntervals());
        Assert.equals(set.getIntervals(), set.cut(hoursInterval(4, 4)).getIntervals());
        Assert.hasSize(2, set.cut(hoursInterval(3, 3)).getIntervals());
    }

    private static InstantInterval hoursInterval(int startHour, int endHour) {
        return new InstantInterval(
                MoscowTime.instant(2013, 1, 21, startHour, 0), MoscowTime.instant(2013, 1, 21, endHour, 0));
    }
}
