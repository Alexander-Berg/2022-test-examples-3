package ru.yandex.calendar.logic.telemost;

import java.util.OptionalLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.frontend.bender.RawJson;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.commune.bazinga.impl.OnetimeJob;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.calendar.logic.sharing.InvitationProcessingMode.SAVE_ONLY;
import static ru.yandex.calendar.logic.telemost.TelemostEventDataUtils.getBroadcastLink;
import static ru.yandex.calendar.logic.telemost.TelemostEventDataUtils.getConferenceLink;
import static ru.yandex.calendar.logic.telemost.TelemostEventDataUtils.needGenerateConference;
import static ru.yandex.calendar.logic.telemost.TelemostEventDataUtils.needGenerateBroadcast;

@ContextConfiguration(classes = TelemostManagerTest.MockedContextConfiguration.class)
public class TelemostManagerTest extends AbstractConfTest implements TelemostConstants {

    private static final String TELEMOST_LINK = "https://telemost.com/j/x";
    private static final String TELEMOST_BROADCAST_LINK = "https://telemost.com/j/y";
    private static final String LINK_MESSAGE = LINK_TO_CONFERENCE_WITHOUT_BROADCAST_PREFIX_RU + TELEMOST_LINK + "\n";
    private static final String LINK_BROADCAST_MESSAGE =
            LINK_TO_CONFERENCE_WITH_BROADCAST_PREFIX_RU + TELEMOST_LINK + "\n\n" +
                    LINK_TO_BROADCAST_PREFIX_RU + TELEMOST_BROADCAST_LINK + "\n";
    private static final String PROMISE_MESSAGE = LINK_PROMISE_MESSAGE_WITHOUT_BROADCAST_RU + "\n";
    private static final String PROMISE_BROADCAST_MESSAGE = LINK_PROMISE_MESSAGE_WITH_BROADCAST_RU + "\n";

    @Autowired
    private TelemostClient telemostClient;
    @Autowired
    private TelemostManager telemostManager;
    @Autowired
    private TelemostJobStorage jobStorage;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private TestManager testManager;

    @Before
    @Override
    public void setUpAll() {
        super.setUpAll();
        when(telemostClient.createConference(any())).thenReturn(TELEMOST_LINK);
        when(telemostClient.createBroadcast(any(), any())).thenReturn(TELEMOST_BROADCAST_LINK);
        clearInvocations(telemostClient);
    }

