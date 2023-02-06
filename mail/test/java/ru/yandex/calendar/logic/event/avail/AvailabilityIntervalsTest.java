package ru.yandex.calendar.logic.event.avail;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.joda.time.chrono.ISOChronology;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.util.dates.InstantIntervalSet;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author ssytnik
 */
public class AvailabilityIntervalsTest {
    private static final Chronology CHRONO = ISOChronology.getInstanceUTC();
    private static final DateTime BASE = new DateTime(
        2009, DateTimeConstants.MARCH, 11, 18, 45, 0, 0, CHRONO
    );

    private static AvailabilityInterval create(ReadableInstant start, ReadableInstant end, Availability a) {
        return new AvailabilityInterval(new InstantInterval(start, end), a, AvailabilityEventInfo.empty(), "test");
    }
    private static ListF<AvailabilityInterval> createSet(AvailabilityInterval... ais) {
        ListF<AvailabilityInterval> res = Cf.list(ais);
        return res.sorted(new AvailIntervalComparator());
    }

    // get() method tests

    @Test
    public void empty() {
        Assert.assertTrue(AvailabilityIntervals.createSuccessiveAvailIntervals(createSet(), false).isEmpty());
    }

    @Test
    public void single() {
        AvailabilityInterval i = create(BASE, BASE.plusHours(1), Availability.BUSY);
        Assert.A.equals(i, AvailabilityIntervals.createSuccessiveAvailIntervals(createSet(i), false).first());
    }

    @Test
    public void intersection() {
        AvailabilityInterval i1 = create(BASE, BASE.plusHours(2), Availability.BUSY);
        AvailabilityInterval i2 = create(BASE.plusHours(1), BASE.plusHours(3), Availability.BUSY);
        AvailabilityInterval interval = AvailabilityIntervals.createSuccessiveAvailIntervals(
                createSet(i1, i2), false).first();
        Assert.A.equals(new InstantInterval(BASE, BASE.plusHours(3)), interval.getInterval());
        Assert.A.equals(Availability.BUSY, interval.getAvailability());
    }

    @Test
    public void intersectionAvailability() {
        AvailabilityInterval i1 = create(BASE, BASE.plusHours(3), Availability.MAYBE);
        AvailabilityInterval i2 = create(BASE.plusHours(1), BASE.plusHours(4), Availability.BUSY);
        AvailabilityInterval i3 = create(BASE.plusHours(2), BASE.plusHours(5), Availability.MAYBE);

        ListF<AvailabilityInterval> intervals = AvailabilityIntervals.createSuccessiveAvailIntervals(
                createSet(i3, i2, i1), false);

        Assert.A.equals(new InstantInterval(BASE, BASE.plusHours(1)), intervals.get(0).getInterval());
        Assert.A.equals(Availability.MAYBE, intervals.get(0).getAvailability());

        Assert.A.equals(new InstantInterval(BASE.plusHours(1), BASE.plusHours(4)), intervals.get(1).getInterval());
        Assert.A.equals(Availability.BUSY, intervals.get(1).getAvailability());

        Assert.A.equals(new InstantInterval(BASE.plusHours(4), BASE.plusHours(5)), intervals.get(2).getInterval());
        Assert.A.equals(Availability.MAYBE, intervals.get(2).getAvailability());
    }

    // getBounded() method tests

    @Test
    public void bounded() {
        AvailabilityInterval i1 = create(BASE, BASE.plusHours(3), Availability.MAYBE);
        AvailabilityInterval i2 = create(BASE.plusHours(1), BASE.plusHours(4), Availability.BUSY);
        AvailabilityInterval i3 = create(BASE.plusHours(2), BASE.plusHours(5), Availability.MAYBE);

        ListF<AvailabilityInterval> intervals = new AvailabilityIntervals(createSet(i3, i2, i1), BASE.minusHours(1).toInstant(), BASE.plusHours(3).toInstant())
                .bounded().merged();

        Assert.A.equals(new InstantInterval(BASE, BASE.plusHours(1)), intervals.get(0).getInterval());
        Assert.A.equals(Availability.MAYBE, intervals.get(0).getAvailability());

        Assert.A.equals(new InstantInterval(BASE.plusHours(1), BASE.plusHours(3)), intervals.get(1).getInterval());
        Assert.A.equals(Availability.BUSY, intervals.get(1).getAvailability());
    }

    @Test
    public void gap() {
        AvailabilityInterval i1 = create(BASE.minusDays(4), BASE.minusDays(2), Availability.BUSY);
        AvailabilityInterval i2 = create(BASE.minusDays(1), BASE.plusDays(1), Availability.BUSY);
        AvailabilityInterval i3 = create(BASE.plusDays(2), BASE.plusDays(4), Availability.BUSY);

        ListF<AvailabilityInterval> intervals = new AvailabilityIntervals(createSet(i1, i2, i3),
                BASE.minusDays(3).toInstant(), BASE.plusDays(3).toInstant()).bounded().merged();

        Assert.A.equals(new InstantInterval(BASE.minusDays(3), BASE.minusDays(2)), intervals.get(0).getInterval());
        Assert.A.equals(Availability.BUSY, intervals.get(0).getAvailability());

        Assert.A.equals(new InstantInterval(BASE.minusDays(1), BASE.plusDays(1)), intervals.get(1).getInterval());
        Assert.A.equals(Availability.BUSY, intervals.get(1).getAvailability());

        Assert.A.equals(new InstantInterval(BASE.plusDays(2), BASE.plusDays(3)), intervals.get(2).getInterval());
        Assert.A.equals(Availability.BUSY, intervals.get(2).getAvailability());
    }

