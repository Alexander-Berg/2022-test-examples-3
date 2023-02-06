package ru.yandex.calendar.logic.event;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple4;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

public class EventRoutinesGetSortedInstancesTest extends AbstractDbDataTest {
    @Autowired
    protected EventRoutines eventRoutines;
    @Autowired
    private TestManager testManager;

    private final Instant startMs = new DateTime(2009, 4, 27, 0, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
    private final Instant endMs = new DateTime(2009, 5, 2, 23, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();

    @Test
    public void singleEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14200").getUid();
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "singleEvent");
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                event.getStartTs(), Option.of(event.getEndTs()), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(1, infoSet);
        Assert.A.equals(infoSet.first().getInterval().getStart().toInstant(), event.getStartTs());
        Assert.A.equals(infoSet.first().getInterval().getEnd().toInstant(), event.getEndTs());
    }

    @Test
    public void eventWithRdate1() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14201").getUid();
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "eventWithRdate1");
        Rdate rdate = testManager.createRdate(TestDateTimes.addDaysMoscow(event.getStartTs(), 2), event.getId());
        testManager.updateEventTimeIndents(event);

        Instant startSearchInterval = event.getStartTs();
        Instant endSearchInterval = TestDateTimes.addDaysMoscow(event.getStartTs(), 3);
        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startSearchInterval, Option.of(endSearchInterval),
                LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(2, infoSet);
        Assert.A.equals(infoSet.get(0).getInterval().getStart().toInstant(), event.getStartTs());
        Assert.A.equals(infoSet.get(0).getInterval().getEnd().toInstant(), event.getEndTs());
        checkRdateInstance(infoSet.get(1), rdate.getStartTs(), event);
    }

    @Test
    public void eventWithRdate2() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14202").getUid();
        Tuple2<Event, Rdate> t = testManager.createEventWithRdate(uid);
        Event event = t._1;
        Rdate rdate = t._2;
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                t._2.getStartTs(), Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(1, infoSet);
        checkRdateInstance(infoSet.first(), rdate.getStartTs(), event);
    }

    @Test
    public void eventWithRepetition() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14203").getUid();
        Event event = testManager.createEventWithDailyRepetition(uid);
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(6, infoSet);
        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        for (EventInstanceInfo info : infoSet) {
            Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
            Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            mdtSt.addDays(1);
            mdtEnd.addDays(1);
        }
    }

    @Test
    public void eventWithRepetitionAndExdate() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14204").getUid();
        Event event = testManager.createEventWithRepetitionAndExdate(uid)._1;
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid), startMs,
                Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(5, infoSet);
        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        int i = 0;
        for (EventInstanceInfo info : infoSet) {
            int addDays = i != 1 ? 1 : 2;
            Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
            Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            mdtSt.addDays(addDays);
            mdtEnd.addDays(addDays);
            i++;
        }
    }

    @Test
    public void recurEventWithRepetition() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14205").getUid();
        Tuple2<Event, Event> t = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event event = t._1;
        Event recurrence = t._2;
        testManager.updateEventTimeIndents(event, recurrence);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(6, infoSet);
        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        int i = 0;
        for (EventInstanceInfo info : infoSet) {
            if (i == 1) {
                checkRecurInstance(info, recurrence);
            } else {
                Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
                Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            }
            mdtSt.addDays(1);
            mdtEnd.addDays(1);
            i++;
        }
    }

    @Test
    public void eventWithRepetitionAndRecurInstWithSameTime() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14206").getUid();
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSameTime(uid, 0);
        Event event = events._1;
        testManager.updateEventTimeIndents(events._1, events._2);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(6, infoSet);
        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        for (EventInstanceInfo info : infoSet) {
            Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
            Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            mdtSt.addDays(1);
            mdtEnd.addDays(1);
        }
    }

    @Test
    public void recurEventAndFuture() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14207").getUid();
        Tuple4<Event, Long, Rdate, Event> t = testManager.createEventWithRepetitionAndRdateAndRecurrence(uid);
        Event event = t._1;
        Rdate rdate = t._3;
        Event recurrence = t._4;
        testManager.updateEventTimeIndents(event, recurrence);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        Assert.assertHasSize(7, infoSet);

        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        int i = 0;
        for (EventInstanceInfo info : infoSet) {
            if (i == 1) {
                checkRecurInstance(info, recurrence);
            } else if (i == 4) {
                checkRdateInstance(info, rdate.getStartTs(), event);
            } else {
                Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
                Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            }
            if (i != 4) {
                mdtSt.addDays(1);
                mdtEnd.addDays(1);
            }
            i++;
        }
    }

    private void checkRdateInstance(EventInstanceInfo info, Instant rdateTs, Event event) {
        Assert.A.equals(info.getInterval().getStart(), rdateTs);
        MutableDateTime mdt = new MutableDateTime(rdateTs.getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        mdt.add(event.getEndTs().getMillis() - event.getStartTs().getMillis());
        Assert.A.equals(info.getInterval().getEnd(), mdt.toInstant());
    }

    private void checkRecurInstance(EventInstanceInfo info, Event recurrence) {
        Assert.assertEquals(info.getInterval().getStart().getMillis(), recurrence.getStartTs().getMillis());
        Assert.assertEquals(info.getInterval().getEnd().getMillis(), recurrence.getEndTs().getMillis());
    }

    @Test
    public void eventWithRepetitionAndDueTsAndRdate() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14208").getUid();
        long repetitionId = testManager.createDailyRepetitionWithDueTs(
                new DateTime(2009, 4, 29, 17, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant());
        Event event = testManager.createRepeatedEvent(uid, repetitionId);
        Rdate rdate = testManager.createRdate(event.getId());
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(uid),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(uid, false), ActionSource.WEB);
        MutableDateTime mdtSt = new MutableDateTime(event.getStartTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        MutableDateTime mdtEnd = new MutableDateTime(event.getEndTs().getMillis(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        Assert.assertHasSize(4, infoSet);
        int i = 0;
        for (EventInstanceInfo info : infoSet) {
            if (i == 3) {
                checkRdateInstance(info, rdate.getStartTs(), event);
            } else {
                Assert.A.equals(mdtSt.toInstant(), info.getInterval().getStart());
                Assert.A.equals(mdtEnd.toInstant(), info.getInterval().getEnd());
            }
            mdtSt.addDays(1);
            mdtEnd.addDays(1);
            i++;
        }
    }

    @Test
    public void eventWithTwoInvitedResources() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14209").getUid();
        Resource smolny = testManager.cleanAndCreateSmolny();
        Resource threeLittlePigs = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(uid, "eventWithTwoInvitedResources");
        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), smolny);
        testManager.addResourceParticipantToEvent(event.getId(), threeLittlePigs);
        testManager.updateEventTimeIndents(event);

        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesOnResource(
                Option.of(uid), EventGetProps.any(), Cf.list(smolny.getId(), threeLittlePigs.getId()),
                EventLoadLimits.intersectsInterval(event.getStartTs(), event.getEndTs()), ActionSource.WEB);

        Assert.A.hasSize(2, infoSet);
    }

    @Test
    public void rejectedEventAndSizeLimits() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(12).getUid();

        DateTime start = DateTime.now(MoscowTime.TZ);
        createUserEvent(uid, start.plusHours(0), Decision.YES);
        createUserEvent(uid, start.plusHours(1), Decision.NO);
        createUserEvent(uid, start.plusHours(2), Decision.NO);
        createUserEvent(uid, start.plusHours(3), Decision.YES);
        createUserEvent(uid, start.plusHours(4), Decision.NO);
        createUserEvent(uid, start.plusHours(5), Decision.NO);
        createUserEvent(uid, start.plusHours(6), Decision.YES);

        ListF<EventInstanceInfo> instances = eventRoutines.getSortedInstancesIMayView(
                Option.of(uid), EventGetProps.any(), LayerIdPredicate.allForUser(uid, false),
                EventLoadLimits.limitResultSize(3), ActionSource.WEB);

        Assert.hasSize(3, instances);
        Assert.forAll(instances, EventInstanceInfo.notRejectedF());
    }

    private void createUserEvent(PassportUid uid, ReadableInstant startTs, Decision decision) {
        Event event = testManager.createDefaultEvent(uid, "Event " + startTs, startTs.toInstant());
        testManager.addUserParticipantToEvent(event.getId(), uid, decision, false);
        testManager.updateEventTimeIndents(event);
    }
}
