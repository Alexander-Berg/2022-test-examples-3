package ru.yandex.calendar.frontend.api.mail;

import java.util.List;
import java.util.Optional;

import lombok.val;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarRequest;
import ru.yandex.calendar.CalendarRequestHandle;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.EventUserWithNotifications;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.log.reqid.RequestIdStack;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = MailEventManagerContextConfiguration.class)
public class MailEventManagerTest extends AbstractConfTest {
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private MailEventManager mailEventManager;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private TestManager testManager;

    private CalendarRequestHandle handle;
    private TestUserInfo testUser;
    private PassportUid testUid;
    private TestUserInfo anotherTestUser;
    private PassportUid anotherTestUid;

    private static final Instant NOW = TestDateTimes.moscow(2018, 6, 20, 14, 38);
    private static final ActionInfo ACTION_INFO = new ActionInfo(ActionSource.UNKNOWN, "test-rid", NOW);
    private static final Instant OLD_INSTANT = TestDateTimes.moscow(2000, 1, 1, 0, 0);
    private static final ActionInfo OLD_ACTION_INFO = new ActionInfo(ActionSource.UNKNOWN, "test-rid", OLD_INSTANT);
    private static final String ACCEPT_BUTTON_TEXT = "accept";
    private static final String DECLINE_BUTTON_TEXT = "decline";

    @Before
    public void setUp() {
        handle = CalendarRequest.push(ActionSource.MAIL, "test");
        testUser = testManager.prepareUser("test-user");
        testUid = testUser.getUid();
        anotherTestUser = testManager.prepareUser("test-user2");
        anotherTestUid = anotherTestUser.getUid();
    }

    private EventByIcsUrlFetcher wrapSafe(EventByIcsUrlFetcherUnsafe unsafe) {
        return (uid, calendar, language, actionInfo) -> {
            try {
                return MailEventInfoOrRefusal.info(unsafe.fetchEvent(uid, calendar, language, actionInfo));
            } catch (MailEventException e) {
                return MailEventInfoOrRefusal.refusal(e.getMessage());
            }
        };
    }

    @FunctionalInterface
    public interface EventByIcsUrlFetcher {
        MailEventInfoOrRefusal fetchEvent(PassportUid uid, IcsCalendar calendar, Optional<Language> language, ActionInfo actionInfo);
    }

    @FunctionalInterface
    public interface EventByIcsUrlFetcherUnsafe {
        MailEventInfo fetchEvent(PassportUid uid, IcsCalendar calendar, Optional<Language> language, ActionInfo actionInfo);
    }

    @After
    public void tearDown() {
        handle.popSafely();
    }