    @Test
    public void random() {
        for (int i = 0; i < 1000; ++i) {
            ListF<AvailabilityInterval> availIntervals = Cf.arrayList();
            int length = 1 + Random2.R.nextInt(6);

            for (int j = 0; j < length; ++j) {
                availIntervals.add(randomAvailInterval());
            }

            ListF<AvailabilityInterval> successive = Cf.toList(AvailabilityIntervals.createSuccessiveAvailIntervals(
                    availIntervals, false));

            InstantIntervalSet expectedUnion = InstantIntervalSet.union(availIntervals.map(AvailabilityInterval.intervalF));
            InstantIntervalSet actualUnion = InstantIntervalSet.union(successive.map(AvailabilityInterval.intervalF));
            Assert.A.equals(expectedUnion, actualUnion);
        }
    }

    private final ListF<Instant> points = Cf.range(0, 20)
                .map(Cf.Integer.multiplyF().bind1(3600 * 1000))
                .map(Cf.Integer.toLongF())
                .map(Cf.Long.plusF().bind1(new DateTime(2010, 12, 26, 0, 0, 0, 0, DateTimeZone.UTC).getMillis()))
                .map(Instant::new);

    private AvailabilityInterval randomAvailInterval() {
        ListF<Instant> startEnd = Random2.R.randomElements(points, 2).sorted();
        return new AvailabilityInterval(
                new InstantInterval(startEnd.first(), startEnd.last()),
                Random2.R.nextBoolean() ? Availability.MAYBE : Availability.BUSY,
                AvailabilityEventInfo.empty(), "test");
    }

    @Test
    public void resources() {
        ResourceInfo one = resourceInfo("one", 1);
        ResourceInfo two = resourceInfo("two", 2);
        ResourceInfo three = resourceInfo("three", 3);
        ResourceInfo four = resourceInfo("four", 4);

        AvailabilityInterval ai1 = new AvailabilityInterval(new InstantInterval(0, 20), Availability.BUSY,
                AvailabilityEventInfo.empty(), "ai1");
        AvailabilityInterval ai2 = new AvailabilityInterval(new InstantInterval(10, 30), Availability.BUSY,
                AvailabilityEventInfo.resources(Cf.list(one)), "ai2");
        AvailabilityInterval ai3 = new AvailabilityInterval(new InstantInterval(20, 40), Availability.BUSY,
                AvailabilityEventInfo.resources(Cf.list(one, two)), "ai3");
        AvailabilityInterval ai4 = new AvailabilityInterval(new InstantInterval(30, 50), Availability.BUSY,
                AvailabilityEventInfo.resources(Cf.list(two, three)), "ai4");
        AvailabilityInterval ai5 = new AvailabilityInterval(new InstantInterval(39, 60), Availability.BUSY,
                AvailabilityEventInfo.resources(Cf.list(three, four)), "ai5");

        ListF<AvailabilityInterval> successive = Cf.toList(AvailabilityIntervals.createSuccessiveAvailIntervals(
                Cf.list(ai1, ai2, ai3, ai4, ai5), false));

        Assert.A.equals(new InstantInterval(0, 10), successive.get(0).getInterval());
        Assert.A.equals(Cf.<ResourceInfo>list(), successive.get(0).getResources());

        Assert.A.equals(new InstantInterval(10, 20), successive.get(1).getInterval());
        Assert.A.equals(Cf.list(one), successive.get(1).getResources());

        Assert.A.equals(new InstantInterval(20, 30), successive.get(2).getInterval());
        Assert.A.equals(Cf.list(one, two), successive.get(2).getResources());

        Assert.A.equals(new InstantInterval(30, 39), successive.get(3).getInterval());
        Assert.A.equals(Cf.list(one, two, three), successive.get(3).getResources());

        Assert.A.equals(new InstantInterval(39, 40), successive.get(4).getInterval());
        Assert.A.equals(Cf.list(one, two, three, four), successive.get(4).getResources());

        Assert.A.equals(new InstantInterval(40, 50), successive.get(5).getInterval());
        Assert.A.equals(Cf.list(two, three, four), successive.get(5).getResources());

        Assert.A.equals(new InstantInterval(50, 60), successive.get(6).getInterval());
        Assert.A.equals(Cf.list(three, four), successive.get(6).getResources());

    }

    private ResourceInfo resourceInfo(String resourceName, long id) {
        Resource r = new Resource();
        r.setExchangeName(resourceName);
        r.setId(id);
        return new ResourceInfo(r, new Office());
    }

}
