package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.cmd.run.ResourceBusyOverlapException;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventResource;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class EventWebManagerUpdateResourcesToFutureTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private EventDbManager eventDbManager;

    private final DateTime masterStart = MoscowTime.dateTime(2016, 10, 20, 20, 0);
    private final DateTime recurrenceStart = masterStart.plusDays(3);

    private final ActionInfo actionInfo = ActionInfo.webTest(masterStart.plusDays(2).toInstant());

    private TestUserInfo user;
    private Resource resource1;
    private Resource resource2;
    private Resource resource3;

    @Before
    public void cleanBeforeTest() {
        user = testManager.prepareYandexUser(TestManager.createDbrylev());
        resource1 = testManager.cleanAndCreateResourceWithNoSyncWithExchange("resource-1", "First");
        resource2 = testManager.cleanAndCreateResourceWithNoSyncWithExchange("resource-2", "Second");
        resource3 = testManager.cleanAndCreateResourceWithNoSyncWithExchange("resource-3", "Third");
    }

    @Test
    public void skipExcludedRecurrences() {
        Event master = createRepeatingMeeting(resource1);
        Event recurrence = createTimeUnchangedRecurrence(master, recurrenceStart.plusDays(1), resource2);
        Event movedRecurrence = createTimeChangedRecurrence(master, recurrenceStart, resource2);

        updateAddResource(master, resource3);

        assertNotParticipates(master, resource2);
        assertNotParticipates(recurrence, resource1);
        assertNotParticipates(movedRecurrence, resource1);

        assertParticipates(master, resource1);
        assertParticipates(recurrence, resource2);
        assertParticipates(movedRecurrence, resource2);
    }

    @Test
    public void skipPastRecurrences() {
        Event master = createRepeatingMeeting(resource1);
        Event recurrence = createTimeUnchangedRecurrence(master, masterStart, resource1);

        updateAddResource(master, resource2);

        assertParticipates(master, resource2);
        assertNotParticipates(recurrence, resource2);
    }

    @Test
    public void ignoreBusyForTimeChangedRecurrences() {
        Event master = createRepeatingMeeting(resource1);
        Event freeRecurrence = createTimeChangedRecurrence(master, recurrenceStart, resource1);
        Event busyRecurrence = createTimeChangedRecurrence(master, recurrenceStart.plusDays(1), resource1);

        createSingleMeeting(busyRecurrence.getStartTs(), resource2);

        updateAddResource(master, resource2);

        assertParticipates(master, resource2);
        assertParticipates(freeRecurrence, resource2);
        assertNotParticipates(busyRecurrence, resource2);
    }

    @Test
    public void throwBusyForTimeUnchangedRecurrences() {
        Event master = createRepeatingMeeting(resource1);
        Event busyRecurrence = createTimeUnchangedRecurrence(master, recurrenceStart, resource1);

        createSingleMeeting(busyRecurrence.getStartTs(), resource2);

        Assert.assertThrows(() -> updateAddResource(master, resource2), ResourceBusyOverlapException.class);
    }

    @Test
    public void addResourceToRecurrenceAndFuture() {
        Event master = createRepeatingMeeting();
        Event recurrence = createTimeChangedRecurrence(master, recurrenceStart);

        Assert.none(updateRecurrenceAddResource(recurrence, resource1, recurrenceStart.toInstant()).getNewEventId());

        assertParticipates(master, resource1);
        assertParticipates(recurrence, resource1);
    }

    @Test
    public void addResourceToTailByRecurrence() {
        Event master = createRepeatingMeeting();
        Event recurrence = createTimeChangedRecurrence(master, recurrenceStart);

        ModificationInfo info = updateRecurrenceAddResource(recurrence, resource1, masterStart);

        Assert.some(info.getNewEvent());
        assertParticipates(info.getNewEvent().get().getEvent(), resource1);

        assertNotParticipates(master, resource1);
        Assert.none(eventDbManager.getEventByIdSafe(recurrence.getId()));
    }

    @Test
    public void addResourceToTail() {
        Event master = createRepeatingMeeting();

        ModificationInfo info = updateAddResource(master, resource1, Option.of(Days.THREE));

        Assert.some(info.getNewEvent());
        assertParticipates(info.getNewEvent().get().getEvent(), resource1);

        assertNotParticipates(master, resource1);
    }

    private void assertParticipates(Event event, Resource resource) {
        Assert.some(findEventResource(event, resource),
                resource.getExchangeName().get() + " expected to be participant of " + event.getName());
    }

    private void assertNotParticipates(Event event, Resource resource) {
        Assert.none(findEventResource(event, resource),
                resource.getExchangeName().get() + " expected not to be participant of " + event.getName());
    }

    private Option<EventResource> findEventResource(Event event, Resource resource) {
        return eventResourceDao.findEventResourceByEventIdAndResourceId(event.getId(), resource.getId());
    }

    private ModificationInfo updateAddResource(Event event, Resource resource) {
        return updateAddResource(event, resource, Option.empty());
    }

    private ModificationInfo updateAddResource(Event event, Resource resource, Option<Days> instanceShift) {
        EventData data = new EventData();
        data.getEvent().setFields(event.copy());

        instanceShift.forEach(shift -> {
            data.setInstanceStartTs(event.getStartTs().plus(shift.toStandardDuration()));
            data.getEvent().setStartTs(event.getStartTs().plus(shift.toStandardDuration()));
            data.getEvent().setEndTs(event.getEndTs().plus(shift.toStandardDuration()));
        });

        data.setTimeZone(MoscowTime.TZ);
        data.setInvData(new EventInvitationUpdateData(Cf.list(ResourceRoutines.getResourceEmail(resource)), Cf.list()));
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        return eventWebManager.update(user.getUserInfo(), data, Option.empty(), true, actionInfo);
    }

    private ModificationInfo updateRecurrenceAddResource(Event recurrence, Resource resource, ReadableInstant now) {
        EventData data = new EventData();
        data.getEvent().setFields(recurrence.copy());
        data.getEvent().setRecurrenceIdNull();

        data.setInstanceStartTs(recurrence.getRecurrenceId().get());
        data.getEvent().setStartTs(recurrence.getRecurrenceId().get());
        data.getEvent().setEndTs(recurrence.getRecurrenceId().get()
                .plus(new Duration(recurrence.getStartTs(), recurrence.getEndTs())));

        data.setTimeZone(MoscowTime.TZ);
        data.setInvData(new EventInvitationUpdateData(Cf.list(ResourceRoutines.getResourceEmail(resource)), Cf.list()));
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        return eventWebManager.update(user.getUserInfo(), data, Option.empty(), true, ActionInfo.webTest(now));
    }

    private Event createSingleMeeting(ReadableInstant start, Resource... resources) {
        Event e = testManager.createDefaultEvent(user.getUid(), "Single", start);

        addParticipantsToMeeting(e, resources);
        return e;
    }

    private Event createRepeatingMeeting(Resource... resources) {
        Event e = testManager.createDefaultEvent(user.getUid(), "Repeating", masterStart);
        testManager.createDailyRepetitionAndLinkToEvent(e.getId());

        addParticipantsToMeeting(e, resources);
        return e;
    }

    private Event createTimeUnchangedRecurrence(Event masterMeeting, ReadableInstant recurrenceId, Resource... resources) {
        Event e = testManager.createDefaultRecurrence(user.getUid(), masterMeeting.getId(), recurrenceId);

        addParticipantsToMeeting(e, resources);
        return e;
    }

    private Event createTimeChangedRecurrence(Event masterMeeting, ReadableInstant recurrenceId, Resource... resources) {
        Event e = new Event();
        e.setFields(createTimeUnchangedRecurrence(masterMeeting, recurrenceId, resources));

        e.setStartTs(e.getStartTs().plus(Duration.standardHours(1)));
        e.setEndTs(e.getEndTs().plus(Duration.standardHours(1)));

        eventDbManager.updateEvent(e, actionInfo);
        return e;
    }

    private void addParticipantsToMeeting(Event e, Resource... resources) {
        testManager.addUserParticipantToEvent(e.getId(), user, Decision.YES, true);

        Cf.x(resources).forEach(r -> testManager.addResourceParticipantToEvent(e.getId(), r));

        testManager.updateEventTimeIndents(e);
    }
}
