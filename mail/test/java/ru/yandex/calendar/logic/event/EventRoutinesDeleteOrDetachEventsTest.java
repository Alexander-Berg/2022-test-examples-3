package ru.yandex.calendar.logic.event;

import static org.junit.Assert.assertEquals;

import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.web.EventWebManagerDeleteEventTest;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.inside.passport.PassportUid;

/**
 * XXX is it possible to combine {@link Parameterized} and {@link CalendarSpringJUnit4ClassRunner} ?
 * @author ssytnik
 */
public class EventRoutinesDeleteOrDetachEventsTest extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;

    private static enum EventContents {
        MASTER_AND_RECURS_AT_BEGIN,
        MASTER_AND_RECURS,
        RECURS_ONLY
    };

    /**
     * @see EventWebManagerDeleteEventTest#testDeleteRecurEventWithRepetition()
     */
    @Test
    public void deleteEventWithRecurIds() throws Exception {
        doDeletionTest(EventContents.MASTER_AND_RECURS, Option.<Boolean>empty(), new Instant());
    }

    @Test
    public void deleteEventWithRecurIdsInTheBeginning() throws Exception {
        doDeletionTest(EventContents.MASTER_AND_RECURS_AT_BEGIN, Option.<Boolean>empty(), new Instant());
    }

    @Test
    public void deleteRecurIds() throws Exception {
        doDeletionTest(EventContents.RECURS_ONLY, Option.<Boolean>empty(), new Instant());
    }

    @Test
    public void detachMeetingEventWithRecurIds() throws Exception {
        doDeletionTest(EventContents.MASTER_AND_RECURS, Option.of(true), new Instant());
    }

    @Test
    public void detachMeetingRecurIds() throws Exception {
        doDeletionTest(EventContents.RECURS_ONLY, Option.of(true), new Instant());
    }

    @Test
    public void detachEventWithRecurIds() throws Exception {
        doDeletionTest(EventContents.MASTER_AND_RECURS, Option.of(false), new Instant());
    }

    @Test
    public void detachRecurIds() throws Exception {
        doDeletionTest(EventContents.RECURS_ONLY, Option.of(false), new Instant());
    }

    // missing test: 'uid' creates main + 2 recur., attaches to HIS another 'layer2'. Test: detach all from 'layer2'.

    // missing test: 'uid' creates main, invites 'uid2', 'uid2' accepts invitation. 'uid' deletes event - no mails.

    private void doDeletionTest(EventContents ec, Option<Boolean> goesToMeetingO, Instant now) throws Exception {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-14101");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-14102");
        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = user2.getUid();

        boolean withMasterEvent = ec != EventContents.RECURS_ONLY;
        boolean offsetsAtBegin = ec == EventContents.MASTER_AND_RECURS_AT_BEGIN;

        String externalId = CalendarUtils.generateExternalId();

        Event masterFields = new Event();
        masterFields.setName("Master");
        masterFields.setStartTs(TestManager.DEFAULT_TIME);
        masterFields.setEndTs(TestDateTimes.plusHours(TestManager.DEFAULT_TIME, 1));
        masterFields.setRepetitionId(testManager.createDailyRepetition());

        // create first recurrence
        int offsetDays1 = offsetsAtBegin ? 0 : 2;
        Event recurrenceFields1 = new Event();
        recurrenceFields1.setName("First recurrence event");
        recurrenceFields1.setRecurrenceId(TestDateTimes.addDaysMoscow(masterFields.getStartTs(), offsetDays1));
        recurrenceFields1.setStartTs(TestDateTimes.addDaysMoscow(masterFields.getStartTs(), offsetDays1));
        recurrenceFields1.setEndTs(TestDateTimes.addDaysMoscow(masterFields.getEndTs(), offsetDays1));

        // create second recurrence
        int offsetDays2 = offsetsAtBegin ? 1 : 4;
        Event recurrenceFields2 = new Event();
        recurrenceFields2.setName("Second recurrence event");
        recurrenceFields2.setRecurrenceId(TestDateTimes.addDaysMoscow(masterFields.getStartTs(), offsetDays2));
        recurrenceFields2.setStartTs(TestDateTimes.addDaysMoscow(masterFields.getStartTs(), offsetDays2));
        recurrenceFields2.setEndTs(TestDateTimes.addDaysMoscow(masterFields.getEndTs(), offsetDays2));

        Tuple2List<Event, String> eventsExternalIds = Cf.Tuple2List.arrayList();
        if (withMasterEvent) {
            eventsExternalIds.add(masterFields, externalId);
        }
        eventsExternalIds.add(recurrenceFields1, externalId);
        eventsExternalIds.add(recurrenceFields2, externalId);
        ListF<Event> createdEvents = testManager.batchCreateEvent(uid1, eventsExternalIds);

        for (Event event : createdEvents) {
            testManager.addUserParticipantToEvent(event.getId(), uid1, Decision.UNDECIDED, true);
        }
        TestUserInfo user;
        if (!goesToMeetingO.isPresent()) {
            user = user1;
        } else {
            user = user2;
            for (Event event : createdEvents) {
                testManager.addUserParticipantToEvent(event.getId(), uid2, Decision.YES, false);
            }
        }

        int eventsCount = withMasterEvent ? 3 : 2;
        long layerId = user.getDefaultLayerId();
        assertEquals(eventsCount, getCountOfEventsInLayer(layerId));
        eventRoutines.deleteOrDetachEventsByExternalId(user.getUserInfo(), layerId, externalId, ActionInfo.webTest());
        assertEquals(0, getCountOfEventsInLayer(layerId));
    }

    private int getCountOfEventsInLayer(long layerId) {
        return eventLayerDao.findEventLayersByLayerId(layerId).size();
    }
}
