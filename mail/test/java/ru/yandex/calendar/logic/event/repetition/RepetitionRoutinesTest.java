package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.generic.AbstractUtcCtxConfTest;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

public class RepetitionRoutinesTest extends AbstractUtcCtxConfTest {
    private static final Logger logger = LoggerFactory.getLogger(EventRoutines.class);

    @Autowired
    protected RepetitionRoutines repetitionRoutines;
    @Autowired
    protected EventRoutines eventRoutines;

    @Test
    public void repetitionInstSet() {
        InstantInterval ei1 = new InstantInterval(
            TestDateTimes.utc(2005, 1, 1, 12, 0),
            TestDateTimes.utc(2005, 1, 3, 23, 0)
        );
        Repetition r1 = createRepetition(
                TestDateTimes.utc(2005, 8, 1, 0, 0), RegularRepetitionRule.WEEKLY, 1, "sat"
        );
        InstantInterval ei2 = new InstantInterval(
            TestDateTimes.utc(2005, 1, 22, 12, 0),
            TestDateTimes.utc(2005, 1, 22, 13, 0)
        );
        Repetition r2 = createRepetition(
                TestDateTimes.utc(2005, 1, 31, 0, 0), RegularRepetitionRule.DAILY, 1, null
        );
        InstantInterval ei3 = new InstantInterval(
            TestDateTimes.utc(2005, 1, 4, 12, 0),
            TestDateTimes.utc(2005, 1, 4, 13, 0)
        );
        Repetition r3 = createRepetition(
                TestDateTimes.utc(2005, 1, 31, 0, 0), RegularRepetitionRule.WEEKLY, 1, "tue,wed,thu,fri"
        );

        Assert.assertTrue(testRepetitionInstSetIntersection(ei1, r1, ei2, r2));
        Assert.assertFalse(testRepetitionInstSetIntersection(ei1, r1, ei3, r3));
    }

    @Test
    public void repetitionInstSet2() {
        InstantInterval ei1 = new InstantInterval(
                TestDateTimes.utc(2009, 4, 28, 8, 30),
                TestDateTimes.utc(2009, 4, 28, 12, 30)
            );
        InstantInterval ei2 = new InstantInterval(
            TestDateTimes.utc(2009, 4, 27, 10, 0),
            TestDateTimes.utc(2009, 4, 27, 11, 0)
        );
        Repetition r2 = createRepetition(
                TestDateTimes.utc(2009, 5, 1, 0, 0), RegularRepetitionRule.DAILY, 1, null
        );
        Assert.assertTrue(testRepetitionInstSetIntersection(ei1, null, ei2, r2));
    }

    @Test
    public void repetitionInstSet3() {
        InstantInterval ei1 = new InstantInterval(
            TestDateTimes.utc(2009, 6, 22, 4, 0),
            TestDateTimes.utc(2009, 6, 22, 5, 0)
        );
        Repetition r1 = createRepetition(
                TestDateTimes.utc(2010, 12, 31, 0, 0), RegularRepetitionRule.DAILY, 5, null
        );
        InstantInterval ei2 = new InstantInterval(
            TestDateTimes.utc(2009, 7, 12, 9, 04).getMillis(),
            TestDateTimes.utc(2009, 7, 12, 15, 15).getMillis()
        );
        Repetition r2 = createRepetitionWithoutDueTs(
                RegularRepetitionRule.YEARLY, 2, "wed"
        );

        Assert.assertFalse(testRepetitionInstSetIntersection(ei1, r1, ei2, r2));
    }

    private boolean testRepetitionInstSetIntersection(InstantInterval ei1, Repetition r1, InstantInterval ei2, Repetition r2) {
        RepetitionInstanceInfo rii1 = RepetitionInstanceInfo.create(ei1, DateTimeZone.UTC, Option.ofNullable(r1));
        RepetitionInstanceInfo rii2 = RepetitionInstanceInfo.create(ei2, DateTimeZone.UTC, Option.ofNullable(r2));

        Option<Instant> endTs1 = rii1.getRepetitionEndOrElseEventEnd();
        Option<Instant> endTs2 = rii2.getRepetitionEndOrElseEventEnd();

        Instant tillMs = RepetitionInstanceSet.minBoundedByMaxCheckPeriod(endTs1, endTs2);
        RepetitionInstanceSet ris1 = new RepetitionInstanceSet(rii1, new Instant(0), tillMs);
        RepetitionInstanceSet ris2 = new RepetitionInstanceSet(rii2, new Instant(0), tillMs);
        return ris1.intersects(ris2);
    }

    @Test
    public void hasInstancesAfter() {
        Instant start = TestDateTimes.utc(2012, 1, 1, 9, 0);
        Instant end = TestDateTimes.utc(2012, 1, 1, 10, 0);
        Instant dueTs = TestDateTimes.utc(2012, 1, 30, 0, 0);

        RepetitionInstanceInfo r1 = createRepetitionInstanceInfo(start, end, dueTs);

        Assert.isTrue(repetitionRoutines.hasInstancesAfter(r1, TestDateTimes.utc(2012, 1, 29, 0, 0)));
        Assert.isTrue(repetitionRoutines.hasInstancesAfter(r1, TestDateTimes.utc(2011, 12, 31, 0, 0)));
        Assert.isFalse(repetitionRoutines.hasInstancesAfter(r1, TestDateTimes.utc(2012, 2, 1, 0, 0)));

        RepetitionInstanceInfo r2 = createRepetitionInstanceInfoWithoutDueTs(start, end);

        Assert.isTrue(repetitionRoutines.hasInstancesAfter(r2, TestDateTimes.utc(2012, 1, 29, 0, 0)));
        Assert.isTrue(repetitionRoutines.hasInstancesAfter(r2, TestDateTimes.utc(2011, 12, 31, 0, 0)));
        Assert.isTrue(repetitionRoutines.hasInstancesAfter(r2, TestDateTimes.utc(2012, 2, 1, 0, 0)));
    }

