package ru.yandex.calendar.logic.event;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author ssytnik
 */
public class EventRoutinesGetSingleInstanceTest extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private TestManager testManager;

    @Test
    public void recurIdAtStartNotMoved() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14300").getUid();
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSameTime(uid, 0);
        doCheckGetSingleInstance(0, uid, events._2.getId());
    }

    @Test
    public void recurIdAtStartMovedToPast() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14201").getUid();
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSameTime(uid, -2);
        doCheckGetSingleInstance(-2, uid, events._2.getId());
    }

    @Test
    public void recurIdAtStartMovedToFuture() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14202").getUid();
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurInstWithSameTime(uid, 2);
        doCheckGetSingleInstance(2, uid, events._2.getId());
    }

    @Test
    public void twoExdatesInTheBeginning() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14203").getUid();
        // Event + repetition, 1st and 2nd instances are exdates
        Event event = testManager.createEventWithDailyRepetition(uid);
        testManager.createExdate(event.getStartTs(), event.getId());
        testManager.createExdate(TestDateTimes.addDaysMoscow(event.getStartTs(), 1), event.getId());
        doCheckGetSingleInstance(2 * 24, uid, event.getId());
    }

    @Test
    public void recurrenceByMaster() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14204").getUid();
        Event master = testManager.createEventWithDailyRepetition(uid);

        Instant recurrenceId = TestDateTimes.addDaysMoscow(master.getStartTs(), 3);
        Event recurrence = testManager.createDefaultRecurrence(uid, master.getId(), recurrenceId);

        EventInstanceInfo eventInstance = eventRoutines.getSingleInstance(
                Option.of(uid), Option.of(recurrenceId), master.getId(), ActionSource.WEB);

        Assert.equals(recurrence.getId(), eventInstance.getEventId());
        Assert.equals(recurrence.getStartTs(), eventInstance.getInterval().getStart());
    }

    @Test
    public void searchNearStart() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14205").getUid();
        Event master = testManager.createEventWithDailyRepetition(uid);

        Function<Instant, EventInstanceInfo> getInstanceF = startTs -> eventRoutines.getSingleInstance(
                Option.of(uid), Option.of(startTs), true, false, master.getId(), ActionSource.WEB);

        Instant recurrenceId = TestDateTimes.addDaysMoscow(master.getStartTs(), 3);
        Duration hour = Duration.standardHours(1);

        Assert.equals(recurrenceId,getInstanceF.apply(recurrenceId.plus(hour)).getInterval().getStart());
        Assert.equals(recurrenceId, getInstanceF.apply(recurrenceId.minus(hour)).getInterval().getStart());

        testManager.createDefaultRecurrence(uid, master.getId(), recurrenceId);

        Assert.equals(recurrenceId, getInstanceF.apply(recurrenceId.plus(hour)).getInterval().getStart());
        Assert.equals(recurrenceId, getInstanceF.apply(recurrenceId.minus(hour)).getInterval().getStart());
    }

    private void doCheckGetSingleInstance(int expectedFirstInstanceShiftHours, PassportUid uid, long eventId) {
        EventInstanceInfo eii = eventRoutines.getSingleInstance(
                Option.of(uid), Option.<Instant>empty(),
                eventId, ActionSource.WEB
        );
        Instant expectedMs = TestManager.eventStartTs.toDateTime(MoscowTime.TZ)
                .plusHours(expectedFirstInstanceShiftHours).toInstant();

        Assert.A.equals(expectedMs, eii.getInterval().getStart());
    }
}