    private void noEventsCase(EventByIcsUrlFetcher eventFetcher) {
        val calendar = new IcsCalendar();
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);
        validateRefusalResponse(result, "no-events");
    }

    @Test
    public void importEventByIcsUrlNoEvents() {
        noEventsCase(mailEventManager::importEventByIcsUrl);
    }

    @Test
    public void getEventInfoByIcsUrlNoEvents() {
        noEventsCase(mailEventManager::getEventInfoByIcsUrl);
    }

    @Test
    public void parseByIcsUrlNoEvents() {
        noEventsCase(wrapSafe(mailEventManager::parseEvent));
    }

    @Test
    public void loadByIcsUrlNoEvents() {
        noEventsCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase));
    }

    public void unsupportedMethodCase(EventByIcsUrlFetcher eventFetcher) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId).addProperty(IcsMethod.REFRESH);

        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);
        validateRefusalResponse(result, "unsupported-ics-method");
    }

    @Test
    public void importEventByIcsUrlUnsupportedMethod() {
        unsupportedMethodCase(mailEventManager::importEventByIcsUrl);
    }

    @Test
    public void getEventInfoByIcsUrlUnsupportedMethod() {
        unsupportedMethodCase(mailEventManager::getEventInfoByIcsUrl);
    }

    @Test
    public void parseByIcsUrlUnsupportedMethod() {
        unsupportedMethodCase(wrapSafe(mailEventManager::parseEvent));
    }

    @Test
    public void loadByIcsUrlUnsupportedMethod() {
        unsupportedMethodCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase));
    }

    public void undecidedCase(EventByIcsUrlFetcher eventFetcher, boolean hasEventUrl) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        if (!hasEventUrl) {
            event.unsetField(EventFields.ID);
        }

        validateEventResponse(event, externalId, result,
                List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT), false, false, Optional.of(MailDecision.UNDECIDED), hasEventUrl);
    }

    @Test
    public void importEventByIcsUrlUndecided() {
        undecidedCase(mailEventManager::importEventByIcsUrl, true);
    }

    @Test
    public void getEventInfoByIcsUrlUndecided() {
        undecidedCase(mailEventManager::getEventInfoByIcsUrl, true);
    }

    @Test
    public void parseByIcsUrlUndecided() {
        undecidedCase(wrapSafe(mailEventManager::parseEvent), false);
    }

    @Test
    public void loadByIcsUrlUndecided() {
        undecidedCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true);
    }

    public void acceptedCase(EventByIcsUrlFetcher eventFetcher, boolean checkEventUrl, MailDecision mailDecision, List<String> buttons) {
        val event = prepareEventForIcsImport(Decision.YES);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        validateEventResponse(event, externalId, result, buttons, false, false, Optional.of(mailDecision), checkEventUrl);
    }

    @Test
    public void importEventByIcsUrlAccepted() {
        acceptedCase(mailEventManager::importEventByIcsUrl, true, MailDecision.ACCEPTED, List.of(DECLINE_BUTTON_TEXT));
    }

    @Test
    public void getEventInfoByIcsUrlAccepted() {
        acceptedCase(mailEventManager::getEventInfoByIcsUrl, true, MailDecision.ACCEPTED, List.of(DECLINE_BUTTON_TEXT));
    }

    @Test
    public void parseByIcsUrlAccepted() {
        acceptedCase(wrapSafe(mailEventManager::parseEvent), false, MailDecision.UNDECIDED, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void loadByIcsUrlAccepted() {
        acceptedCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true, MailDecision.ACCEPTED, List.of(DECLINE_BUTTON_TEXT));
    }

    public void declinedCase(EventByIcsUrlFetcher eventFetcher, boolean checkEventUrl, MailDecision mailDecision, List<String> buttons) {
        val event = prepareEventForIcsImport(Decision.NO);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        validateEventResponse(event, externalId, result, buttons, false, false, Optional.of(mailDecision), checkEventUrl);
    }

    @Test
    public void importEventByIcsUrlDeclined() {
        declinedCase(mailEventManager::importEventByIcsUrl, true, MailDecision.DECLINED, List.of(ACCEPT_BUTTON_TEXT));
    }

    @Test
    public void getEventInfoByIcsUrlDeclined() {
        declinedCase(mailEventManager::getEventInfoByIcsUrl, true, MailDecision.DECLINED, List.of(ACCEPT_BUTTON_TEXT));
    }

    @Test
    public void parseByIcsUrlDeclined() {
        declinedCase(wrapSafe(mailEventManager::parseEvent), false, MailDecision.UNDECIDED, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void loadByIcsUrlDeclined() {
        declinedCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true, MailDecision.DECLINED, List.of(ACCEPT_BUTTON_TEXT));
    }

    public void organizerCase(EventByIcsUrlFetcher eventFetcher, boolean checkEventUrl) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(testUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        validateEventResponse(event, externalId, result, emptyList(), false, false, Optional.of(MailDecision.UNDECIDED), checkEventUrl);
    }

    @Test
    public void importEventByIcsUrlOrganizer() {
        organizerCase(mailEventManager::importEventByIcsUrl, true);
    }

    @Test
    public void getEventInfoByIcsUrlOrganizer() {
        organizerCase(mailEventManager::getEventInfoByIcsUrl, true);
    }

    @Test
    public void parseByIcsUrlOrganizer() {
        organizerCase(wrapSafe(mailEventManager::parseEvent), false);
    }

    @Test
    public void loadByIcsUrlOrganizer() {
        organizerCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true);
    }

    public void pastCase(EventByIcsUrlFetcher eventFetcher, boolean checkEventUrl) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), ACTION_INFO);

        validateEventResponse(event, externalId, result, emptyList(), false, true, Optional.of(MailDecision.UNDECIDED), checkEventUrl);
    }

    @Test
    public void importEventByIcsUrlPast() {
        pastCase(mailEventManager::importEventByIcsUrl, true);
    }

    @Test
    public void getEventInfoByIcsUrlPast() {
        pastCase(mailEventManager::getEventInfoByIcsUrl, true);
    }

    @Test
    public void parseByIcsUrlPast() {
        pastCase(wrapSafe(mailEventManager::parseEvent), false);
    }

    @Test
    public void loadByIcsUrlPast() {
        pastCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true);
    }

    public void canceledCase(EventByIcsUrlFetcher eventFetcher, boolean canceled, List<String> buttons) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        eventRoutines.deleteEvent(Option.of(testUser.getUserInfo()), event.getId(), InvitationProcessingMode.SAVE_ATTACH, ACTION_INFO);

        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        validateEventResponse(event, externalId, result, buttons, canceled, false, Optional.of(MailDecision.UNDECIDED), false);
    }

    @Test
    public void importEventByIcsUrlCanceled() {
        canceledCase(mailEventManager::importEventByIcsUrl, true, emptyList());
    }

    @Test
    public void getEventInfoByIcsUrlCanceled() {
        canceledCase(mailEventManager::getEventInfoByIcsUrl, true, emptyList());
    }

    @Test
    public void parseByIcsUrlCanceled() {
        canceledCase(wrapSafe(mailEventManager::parseEvent), false, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void loadByIcsUrlCanceled() {
        canceledCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true, emptyList());
    }

    public void withoutTheUserCase(EventByIcsUrlFetcher eventFetcher, boolean eventResponse, boolean checkEventUrl, List<String> buttons) {
        val event = prepareEventForIcsImport();
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        if (eventResponse) {
            validateEventResponse(event, externalId, result, buttons, false, false, Optional.empty(), checkEventUrl);
        } else {
            validateRefusalResponse(result, "Event not found");
        }
    }

    @Test
    public void importEventByIcsUrlWithoutTheUser() {
        withoutTheUserCase(mailEventManager::importEventByIcsUrl, true, true, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void getEventInfoByIcsUrlWithoutTheUser() {
        withoutTheUserCase(mailEventManager::getEventInfoByIcsUrl, true, false, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void parseByIcsUrlWithoutTheUser() {
        withoutTheUserCase(wrapSafe(mailEventManager::parseEvent), true, false, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT));
    }

    @Test
    public void loadByIcsUrlWithoutTheUser() {
        withoutTheUserCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), false, false, emptyList());
    }

    public void notExistsCase(EventByIcsUrlFetcher eventFetcher, boolean eventResponse) {
        val event = new Event();
        event.setName("Event name");
        event.setStartTs(NOW);
        event.setEndTs(NOW);
        event.setCreatorUid(testUid);
        event.setIsAllDay(false);

        val externalId = "AnImpossibleExternalEventId";

        val calendar = getIcsCalendar(event, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        if (eventResponse) {
            validateEventResponse(event, externalId, result, List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT),
                    false, false, Optional.of(MailDecision.UNDECIDED), false);
        } else {
            validateRefusalResponse(result, "Event not found");
        }
    }


    @Test
    public void importEventByIcsUrlNotExists() {
        notExistsCase(mailEventManager::importEventByIcsUrl, true);
    }

    @Test
    public void getEventInfoByIcsNotExists() {
        notExistsCase(mailEventManager::getEventInfoByIcsUrl, true);
    }

    @Test
    public void parseByIcsUrlNotExists() {
        notExistsCase(wrapSafe(mailEventManager::parseEvent), true);
    }

    @Test
    public void loadByIcsUrlNotExists() {
        notExistsCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), false);
    }

    public void notActualCase(EventByIcsUrlFetcher eventFetcher, boolean checkEventUrl, boolean returnsNewEvent) {
        val event = prepareEventForIcsImport(Decision.UNDECIDED);
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();

        val newEvent = event.clone();
        newEvent.setName("New name");
        newEvent.setEndTs(event.getEndTs().plus(Duration.standardHours(2)));
        val calendar = getIcsCalendar(newEvent, externalId);
        val result = eventFetcher.fetchEvent(anotherTestUid, calendar, Optional.empty(), OLD_ACTION_INFO);

        validateEventResponse(returnsNewEvent ? newEvent : event, externalId, result,
                List.of(ACCEPT_BUTTON_TEXT, DECLINE_BUTTON_TEXT), false, false, Optional.of(MailDecision.UNDECIDED), checkEventUrl);
    }

    @Test
    public void importEventByIcsUrlNotActual() {
        notActualCase(mailEventManager::importEventByIcsUrl, true, false);
    }

    @Test
    public void getEventInfoByIcsNotActual() {
        notActualCase(mailEventManager::getEventInfoByIcsUrl, true, false);
    }

    @Test
    public void parseByIcsUrlNotActual() {
        notActualCase(wrapSafe(mailEventManager::parseEvent), false, true);
    }

    @Test
    public void loadByIcsUrlNotActual() {
        notActualCase(wrapSafe(mailEventManager::loadEventInfoByIcsUrlFromDataBase), true, false);
    }

    private void validateRefusalResponse(MailEventInfoOrRefusal refusal, String refusalText) {
        assertThat(refusal).isInstanceOf(MailEventInfoOrRefusal.Refusal.class);
        val reason = ((MailEventInfoOrRefusal.Refusal) refusal).getReason();
        assertThat(reason).isEqualTo(refusalText);
    }

    private void validateEventResponse(Event event, String externalId, MailEventInfoOrRefusal result, List<String> actions,
                                       boolean canceled, boolean past, Optional<MailDecision> mailDecisionO, boolean checkEventUrl) {
        val info = ((MailEventInfoOrRefusal.Info) result).getInfo();

        assertThat(info.getExternalId()).isEqualTo(externalId);
        assertThat(info.getName()).isEqualTo(event.getName());
        assertThat(info.getCalendarMailType().toOptional()).isEmpty();
        assertThat(info.getStart()).isEqualTo(event.getStartTs());
        assertThat(info.getEnd()).isEqualTo(event.getEndTs());
        assertThat(info.getBaseInfo().getParticipants().getOrganizer().toOptional())
                .usingFieldByFieldValueComparator()
                .hasValue(new MailParticipantInfo.YandexUser("", testUser.getEmail(), MailDecision.ACCEPTED, testUser.getLoginRaw()));
        if (checkEventUrl) {
            assertThat(info.getCalendarUrl()).isEqualTo(Option.of("http://calendar.common.yandex.ru/event?event_id=" + event.getId()));
        }

        mailDecisionO.ifPresent(mailDecision -> {
            val attendees = info.getBaseInfo().getParticipants().getAttendees();
            assertThat(attendees).hasSize(1);
            assertThat(attendees.getO(0).toOptional())
                    .usingFieldByFieldValueComparator()
                    .hasValue(new MailParticipantInfo.YandexUser("", anotherTestUser.getEmail(), mailDecision, anotherTestUser.getLoginRaw()));
        });
        assertThat(info.getActions().toArray()).containsExactlyInAnyOrderElementsOf(actions);
        assertThat(info.isCancelled()).isEqualTo(canceled);
        assertThat(info.isPast()).isEqualTo(past);
    }

    private Event prepareEventForIcsImport(Decision decision) {
        return prepareEventForIcsImport(Optional.of(decision));
    }

    private Event prepareEventForIcsImport() {
        return prepareEventForIcsImport(Optional.empty());
    }

    private Event prepareEventForIcsImport(Optional<Decision> decisionOpt) {
        val event = testManager.createDefaultEvent(TestManager.UID, "Event name");
        testManager.createEventUser(testUid, event.getId(), Decision.YES, Option.of(true));
        decisionOpt.ifPresent(decision -> testManager.createEventUser(anotherTestUid, event.getId(), decision, Option.of(false)));
        return event;
    }

    private IcsCalendar getIcsCalendar(Event event, String externalId) {
        val icsEvent = new IcsVEvent()
                .withUid(externalId)
                .withSummary(event.getName())
                .withDtStart(event.getStartTs())
                .withDtEnd(event.getEndTs())
                .withOrganizer(testUser.getEmail())
                .addAttendee(anotherTestUser.getEmail());
        return new IcsCalendar().addComponent(icsEvent);
    }

    @Test
    public void getEventInfoForNewEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10015");

        TestEventData conflictingEvent = TestEventData.builder()
                .setName("conflicting event").setLocation("somewhere")
                .setStartUtc(2012, 12, 3, 13, 0).setEndUtc(2012, 12, 3, 13, 30)
                .setDecision(Decision.YES)
                .build();

        createEvent(user, conflictingEvent);

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0001")
                .setName("event from mail").setLocation("somewhere else")
                .setStartUtc(2012, 12, 3, 12, 0).setEndUtc(2012, 12, 3, 15, 0)
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .build();

        MailEventInfo eventInfo = mailEventManager.getEventInfo(eventData, actionInfo());

        MailEventInfo.Conflict expectedConflict = MailEventInfo.Conflict.builder()
                .setName(conflictingEvent.name).setLocation(conflictingEvent.location)
                .setStart(conflictingEvent.start).setEnd(conflictingEvent.end)
                .build();

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(eventData.getExternalId())
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStart(eventData.getStart()).setEnd(eventData.getEnd()).setAllDay(false)
                .setDecision(MailDecision.UNDECIDED)
                .setConflict(expectedConflict)
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void getEventInfoForNewAllDayEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10016");

        TestEventData ignoredConflict = TestEventData.builder()
                .setName("conflicting event").setLocation("somewhere")
                .setStartUtc(2012, 12, 5, 11, 0).setEndUtc(2012, 12, 5, 11, 30)
                .setDecision(Decision.YES)
                .build();

        createEvent(user, ignoredConflict);

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0002")
                .setName("all day event from mail")
                .setStartUtc(2012, 12, 5, 0, 0).setEndUtc(2012, 12, 6, 0, 0)
                .setAllDay(true).setStartClientTimezoneOffset(0)
                .setLocation("somewhere else")
                .build();

        MailEventInfo eventInfo = mailEventManager.getEventInfo(eventData, actionInfo());

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(eventData.getExternalId())
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStartUtc(2012, 12, 5, 0, 0).setEndUtc(2012, 12, 6, 0, 0).setAllDay(true)
                .setDecision(MailDecision.UNDECIDED)
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void getEventInfoForExistingAcceptedEvent() {
        boolean shouldOmitConflict = true;
        boolean shouldOmitNotification = false;

        getEventInfoForExistingEvent(Decision.YES, shouldOmitConflict, shouldOmitNotification);
    }

    @Test
    public void getEventInfoForExistingDeclinedEvent() {
        boolean shouldOmitConflict = true;
        boolean shouldOmitNotification = true;

        getEventInfoForExistingEvent(Decision.NO, shouldOmitConflict, shouldOmitNotification);
    }

    @Test
    public void getEventInfoForExistingUndecidedEvent() {
        boolean shouldOmitConflict = false;
        boolean shouldOmitNotification = true;

        getEventInfoForExistingEvent(Decision.UNDECIDED, shouldOmitConflict, shouldOmitNotification);
    }

    private void getEventInfoForExistingEvent(
            Decision decision, boolean shouldOmitConflict, boolean shouldOmitNotification)
    {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10018");

        TestEventData existingEvent = TestEventData.builder()
                .setExternalId("msg0003")
                .setName("event from mail").setLocation("somewhere else")
                .setStartUtc(2012, 12, 4, 11, 15).setEndUtc(2012, 12, 4, 11, 45)
                .setDecision(decision)
                .addNotification(Notification.sms(Duration.standardMinutes(-15)))
                .build();

        createEvent(user, existingEvent);

        TestEventData conflictingEvent = TestEventData.builder()
                .setName("conflicting event").setLocation("over there")
                .setStartUtc(2012, 12, 4, 11, 0).setEndUtc(2012, 12, 4, 13, 0)
                .setDecision(Decision.YES)
                .build();

        createEvent(user, conflictingEvent);

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId(existingEvent.externalId.get())
                .setName("event from mail").setLocation("somewhere")
                .setStartUtc(2012, 12, 4, 12, 30).setEndUtc(2012, 12, 4, 13, 0)
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .build();

        MailEventInfo eventInfo = mailEventManager.getEventInfo(eventData, actionInfo());

        MailEventInfo.Conflict possiblyExpectedConflict = MailEventInfo.Conflict.builder()
                .setName(conflictingEvent.name).setLocation(conflictingEvent.location)
                .setStart(conflictingEvent.start).setEnd(conflictingEvent.end)
                .build();

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(existingEvent.externalId.get())
                .setName(existingEvent.name).setLocation(existingEvent.location)

                .setStart(existingEvent.start).setEnd(existingEvent.end).setAllDay(false)
                .setDecision(MailDecision.fromDecision(existingEvent.decision))

                .setConflict(Option.when(!shouldOmitConflict, possiblyExpectedConflict))
                .setNotification(Option.when(!shouldOmitNotification, existingEvent.notifications.single()))

                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void acceptNewEventAndSetSmsNotification() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10020");

        TestEventData ignoredConflict = TestEventData.builder()
                .setName("conflicting event").setLocation("over there")
                .setStartUtc(2012, 12, 6, 9, 30).setEndUtc(2012, 12, 6, 11, 30)
                .setDecision(Decision.YES)
                .build();

        createEvent(user, ignoredConflict);

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0006")
                .setName("event from mail").setLocation("somewhere")
                .setStartUtc(2012, 12, 6, 9, 15).setEndUtc(2012, 12, 6, 9, 45)
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .setDecision(MailDecision.ACCEPTED)
                .setNotification(Notification.sms(Duration.standardMinutes(-5)))
                .build();

        MailEventInfo eventInfo = mailEventManager.createOrUpdateEvent(eventData, actionInfo());

        TestEventData expectedEvent = TestEventData.builder()
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStart(eventData.getStart()).setEnd(eventData.getEnd())
                .setDecision(Decision.YES)
                .addNotification(eventData.getNotification().get())
                .build();

        checkEvent(user, eventData.getExternalId(), expectedEvent);

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(eventData.getExternalId())
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStart(eventData.getStart()).setEnd(eventData.getEnd()).setAllDay(eventData.isAllDay())
                .setDecision(eventData.getDecision().get())
                .setNotification(eventData.getNotification().get())
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void declineNewEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10021");

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0007")
                .setName("event from mail").setLocation("somewhere")
                .setStartUtc(2012, 12, 6, 9, 30).setEndUtc(2012, 12, 6, 10, 30)
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .setDecision(MailDecision.DECLINED)
                .setNotification(Notification.sms(Duration.standardMinutes(-5)))
                .build();

        MailEventInfo eventInfo = mailEventManager.createOrUpdateEvent(eventData, actionInfo());

        TestEventData expectedEvent = TestEventData.builder()
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStart(eventData.getStart()).setEnd(eventData.getEnd())
                .setDecision(Decision.NO)
                .addNotification(eventData.getNotification().get())
                .build();

        checkEvent(user, "msg0007", expectedEvent);

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(eventData.getExternalId())
                .setName(eventData.getName()).setLocation(eventData.getLocation())
                .setStart(eventData.getStart()).setEnd(eventData.getEnd()).setAllDay(eventData.isAllDay())
                .setDecision(eventData.getDecision().get())
                // no notification
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void declineEventAcceptedBefore() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10022");

        MailEventData accepted = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0008")
                .setName("event from mail").setLocation("somewhere")
                .setStartUtc(2012, 12, 6, 0, 0).setEndUtc(2012, 12, 7, 0, 0)
                .setAllDay(true).setStartClientTimezoneOffset(0)
                .setDecision(MailDecision.ACCEPTED)
                .setNotification(Notification.sms(Duration.standardHours(-1)))
                .build();

        mailEventManager.createOrUpdateEvent(accepted, actionInfo());

        MailEventData declined = accepted.toBuilder()
                .setDecision(MailDecision.DECLINED)
                .clearNotification()
                .build();

        MailEventInfo eventInfo = mailEventManager.createOrUpdateEvent(declined, actionInfo());

        TestEventData expectedEvent = TestEventData.builder()
                .setName(declined.getName()).setLocation(declined.getLocation())
                .setStart(declined.getStart()).setEnd(declined.getEnd())
                .setDecision(Decision.NO)
                .build();

        checkEvent(user, "msg0008", expectedEvent);

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(declined.getExternalId())
                .setName(declined.getName()).setLocation(declined.getLocation())
                .setStart(declined.getStart()).setEnd(declined.getEnd()).setAllDay(declined.isAllDay())
                .setDecision(declined.getDecision().get())
                // no notification
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void updateDecision() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(13);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "Name");
        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee, Decision.UNDECIDED, false);

        String externalId = mainEventDao.findMainEventByEventId(event.getId()).getExternalId();

        Assert.none(mailEventManager.updateDecision(attendee.getUid(),
                externalId, Option.empty(), MailDecision.ACCEPTED, ActionInfo.webTest()).getRefusalReason());

        Assert.some(Decision.YES, eventUserRoutines.findEventUserDecision(attendee.getUid(), event.getId()));

        Assert.none(mailEventManager.updateDecision(attendee.getUid(),
                externalId, Option.empty(), MailDecision.DECLINED, ActionInfo.webTest()).getRefusalReason());

        Assert.some(Decision.NO, eventUserRoutines.findEventUserDecision(attendee.getUid(), event.getId()));

        Assert.some("already-decided", mailEventManager.updateDecision(attendee.getUid(),
                externalId, Option.empty(), MailDecision.DECLINED, ActionInfo.webTest()).getRefusalReason());

        Assert.some("event-not-found", mailEventManager.updateDecision(attendee.getUid(),
                "X", Option.empty(), MailDecision.DECLINED, ActionInfo.webTest()).getRefusalReason());
    }

    @Test
    public void acceptingSecondTimeUpdatesEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10023");

        MailEventData firstVersion = MailEventData.builder()
                .setUid(user.getUid()).setExternalId("msg0009")
                .setName("first version").setLocation("somewhere")
                .setStartUtc(2012, 12, 6, 14, 0).setEndUtc(2012, 12, 6, 15, 0)
                .setAllDay(false).setStartClientTimezoneOffset(0)

                .setDecision(MailDecision.ACCEPTED)
                .setNotification(Notification.sms(Duration.standardMinutes(-30)))
                .build();

        mailEventManager.createOrUpdateEvent(firstVersion, actionInfo());

        MailEventData secondVersion = firstVersion.toBuilder()
                .setName("second version").setLocation("somewhere else")
                .setStart(firstVersion.getStart().plus(360000)).setEnd(firstVersion.getEnd().plus(360000))
                .setNotification(Notification.sms(Duration.standardMinutes(-15)))
                .build();

        MailEventInfo eventInfo = mailEventManager.createOrUpdateEvent(secondVersion, actionInfo());

        TestEventData expectedEvent = TestEventData.builder()
                .setName(secondVersion.getName()).setLocation(secondVersion.getLocation())
                .setStart(secondVersion.getStart()).setEnd(secondVersion.getEnd())
                .setDecision(Decision.YES)
                .addNotification(secondVersion.getNotification().get())
                .build();

        checkEvent(user, "msg0009", expectedEvent);

        MailEventInfo expectedEventInfo = MailEventInfo.builder()
                .setExternalId(secondVersion.getExternalId())
                .setName(secondVersion.getName()).setLocation(secondVersion.getLocation())
                .setStart(secondVersion.getStart()).setEnd(secondVersion.getEnd()).setAllDay(secondVersion.isAllDay())
                .setDecision(MailDecision.ACCEPTED)
                .setNotification(secondVersion.getNotification().get())
                .build();

        assertEventInfoEquals(expectedEventInfo, eventInfo);
    }

    @Test
    public void updatingSmsOrEmailPreservesOtherNotificationChannels() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10024");

        TestEventData existingEvent = TestEventData.builder()
                .setExternalId("msg0010")
                .setName("event from mail").setLocation("somewhere")
                .setStartUtc(2012, 12, 6, 14, 0).setEndUtc(2012, 12, 6, 15, 0)

                .setLocation("somewhere")
                .setDecision(Decision.YES)
                .addNotification(Notification.display(Duration.standardMinutes(-1)))
                .addNotification(Notification.sms(Duration.standardMinutes(-15)))
                .addNotification(Notification.email(Duration.standardMinutes(-5)))
                .build();

        createEvent(user, existingEvent);

        MailEventData eventData = MailEventData.builder()
                .setUid(user.getUid()).setExternalId(existingEvent.externalId.get())
                .setName(existingEvent.name).setLocation(existingEvent.location)
                .setStart(existingEvent.start).setEnd(existingEvent.end)
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .setDecision(MailDecision.ACCEPTED)
                .setNotification(Notification.sms(Duration.standardHours(-3)))
                .build();

        mailEventManager.createOrUpdateEvent(eventData, actionInfo());

        TestEventData expectedEvent = existingEvent.toBuilder()
                .clearNotifications()
                .addNotification(Notification.display(Duration.standardMinutes(-1)))
                .addNotification(Notification.sms(Duration.standardHours(-3)))
                .build();

        checkEvent(user, "msg0010", expectedEvent);

        eventData = eventData.toBuilder().setNotification(Notification.email(Duration.standardHours(-3))).build();

        mailEventManager.createOrUpdateEvent(eventData, actionInfo());

        expectedEvent = expectedEvent.toBuilder()
                .clearNotifications()
                .addNotification(Notification.email(Duration.standardHours(-3)))
                .addNotification(Notification.display(Duration.standardMinutes(-1)))
                .build();

        checkEvent(user, "msg0010", expectedEvent);
    }

    @Test
    public void acceptExistingMeetingByAttendee() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10025");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10026");

        Event e = testManager.createDefaultEvent(organizer.getUid(), "Existing meeting");
        testManager.addUserParticipantToEvent(e.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(e.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        String externalId = mainEventDao.findMainEventByEventId(e.getId()).getExternalId();

        MailEventData eventData = MailEventData.builder()
                .setUid(attendee.getUid()).setExternalId(externalId)
                .setName(e.getName()).setLocation(e.getLocation())
                .setStart(e.getStartTs()).setEnd(e.getEndTs())
                .setAllDay(false).setStartClientTimezoneOffset(0)
                .setDecision(MailDecision.ACCEPTED)
                .setNotification(Notification.sms(Duration.standardMinutes(-15)))
                .build();

        mailEventManager.createOrUpdateEvent(eventData, actionInfo());

        EventUserWithNotifications eu = notificationDbManager
                .getEventUserWithNotificationsByUidAndEventId(attendee.getUid(), e.getId()).get();

        Assert.equals(Cf.toList(eventData.getNotification()), eu.getNotifications().getNotifications());
        Assert.equals(Decision.YES, eu.getEventUser().getDecision());
    }

    @Test
    public void getEventInfo_for_infinite_repetition_should_return_empty_dueTs() {
        val repetitionId = testManager.createDailyRepetition();
        val event = testManager.createRepeatedEvent(testUid, repetitionId);
        val eventInfo = getRepeatedEventInfo(event);

        val repetitionInfo = eventInfo.getRepetitionInfo();
        assertThat(repetitionInfo.getDueTs()).isEmpty();
        assertThat(repetitionInfo.getLatestInstanceTs()).isEmpty();
    }

    @Test
    public void getEventInfo_for_repetition_should_return_correct_dueTs() {
        val repetitionDueTs = TestManager.eventStartTs.plus(Duration.standardDays(5));
        val repetitionId = testManager.createDailyRepetitionWithDueTs(repetitionDueTs);
        val event = testManager.createRepeatedEvent(testUid, repetitionId);

        val eventInfo = getRepeatedEventInfo(event);
        val repetitionInfo = eventInfo.getRepetitionInfo();
        val expectedLastInstanceTs = repetitionDueTs
                .plus(Duration.standardHours(2))
                .minus(Duration.standardDays(1));

        assertThat(repetitionInfo.getDueTs()).contains(repetitionDueTs);
        assertThat(repetitionInfo.getLatestInstanceTs()).contains(expectedLastInstanceTs);
    }

    @Test
    public void getEventInfo_for_repetition_where_last_event_is_exdate_should_return_correct_dueTs() {
        val repetitionDueTs = TestManager.eventStartTs.plus(Duration.standardDays(5));
        val repetitionId = testManager.createDailyRepetitionWithDueTs(repetitionDueTs);
        val event = testManager.createRepeatedEvent(testUid, repetitionId);
        testManager.createExdate(repetitionDueTs.minus(Duration.standardDays(1)), event.getId());

        val eventInfo = getRepeatedEventInfo(event);
        val repetitionInfo = eventInfo.getRepetitionInfo();
        val expectedLatestInstanceTs = repetitionDueTs
                .minus(Duration.standardDays(2))
                .plus(Duration.standardHours(2));

        assertThat(repetitionInfo.getDueTs()).contains(repetitionDueTs);
        assertThat(repetitionInfo.getLatestInstanceTs()).contains(expectedLatestInstanceTs);
    }

    @Test
    public void getEventInfo_for_repetition_with_recurrence_should_return_correct_dueTs() {
        val repetitionDueTs = TestManager.eventStartTs.plus(Duration.standardDays(5));
        val repetitionId = testManager.createDailyRepetitionWithDueTs(repetitionDueTs);
        val recurrenceStartTs = repetitionDueTs.plus(Duration.standardDays(2));
        val recurrenceEndTs = recurrenceStartTs.plus(Duration.standardHours(2));
        val event = testManager.createEventWithRepetitionAndRecurrence(testUid, repetitionId, recurrenceStartTs,
                recurrenceEndTs)._1;

        val eventInfo = getRepeatedEventInfo(event);
        val repetitionInfo = eventInfo.getRepetitionInfo();

        assertThat(repetitionInfo.getDueTs()).contains(repetitionDueTs);
        assertThat(repetitionInfo.getLatestInstanceTs()).contains(recurrenceEndTs);
    }

    @Test
    public void getEventInfo_for_repetition_with_rdate_should_return_correct_dueTs() {
        val repetitionDueTs = TestManager.eventStartTs.plus(Duration.standardDays(5));
        val repetitionId = testManager.createDailyRepetitionWithDueTs(repetitionDueTs);
        val event = testManager.createRepeatedEvent(testUid, repetitionId);

        val rdateStartTs = repetitionDueTs.plus(Duration.standardDays(2));
        val rdateEndTs = rdateStartTs.plus(Duration.standardHours(2));
        testManager.createRdate(rdateStartTs, rdateEndTs, event.getId());

        val eventInfo = getRepeatedEventInfo(event);
        val repetitionInfo = eventInfo.getRepetitionInfo();

        assertThat(repetitionInfo.getDueTs()).contains(repetitionDueTs);
        assertThat(repetitionInfo.getLatestInstanceTs()).contains(rdateEndTs);
    }

    private MailEventInfo getRepeatedEventInfo(Event event) {
        val externalId = mainEventDao.findMainEventById(event.getMainEventId()).getExternalId();
        val eventData = MailEventData.builder()
                .setUid(testUid)
                .setExternalId(externalId)
                .setName(event.getName())
                .setLocation("somewhere")
                .setDescription("some description")
                .setAllDay(event.getIsAllDay())
                .setStart(event.getStartTs())
                .setEnd(event.getEndTs())
                .setStartClientTimezoneOffset(3)
                .build();
        return mailEventManager.getEventInfo(eventData, ACTION_INFO);
    }

    private void createEvent(TestUserInfo user, TestEventData e) {
        ActionInfo actionInfo = CalendarRequest.getCurrent().getActionInfo();

        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(user.getUid());
        String externalId = e.externalId.getOrElse(CalendarUtils.generateExternalId());
        long mainEventId = eventRoutines.createMainEvent(externalId, tz, actionInfo);

        Event eventFields = new Event();
        eventFields.setStartTs(e.start);
        eventFields.setEndTs(e.end);
        eventFields.setLocation(e.location);
        Event event = testManager.createDefaultEvent(user.getUid(), e.name, eventFields, mainEventId);

        long eventUserId = testManager.createEventUser(user.getUid(), event.getId(), e.decision, Option.<Boolean>empty());
        notificationDbManager.saveEventNotifications(eventUserId, e.notifications);

        testManager.createEventLayer(user.getDefaultLayerId(), event.getId());

        testManager.updateEventTimeIndents(event);
    }

    private void checkEvent(TestUserInfo user, String externalId, TestEventData e) {
        EventInfo eventInfo = findEvent(user, externalId);

        Event event = eventInfo.getEvent();
        Assert.equals(e.name, event.getName(), "name");
        Assert.equals(e.start, event.getStartTs(), "startTs");
        Assert.equals(e.end, event.getEndTs(), "endTs");
        Assert.equals(e.location, event.getLocation(), "location");

        EventUserWithNotifications eventUserWithNotifications
                = eventInfo.getEventUserWithNotifications().getOrThrow("eventUser not found");

        EventUser eventUser = eventUserWithNotifications.getEventUser();
        Assert.equals(e.decision, eventUser.getDecision(), "decision");

        ListF<Notification> notifications = eventUserWithNotifications.getNotifications().getNotifications();
        Assert.assertListsEqual(
                e.notifications.sortedBy(Notification::getChannel),
                notifications.filterNot(n -> n.channelIs(Channel.PANEL)).sortedBy(Notification::getChannel));
    }

    private EventInfo findEvent(TestUserInfo user, String externalId) {
        Event event = eventRoutines
                .findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(user.getUid()), externalId)
                .getOrThrow("event not found");

        ActionInfo actionInfo = CalendarRequest.getCurrent().getActionInfo();
        return eventInfoDbLoader
                .getEventInfosByEvents(Option.of(user.getUid()), Cf.list(event), actionInfo.getActionSource())
                .single();
    }

    private static void assertEventInfoEquals(MailEventInfo expected, MailEventInfo actual) {
        Assert.equals(expected.getName(), actual.getName(), "name");
        Assert.equals(expected.getStart(), actual.getStart(), "start");
        Assert.equals(expected.getEnd(), actual.getEnd(), "end");
        Assert.equals(expected.getLocation(), actual.getLocation(), "location");
        Assert.equals(expected.getDecision(), actual.getDecision(), "decision");
        Assert.equals(expected.getNotification(), actual.getNotification(), "notification");
        Assert.equals(expected.getConflict().isPresent(), actual.getConflict().isPresent(), "conflict.isDefined");
        if (expected.getConflict().isPresent() && actual.getConflict().isPresent()) {
            assertConflictEquals(expected.getConflict().get(), actual.getConflict().get());
        }
    }

    private static void assertConflictEquals(MailEventInfo.Conflict expected, MailEventInfo.Conflict actual) {
        Assert.equals(expected.getName(), actual.getName(), "conflict.name");
        Assert.equals(expected.getStart(), actual.getStart(), "conflict.start");
        Assert.equals(expected.getEnd(), actual.getEnd(), "conflict.start");
        Assert.equals(expected.getLocation(), actual.getLocation(), "conflict.location");
    }

    private static ActionInfo actionInfo() {
        RequestIdStack.Handle handle = RequestIdStack.pushIfNotYet();
        return ActionInfo.test(ActionSource.MAIL, handle.getValue(), MoscowTime.instant(2012, 12, 1, 0, 0));
    }

    private static class TestEventData {

        public final Option<String> externalId;
        public final String name;
        public final Instant start;
        public final Instant end;
        public final String location;
        public final Decision decision;
        public final ListF<Notification> notifications;

        private TestEventData(
                Option<String> externalId, String name, Instant start, Instant end,
                String location, Decision decision, ListF<Notification> notifications)
        {
            this.externalId = externalId;
            this.name = name;
            this.start = start;
            this.end = end;
            this.location = location;
            this.decision = decision;
            this.notifications = notifications;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        public static class Builder {

            private Option<String> externalId;
            private Option<String> name;
            private Option<Instant> start;
            private Option<Instant> end;
            private Option<String> location;
            private Option<Decision> decision;
            private ListF<Notification> notifications;

            public Builder() {
                this.externalId = Option.empty();
                this.name = Option.empty();
                this.start = Option.empty();
                this.end = Option.empty();
                this.location = Option.empty();
                this.decision = Option.empty();
                this.notifications = Cf.arrayList();
            }

            public Builder(TestEventData e) {
                this.externalId = e.externalId;
                this.name = Option.of(e.name);
                this.start = Option.of(e.start);
                this.end = Option.of(e.end);
                this.location = Option.of(e.location);
                this.decision = Option.of(e.decision);
                this.notifications = Cf.toArrayList(e.notifications);
            }

            public TestEventData build() {
                return new TestEventData(externalId, name.get(), start.get(), end.get(),
                        location.get(), decision.get(), Cf.toList(notifications));
            }

            public Builder setExternalId(String externalId) {
                this.externalId = Option.of(externalId);
                return this;
            }

            public Builder setName(String name) {
                this.name = Option.of(name);
                return this;
            }

            public Builder setStart(Instant start) {
                this.start = Option.of(start);
                return this;
            }

            public Builder setStartUtc(int year, int month, int day, int hour, int minute) {
                return setStart(TestDateTimes.utc(year, month, day, hour, minute));
            }

            public Builder setEnd(Instant end) {
                this.end = Option.of(end);
                return this;
            }

            public Builder setEndUtc(int year, int month, int day, int hour, int minute) {
                return setEnd(TestDateTimes.utc(year, month, day, hour, minute));
            }

            public Builder setLocation(String location) {
                this.location = Option.of(location);
                return this;
            }

            public Builder setDecision(Decision decision) {
                this.decision = Option.of(decision);
                return this;
            }

            public Builder addNotification(Notification notification) {
                this.notifications.add(notification);
                return this;
            }

            public Builder clearNotifications() {
                this.notifications = Cf.arrayList();
                return this;
            }

        }

    }

}