    private RepetitionInstanceInfo createRepetitionInstanceInfo(Instant start, Instant end, Instant dueTs) {
        Repetition r = createRepetition(dueTs, RegularRepetitionRule.WEEKLY, 1, "sun");
        return createRepetitionInstanceInfo(start, end, r);
    }

    private RepetitionInstanceInfo createRepetitionInstanceInfoWithoutDueTs(Instant start, Instant end) {
        Repetition r = createRepetitionWithoutDueTs(RegularRepetitionRule.WEEKLY, 1, "sun");
        return createRepetitionInstanceInfo(start, end, r);
    }

    private RepetitionInstanceInfo createRepetitionInstanceInfo(Instant start, Instant end, Repetition r) {
        InstantInterval interval = new InstantInterval(start, end);
        return new RepetitionInstanceInfo(interval, DateTimeZone.UTC, Option.of(r), Cf.list(), Cf.list(), Cf.list());
    }


    @Test
    public void perfomance() {
        InstantInterval ei1 = new InstantInterval(
                TestDateTimes.utc(2009, 4, 28, 8, 30),
                TestDateTimes.utc(2009, 4, 28, 12, 30)
        );
        Repetition r1 = createRepetition(
                TestDateTimes.utc(2010, 5, 1, 0, 0), RegularRepetitionRule.MONTHLY_NUMBER, 1, null
        );
        InstantInterval ei2 = new InstantInterval(
            TestDateTimes.utc(2009, 4, 27, 10, 0),
            TestDateTimes.utc(2009, 4, 27, 11, 0)
        );
        Repetition r2 = createRepetition(
                TestDateTimes.utc(2010, 5, 1, 0, 0), RegularRepetitionRule.DAILY, 1, null
        );
        RepetitionInstanceInfo rii1 = RepetitionInstanceInfo.create(ei1, DateTimeZone.UTC, Option.of(r1));
        RepetitionInstanceInfo rii2 = RepetitionInstanceInfo.create(ei2, DateTimeZone.UTC, Option.of(r2));

        Option<Instant> endMs = rii1.getRepetitionEndOrElseEventEnd();
        RepetitionInstanceSet ris1 = new RepetitionInstanceSet(rii1, new Instant(0), endMs.get());
        RepetitionInstanceSet ris2 = new RepetitionInstanceSet(rii2, new Instant(0), endMs.get());
        for (int i = 0; i < 10000; i++) {
            ris1.intersects(ris2);
        }
    }

    private Repetition createRepetition(
            Instant dueTs, RegularRepetitionRule type, int rEach, String rWeeklyDays)
    {
        Repetition r = createRepetitionWithoutDueTs(type, rEach, rWeeklyDays);
        r.setDueTs(dueTs);
        return r;
    }

    private Repetition createRepetitionWithoutDueTs(RegularRepetitionRule type, int rEach, String rWeeklyDays)
    {
        Repetition r = new Repetition();
        r.setDueTsNull();
        r.setType(type);
        r.setId(1L);
        r.setREach(rEach);
        r.setRWeeklyDays(rWeeklyDays);
        return r;
    }

    @Test
    public void getNotPastEvents() {
        EventAndRepetition ongoingMaster = makeEventAndRepetition(
                TestDateTimes.utc(2017, 11, 12, 8, 30), true, Option.empty());
        EventAndRepetition endedMaster = makeEventAndRepetition(
                TestDateTimes.utc(2017, 11, 12, 8, 30), true, Option.of(TestDateTimes.utc(2017, 11, 20, 8, 30)));
        EventAndRepetition pastRecurrence = makeEventAndRepetition(
                TestDateTimes.utc(2017, 11, 19, 8, 30), false, Option.empty());
        EventAndRepetition futureRecurrence = makeEventAndRepetition(
                TestDateTimes.utc(2017, 11, 26, 8, 30), false, Option.empty());

        ListF<EventAndRepetition> notPastEvents = Cf.list(ongoingMaster, endedMaster, pastRecurrence, futureRecurrence)
                .filter(e -> e.goesOnAfter(TestDateTimes.utc(2017, 11, 24, 12, 27)));

        Assert.equals(Cf.list(ongoingMaster, futureRecurrence), notPastEvents);
    }

    private EventAndRepetition makeEventAndRepetition(
            Instant startTs, boolean repeating, Option<Instant> dueTs)
    {
        Instant endTs = startTs.plus(Duration.standardHours(1));

        Event event = new Event();
        event.setStartTs(startTs);
        event.setEndTs(endTs);

        RepetitionInstanceInfo repetitionInstanceInfo = repeating
                ? dueTs.isPresent()
                        ? createRepetitionInstanceInfo(startTs, endTs, dueTs.get())
                        : createRepetitionInstanceInfoWithoutDueTs(startTs, endTs)
                : RepetitionInstanceInfo.noRepetition(new InstantInterval(startTs, endTs));
        return new EventAndRepetition(event, repetitionInstanceInfo);
    }
}