    @Test
    public void createSingleConferenceEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11401").getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceData());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(LINK_MESSAGE, created.getDescription());
        Assert.some(TELEMOST_LINK, getConferenceLink(created));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(created.getId()));
    }

    @Test
    public void createSingleConferenceWithBroadcastEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11401").getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostBroadcastEvent");
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(LINK_BROADCAST_MESSAGE, created.getDescription());
        Assert.some(TELEMOST_LINK, getConferenceLink(created));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(created));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(created.getId()));
    }

    @Test
    public void createRepeatingConferenceEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11402").getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(PROMISE_MESSAGE, created.getDescription());
        Assert.none(getConferenceLink(created));
        Assert.some(findGenerationJobScheduleTime(created.getId()));
    }

    @Test
    public void createRepeatingConferenceWithBroadcastEvent() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-11402").getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostBroadcastEvent");
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(PROMISE_BROADCAST_MESSAGE, created.getDescription());
        Assert.none(getConferenceLink(created));
        Assert.none(getBroadcastLink(created));
        Assert.some(findGenerationJobScheduleTime(created.getId()));
    }

    @Test
    public void updateSingleEventMakeConference() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11403");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostEvent");
        Event dataEvent = eventData.getEvent();
        dataEvent.setDescription("Source description");

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(dataEvent.getDescription(), created.getDescription());
        Assert.none(getConferenceLink(created));

        dataEvent.setDescription("Updated description");
        dataEvent.setData(createGeneratesConferenceData());

        eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo());
        Event updated = eventDbManager.getEventById(created.getId());

        Assert.equals(LINK_MESSAGE + "\n" + dataEvent.getDescription(), updated.getDescription());
        Assert.some(TELEMOST_LINK, getConferenceLink(updated));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void updateSingleEventMakeConferenceWithBroadcastBroadcast() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11403");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostBroadcastEvent");
        Event dataEvent = eventData.getEvent();
        dataEvent.setDescription("Source description");

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.equals(dataEvent.getDescription(), created.getDescription());
        Assert.none(getConferenceLink(created));
        Assert.none(getBroadcastLink(created));

        dataEvent.setDescription("Updated description");
        dataEvent.setData(createGeneratesConferenceWithBroadcastData());

        eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo());
        Event updated = eventDbManager.getEventById(created.getId());

        Assert.equals(LINK_BROADCAST_MESSAGE + "\n" + dataEvent.getDescription(), updated.getDescription());
        Assert.some(TELEMOST_LINK, getConferenceLink(updated));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(updated));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void updateSingleInstanceMakeConference() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11404");
        PassportUid uid = user.getUid();

        Event master = testManager.createEventWithDailyRepetition(uid);
        EventData eventData = createRecurrenceUpdateDataWithConference(uid, master, master.getStartTs());

        Option<Long> instanceId = eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo());
        Assert.some(instanceId);

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedInstance = eventDbManager.getEventById(instanceId.get());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.some(TELEMOST_LINK, getConferenceLink(updatedInstance));
        Assert.equals(LINK_MESSAGE, updatedInstance.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updatedInstance.getId()));
    }

    @Test
    public void updateSingleInstanceMakeConferenceBroadcast() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11404");
        PassportUid uid = user.getUid();

        Event master = testManager.createEventWithDailyRepetition(uid);
        EventData eventData = createRecurrenceUpdateDataWithConferenceWithBroadcast(uid, master, master.getStartTs());

        Option<Long> instanceId = eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo());
        Assert.some(instanceId);

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedInstance = eventDbManager.getEventById(instanceId.get());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.none(getBroadcastLink(updatedMaster));
        Assert.some(TELEMOST_LINK, getConferenceLink(updatedInstance));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(updatedInstance));
        Assert.equals(LINK_BROADCAST_MESSAGE, updatedInstance.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updatedInstance.getId()));
    }

    @Test
    public void updateRecurrenceInstanceMakeConference() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11405");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();
        Event recurrence = events.get2();

        EventData eventData = createRecurrenceUpdateDataWithConference(uid, recurrence, recurrence.getStartTs());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo()));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(recurrence.getId());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.some(TELEMOST_LINK, getConferenceLink(updatedRecurrence));
        Assert.equals(LINK_MESSAGE, updatedRecurrence.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updatedRecurrence.getId()));
    }

    @Test
    public void updateRecurrenceInstanceMakeConferenceWithBroadcast() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11405");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();
        Event recurrence = events.get2();

        EventData eventData = createRecurrenceUpdateDataWithConferenceWithBroadcast(uid, recurrence, recurrence.getStartTs());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, false, actionInfo()));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(recurrence.getId());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.none(getBroadcastLink(updatedMaster));
        Assert.some(TELEMOST_LINK, getConferenceLink(updatedRecurrence));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(updatedRecurrence));
        Assert.equals(LINK_BROADCAST_MESSAGE, updatedRecurrence.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updatedRecurrence.getId()));
    }

    @Test
    public void updateRepeatingEventWithRecurrenceMakeConference() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11406");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();
        Event recurrence = events.get2();

        EventData eventData = createRepeatingUpdateDataWithConference(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(recurrence.getId());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.equals(PROMISE_MESSAGE, updatedMaster.getDescription());
        Assert.some(updatedMaster.getStartTs(), findGenerationJobInstanceStart(updatedMaster.getId()));

        Assert.none(getConferenceLink(updatedRecurrence));
        Assert.equals(PROMISE_MESSAGE, updatedRecurrence.getDescription());
        Assert.some(updatedRecurrence.getStartTs(), findGenerationJobInstanceStart(updatedRecurrence.getId()));
    }

    @Test
    public void updateRepeatingEventWithRecurrenceMakeConferenceWithBroadcast() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11406");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();
        Event recurrence = events.get2();

        EventData eventData = createRepeatingUpdateDataWithConferenceWithBroadcast(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(recurrence.getId());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.equals(PROMISE_BROADCAST_MESSAGE, updatedMaster.getDescription());
        Assert.some(updatedMaster.getStartTs(), findGenerationJobInstanceStart(updatedMaster.getId()));

        Assert.none(getConferenceLink(updatedRecurrence));
        Assert.equals(PROMISE_BROADCAST_MESSAGE, updatedRecurrence.getDescription());
        Assert.some(updatedRecurrence.getStartTs(), findGenerationJobInstanceStart(updatedRecurrence.getId()));
    }

    @Test
    public void updateRepeatingEventWithTelemostRecurrenceMakeConference() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11407");
        PassportUid uid = user.getUid();

        Event master = testManager.createEventWithDailyRepetition(uid);
        Instant instanceStart = master.getStartTs().plus(Duration.standardDays(1));

        EventData recurrenceData = createRecurrenceUpdateDataWithConference(uid, master, instanceStart);
        Option<Long> instanceId = eventWebManager.update(user.getUserInfo(), recurrenceData, false, actionInfo());

        verify(telemostClient).createConference(any());
        clearInvocations(telemostClient);

        Event createdRecurrence = eventDbManager.getEventById(instanceId.get());

        Assert.some(TELEMOST_LINK, getConferenceLink(createdRecurrence));
        Assert.equals(LINK_MESSAGE, createdRecurrence.getDescription());

        EventData eventData = createRepeatingUpdateDataWithConference(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(instanceId.get());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.equals(PROMISE_MESSAGE, updatedMaster.getDescription());
        Assert.some(updatedMaster.getStartTs(), findGenerationJobInstanceStart(updatedMaster.getId()));

        verify(telemostClient, never()).createConference(any());

        Assert.equals(getConferenceLink(createdRecurrence), getConferenceLink(updatedRecurrence));
        Assert.equals(createdRecurrence.getDescription(), updatedRecurrence.getDescription());
        Assert.none(findGenerationJobInstanceStart(updatedRecurrence.getId()));
    }

    @Test
    public void updateRepeatingEventWithTelemostBroadcastRecurrenceMakeConferenceWithBroadcast() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11407");
        PassportUid uid = user.getUid();

        Event master = testManager.createEventWithDailyRepetition(uid);
        Instant instanceStart = master.getStartTs().plus(Duration.standardDays(1));

        EventData recurrenceData = createRecurrenceUpdateDataWithConferenceWithBroadcast(uid, master, instanceStart);
        Option<Long> instanceId = eventWebManager.update(user.getUserInfo(), recurrenceData, false, actionInfo());

        verify(telemostClient).createConference(any());
        clearInvocations(telemostClient);

        Event createdRecurrence = eventDbManager.getEventById(instanceId.get());

        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(createdRecurrence));
        Assert.equals(LINK_BROADCAST_MESSAGE, createdRecurrence.getDescription());

        EventData eventData = createRepeatingUpdateDataWithConferenceWithBroadcast(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event updatedMaster = eventDbManager.getEventById(master.getId());
        Event updatedRecurrence = eventDbManager.getEventById(instanceId.get());

        Assert.none(getConferenceLink(updatedMaster));
        Assert.none(getBroadcastLink(updatedMaster));
        Assert.equals(PROMISE_BROADCAST_MESSAGE, updatedMaster.getDescription());
        Assert.some(updatedMaster.getStartTs(), findGenerationJobInstanceStart(updatedMaster.getId()));

        verify(telemostClient, never()).createConference(any());

        Assert.equals(getConferenceLink(createdRecurrence), getConferenceLink(updatedRecurrence));
        Assert.equals(getBroadcastLink(createdRecurrence), getBroadcastLink(updatedRecurrence));
        Assert.equals(createdRecurrence.getDescription(), updatedRecurrence.getDescription());
        Assert.none(findGenerationJobInstanceStart(updatedRecurrence.getId()));
    }

    @Test
    public void updateRepeatingConferenceEventMakeSingle() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11408");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.none(getConferenceLink(created));
        Assert.equals(PROMISE_MESSAGE, created.getDescription());
        Assert.some(findGenerationJobScheduleTime(created.getId()));

        eventData.setRepetition(RepetitionRoutines.createNoneRepetition());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo()));

        Event updated = eventDbManager.getEventById(created.getId());

        Assert.some(TELEMOST_LINK, getConferenceLink(updated));
        Assert.equals(LINK_MESSAGE, updated.getDescription());
        Assert.none(findGenerationJobScheduleTime(updated.getId()));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void updateRepeatingConferenceWithBroadcastEventMakeSingle() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11408");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.none(getConferenceLink(created));
        Assert.none(getBroadcastLink(created));
        Assert.equals(PROMISE_BROADCAST_MESSAGE, created.getDescription());
        Assert.some(findGenerationJobScheduleTime(created.getId()));

        eventData.setRepetition(RepetitionRoutines.createNoneRepetition());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo()));

        Event updated = eventDbManager.getEventById(created.getId());

        Assert.some(TELEMOST_LINK, getConferenceLink(updated));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(updated));
        Assert.equals(LINK_BROADCAST_MESSAGE, updated.getDescription());
        Assert.none(findGenerationJobScheduleTime(updated.getId()));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void updateSingleConferenceEventMakeRepeating() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11409");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceData());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.some(TELEMOST_LINK, getConferenceLink(created));
        Assert.equals(LINK_MESSAGE, created.getDescription());
        Assert.none(findGenerationJobScheduleTime(created.getId()));

        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo()));

        Event updated = eventDbManager.getEventById(created.getId());

        Assert.none(getConferenceLink(updated));
        Assert.equals(PROMISE_MESSAGE, updated.getDescription());
        Assert.some(findGenerationJobScheduleTime(updated.getId()));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void updateSingleConferenceWithBroadcastEventMakeRepeating() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11409");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "singleTelemostEvent");
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());

        Event created = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo()).getEvent();

        Assert.some(TELEMOST_LINK, getConferenceLink(created));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(created));
        Assert.equals(LINK_BROADCAST_MESSAGE, created.getDescription());
        Assert.none(findGenerationJobScheduleTime(created.getId()));

        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo()));

        Event updated = eventDbManager.getEventById(created.getId());

        Assert.none(getConferenceLink(updated));
        Assert.none(getBroadcastLink(updated));
        Assert.equals(PROMISE_BROADCAST_MESSAGE, updated.getDescription());
        Assert.some(findGenerationJobScheduleTime(updated.getId()));

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(updated.getId()));
    }

    @Test
    public void generatePromisedLinkForRecurrenceInstance() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11410");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();

        EventData eventData = createRepeatingUpdateDataWithConference(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event recurrence = eventDbManager.getEventById(events.get2().getId());
        Assert.equals(PROMISE_MESSAGE, recurrence.getDescription());

        generateLinkOrReschedule(recurrence, findGenerationJobScheduleTime(recurrence.getId()).get());
        Event updated = eventDbManager.getEventById(recurrence.getId());

        Assert.isTrue(needGenerateConference(updated));
        Assert.some(TELEMOST_LINK, getConferenceLink(updated));
        Assert.equals(LINK_MESSAGE, updated.getDescription());

        Assert.none(findGenerationJobScheduleTime(recurrence.getId()));
        verify(telemostClient).linkCalendarEvent(any(), any(), eq(recurrence.getId()));
    }

    @Test
    public void generatePromisedBroadcastLinkForRecurrenceInstance() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11410");
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        Event master = events.get1();

        EventData eventData = createRepeatingUpdateDataWithConferenceWithBroadcast(uid, master);
        Assert.none(eventWebManager.update(user.getUserInfo(), eventData, true, actionInfo(master.getStartTs())));

        Event recurrence = eventDbManager.getEventById(events.get2().getId());
        Assert.equals(PROMISE_BROADCAST_MESSAGE, recurrence.getDescription());

        generateLinkOrReschedule(recurrence, findGenerationJobScheduleTime(recurrence.getId()).get());
        Event updated = eventDbManager.getEventById(recurrence.getId());

        Assert.isTrue(needGenerateBroadcast(updated));
        Assert.some(TELEMOST_LINK, getConferenceLink(updated));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(updated));
        Assert.equals(LINK_BROADCAST_MESSAGE, updated.getDescription());

        Assert.none(findGenerationJobScheduleTime(recurrence.getId()));
        verify(telemostClient).linkCalendarEvent(any(), any(), eq(recurrence.getId()));
    }

    @Test
    public void generatePromisedLinkForSingleInstance() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11411");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostEvent");
        Instant masterStart = eventData.getEvent().getStartTs();

        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());
        eventData.getRepetition().setDueTs(masterStart.plus(Duration.standardDays(3)));
        eventData.getEvent().setData(createGeneratesConferenceData());

        Event master = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo(masterStart)).getEvent();
        Option<Instant> scheduled = findGenerationJobScheduleTime(master.getId());

        Instant dayAfterScheduled = scheduled.get().plus(Duration.standardDays(1));
        Instant twoDaysAfterScheduled = dayAfterScheduled.plus(Duration.standardDays(1));

        generateLinkOrReschedule(master, dayAfterScheduled);
        Event recurrence = findRecurrences(master).single();

        Assert.isTrue(needGenerateConference(recurrence));
        Assert.some(TELEMOST_LINK, getConferenceLink(recurrence));
        Assert.equals(LINK_MESSAGE, recurrence.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(recurrence.getId()));

        Assert.none(findGenerationJobScheduleTime(recurrence.getId()));
        Assert.some(twoDaysAfterScheduled, findGenerationJobScheduleTime(master.getId()));

        generateLinkOrReschedule(master, twoDaysAfterScheduled);
        Assert.equals(
                Cf.list(dayAfterScheduled, twoDaysAfterScheduled).map(this::scheduleTimeToInstanceStart),
                findRecurrences(master).filterMap(Event::getRecurrenceId)
        );
        Assert.none(findGenerationJobScheduleTime(master.getId()));
    }

    @Test
    public void generatePromisedLinkBroadcastForSingleInstance() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-11411");
        PassportUid uid = user.getUid();

        EventData eventData = testManager.createDefaultEventData(uid, "repeatingTelemostBroadcastEvent");
        Instant masterStart = eventData.getEvent().getStartTs();

        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());
        eventData.getRepetition().setDueTs(masterStart.plus(Duration.standardDays(3)));
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());

        Event master = eventWebManager.createUserEvent(uid, eventData, SAVE_ONLY, actionInfo(masterStart)).getEvent();
        Option<Instant> scheduled = findGenerationJobScheduleTime(master.getId());

        Instant dayAfterScheduled = scheduled.get().plus(Duration.standardDays(1));
        Instant twoDaysAfterScheduled = dayAfterScheduled.plus(Duration.standardDays(1));

        generateLinkOrReschedule(master, dayAfterScheduled);
        Event recurrence = findRecurrences(master).single();

        Assert.isTrue(needGenerateBroadcast(recurrence));
        Assert.some(TELEMOST_LINK, getConferenceLink(recurrence));
        Assert.some(TELEMOST_BROADCAST_LINK, getBroadcastLink(recurrence));
        Assert.equals(LINK_BROADCAST_MESSAGE, recurrence.getDescription());

        verify(telemostClient).linkCalendarEvent(any(), any(), eq(recurrence.getId()));

        Assert.none(findGenerationJobScheduleTime(recurrence.getId()));
        Assert.some(twoDaysAfterScheduled, findGenerationJobScheduleTime(master.getId()));

        generateLinkOrReschedule(master, twoDaysAfterScheduled);
        Assert.equals(
                Cf.list(dayAfterScheduled, twoDaysAfterScheduled).map(this::scheduleTimeToInstanceStart),
                findRecurrences(master).filterMap(Event::getRecurrenceId)
        );
        Assert.none(findGenerationJobScheduleTime(master.getId()));
    }

    private void generateLinkOrReschedule(Event event, Instant now) {
        telemostManager.cancelScheduledGeneration(event); // Task has ActiveUidDropType.WHEN_RUNNING so fair enough
        telemostManager.generateLinkOrReschedule(event.getId(), actionInfo(now));
    }

    private EventData createRepeatingUpdateDataWithConference(PassportUid uid, Event event) {
        EventData eventData = testManager.createDefaultEventData(uid, event.getName());

        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(event.getStartTs());
        eventData.getEvent().setEndTs(event.getEndTs());
        eventData.getEvent().setData(createGeneratesConferenceData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        return eventData;
    }

    private EventData createRepeatingUpdateDataWithConferenceWithBroadcast(PassportUid uid, Event event) {
        EventData eventData = testManager.createDefaultEventData(uid, event.getName());

        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(event.getStartTs());
        eventData.getEvent().setEndTs(event.getEndTs());
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        return eventData;
    }

    private EventData createRecurrenceUpdateDataWithConference(PassportUid uid, Event source, Instant instanceStart) {
        EventData eventData = testManager.createDefaultEventData(uid, source.getName(), instanceStart);

        eventData.setInstanceStartTs(instanceStart);
        eventData.getEvent().setId(source.getId());
        eventData.getEvent().setData(createGeneratesConferenceData());

        return eventData;
    }

    private EventData createRecurrenceUpdateDataWithConferenceWithBroadcast(PassportUid uid, Event source, Instant instanceStart) {
        EventData eventData = testManager.createDefaultEventData(uid, source.getName(), instanceStart);

        eventData.setInstanceStartTs(instanceStart);
        eventData.getEvent().setId(source.getId());
        eventData.getEvent().setData(createGeneratesConferenceWithBroadcastData());

        return eventData;
    }

    private ListF<Event> findRecurrences(Event master) {
        return eventDbManager.getEventsByMainEventIdForUpdate(master.getMainEventId())
                .filterNot(e -> e.getId().equals(master.getId()));
    }

    private Option<Instant> findGenerationJobScheduleTime(long eventId) {
        return jobStorage.findActiveJob(eventId).map(OnetimeJob::getScheduleTime);
    }

    private Option<Instant> findGenerationJobInstanceStart(long eventId) {
        return findGenerationJobScheduleTime(eventId).map(this::scheduleTimeToInstanceStart);
    }

    private Instant scheduleTimeToInstanceStart(Instant scheduleTime) {
        return scheduleTime.plus(Duration.standardHours(GENERATE_HOURS_BEFORE_START));
    }

    private static RawJson createGeneratesConferenceData() {
        JsonNode json = new ObjectNode(null, Cf.map(FIELD_CONFERENCE_GENERATES, BooleanNode.TRUE));
        return new RawJson(json.toString());
    }

    private static RawJson createGeneratesConferenceWithBroadcastData() {
        JsonNode json = new ObjectNode(
                null,
                Cf.map(FIELD_CONFERENCE_GENERATES, BooleanNode.TRUE, FIELD_BROADCAST_GENERATES, BooleanNode.TRUE)
        );
        return new RawJson(json.toString());
    }

    private static ActionInfo actionInfo() {
        return withTvmId(ActionInfo.webTest());
    }

    private static ActionInfo actionInfo(ReadableInstant now) {
        return withTvmId(ActionInfo.webTest(now));
    }

    private static ActionInfo withTvmId(ActionInfo actionInfo) {
        return actionInfo.withTvmId(OptionalLong.of(2002194));
    }

    @ContextConfiguration
    @Import(TestBaseContextConfiguration.class)
    public static class MockedContextConfiguration {
        @Bean
        public TelemostClient telemostClient() {
            return mock(TelemostClient.class);
        }
    }
}
