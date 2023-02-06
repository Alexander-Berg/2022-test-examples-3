package ru.yandex.calendar.logic.event;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;

public class EventRoutinesGetMainInstOfEventTest extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private TestManager testManager;

    @Test
    public void getMainInstOfRepeatedEvent1() throws Exception {
        Tuple2<Event, Rdate> event = testManager.createEventWithRdate(TestManager.UID);
        String externalId = eventDao.findExternalIdByEventId(event._1.getId());
        eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(TestManager.UID), externalId).get();
    }

    // test get main instance of repeated event with rrule
    @Test
    public void getMainInstOfRepeatedEvent2() throws Exception {
        Event event = testManager.createEventWithDailyRepetition(TestManager.UID);
        String externalId = eventDao.findExternalIdByEventId(event.getId());
        eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(TestManager.UID), externalId).get();
    }

    // test get main instance of repeated event with rrule and recurrence_id
    @Test
    public void getMainInstOfRepeatedEvent3() throws Exception {
        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(TestManager.UID);
        String externalId = eventDao.findExternalIdByEventId(events._1.getId());
        eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(TestManager.UID), externalId).get();
    }
}
