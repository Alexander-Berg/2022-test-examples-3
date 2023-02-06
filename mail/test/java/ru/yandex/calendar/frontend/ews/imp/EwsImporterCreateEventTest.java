package ru.yandex.calendar.frontend.ews.imp;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.LegacyFreeBusyType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.SensitivityChoicesType;
import lombok.SneakyThrows;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.DeletedEvent;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.archive.DeletedEventDao;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.imp.IcsImporterFromFileTest;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.calendar.test.auto.db.util.TestManager.NEXT_YEAR;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class EwsImporterCreateEventTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    protected EwsImporter ewsImporter;
    @Autowired
    protected GenericBeanDao genericBeanDao;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private DeletedEventDao deletedEventDao;

    @Test
    public void createEventsWithoutExternalId() throws DatatypeConfigurationException {
        val user = new PassportLogin("tester11");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer = testManager.createDefaultLayerForUser(uid, now.minus(Duration.standardDays(1)));

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR + 1, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "Exchange event");
        val exchangeId = calItem.getItemId().getId();

        val subjectId = UidOrResourceId.user(uid);
        val actionInfo = ActionInfo.exchangeTest(now);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);
        Option<Event> eventO = eventRoutines.findEventByExchangeId(exchangeId);
        assertThat(eventO).isNotEmpty();

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);

        // Update event subject
        val lastModifiedTs1 = eventO.get().getLastUpdateTs().plus(2000);
        val updatedCalItem1 = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "New exchange event", lastModifiedTs1);
        updatedCalItem1.getItemId().setId(exchangeId);
        ewsImporter.createOrUpdateEventForTest(subjectId, updatedCalItem1, actionInfo, false);
        eventO = eventRoutines.findEventByExchangeId(exchangeId);

        assertThat(eventO).isNotEmpty();
        assertThat(eventO.get().getName()).isEqualTo(updatedCalItem1.getSubject());
        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(eventO.get().getId(), lastModifiedTs1);

        // Set event external_id
        val lastModifiedTs2 = eventO.get().getLastUpdateTs().plus(2000);
        val updatedCalItem2 = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "New exchange name 2", lastModifiedTs2);
        updatedCalItem2.setUID(Random2.R.nextAlnum(10));
        updatedCalItem2.getItemId().setId(exchangeId);
        ewsImporter.createOrUpdateEventForTest(subjectId, updatedCalItem2, actionInfo, false);
        eventO = eventRoutines.findEventByExchangeId(exchangeId);
        assertThat(eventO).isNotEmpty();
        val eventsByExternalId = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(uid), updatedCalItem2.getUID());

        assertThat(eventsByExternalId).hasSize(1);
        assertThat(eventsByExternalId.first().getId()).isEqualTo(eventO.get().getId());
        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(eventO.get().getId(), lastModifiedTs2);
    }

    @Test
    public void tryToRecreateEventFromYaTeamCalendarBySynchronizer() throws Exception {
        tryToRecreateEventFromYaTeamCalendar(ActionInfo.exchangeTest());
    }

    @Test
    public void tryToRecreateEventFromYaTeamCalendarByPushNotificaiton() throws Exception {
        tryToRecreateEventFromYaTeamCalendar(ActionInfo.exchangeTest());
    }

    private void tryToRecreateEventFromYaTeamCalendar(ActionInfo actionInfo) throws Exception {
        val r = testManager.cleanAndCreateThreeLittlePigs();
        val resourceEmail = ResourceRoutines.getResourceEmail(r);

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "tryTorecreateEventFromYaTeamCalendar");
        TestCalItemFactory.setOrganizer(calItem, resourceEmail);

        calItem.getExtendedProperty().add(EwsUtils.createSourceExtendedProperty());

        val subjectId = UidOrResourceId.resource(r.getId());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);

        val eventO = eventRoutines.findEventByExchangeId(calItem.getItemId().getId());
        assertThat(actionInfo.getActionSource() == ActionSource.EXCHANGE_SYNCH).isEqualTo(eventO.isPresent());
    }

    // http://wiki.yandex-team.ru/Calendar/exchange/attendees
    @Test
    public void attendeeResourceIsAddedToParticipantsViaHisNotificationChannel() throws Exception {
        userManager.registerYandexUserForTest(TestManager.createResourceMaster());

        val user = testManager.prepareRandomYaTeamUser(3);
        val smolny = testManager.cleanAndCreateSmolny();

        testManager.updateIsEwser(user);

        val userEmail = user.getEmail();
        val smolnyEmail = ResourceRoutines.getResourceEmail(smolny);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer = testManager.createDefaultLayerForUser(user.getUid(), now.minus(Duration.standardDays(1)));

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR + 1, 5, 18, 16, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "attendeeResourceIsAddedToParticipantsViaHisNotificationChannel");
        TestCalItemFactory.setOrganizer(calItem, userEmail);
        TestCalItemFactory.addAttendee(calItem, userEmail, ResponseTypeType.ACCEPT);

        val subjectId = UidOrResourceId.resource(smolny.getId());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(now), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        val participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        assertThat(participants.isMeeting()).isTrue();
        assertThat(participants.getAllAttendeesButNotOrganizer().single().getEmail()).isEqualTo(smolnyEmail);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);
    }

    // https://jira.yandex-team.ru/browse/CAL-3228
    @Test
    public void each4thThursdayRepetition() throws Exception {
        val user = testManager.prepareRandomYaTeamUser(4);

        val dateTime = TestDateTimes.moscowDateTime(NEXT_YEAR, 4, 28, 10, 0);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "each4thThursdayRepetition");
        TestCalItemFactory.addMonthlyDayWeeknoRecurrence(calItem);

        val subjectId = UidOrResourceId.user(user.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        val repetition = eventDao.findRepetitionById(event.getRepetitionId().get());

        assertThat(repetition.getType()).isEqualTo(RegularRepetitionRule.MONTHLY_DAY_WEEKNO);
        assertThat(repetition.getRMonthlyLastweek().get()).isFalse();
    }

    // https://jira.yandex-team.ru/browse/CAL-3202
    @Test
    public void layerNotificationIsUsedInEvent() throws Exception {
        val user = testManager.prepareRandomYaTeamUser(5);
        val uid = user.getUid();

        val notifications = testManager.sms25MinutesBefore();
        val expectedNotification = notifications.plus(new Notification(Channel.XIVA, Duration.ZERO));

        layerRoutines.updateNotification(uid, user.getDefaultLayerId(),
                NotificationsData.updateFromWeb(notifications), false, ActionInfo.webTest());

        val dateTime = TestDateTimes.moscowDateTime(NEXT_YEAR, 6, 8, 15, 0);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "layerNotificationIsUsed");

        val subjectId = UidOrResourceId.user(uid);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        val eventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), uid).get();
        val actualNotification = notificationDbManager.getNotificationsByEventUserId(eventUser.getId());

        assertThat(actualNotification.getNotifications().unique()).isEqualTo(expectedNotification.unique());
    }

    @Test
    public void layerNotificationIsUsedInMeetingImportFromAttendee() throws Exception {
        val organizer = testManager.prepareRandomYaTeamUser(6);
        val attendee = testManager.prepareRandomYaTeamUser(7);

        val notifications = testManager.sms25MinutesBefore();
        val expectedNotifications = notifications.plus(new Notification(Channel.XIVA, Duration.ZERO));

        layerRoutines.updateNotification(attendee.getUid(), attendee.getDefaultLayerId(),
                NotificationsData.updateFromWeb(notifications), false, ActionInfo.webTest());

        val dateTime = TestDateTimes.moscowDateTime(NEXT_YEAR, 6, 8, 15, 0);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "layerNotificationIsUsedInMeeting");
        TestCalItemFactory.setOrganizer(calItem, organizer.getEmail());
        TestCalItemFactory.addAttendee(calItem, attendee.getEmail(), ResponseTypeType.ACCEPT);

        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(attendee.getUid()), calItem, ActionInfo.exchangeTest(), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        val attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();

        val eventUserId = attendeeEventUser.getId();
        val attendeeEventNotification = notificationDbManager.getNotificationsByEventUserId(eventUserId);

        assertThat(attendeeEventNotification.getNotifications().unique()).isEqualTo(expectedNotifications.unique());
    }

    @Test
    public void layerNotificationIsUsedInMeetingImportFromOrganizer() throws Exception {
        val organizer = testManager.prepareRandomYaTeamUser(8);
        val attendee = testManager.prepareRandomYaTeamUser(9);

        val notifications = testManager.sms25MinutesBefore();
        val expectedNotifications = notifications.plus(new Notification(Channel.XIVA, Duration.ZERO));

        layerRoutines.updateNotification(organizer.getUid(), organizer.getDefaultLayerId(),
                NotificationsData.updateFromWeb(notifications), false, ActionInfo.webTest());

        val dateTime = TestDateTimes.moscowDateTime(NEXT_YEAR, 6, 8, 15, 0);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "layerNotificationIsUsedInMeeting");
        TestCalItemFactory.setOrganizer(calItem, organizer.getEmail());
        TestCalItemFactory.addAttendee(calItem, organizer.getEmail(), ResponseTypeType.ACCEPT);
        TestCalItemFactory.addAttendee(calItem, attendee.getEmail(), ResponseTypeType.ACCEPT);

        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(organizer.getUid()), calItem, ActionInfo.exchangeTest(), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        val organizerEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), organizer.getUid()).get();

        val eventUserId = organizerEventUser.getId();
        val organizerEventNotification = notificationDbManager.getNotificationsByEventUserId(eventUserId);

        assertThat(organizerEventNotification.getNotifications().unique()).isEqualTo(expectedNotifications.unique());
    }

    @Test
    public void createPrivateMeeting() throws Exception {
        val organizer = testManager.prepareRandomYaTeamUser(6);
        val attendee = testManager.prepareRandomYaTeamUser(7);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val organizerLayer = testManager
                .createDefaultLayerForUser(organizer.getUid(), now.minus(Duration.standardDays(1)));
        val attendeeLayer = testManager
                .createDefaultLayerForUser(attendee.getUid(), now.minus(Duration.standardDays(1)));


        val dateTime = TestDateTimes.moscowDateTime(NEXT_YEAR + 2, 7, 13, 12, 0);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "createPrivateMeeting");
        TestCalItemFactory.setOrganizer(calItem, organizer.getEmail());
        TestCalItemFactory.addAttendee(calItem, organizer.getEmail(), ResponseTypeType.ACCEPT);
        TestCalItemFactory.addAttendee(calItem, attendee.getEmail(), ResponseTypeType.ACCEPT);
        calItem.setSensitivity(SensitivityChoicesType.PRIVATE);

        val subjectId = UidOrResourceId.user(organizer.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(now), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        assertThat(event.getPermAll()).isEqualTo(EventActionClass.NONE);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer, attendeeLayer), now);
    }

    @Test
    public void properStartTsInNotAllDayEvent() throws Exception {
        properStartTs(false);
    }

    /**
     * @url https://jira.yandex-team.ru/browse/CAL-4025
     * @see IcsImporterFromFileTest#properStartTs()
     */
    @Test
    public void properStartTsInAllDayEvent() throws Exception {
        properStartTs(true);
    }

    private void properStartTs(boolean isAllDay) throws Exception {
        val user = testManager.prepareRandomYaTeamUser(10);

        val start = TestDateTimes.moscowDateTime(NEXT_YEAR, 9, 28, 0, 0);

        val calItem = new CalendarItemType();
        EwsUtils.setStartEndTimezone(calItem, start.getZone());

        calItem.setStart(EwsUtils.instantToXMLGregorianCalendar(start.toInstant(), DateTimeZone.UTC));
        calItem.setEnd(EwsUtils.instantToXMLGregorianCalendar(start.plusDays(1).toInstant(), DateTimeZone.UTC));
        calItem.setIsAllDayEvent(isAllDay);

        calItem.setSubject("properStartTs(" + isAllDay + ")");
        calItem.setItemId(EwsUtils.createItemId(Random2.R.nextAlnum(8)));
        calItem.setDateTimeStamp(calItem.getStart());
        calItem.setLastModifiedTime(calItem.getStart());

        val subjectId = UidOrResourceId.user(user.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(), false);

        val event = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        assertThat(event.getStartTs()).isEqualTo(start.toInstant());
    }

    @Test
    public void singleUserEventAvailability() throws Exception {
        val user = testManager.prepareRandomYaTeamUser(1046);

        val start = TestDateTimes.moscowDateTime(NEXT_YEAR, 11, 21, 0, 0);

        val item = new CalendarItemType();
        item.setLegacyFreeBusyStatus(LegacyFreeBusyType.FREE);
        item.setSubject("Андрей Себрант День рождения");
        TestCalItemFactory.setOrganizer(item, user.getEmail());

        item.setStart(EwsUtils.instantToXMLGregorianCalendar(start.toInstant(), MoscowTime.TZ));
        item.setEnd(EwsUtils.instantToXMLGregorianCalendar(start.plusDays(1).toInstant(), MoscowTime.TZ));

        EwsUtils.setStartEndTimezone(item, start.getZone());

        item.setLastModifiedTime(item.getStart());
        item.setDateTimeStamp(item.getStart());

        item.setUID(CalendarUtils.generateExternalId());
        item.setItemId(EwsUtils.createItemId(Random2.R.nextAlnum(8)));

        val subjectId = UidOrResourceId.user(user.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, item, ActionInfo.exchangeTest(), false);

        val eventUser = eventUserDao.findEventUserByExchangeId(item.getItemId().getId()).get();
        assertThat(eventUser.getAvailability()).isEqualTo(Availability.AVAILABLE);
    }

    @SneakyThrows
    private void createOutlookException(int year, int expectedSize, BiConsumer<String, Instant> recurrenceHandler) {
        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val eventStartTs = new DateTime(year, 4, 27, 10, 0, 0, 0, tz).toInstant();
        val eventEndTs = new DateTime(year, 4, 27, 12, 0, 0, 0, tz).toInstant();
        val user = testManager.prepareRandomYaTeamUser(10);
        val event = testManager.createRepeatedEvent(user.getUid(), testManager.createDailyRepetition(), eventStartTs, eventEndTs);

        val externalId =
                eventDbManager.getMainEventWithRelationsById(event.getMainEventId()).getMainEvent().getExternalId();

        recurrenceHandler.accept(externalId, event.getStartTs());

        val recurCalItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), tz), "Recurrence event name", event.getLastUpdateTs().plus(1000));

        TestCalItemFactory.setOrganizer(recurCalItem, user.getEmail());
        recurCalItem.setUID(externalId);
        val recurrenceId = event.getStartTs();
        recurCalItem.setRecurrenceId(EwsUtils.instantToXMLGregorianCalendar(recurrenceId, tz));
        recurCalItem.getExtendedProperty().add(EwsUtils.createSourceExtendedProperty());

        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(user.getUid()), recurCalItem, ActionInfo.exchangeTest(), false);

        val events = eventDbManager.getMainEventWithRelationsById(event.getMainEventId()).getEvents();
        assertThat(events).hasSize(expectedSize);
    }

    @Test
    public void createInOutlookAnExceptionForWebCreatedMaster() {
        createOutlookException(NEXT_YEAR, 2, (x, y) -> {});
    }

    @Test
    public void createOutdatedInOutlookAnExceptionForWebCreatedMaster() {
        // We expect only one event (master) that was created directly in our database,
        // not the recurrent one pulled from Exchange.
        createOutlookException(2009, 1, (x, y) -> {});
    }

    @Test
    public void checkRecurrenceThatIsAlreadyMarkedAsDeletedInOurDatabase() {
        createOutlookException(NEXT_YEAR, 1, this::markRecurrenceAsDeleted);
    }

    @Test
    public void createEventWithConferenceUrl() throws DatatypeConfigurationException {
        val user = new PassportLogin("tester11");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer = testManager.createDefaultLayerForUser(uid, now.minus(Duration.standardDays(1)));

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR + 1, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "Exchange event");
        val exchangeId = calItem.getItemId().getId();

        val subjectId = UidOrResourceId.user(uid);
        val actionInfo = ActionInfo.exchangeTest(now);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);
        Option<Event> eventO = eventRoutines.findEventByExchangeId(exchangeId);
        assertThat(eventO).isNotEmpty();
        assertThat(eventO.get().getConferenceUrl().orElse("")).isEqualTo("");

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);

        // Update event conference url
        val lastModifiedTs1 = eventO.get().getLastUpdateTs().plus(2000);
        val updatedCalItem1 = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "New exchange event", lastModifiedTs1);
        updatedCalItem1.getItemId().setId(exchangeId);
        updatedCalItem1.setMeetingWorkspaceUrl("http://url");
        ewsImporter.createOrUpdateEventForTest(subjectId, updatedCalItem1, actionInfo, false);
        eventO = eventRoutines.findEventByExchangeId(exchangeId);

        assertThat(eventO).isNotEmpty();
        assertThat(eventO.get().getConferenceUrl().orElse("")).isEqualTo("http://url");
        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(eventO.get().getId(), lastModifiedTs1);
    }

    private void markRecurrenceAsDeleted(String externalId, Instant recurrenceId) {
        val deletedEvent = new DeletedEvent();

        deletedEvent.setRecurrenceId(recurrenceId);
        deletedEvent.setExternalId(externalId);
        deletedEvent.setName("Test deletion");
        deletedEvent.setDeletionTs(Instant.now());
        deletedEvent.setDeletionSource(ActionSource.UNKNOWN);
        deletedEvent.setStartTs(recurrenceId);
        deletedEvent.setDeletionReqId("Kill em all");
        deletedEvent.setId(13666L);

        deletedEventDao.saveDeletedEvents(Cf.list(deletedEvent));
    }
}
