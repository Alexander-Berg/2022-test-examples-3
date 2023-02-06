package ru.yandex.calendar.logic.ics.imp;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.MainEventWithRelations;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsImporterFromFileNonMeetingTest extends IcsImporterFromFileTestBase {

    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private NotificationRoutines notificationRoutines;
    @Autowired
    private EventDbManager eventDbManager;


    //////////////////////////////
    /// Single event use cases ///
    /**
     * First ics: none
     * Second ics: single event file
     * Check, that new event was added
     */
    @Test
    public void singleEventNotFound() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11001").getUid();

        long eventId = importIcsByUid("update/testSingleEventNotFound.ics", uid).getProcessedEventIds().single();
        Event event = eventDao.findEventById(eventId);
        Assert.A.none(event.getRecurrenceId());
    }

    @Test
    public void singleEventNeedToUpdate() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11002").getUid();

        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);

        Event event = new Event();
        event.setStartTs(TestDateTimes.moscow(2009, 10, 24, 15, 0));
        event.setEndTs(TestDateTimes.moscow(2009, 10, 24, 16, 0));
        event.setDtstamp(TestDateTimes.moscow(2009, 10, 21, 0, 0)); // XXX: dtstamp is ignored now
        event.setLastUpdateTs(TestDateTimes.moscow(2009, 10, 21, 0, 0));
        event.setSequence(0);
        event.setName("event");

        String externalId = CalendarUtils.generateExternalId();
        Tuple2List<Event, String> eventsExternalIds = Tuple2List.fromPairs(event, externalId);
        Event createdEvent = testManager.batchCreateEventOnLayer(uid, eventsExternalIds, layerId).single();
        Event oldEvent = eventDao.findEventById(createdEvent.getId());
        Assert.A.none(oldEvent.getRecurrenceId());

        String updatedIcs =
            "BEGIN:VCALENDAR\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091024T150000\n" +
            "DTEND;TZID=Europe/Moscow:20091024T160000\n" +
            "DTSTAMP:20091022T000000Z\n" +
            "UID:" + externalId +"\n" +
            "SEQUENCE:0\n" +
            "STATUS:CONFIRMED\n" +
            "SUMMARY:new event\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n" +
            "";

        importCalendar(uid, IcsCalendar.parseString(updatedIcs));
        Event newEvent = eventDao.findEventById(createdEvent.getId());
        Assert.A.none(newEvent.getRecurrenceId());

        Assert.A.notEquals(oldEvent.getName(), newEvent.getName());
    }

    /**
     * First ics: update single event
     * Second ics: previous version of single event
     * Check, that event was NOT updated
     */
    @Test
    public void singleEventUpdated() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11003").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@

        long eventId1 = importIcsByUid("update/testSingleEventUpdated_upd.ics", uid).getProcessedEventIds().single();

        Event oldEvent = eventDao.findEventById(eventId1);
        Assert.A.none(oldEvent.getRecurrenceId());

        long eventId2 = importIcsByUid("update/testSingleEventUpdated.ics", uid).getProcessedEventIds().single();

        Event newEvent = eventDao.findEventById(eventId2);
        Assert.A.none(newEvent.getRecurrenceId());
        Assert.A.equals(oldEvent.getName(), newEvent.getName());
    }

    ////////////////////////////////
    /// Repeated event use cases ///
    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: updated name of main event
     * Check, that main event was updated and recurrence event was removed
     */
    @Test
    public void repEventNeedToUpdate() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11004").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@

        IcsImportStats stats1 = importIcsByUid("update/testRepEventNeedToUpdate.ics", uid);
        Assert.assertEquals(2, stats1.getProcessedCount());
        Assert.assertEquals(2, stats1.getTotalCount());

        Event oldMainEvent = eventDao.findMasterEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        Event oldRecEvent = eventDao.findRecurrenceEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        Assert.A.equals(oldMainEvent.getMainEventId(), oldRecEvent.getMainEventId());

        IcsImportStats stats2 = importIcsByUid("update/testRepEventNeedToUpdate_upd.ics", uid);
        Assert.assertEquals(1, stats2.getProcessedCount());
        Assert.assertEquals(1, stats2.getTotalCount());

        SetF<Long> allProcessedEventIds = stats1.getProcessedEventIds().plus(stats2.getProcessedEventIds());

        Event newMainEvent = eventDao.findMasterEventsWithPossibleIds(allProcessedEventIds.toList()).single();
        Assert.assertFalse(oldMainEvent.getName().equals(newMainEvent.getName()));

        ListF<Event> allEventsInGroup = eventDao.findEventsByMainId(oldRecEvent.getMainEventId());
        Assert.A.hasSize(1, allEventsInGroup);
        Assert.A.equals(newMainEvent.getId(), allEventsInGroup.single().getId());
    }

    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: updated time of main event
     * Check, that main event was updated and recurrence event was removed
     */
    @Test
    public void repEventNeedToUpdateTimeRep() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11005").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@

        IcsImportStats stats1 = importIcsByUid("update/testRepEventNeedToUpdateTimeRep.ics", uid);
        Assert.assertEquals(2, stats1.getProcessedCount());
        Assert.assertEquals(2, stats1.getTotalCount());

        Event oldMainEvent = eventDao.findMasterEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        Event oldRecEvent = eventDao.findRecurrenceEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        oldRecEvent.toString();

        IcsImportStats stats2 = importIcsByUid("update/testRepEventNeedToUpdateTimeRep_time_upd.ics", uid);
        Assert.assertEquals(1, stats2.getProcessedCount());
        Assert.assertEquals(1, stats2.getTotalCount());

        SetF<Long> allProcessedEventIds = stats1.getProcessedEventIds().plus(stats2.getProcessedEventIds());

        Event newMainEvent = eventDao.findMasterEventsWithPossibleIds(allProcessedEventIds.toList()).single();
        Assert.assertFalse(oldMainEvent.getName().equals(newMainEvent.getName()));

        ListF<Event> recurrenceEvents = eventDao.findRecurrenceEventsWithPossibleIds(allProcessedEventIds.toList());
        Assert.A.hasSize(0, recurrenceEvents);
    }

    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: updated recurrence event and previous version of main event, but with new timestamp
     * Check, that main event wasn't changed and recurrence event was updated
     */
    @Test
    public void repEvent2NeedToUpdate() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11007").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        IcsImportStats stats1 = importIcsByUid("update/rep_event2.ics", uid);
        Assert.assertEquals(2, stats1.getProcessedCount());
        Assert.assertEquals(2, stats1.getTotalCount());

        Event oldMainEvent = eventDao.findMasterEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        Event oldRecEvent = eventDao.findRecurrenceEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();

        IcsImportStats stats2 = importIcsByUid("update/rep_event2_upd.ics", uid);
        Assert.assertEquals(2, stats2.getProcessedCount());
        Assert.assertEquals(2, stats2.getTotalCount());

        SetF<Long> allProcessedEventIds = stats1.getProcessedEventIds().plus(stats2.getProcessedEventIds());

        Event newMainEvent = eventDao.findMasterEventsWithPossibleIds(allProcessedEventIds.toList()).single();
        Assert.A.equals(oldMainEvent.getName(), newMainEvent.getName());

        ListF<Event> recurrenceEvents = eventDao.findRecurrenceEventsWithPossibleIds(allProcessedEventIds.toList());
        Assert.A.hasSize(1, recurrenceEvents);
        Event newEvent = recurrenceEvents.single();
        Assert.assertFalse(oldRecEvent.getName().equals(newEvent.getName()));
    }

    /**
     * First ics: previous version of main event and new version of recurrence event
     * Second ics: previous version of main event and previous version of recurrence event
     * Check, that nothing changed
     */
    @Test
    public void repEvent2Updated() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11008").getUid();

        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);

        Event masterEvent = new Event();
        masterEvent.setStartTs(TestDateTimes.moscow(2009, 10, 22, 10, 0));
        masterEvent.setEndTs(TestDateTimes.moscow(2009, 10, 22, 11, 0));
        masterEvent.setDtstamp(TestDateTimes.moscow(2009, 10, 22, 13, 53));
        masterEvent.setSequence(0);
        masterEvent.setName("event");

        Event recurrenceEvent = new Event();
        recurrenceEvent.setStartTs(TestDateTimes.moscow(2009, 10, 22, 16, 0));
        recurrenceEvent.setEndTs(TestDateTimes.moscow(2009, 10, 22, 17, 0));
        recurrenceEvent.setDtstamp(TestDateTimes.moscow(2009, 10, 22, 13, 53));
        recurrenceEvent.setSequence(0);
        recurrenceEvent.setName("new event");
        recurrenceEvent.setRecurrenceId(TestDateTimes.moscow(2009, 10, 24, 10, 0));

        Tuple2List<Event, String> eventsExternalIds = Tuple2List.fromPairs(
                masterEvent, "sdh34rjkh3rqkh2l12k4h24kj3h4@google.com",
                recurrenceEvent, "sdh34rjkh3rqkh2l12k4h24kj3h4@google.com");
        ListF<Event> events = testManager.batchCreateEventOnLayer(uid, eventsExternalIds, layerId);

        final SetF<Long> eventIds = events.map(Event.getIdF()).unique();
        Event oldMainEvent = eventDao.findMasterEventsWithPossibleIds(eventIds.toList()).single();
        Event oldRecurrenceEvent = eventDao.findRecurrenceEventsWithPossibleIds(eventIds.toList()).single();
        testManager.createDailyRepetitionAndLinkToEvent(oldMainEvent.getId());

        String ics2 =
            "BEGIN:VCALENDAR\n" +
            "METHOD:PUBLISH\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091024T150000\n" +
            "DTEND;TZID=Europe/Moscow:20091024T160000\n" +
            "DTSTAMP:20091022T135205Z\n" +
            "UID:sdh34rjkh3rqkh2l12k4h24kj3h4@google.com\n" +
            "RECURRENCE-ID;TZID=Europe/Moscow:20091024T100000\n" +
            "CREATED:20091022T135136Z\n" +
            "LAST-MODIFIED:20091022T135144Z\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:event\n" +
            "END:VEVENT\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091022T100000\n" +
            "DTEND;TZID=Europe/Moscow:20091022T110000\n" +
            "RRULE:FREQ=DAILY;UNTIL=20091027T070000Z;WKST=MO\n" +
            "DTSTAMP:20091022T135205Z\n" +
            "UID:sdh34rjkh3rqkh2l12k4h24kj3h4@google.com\n" +
            "CREATED:20091022T135136Z\n" +
            "LAST-MODIFIED:20091022T135136Z\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:event\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n" +
            "";

        IcsImportStats stats2 = importCalendar(uid, IcsCalendar.parseString(ics2));
        Assert.assertEquals(2, stats2.getProcessedCount());
        Assert.assertEquals(2, stats2.getTotalCount());

        SetF<Long> allProcessedEventIds = eventIds.plus(stats2.getProcessedEventIds());

        Event newMainEvent = eventDao.findMasterEventsWithPossibleIds(allProcessedEventIds.toList()).single();
        Assert.A.equals(oldMainEvent.getName(), newMainEvent.getName());

        ListF<Event> recurrenceEvents = eventDao.findRecurrenceEventsWithPossibleIds(allProcessedEventIds.toList());
        Assert.A.hasSize(1, recurrenceEvents);
        Event newRecurrenceEvent = recurrenceEvents.single();
        Assert.A.equals(oldRecurrenceEvent.getName(), newRecurrenceEvent.getName());
    }

    @Test
    public void recurEventNotFound() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11009").getUid();

        String ics =
            "BEGIN:VCALENDAR\n" +
            "PRODID:-//Google Inc//Google Calendar 70.9054//EN\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091024T160000\n" +
            "DTEND;TZID=Europe/Moscow:20091024T170000\n" +
            "DTSTAMP:20091022T135259Z\n" +
            "UID:89hqv21egu6mf0hu0pd72hlv64@google.com\n" +
            "RECURRENCE-ID;TZID=Europe/Moscow:20091024T100000\n" +
            "CREATED:20091022T135136Z\n" +
            "DESCRIPTION:\n" +
            "LAST-MODIFIED:20091022T135234Z\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:new event\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n" +
            "";

        long eventId = importCalendar(uid, IcsCalendar.parseString(ics)).getProcessedEventIds().single();
        Assert.A.some(eventDao.findEventById(eventId).getRecurrenceId());
    }

    @Test
    public void importNewRecurrence() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11010").getUid();

        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);

        Event masterEvent = new Event();
        masterEvent.setStartTs(TestDateTimes.moscow(2009, 10, 22, 10, 0));
        masterEvent.setEndTs(TestDateTimes.moscow(2009, 10, 22, 11, 0));
        masterEvent.setDtstamp(TestDateTimes.moscow(2009, 10, 22, 13, 52));
        masterEvent.setSequence(0);
        masterEvent.setName("event");

        Tuple2List<Event, String> eventsExternalIds = Tuple2List.fromPairs(
                masterEvent, "12345678901234567890@google.com");
        masterEvent = testManager.batchCreateEventOnLayer(uid, eventsExternalIds, layerId).single();
        testManager.createDailyRepetitionAndLinkToEvent(masterEvent.getId());

        EventUser eventUser = new EventUser();
        eventUser.setEventId(masterEvent.getId());
        eventUser.setUid(uid);
        eventUser.setDecision(Decision.UNDECIDED);
        eventUser.setAvailability(Availability.MAYBE);
        eventUser.setIsOrganizer(false);
        eventUser.setIsAttendee(false);
        eventUserDao.saveEventUser(eventUser, ActionInfo.webTest());

        String icsWithRecurrenceEvent =
            "BEGIN:VCALENDAR\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091022T160000\n" +
            "DTEND;TZID=Europe/Moscow:20091022T170000\n" +
            "DTSTAMP:20091022T135259Z\n" +
            "RRULE:FREQ=DAILY\n" +
            "UID:12345678901234567890@google.com\n" +
            "CREATED:20091022T135136Z\n" +
            "LAST-MODIFIED:20091022T135234Z\n" +
            "SEQUENCE:1\n" +
            "SUMMARY:event\n" +
            "END:VEVENT\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/Moscow:20091024T160000\n" +
            "DTEND;TZID=Europe/Moscow:20091024T170000\n" +
            "DTSTAMP:20091022T135259Z\n" +
            "UID:12345678901234567890@google.com\n" +
            "RECURRENCE-ID;TZID=Europe/Moscow:20091024T160000\n" +
            "CREATED:20091022T135136Z\n" +
            "LAST-MODIFIED:20091022T135234Z\n" +
            "SEQUENCE:1\n" +
            "SUMMARY:new event\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n" +
            "";

        importCalendar(uid, IcsCalendar.parseString(icsWithRecurrenceEvent));

        MainEventWithRelations mainEventWithRelations = eventDbManager.getMainEventWithRelationsById(masterEvent.getMainEventId());

        Assert.A.hasSize(1, mainEventWithRelations.getRecurrenceEvents());
        Assert.A.hasSize(1, mainEventWithRelations.getMasterEvents());

        Assert.A.equals(masterEvent.getName(), mainEventWithRelations.getMasterEvents().single().getEvent().getName());
        Assert.A.equals("new event",  mainEventWithRelations.getRecurrenceEvents().single().getEvent().getName());
    }

    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: new version of main event and new version of recurrence event (non time change)
     * Check, that both events were changed
     */
    @Test
    public void mainAndRecurEventUpdated() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11011").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        IcsImportStats stats1 = importIcsByUidWithStatCheck("update/rep_event6.ics", uid, (new long[] {2, 2}));

        Event oldMainEvent = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("recurrence_id IS NULL").and(EventFields.ID.column().inSet(stats1.getProcessedEventIds()))).get();
        Event oldRecEvent = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("recurrence_id IS NOT NULL").and(EventFields.ID.column().inSet(stats1.getProcessedEventIds()))).get();

        IcsImportStats stats2 = importIcsByUidWithStatCheck("update/rep_event6_upd.ics", uid, (new long[] {2, 2}));

        SetF<Long> allProcessedEventIds = stats1.getProcessedEventIds().plus(stats2.getProcessedEventIds());

        Event newMainEvent = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("recurrence_id IS NULL").and(EventFields.ID.column().inSet(allProcessedEventIds))).get();
        String oldName = oldMainEvent.getName();
        String newName = newMainEvent.getName();
        Assert.assertFalse(oldName.equals(newName));
        Event newRecEvent = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("recurrence_id IS NOT NULL").and(EventFields.ID.column().inSet(allProcessedEventIds))).get();
        String oldName1 = oldRecEvent.getName();
        String newName1 = newRecEvent.getName();
        Assert.assertFalse(oldName1.equals(newName1));
    }

    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: new version of main event and new version of recurrence event (time change)
     * Check, that both events were changed
     */
    @Test
    public void mainAndRecurEventUpdated2() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11012").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        IcsImportStats stats1 = importIcsByUid("update/rep_event7.ics", uid);
        Assert.assertEquals(2, stats1.getProcessedCount());
        Assert.assertEquals(2, stats1.getTotalCount());

        Event oldMainEvent = eventDao.findMasterEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();
        Event oldRecEvent = eventDao.findRecurrenceEventsWithPossibleIds(stats1.getProcessedEventIds().toList()).single();

        IcsImportStats stats2 = importIcsByUid("update/rep_event7_upd.ics", uid);
        Assert.hasSize(2, stats2.getProcessedEventIds());
        Assert.assertEquals(2, stats2.getTotalCount());

        SetF<Long> allProcessedEventIds = stats1.getProcessedEventIds().plus(stats2.getProcessedEventIds());

        Event newMainEvent = eventDao.findMasterEventsWithPossibleIds(allProcessedEventIds.toList()).single();
        Assert.assertFalse(oldMainEvent.getName().equals(newMainEvent.getName()));

        ListF<Event> recurrenceEvents = eventDao.findRecurrenceEventsWithPossibleIds(allProcessedEventIds.toList());

        Assert.A.hasSize(1, recurrenceEvents);
        Assert.A.notEquals(oldRecEvent.getName(), recurrenceEvents.single().getName());
    }


    /**
     * First ics: single event
     * Second ics: update single event
     */
    @Test
    public void singleEventSequence() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11013").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        long eventId = importIcsByUid("update/testSingleEventSequence.ics", uid).getNewEventIds().single();

        Event oldEvent = eventDao.findEventById(eventId);
        Assert.A.equals(0, oldEvent.getSequence());

        long updatedEventId = importIcsByUid("update/testSingleEventSequence_upd.ics", uid).getUpdatedEventIds().single();

        Assert.A.equals(eventId, updatedEventId);

        Event newEvent = eventDao.findEventById(eventId);
        Assert.A.equals(0, newEvent.getSequence());
    }

    /**
     * First ics: main event with repetition and recurrence event
     * Second ics: updated time of main event
     */
    @Test
    public void repEventSequence() throws Exception {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11014").getUid();

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        SetF<Long> firstEventIds = importIcsByUid("update/rep_event.ics", uid).getNewEventIds();
        SetF<Long> secondEventIds = importIcsByUid("update/rep_event_time_upd.ics", uid).getUpdatedEventIds();

        SetF<Long> eventIds = firstEventIds.plus(secondEventIds);

        Event event = eventDao.findMasterEventsWithPossibleIds(eventIds.toList()).single();
        Assert.A.equals(4, event.getSequence());
    }
}
