package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.*;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVAlarm;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsRelated;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAction;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsOrganizer;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTrigger;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.param.InvitationMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.logic.sharing.perm.EventInfoForPermsCheck;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * See http://wiki.yandex-team.ru/calendar/unittests
 */
public class IcsImporterMeetingCreateTest extends AbstractConfTest {
    // TODO test 3 - resource. For now, we send email to exchange, which is inadmissible for tests.

    private static final Logger logger = LoggerFactory.getLogger(IcsImporterMeetingCreateTest.class);

    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private NotificationDbManager notificationDbManager;

    @Test
    public void organizerCreatesEventWhichAttendeeAccepts() { // test 4
        createEventWhichAttendeeAccepts(true);
    }

    @Test
    public void attendeeCreatesAndAcceptsEvent() { // test 6
        createEventWhichAttendeeAccepts(false);
    }

    private void createEventWhichAttendeeAccepts(boolean isByOrganizer) {
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10121");
        TestUserInfo u2 = testManager.prepareUser("yandex-team-mm-10122");

        PassportLogin user1 = u1.getLogin();
        PassportLogin user2 = u2.getLogin();

        PassportUid uid1 = u1.getUid();
        PassportUid uid2 = u2.getUid();

        IcsImportMode importMode = IcsImportMode.incomingEmailFromMailhook(TestDateTimes.moscow(2011, 11, 21, 21, 28))
                .withActionInfoFreezedNowForTest();

        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));
        long user1Layer = testManager.createDefaultLayerForUser(uid1, beforeNow);
        long user2Layer = testManager.createDefaultLayerForUser(uid2, beforeNow);

        IcsVEvent vevent = new IcsVEvent();
        String actor = isByOrganizer ? "organizer" : "attendee";
        vevent = vevent.withSummary("Night time event for " + actor + " to receive");
        vevent = vevent.withOrganizer(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user2.getNormalizedValue() + "@yandex.ru"));
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 10, 23, 30));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 10, 11, 7, 30));
        vevent = vevent.withSequenece(0);

        PassportUid actorUid = isByOrganizer ? uid1 : uid2;

        icsImporter.importIcsStuff(actorUid, vevent.makeCalendar(), importMode);

        // event layers should exist for both organizer and attendee, pointing to the same event
        Event orgEvent = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid1), extId).get();
        Event attEvent = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid2), extId).get();
        Assert.A.equals(orgEvent.getId(), attEvent.getId());
        // do we really care? // gutman@
        Assert.A.equals(isByOrganizer ? uid1 : uid2, orgEvent.getCreatorUid());

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(user1Layer, user2Layer), importMode.getActionInfo().getNow());
    }

    @Test
    public void organizerCreatesEventWhichAttendeeDeclines() { // test 5
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(10121);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(10122);

        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = user2.getUid();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Instant event for organizer");
        vevent = vevent.withOrganizer(user1.getEmail());
        vevent = vevent.addAttendee(user1.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(user2.getEmail(), IcsPartStat.DECLINED);
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 10, 23, 31));
        vevent = vevent.withSequenece(0);

        icsImporter.importIcsStuff(uid1, vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());
        Event event = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid1), extId).get();
        ParticipantInfo participant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(uid2)).get();
        Assert.A.equals(Decision.NO, participant.getDecision());
    }

    @Test
    public void organizerCreatesNewRecurrenceIdInstance() { // test 10
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10121");

        PassportLogin user1 = u1.getLogin();
        Email email1 = new Email(user1.getNormalizedValue() + "@yandex.ru");
        PassportUid uid1 = u1.getUid();

        String externalId = CalendarUtils.generateExternalId();

        IcsImportMode importMode = IcsImportMode.importFile(
                LayerReference.defaultLayer(),
                TestDateTimes.moscow(2011, 11, 21, 21, 28)).withActionInfoFreezedNowForTest();

        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));
        long user1Layer = testManager.createDefaultLayerForUser(uid1, beforeNow);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withOrganizer(email1);
        vevent = vevent.addAttendee(email1);
        vevent = vevent.withUid(externalId);
        vevent = vevent.withSummary("New recurrence-id (very short), no attendees");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 01, 05, 03));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 01, 05, 04));
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(TestDateTimes.moscow(2010, 11, 02, 12, 10));
        Instant recurrenceId = TestDateTimes.moscow(2010, 10, 31, 14, 50); // instant this recurrence is instead of
        vevent = vevent.withRecurrence(recurrenceId);

        icsImporter.importIcsStuff(uid1, vevent.makeCalendar(), importMode);

        Assert.assertEmpty(eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid1), externalId));
        Assert.assertNotEmpty(eventRoutines.getRecurrenceEventInstanceBySubjectIdAndExternalId(UidOrResourceId.user(uid1), externalId, recurrenceId));

        testLayerCollLastUpdateChecker.assertUpdated(user1Layer, importMode.getActionInfo().getNow());
    }

    @Test
    public void nonMeetingEventIsAddedForEachUserSeparately() { // test 12 common part
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10121");
        TestUserInfo u2 = testManager.prepareUser("yandex-team-mm-10122");

        PassportUid uid1 = u1.getUid();
        PassportUid uid2 = u2.getUid();

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));

        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));
        long user1Layer = testManager.createDefaultLayerForUser(uid1, beforeNow);
        long user2Layer = testManager.createDefaultLayerForUser(uid2, beforeNow);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withSequenece(0);
        String externalId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withSummary("Just an event");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 05, 14, 0));

        IcsCalendar calendar = vevent.makeCalendar();

        long eventId1 = icsImporter.importIcsStuff(uid1, calendar, importMode).getNewEventIds().single();
        long eventId2 = icsImporter.importIcsStuff(uid2, calendar, importMode).getNewEventIds().single();

        Assert.A.notEmpty(eventUserDao.findEventUserByEventIdAndUid(eventId1, uid1));
        Assert.A.notEmpty(eventUserDao.findEventUserByEventIdAndUid(eventId2, uid2));

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(user1Layer, user2Layer), importMode.getActionInfo().getNow());
    }

    @Test
    public void importMeetingWhereImNotParticipant() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-10121");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-10122");
        TestUserInfo user3 = testManager.prepareUser("yandex-team-mm-10123");

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStamp(TestDateTimes.moscow(2010, 12, 6, 19, 56));
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 12, 6, 13, 0));
        String externalId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withSummary("meeting where I'm not attendee");
        vevent = vevent.withOrganizer(user1.getEmail());
        vevent = vevent.addAttendee(user1.getEmail());
        vevent = vevent.addAttendee(user2.getEmail());

        IcsImportStats stats = icsImporter.importIcsStuff(user3.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());
        long eventId = stats.getNewEventIds().single();

        Participants participants = eventInvitationManager.getParticipantsByEventId(eventId);
        Assert.A.isTrue(participants.isMeeting());
        Assert.A.hasSize(1, participants.getAllAttendeesButNotOrganizer());

        Assert.A.hasSize(1, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(eventId, user3.getUid()));

        Assert.A.hasSize(0, mailSenderMock.getInvitationMessageParameterss());
    }

    @Test
    public void correctAvailabilityAndDecisionAfterImport() {  // CAL-5456
        importIcsCheckAvailabilityAndDecision(0);
        importIcsCheckAvailabilityAndDecision(1);
        importIcsCheckAvailabilityAndDecision(2);
        importIcsCheckAvailabilityAndDecision(3);
    }

    private void importIcsCheckAvailabilityAndDecision(int actorIndex) {
        TestUserInfo user1 = testManager.prepareRandomYaTeamUser(10011);
        TestUserInfo user2 = testManager.prepareRandomYaTeamUser(10012);
        TestUserInfo user3 = testManager.prepareRandomYaTeamUser(10013);
        TestUserInfo user4 = testManager.prepareRandomYaTeamUser(10014);

        TestUserInfo actor = Cf.list(user1, user2, user3, user4).get(actorIndex);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(user1.getEmail());
        vevent = vevent.addAttendee(user1.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(user2.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(user3.getEmail(), IcsPartStat.DECLINED);
        vevent = vevent.addAttendee(user4.getEmail(), IcsPartStat.TENTATIVE);
        vevent = vevent.withSummary("Correct availability and decision after import");
        vevent = vevent.withDtStart(MoscowTime.instant(2012, 10, 22, 15, 30));
        vevent = vevent.withDtEnd(MoscowTime.instant(2012, 10, 22, 16, 30));

        IcsImportStats importStats = icsImporter.importIcsStuff(
                actor.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        long eventId = importStats.getNewEventIds().single();

        logger.debug("Actor is {} ({})", actor.getLogin(), actor.getUid());
        checkAvailabilityAndDecision(eventId, user1, Availability.BUSY, Decision.YES);
        checkAvailabilityAndDecision(eventId, user2, Availability.BUSY, Decision.YES);
        checkAvailabilityAndDecision(eventId, user3, Availability.AVAILABLE, Decision.NO);
        checkAvailabilityAndDecision(eventId, user4, Availability.MAYBE, Decision.MAYBE);
    }

    private void checkAvailabilityAndDecision(long eventId, TestUserInfo user, Availability expectedAvailability,
            Decision expectedDecision)
    {
        EventUser eventUser = eventRoutines.findEventUser(user.getUid(), eventId).get();
        String loginUid = user.getLogin() + " (" + user.getUid() + ")";
        Assert.equals(expectedAvailability, eventUser.getAvailability(), "availability of " + loginUid);
        Assert.equals(expectedDecision, eventUser.getDecision(), "decision of " + loginUid);
    }

    // CAL-5630
    @Test
    public void createMeetingFromMailhookExcludesResources() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(928);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(929);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.addAttendee(Cf.list(resource).map(ResourceRoutines.getResourceEmailF()).single());

        vevent = vevent.withSummary("Create meeting from mailhook excludes resources");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2012, 10, 17, 17, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2012, 10, 17, 17, 30));

        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                attendee.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());
        long eventId = icsImportStats.getNewEventIds().single();

        Participants p = eventInvitationManager.getParticipantsByEventId(eventId);
        Assert.forAll(p.getParticipantIds(), ParticipantId.isResourceF().notF());
    }

    @Test
    public void incomingIcsNotificationMailhook() {
        incomingIcsNotificationIgnoreOrWelcomeDependingOnActionSource(ActionSource.MAILHOOK);
    }

    @Test
    public void incomingIcsNotificationCaldav() {
        incomingIcsNotificationIgnoreOrWelcomeDependingOnActionSource(ActionSource.CALDAV);
    }

    private void incomingIcsNotificationIgnoreOrWelcomeDependingOnActionSource(ActionSource actionSource) {
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10121");
        TestUserInfo u2 = testManager.prepareUser("yandex-team-mm-10122");

        PassportLogin user1 = u1.getLogin();
        PassportLogin user2 = u2.getLogin();

        PassportUid uid1 = u1.getUid();
        PassportUid uid2 = u2.getUid();

        // Explicitly create layer with notification != default.
        // Hereby we'll be sure that incoming ics won't affect it.
        Layer layerOverrides = new Layer();
        layerOverrides.setName("Layer with overridden notification");

        LayerUser layerUserOverrides = new LayerUser();
        ListF<Notification> layerNotificationData = testManager.sms25MinutesBefore();

        layerRoutines.createUserLayer(uid1, layerNotificationData, layerOverrides, true, layerUserOverrides);
        layerRoutines.createUserLayer(uid2, layerNotificationData, layerOverrides, true, layerUserOverrides);

        String eventName = "Incoming notifications test";

        IcsVAlarm valarm = new IcsVAlarm();
        valarm = valarm.withAction(IcsAction.DISPLAY);
        valarm = valarm.withTrigger(IcsTrigger.createDuration("-PT45M", true, Option.of(IcsRelated.START)));
        valarm = valarm.withDescription("Display alarm for " + eventName);

        IcsVEvent vevent = new IcsVEvent(Cf.list(), Cf.list(valarm));
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user2.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.withSummary(eventName);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 20, 10, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 20, 13, 0));

        IcsImportMode mode;
        if (actionSource == ActionSource.MAILHOOK) {
            mode = IcsImportMode.incomingEmailFromMailhook();
        } else if (actionSource == ActionSource.CALDAV) {
            mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        } else {
            throw new IllegalStateException();
        }

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
            uid1, calendar, mode
        );
        long eventId = icsImportStats.getNewEventIds().single();

        long eventUser1Id = eventUserDao.findEventUserByEventIdAndUid(eventId, uid1).get().getId();
        ListF<Notification> user1ActualNotification = notificationDbManager
                .getNotificationsByEventUserId(eventUser1Id).getNotifications();

        Notification panelNotification = EventUserRoutines.autoPanelNotification();

        ListF<Notification> user1ExpectedNotification;
        if (actionSource == ActionSource.MAILHOOK) {
            user1ExpectedNotification = layerNotificationData.plus(panelNotification);
        } else {
            user1ExpectedNotification = layerNotificationData
                    .plus(Notification.display(Duration.standardMinutes(-45)))
                    .plus(panelNotification);
        }
        Assert.equals(user1ExpectedNotification.unique(), user1ActualNotification.unique());

        long eventUser2Id = eventUserDao.findEventUserByEventIdAndUid(eventId, uid2).get().getId();
        ListF<Notification> user2ActualNotification = notificationDbManager
                .getNotificationsByEventUserId(eventUser2Id).getNotifications();

        ListF<Notification> user2ExpectedNotification = layerNotificationData.plus(panelNotification);
        Assert.equals(user2ExpectedNotification.unique(), user2ActualNotification.unique());
    }

    @Test
    public void organizerCreatesEventWithIdenticalUidAttendees() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(100);
        TestUserInfo attendee = testManager.prepareYandexUser(TestManager.createAkirakozov());
        Email cloneEmail = new Email("akirakozov@ld.yandex.ru");

        Settings data = new Settings();
        data.setYandexEmail(cloneEmail);
        settingsRoutines.updateSettingsByUid(data, attendee.getUid());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("organizerCreatesEventWithIdenticalUidAttendees");
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.addAttendee(cloneEmail);
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 10, 23, 31));
        vevent = vevent.withSequenece(0);

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());
        Event event = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(organizer.getUid()), extId).get();
        Participants participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        Assert.A.hasSize(1, participants.getAllAttendeesButNotOrganizer());
    }

    @Test
    public void eventWithExternalOrganizerIsAddedOnYandexUsersBehalf() {
        TestUserInfo yaUserAttendee = testManager.prepareRandomYaTeamUser(102);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Event with external organizer");
        vevent = vevent.withOrganizer(new Email("external-fake-email@fake-mail.com"));
        // XXX ssytnik: akirakozov@ said that PUBLISH without attendee should not fail. Clone this test (no attendee)?
        vevent = vevent.addAttendee(new Email("external-fake-email@fake-mail.com"));
        vevent = vevent.addAttendee(yaUserAttendee.getEmail());
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 11, 22, 52));
        vevent = vevent.withSequenece(0);

        final PassportUid yaUserAttendeeUid = yaUserAttendee.getUid();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
            yaUserAttendeeUid, vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook()
        );
        long eventId = icsImportStats.getNewEventIds().single();
        EventWithRelations newEvent = eventDbManager.getEventWithRelationsById(eventId);
        EventInfoForPermsCheck eventAuthInfo = authorizer.loadEventInfoForPermsCheck(yaUserAttendee.getUserInfo(), newEvent);
        authorizer.ensureCanViewEvent(yaUserAttendee.getUserInfo(), eventAuthInfo, ActionSource.WEB);
    }

    @Test
    public void incorrectAttendeeEmail() {
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10101");
        TestUserInfo u2 = testManager.prepareUser("yandex-team-mm-10102");

        PassportLogin user1 = u1.getLogin();
        PassportLogin user2 = u2.getLogin();

        PassportUid uid1 = u1.getUid();
        PassportUid uid2 = u2.getUid();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Ics with incorrect e-mail");
        vevent = vevent.withOrganizer(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user2.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addProperty(new IcsAttendee("invalid", Cf.list()));
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 11, 22, 52));
        vevent = vevent.withSequenece(0);

        icsImporter.importIcsStuff(uid1, vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest());

        Event mainEvent = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid2), extId).get();
        Participants participants = eventInvitationManager.getParticipantsByEventId(mainEvent.getId());
        Assert.A.isTrue(participants.isMeeting());
        Assert.A.equals(new Email(user1.getNormalizedValue() + "@yandex.ru"), participants.getOrganizer().getEmail());
        Assert.A.hasSize(1, participants.getAllAttendeesButNotOrganizer());
        Assert.A.equals(new Email(user2.getNormalizedValue() + "@yandex.ru"), participants.getAllAttendeesButNotOrganizer().single().getEmail());
    }

    @Test
    public void incorrectOrganizedEmail() {
        TestUserInfo u1 = testManager.prepareUser("yandex-team-mm-10111");
        TestUserInfo u2 = testManager.prepareUser("yandex-team-mm-10112");

        PassportLogin user1 = u1.getLogin();
        PassportLogin user2 = u2.getLogin();

        PassportUid uid2 = u2.getUid();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Ics with incorrect e-mail");
        vevent = vevent.addProperty(new IcsOrganizer("invalid", Cf.list()));
        vevent = vevent.addAttendee(new Email(user1.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(user2.getNormalizedValue() + "@yandex.ru"));
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 11, 22, 52));
        vevent = vevent.withSequenece(0);

        icsImporter.importIcsStuff(uid2, vevent.makeCalendar(), IcsImportMode.caldavPutToDefaultLayerForTest());

        Event mainEvent = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid2), extId).get();
        Participants participants = eventInvitationManager.getParticipantsByEventId(mainEvent.getId());
        Assert.A.isTrue(!participants.isMeeting());
    }

    @Test
    // can not find event by external organizer
    // see EventRoutines#getMainEventBySubjectIdAndParticipantEmailsAndExternalId
    public void createMeetingWithExternalOrganizerFromDifferentAttendees() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-10130");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-10131");

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("Invitation for user1");
        vevent = vevent.addProperty(new IcsOrganizer(new Email("alex.kirakozov@gmail.com")));
        vevent = vevent.addAttendee(user1.getEmail());
        String externalId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 10, 11, 22, 52));
        vevent = vevent.withSequenece(0);

        // XXX: akirakozov: ics imported must be called at most once during test // stepancheg@
        // import from first user
        icsImporter.importIcsStuff(
                user1.getUid(), vevent.makeCalendar(),
                IcsImportMode.incomingEmailFromMailhook());

        // change attendee user in ics and import from second user
        vevent = vevent.removeAttendee(user1.getEmail());
        vevent = vevent.addAttendee(user2.getEmail());
        icsImporter.importIcsStuff(
                user2.getUid(), vevent.makeCalendar(),
                IcsImportMode.incomingEmailFromMailhook());

        Event event1 = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(user1.getUid()), externalId).get();
        Event event2 = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(user2.getUid()), externalId).get();

        Assert.equals(event1.getId(), event2.getId());

        EventWithRelations event = eventDbManager.getEventWithRelationsByEvent(event1);
        EventInfoForPermsCheck eventAuthInfoForUser1 = authorizer.loadEventInfoForPermsCheck(user1.getUserInfo(), event);
        Assert.isFalse(authorizer.canEditEvent(user1.getUserInfo(), eventAuthInfoForUser1, ActionSource.WEB));
        EventInfoForPermsCheck eventAuthInfoForUser2 = authorizer.loadEventInfoForPermsCheck(user2.getUserInfo(), event);
        Assert.isFalse(authorizer.canEditEvent(user2.getUserInfo(), eventAuthInfoForUser2, ActionSource.WEB));
    }

    @Test
    public void createByOrganizerAndCheckPrimaryLayer() {
        createAndCheckPrimaryLayer(true);
    }

    @Test
    public void createByAttendeeAndCheckPrimaryLayer() {
        createAndCheckPrimaryLayer(false);
    }

    private void createAndCheckPrimaryLayer(boolean byOrganizer) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10140");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10141");

        PassportUid creatorUid = byOrganizer ? organizer.getUid() : attendee.getUid();
        long defaultLayerId  = layerRoutines.createUserLayer(creatorUid);
        long nonDefaultLayerId = layerRoutines.createUserLayer(creatorUid);
        settingsRoutines.updateDefaultLayer(creatorUid, defaultLayerId);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.withSummary("createAndCheckPrimaryLayer(" + byOrganizer + ")");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 20, 10, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 20, 13, 0));

        IcsCalendar calendar = vevent.makeCalendar();
        Option<Long> eventId = icsImporter.importIcsStuff(
                    creatorUid, calendar, IcsImportMode.caldavPut(nonDefaultLayerId, Option.empty())
                ).getNewEventIds().singleO();

        if (byOrganizer) {
            EventLayer organizerEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerId(
                    eventId.get(), nonDefaultLayerId).get();
            Assert.isTrue(organizerEventLayer.getIsPrimaryInst());
        } else {
            Assert.none(eventId);
        }
    }

    @Test
    public void sendEmailsOnCreateFromCaldav() {
        emailsOnCreate(IcsImportMode.caldavPutToDefaultLayerForTest());
    }

    @Test
    public void dontSendEmailsOnCreateFromWebIcs() {
        emailsOnCreate(IcsImportMode.importFile(LayerReference.defaultLayer()));
    }

    private void emailsOnCreate(IcsImportMode icsImportMode) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10150");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-10151");
        TestUserInfo attendee2 = testManager.prepareUser("yandex-team-mm-10152");

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee1.getEmail());
        vevent = vevent.addAttendee(attendee2.getEmail());
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 20, 10, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 20, 13, 0));

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), icsImportMode);

        ListF<InvitationMessageParameters> invitationss = mailSenderMock.getInvitationMessageParameterss();
        Assert.sameSize(invitationss, mailSenderMock.getEventMessageParameters());

        if (icsImportMode.getActionSource() == ActionSource.CALDAV) {
            ListF<PassportUid> notifiedUids = invitationss.filterMap(InvitationMessageParameters.getRecipientUidF());

            Assert.hasSize(2, notifiedUids);
            Assert.equals(Cf.set(attendee1.getUid(), attendee2.getUid()), notifiedUids.unique());
        } else {
            Assert.isEmpty(mailSenderMock.getEventMessageParameters());
        }
    }

    @Test
    public void importMeetingWithResourceOrganizer() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10161");
        Resource organizerResource = testManager.cleanAndCreateThreeLittlePigs();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 12, 6, 13, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 12, 6, 15, 0));
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("importMeetingWithResourceOrganizer");
        vevent = vevent.withOrganizer(resourceRoutines.getExchangeEmail(organizerResource));
        vevent = vevent.addAttendee(creator.getEmail());

        IcsImportStats stats = icsImporter.importIcsStuff(creator.getUid(), vevent.makeCalendar(), IcsImportMode.importFile(LayerReference.defaultLayer()));
        assertThat(stats.getTotalCount()).isEqualTo(1);
        assertThat(stats.getIgnoredEventCount()).isEqualTo(1);
    }

    @Test
    public void importMeetingWithAnotherUserOrganizer() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10181");
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10182");

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 12, 6, 13, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 12, 6, 15, 0));
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("importMeetingWithAnotherUserOrganizer");
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(creator.getEmail());

        IcsImportStats stats = icsImporter.importIcsStuff(
                creator.getUid(), vevent.makeCalendar(), IcsImportMode.importFile(LayerReference.defaultLayer()));
        long eventId = stats.getNewEventIds().single();

        EventWithRelations eventWithRelations = eventDbManager.getEventWithRelationsById(eventId);
        Assert.equals(creator.getUid(), eventWithRelations.getEvent().getCreatorUid());
        Assert.equals(ParticipantId.yandexUid(organizer.getUid()),
                eventWithRelations.getParticipants().getOrganizer().getId());
    }

    @Test
    public void initialStateOfSequencesAndDtstampsByOrganizer() {
        initialStateOfSequencesAndDtstamps(true);
    }

    @Test
    public void initialStateOfSequencesAndDtstampsByAttendee() {
        initialStateOfSequencesAndDtstamps(false);
    }

    private void initialStateOfSequencesAndDtstamps(boolean isByOrganizer) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10171");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-10172");
        TestUserInfo attendee2 = testManager.prepareUser("yandex-team-mm-10173");

        IcsVEvent vevent = new IcsVEvent();
        String extId = CalendarUtils.generateExternalId();
        vevent = vevent.withUid(extId);
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee1.getEmail());
        vevent = vevent.addAttendee(attendee2.getEmail());
        vevent = vevent.withDtStart(TestDateTimes.moscow(2011, 7, 5, 20, 45));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2011, 7, 5, 21, 0));
        PassportUid actorUid = isByOrganizer ? organizer.getUid() : attendee1.getUid();
        IcsImportMode mode = IcsImportMode.incomingEmailFromMailhook()
                .withActionInfoFreezedNowForTest();
        icsImporter.importIcsStuff(actorUid, vevent.makeCalendar(), mode);
        vevent = vevent.withSummary("initialStateOfSequencesAndDtstamps(" + isByOrganizer + ")");
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(TestDateTimes.moscow(2011, 7, 5, 20, 40));

        Event event = eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(actorUid), extId).get();

        testStatusChecker.checkMainEventLastUpdateIsUpdated(event, mode.getActionInfo());
        testStatusChecker.checkEventLastUpdateIsSetOrUpdatedToRequestNow(event.getId(), mode.getActionInfo());

        for (TestUserInfo info : Cf.list(organizer, attendee1, attendee2)) {
            Option<EventUser> eventUserO = eventRoutines.findEventUser(info.getUid(), event.getId());
            Assert.A.some(eventUserO, "event user for " + info.getUid() + " not found");

            testStatusChecker.checkUserSequenceAndDtStampAreInInitialState(eventUserO.get());
        }
    }

    @Test
    // https://jira.yandex-team.ru/browse/CAL-4274
    public void importExistingMeetingToSpecifiedCalendar() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10174");
        TestUserInfo importer = testManager.prepareUser("yandex-team-mm-10175");

        long creatorDefaultLayerId = creator.getDefaultLayerId();
        long importerDefaultLayerId = importer.getDefaultLayerId();
        long importerNonDefaultLayerId = layerRoutines.createUserLayer(importer.getUid());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(creator.getEmail());
        vevent = vevent.addAttendee(creator.getEmail());
        vevent = vevent.withDtStart(TestDateTimes.moscow(2011, 7, 5, 0, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2011, 7, 5, 1, 0));

        IcsCalendar calendar = vevent.makeCalendar();

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        IcsImportStats stats = icsImporter.importIcsStuff(creator.getUid(), calendar, mode);
        long eventId = stats.getNewEventIds().single();

        icsImporter.importIcsStuff(importer.getUid(), calendar, IcsImportMode.caldavPut(importerNonDefaultLayerId, Option.empty()));

        SetF<Long> actualLayerIds = layerDao.findLayersByEventIds(Cf.list(eventId)).get1().map(Layer.getIdF()).unique();
        Assert.equals(Cf.set(importerNonDefaultLayerId, creatorDefaultLayerId), actualLayerIds);

        icsImporter.importIcsStuff(importer.getUid(), calendar, IcsImportMode.caldavPut(importerDefaultLayerId, Option.empty()));

        actualLayerIds = layerDao.findLayersByEventIds(Cf.list(eventId)).get1().map(Layer.getIdF()).unique();
        Assert.equals(Cf.set(importerDefaultLayerId, creatorDefaultLayerId), actualLayerIds);
    }

    @Test
    public void importMeetingWithResourceConflict() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10191");
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        Instant start = TestDateTimes.utc(2012, 12, 20, 22, 0);
        Instant end = start.plus(Duration.standardHours(1));

        Event event = testManager.createDefaultEvent(organizer.getUid(), "some event", start, end);
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);
        testManager.updateEventTimeIndents(event);

        IcsVEvent conflict = new IcsVEvent();
        conflict = conflict.withSummary("another event with same resource and time");
        conflict = conflict.withUid(CalendarUtils.generateExternalId());
        conflict = conflict.withOrganizer(organizer.getEmail());
        conflict = conflict.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        conflict = conflict.addAttendee(TestManager.testExchangeThreeLittlePigsEmail, IcsPartStat.ACCEPTED);
        conflict = conflict.withDtStart(start);
        conflict = conflict.withDtEnd(end);

        try {
            icsImporter.importIcsStuff(organizer.getUid(), conflict.makeCalendar(),
                    IcsImportMode.caldavPutToDefaultLayerForTest(start.minus(777)));

            Assert.fail("CommandRunException should be thrown");
        } catch (CommandRunException ex) {
            Assert.some(Situation.BUSY_OVERLAP, ex.getSituation());
        }
    }

    // CAL-5996, CAL-6343
    @Test
    public void defaultIcsAvailability() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(926);
        TestUserInfo notAttendee = testManager.prepareRandomYaTeamUser(927);

        settingsRoutines.saveEmptySettingsForUid(notAttendee.getUid());
        SettingsYt data = new SettingsYt();
        data.setDefaultIcsAvailability(Availability.MAYBE);
        settingsRoutines.updateSettingsYtByUid(data, notAttendee.getUid());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(new Email("top@yandex-team.ru"));

        vevent = vevent.withSummary("User subscribed and wants to be maybe busy!");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2013, 2, 4, 22, 30));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2013, 2, 4, 23, 0));

        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                notAttendee.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        long createdEventId = icsImportStats.getNewEventIds().single();
        EventUser eu = eventUserDao.findEventUserByEventIdAndUid(createdEventId, notAttendee.getUid()).get();

        Assert.isFalse(eu.getIsAttendee());
        Assert.equals(Availability.MAYBE, eu.getAvailability());
        Assert.equals(Decision.YES, eu.getDecision());
    }

    @Test
    public void nonAttendeeBecomesUndecided() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12222");

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Single");
        vevent = vevent.withDtStart(MoscowTime.instant(2013, 5, 22, 21, 30));
        vevent = vevent.withDtEnd(MoscowTime.instant(2013, 5, 22, 22, 30));
        vevent = vevent.withSequenece(0);

        SetF<Long> newEventIds = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(),
                IcsImportMode.mailWidget(user.getUid(),
                        ActionInfo.webTest().withActionSource(ActionSource.MAIL))).getNewEventIds();

        Assert.hasSize(1, newEventIds);
        Assert.some(Decision.UNDECIDED, eventUserRoutines.findEventUserDecision(user.getUid(), newEventIds.single()));

        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(new Email("organizer@somewhere.com"));
        vevent = vevent.addAttendee(new Email("attendee@somewhere.com"));

        newEventIds = icsImporter.importIcsStuff(user.getUid(), vevent.makeCalendar(),
                IcsImportMode.mailWidget(user.getUid(),
                        ActionInfo.webTest().withActionSource(ActionSource.MAIL))).getNewEventIds();

        Assert.hasSize(1, newEventIds);
        Assert.some(Decision.UNDECIDED, eventUserRoutines.findEventUserDecision(user.getUid(), newEventIds.single()));
    }

} //~
