package ru.yandex.calendar.logic.ics.imp;

import net.fortuna.ical4j.model.Property;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1B;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.ExternalId;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVAlarm;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsValue;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsExDate;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsLocation;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTrigger;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.SendingSmsDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.schedule.ResourceDaySchedule;
import ru.yandex.calendar.logic.resource.schedule.ResourceScheduleManager;
import ru.yandex.calendar.logic.sending.param.CancelEventMessageParameters;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.param.InvitationMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class IcsImporterOrganizerUpdatesMeetingTest extends AbstractConfTest {
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private ResourceScheduleManager resourceScheduleManager;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private SendingSmsDao sendingSmsDao;
    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;

    @Test
    public void organizerRemovesAttendeesFromExistingEvent() { // test 14
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12801");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-12802");
        TestUserInfo attendee2 = testManager.prepareUser("yandex-team-mm-12803");
        TestUserInfo attendee3 = testManager.prepareUser("yandex-team-mm-12804");
        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 12, 1, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "All attendees will be deleted");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee1.getUid(), Decision.MAYBE, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee2.getUid(), Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee3.getUid(), Decision.YES, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow(); // XXX: must not use now
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.withSummary("No attendees now, only organizer");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(7); // > 0

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), importMode);

        EventUser eventUser1 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), organizer.getUid()).get();
        eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), organizer.getUid()).single();

        EventUser eventUser2 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee1.getUid()).get();
        Assert.A.hasSize(0, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), attendee1.getUid()));

        EventUser eventUser3 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee2.getUid()).get();
        Assert.A.hasSize(0, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), attendee2.getUid()));

        EventUser eventUser4 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee3.getUid()).get();
        Assert.A.hasSize(0, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), attendee3.getUid()));

        // it is not clear, whether eventUser1 should be organizer or not
        Assert.A.isFalse(eventUser1.getIsOrganizer());
        Assert.A.isFalse(eventUser1.getIsAttendee());

        Assert.A.isFalse(eventUser2.getIsAttendee());
        Assert.A.isFalse(eventUser3.getIsAttendee());
        Assert.A.isFalse(eventUser4.getIsAttendee());

        Option<ParticipantInfo> att4Invitation =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                        event.getId(), ParticipantId.yandexUid(attendee3.getUid()));
        Assert.assertEmpty(att4Invitation);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizer.getDefaultLayerId(),
                attendee1.getDefaultLayerId(), attendee2.getDefaultLayerId(), attendee3.getDefaultLayerId()),
                importMode.getActionInfo().getNow());
    }

    @Test
    public void organizerInvites() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12815");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12816");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "organizerInvites");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStamp(beforeNow);
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(7);

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), importMode);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());
    }

    @Test
    public void organizerWithInvitationButNotPrimaryLayerOwnerInvites() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(7324);
        TestUserInfo guest = testManager.prepareRandomYaTeamUser(6324);
        TestUserInfo primaryLayerOwner = testManager.prepareRandomYaTeamUser(5324);

        Event event = testManager.createDefaultEvent(creator.getUid(), "event");
        testManager.createEventUser(creator.getUid(), event.getId(), Decision.YES, Option.of(true));

        testManager.createEventLayer(creator.getDefaultLayerId(), event.getId());
        testManager.createEventLayer(primaryLayerOwner.getDefaultLayerId(), event.getId(), true);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(userManager.getEmailByUid(creator.getUid()).get());
        vevent = vevent.addProperty(new IcsAttendee(userManager.getEmailByUid(guest.getUid()).get(), IcsPartStat.ACCEPTED));
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);

        IcsCalendar calendar = new IcsCalendar(Cf.list(vevent));
        icsImporter.importIcsStuff(creator.getUid(), calendar, IcsImportMode.importFile(LayerReference.defaultLayer()));

        final EventUser guestEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), guest.getUid()).get();
        Assert.assertTrue(guestEventUser.getIsAttendee());
    }

    @Test
    public void organizerWithoutEventLayerUpdatesEvent() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(7524);
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(6624);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "event");
        testManager.createEventUser(organizer.getUid(), event.getId(), Decision.YES, Option.of(true));
        event.setCreatorUid(creator.getUid());
        eventDao.updateEvent(event);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(userManager.getEmailByUid(organizer.getUid()).get());
        vevent = vevent.withSummary("new event name");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);

        IcsCalendar calendar = new IcsCalendar(Cf.list(vevent));
        icsImporter.importIcsStuff(organizer.getUid(), calendar, IcsImportMode.importFile(LayerReference.defaultLayer()));
    }

    // http://calendar-web.calendar-back01e.tools.yandex.net:81/z/event?q=723  (but organizer is user, not a resource here)
    @Test
    public void organizerUpdatesEventCreatedByAttendee() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12811");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12812");

        IcsImportMode importMode = IcsImportMode
                .importFile(LayerReference.defaultLayer(), TestDateTimes.moscow(2011, 11, 21, 21, 28))
                .withActionInfoFreezedNowForTest();
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);


        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(attendee.getUid(), "organizerUpdatesEventCreatedByAttendee");
        String externalId = eventDao.findExternalIdByEventId(event.getId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.withSummary("organizerUpdatesEventCreatedByAttendee - updated");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);

        IcsCalendar calendar = vevent.makeCalendar();
        icsImporter.importIcsStuff(organizer.getUid(), calendar, importMode);

        Assert.A.equals(1, mainEventDao.findCountOfMainIds(new ExternalId(externalId)));


        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());
    }

    @Test
    public void locationNotUpdatedIfConsistsOfResourcesNames() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(9876);

        Event data = new Event();
        data.setLocation("Text location");
        Event event = testManager.createDefaultEvent(creator.getUid(), "event", data);

        Resource abc = testManager.cleanAndCreateResource("acb", "abc") ;
        Resource def = testManager.cleanAndCreateResource("def", "def") ;
        testManager.addUserParticipantToEvent(event.getId(), creator, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), abc);
        testManager.addResourceParticipantToEvent(event.getId(), def);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(userManager.getEmailByUid(creator.getUid()).get());
        vevent = vevent.addAttendee(ResourceRoutines.getResourceEmail(abc));
        vevent = vevent.withSummary("new event name");
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.addProperty(new IcsLocation("abc, def"));

        IcsCalendar calendar = new IcsCalendar(Cf.list(vevent));
        IcsImportMode importMode = IcsImportMode.importFile(LayerReference.defaultLayer());

        IcsImportStats stats = passportAuthDomainsHolder.withDomainsForTest("public",
                () -> icsImporter.importIcsStuff(creator.getUid(), calendar, importMode));
        Assert.equals(Cf.set(event.getId()), stats.getUpdatedEventIds());

        Assert.equals(data.getLocation(), eventDao.findEventById(event.getId()).getLocation());

        passportAuthDomainsHolder.withDomainsForTest("yt",
                () -> icsImporter.importIcsStuff(creator.getUid(), calendar, importMode));

        Assert.equals("", eventDao.findEventById(event.getId()).getLocation());
    }

    @Test
    public void sendEmailsOnUpdateFromCaldav() {
        emailsOnUpdate(IcsImportMode.caldavPutToDefaultLayerForTest());
    }

    @Test
    public void dontSendEmailsOnUpdateFromWebIcs() {
        emailsOnUpdate(IcsImportMode.importFile(LayerReference.defaultLayer()));
    }

    private void emailsOnUpdate(IcsImportMode icsImportMode) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12821");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-12822");
        TestUserInfo attendee2 = testManager.prepareUser("yandex-team-mm-12823");

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        Event event = testManager.createDefaultEvent(organizer.getUid(), "emailsOnUpdate from " + icsImportMode);
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee1.getUid(), Decision.MAYBE, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee2.getUid(), Decision.MAYBE, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee1.getEmail());
        vevent = vevent.addAttendee(attendee2.getEmail());
        vevent = vevent.withDtStart(event.getStartTs().plus(100500));
        vevent = vevent.withDtEnd(event.getEndTs().plus(100500));
        vevent = vevent.withSequenece(1);

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), icsImportMode);

        ListF<InvitationMessageParameters> invitationss = mailSenderMock.getInvitationMessageParameterss();
        Assert.sameSize(invitationss, mailSenderMock.getEventMessageParameters());

        if (icsImportMode.getActionSource() == ActionSource.CALDAV) {
            ListF<PassportUid> notifiedUids = invitationss.filterMap(InvitationMessageParameters.getRecipientUidF());

            Assert.hasSize(2, notifiedUids);
            Assert.equals(Cf.set(attendee1.getUid(), attendee2.getUid()), notifiedUids.unique());
        } else {
            Assert.isEmpty(invitationss);
        }
    }

    @Test
    // https://jira.yandex-team.ru/browse/CAL-4417
    public void dontSendEmailsOnUpdateNotification() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12821");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12822");

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        Event event = testManager.createDefaultEvent(organizer.getUid(), "emailsOnNotChangedEvent");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary("emailsOnNotChangedEvent");
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);

        IcsCalendar firstCalendar = vevent
                .withDtStamp(Instant.now())
                .makeCalendar()
                .addComponent(new IcsVAlarm().withTrigger(new IcsTrigger("-PT30M", Cf.list(IcsValue.DURATION))));
        IcsCalendar secondCalendar = vevent
                .withDtStamp(Instant.now().plus(77))
                .makeCalendar()
                .addComponent(new IcsVAlarm().withTrigger(new IcsTrigger("-PT15M", Cf.list(IcsValue.DURATION))));

        icsImporter.importIcsStuff(organizer.getUid(), firstCalendar, IcsImportMode.caldavPutToDefaultLayerForTest());
        ((MailSenderMock) mailSender).clear();
        icsImporter.importIcsStuff(organizer.getUid(), secondCalendar, IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.isEmpty(((MailSenderMock) mailSender).getInvitationMessageParameterss());
    }

    @Test
    public void invitationForNewParticipantFromCaldav() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12831");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-12832");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendee1Layer = testManager.createDefaultLayerForUser(attendee1.getUid(), beforeNow);

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        Event event = testManager.createDefaultEvent(organizer.getUid(), "invitationForNewParticipantFromCaldav");

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());
        vevent = vevent.addAttendee(attendee1.getEmail());
        vevent = vevent.withDtStart(event.getStartTs().plus(100500));
        vevent = vevent.withDtEnd(event.getEndTs().plus(100500));
        vevent = vevent.withSequenece(1);

        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                organizer.getUid(), vevent.makeCalendar(), importMode);

        ListF<InvitationMessageParameters> invitationss = mailSenderMock.getInvitationMessageParameterss();
        Assert.equals(attendee1.getUid(), invitationss.filterMap(InvitationMessageParameters.getRecipientUidF()).single());

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendee1Layer), importMode.getActionInfo().getNow());
    }

    @Test
    public void deletingOneRecurrenceAndCreatingAnotherPreservesMainEvent() {
        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12321");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12322");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "importingOneRecurrenceDoesNotDeleteOthers");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        Event tomorrowRecurrence = TestManager.createRecurrence(event, Duration.standardDays(1), "tomorrow recurrence");
        long tomorrowRecurrenceId = eventDao.saveEvent(tomorrowRecurrence, ActionInfo.webTest());

        testManager.addUserParticipantToEvent(tomorrowRecurrenceId, organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(tomorrowRecurrenceId, attendee.getUid(), Decision.UNDECIDED, false);

        eventDbManager.deleteEventsByIds(Cf.list(event.getId()), true, ActionInfo.webTest());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        Instant dayAfterTomorrowRecurrenceStart = event.getStartTs().plus(Duration.standardDays(2));

        IcsVEvent dayAfterTomorrowRecurrence = new IcsVEvent();
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withUid(externalId);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtStart(dayAfterTomorrowRecurrenceStart);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtEnd(dayAfterTomorrowRecurrenceStart.plus(Duration.standardHours(1)));
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withOrganizer(organizer.getEmail());
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withSummary("day after tomorrow recurrence");
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.addProperty(new IcsRRule("FREQ=DAILY"));
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withRecurrence(dayAfterTomorrowRecurrenceStart);

        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtStart(dayAfterTomorrowRecurrenceStart);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtEnd(dayAfterTomorrowRecurrenceStart.plus(new Duration(event.getStartTs(), event.getEndTs())));
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.removeProperties(Property.RRULE);

        IcsImportStats stats = icsImporter.importIcsStuff(organizer.getUid(), new IcsCalendar().addComponent(dayAfterTomorrowRecurrence),
                IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.A.equals(tomorrowRecurrence.getMainEventId(), eventDao.findEventById(stats.getNewEventIds().single()).getMainEventId());

        try {
            Event deletedTomorrowRecurrence = eventDao.findEventById(tomorrowRecurrenceId);
            Assert.fail("recurrence must be deleted");
        } catch (EmptyResultDataAccessException e) {
        }

    }

    @Test
    public void deleteRecurrenceAndOccurrence() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12841);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(12842);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "emailsAboutDeletedRecurrence");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Event recurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(2)));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.saveEventNotifications(attendee.getUid(), recurrence.getId(), Notification.sms(Duration.ZERO));

        String externalId = mainEventDao.findExternalIdByMainEventId(master.getMainEventId());
        Instant occurrenceStart = master.getStartTs().plus(Duration.standardDays(4));

        IcsVEvent extated = new IcsVEvent();
        extated = extated.withUid(externalId);
        extated = extated.withDtStart(master.getStartTs());
        extated = extated.withDtEnd(master.getEndTs());
        extated = extated.withOrganizer(organizer.getEmail());
        extated = extated.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        extated = extated.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        extated = extated.withSummary("emailsAboutDeletedRecurrence");
        extated = extated.addProperty(new IcsRRule("FREQ=DAILY"));

        extated = extated.addProperty(new IcsExDate(recurrence.getStartTs()));
        extated = extated.addProperty(new IcsExDate(occurrenceStart));

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;

        sendingSmsDao.deleteByUid(attendee.getUid());
        mailSenderMock.clear();
        icsImporter.importIcsStuff(
                organizer.getUid(), extated.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest(recurrence.getStartTs().minus(88888)));

        Assert.forAll(mailSenderMock.getEventMessageParameters(), Function1B.instanceOfF(CancelEventMessageParameters.class));
        Assert.assertListsEqual(
                Cf.list(master.getId(), recurrence.getId()),
                mailSenderMock.getCancelEventMessageParameterss().map(EventMessageParameters.getEventIdF()));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));
    }

    @Test
    public void changeTimeOfEventWithRecurrenceAndExdate() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12841);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(12842);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "changeTimeOfEventWithRecurrenceAndExdate");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.saveEventNotifications(attendee.getUid(), master.getId(), Notification.sms(Duration.ZERO));

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Event recurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(2)));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        Instant exdate = master.getStartTs().plus(Duration.standardDays(4));
        testManager.createExdate(exdate, master.getId());

        IcsVEvent base = new IcsVEvent();
        base = base.withUid(mainEventDao.findExternalIdByMainEventId(master.getMainEventId()));
        base = base.withOrganizer(organizer.getEmail());
        base = base.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        base = base.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);

        IcsVEvent vmaster = base.withSummary(master.getName());
        vmaster = vmaster.addProperty(new IcsRRule("FREQ=DAILY"));
        vmaster = vmaster.withDtStart(master.getStartTs().plus(Duration.standardHours(1)));
        vmaster = vmaster.withDtEnd(master.getEndTs().plus(Duration.standardHours(1)));

        vmaster = vmaster.addProperty(new IcsExDate(exdate));

        IcsVEvent vrecurrence = base.withSummary(recurrence.getName());
        vrecurrence = vrecurrence.withRecurrence(recurrence.getRecurrenceId().get().plus(Duration.standardHours(1)));
        vrecurrence = vrecurrence.withDtStart(recurrence.getStartTs());
        vrecurrence = vrecurrence.withDtEnd(recurrence.getEndTs());

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;

        sendingSmsDao.deleteByUid(attendee.getUid());

        mailSenderMock.clear();
        icsImporter.importIcsStuff(organizer.getUid(),
                new IcsCalendar(Cf.list(vmaster, vrecurrence)),
                IcsImportMode.caldavPutToDefaultLayerForTest(master.getStartTs()));

        Assert.hasSize(1, mailSenderMock.getEventMessageParameters());
        Assert.isTrue(mailSenderMock.getEventMessageParameters().single() instanceof InvitationMessageParameters);
        Assert.equals(master.getId(), mailSenderMock.getEventMessageParameters().single().getEventId());
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));
    }

    // needed for importing ics from mail
    @Test
    public void organizerImportsReplyFromAttendee() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12841);
        TestUserInfo attendee = testManager.prepareRandomYaTeamSuperUser(12842);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "organizerImportsReplyFromAttendee");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());

        icsImporter.importIcsStuff(
                organizer.getUid(), vevent.makeCalendar().withMethod(IcsMethod.REPLY), IcsImportMode.caldavPutToDefaultLayerForTest());

        EventUser attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();
        Assert.A.equals(Decision.YES, attendeeEventUser.getDecision());
        Assert.A.isTrue(attendeeEventUser.getIsAttendee());
    }

    @Test
    public void resourceScheduleCacheInvalidatedOnUpdate() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(9879);

        Event event = testManager.createDefaultEvent(user.getUid(), "resourceScheduleCacheInvalidatedOnUpdate");
        Resource smolny = testManager.cleanAndCreateSmolny() ;

        testManager.addResourceParticipantToEvent(event.getId(), smolny);
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        DateTimeZone tz = DateTimeZone.forID(settingsRoutines.getTimeZoneJavaId(user.getUid()));

        // put to cache
        resourceScheduleManager.getResourceScheduleDataForDays(
                Option.empty(), Cf.list(smolny.getId()),
                Cf.list(event.getStartTs().toDateTime(tz).toLocalDate()), tz, Option.empty(), ActionInfo.webTest());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(user.getEmail());
        vevent = vevent.addAttendee(ResourceRoutines.getResourceEmail(smolny), IcsPartStat.ACCEPTED);
        vevent = vevent.withSummary("resourceScheduleCacheInvalidatedOnUpdate");
        vevent = vevent.withDtStart(event.getStartTs().plus(Duration.standardHours(1)));
        vevent = vevent.withDtEnd(event.getEndTs().plus(Duration.standardHours(1)));
        vevent = vevent.withSequenece(1);

        IcsCalendar calendar = new IcsCalendar(Cf.list(vevent));
        icsImporter.importIcsStuff(user.getUid(), calendar, IcsImportMode.importFile(LayerReference.defaultLayer()));

        ListF<ResourceDaySchedule> schedules = resourceScheduleManager.getResourceScheduleDataForDays(
                Option.empty(), Cf.list(smolny.getId()),
                Cf.list(event.getStartTs().toDateTime(tz).toLocalDate()), tz, Option.empty(), ActionInfo.webTest());

        InstantInterval eventInSmolny = schedules.single().getSchedule().getInstantIntervals().single();

        IcsVTimeZones tzs = IcsVTimeZones.fallback(tz);
        Assert.A.equals(vevent.getStart(tzs), eventInSmolny.getStart());
        Assert.A.equals(vevent.getEnd(tzs), eventInSmolny.getEnd());
    }

    @Test
    public void updateMeetingWithResourceConflict() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12851");
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        Instant start = TestDateTimes.utc(2012, 12, 20, 22, 0);
        Instant end = start.plus(Duration.standardHours(1));

        Event event = testManager.createDefaultEvent(organizer.getUid(), "some event", start, end);
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        Event nextEvent = testManager.createDefaultEvent(organizer.getUid(), "next event", end, end.plus(7777));
        testManager.addUserParticipantToEvent(nextEvent.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(nextEvent.getId(), resource);

        testManager.updateEventTimeIndents(event, nextEvent);

        IcsVEvent conflict = new IcsVEvent();
        conflict = conflict.withDtStart(start);
        conflict = conflict.withDtEnd(end);
        conflict = conflict.withSummary("conflict");
        conflict = conflict.withUid(eventDao.findExternalIdByEventId(nextEvent.getId()));
        conflict = conflict.withOrganizer(organizer.getEmail());
        conflict = conflict.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        conflict = conflict.addAttendee(TestManager.testExchangeThreeLittlePigsEmail, IcsPartStat.ACCEPTED);
        conflict = conflict.withSequenece(nextEvent.getSequence() + 1);

        try {
            icsImporter.importIcsStuff(organizer.getUid(), conflict.makeCalendar(),
                    IcsImportMode.caldavPutToDefaultLayerForTest(start.minus(777)));

            Assert.fail("CommandRunException should be thrown");
        } catch (CommandRunException ex) {
            Assert.some(Situation.BUSY_OVERLAP, ex.getSituation());
        }
    }

    // CAL-9492
    @Test
    public void yamoneyGap() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(11611);

        DateTime start = MoscowTime.dateTime(2017, 4, 5, 21, 45);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail());

        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withSummary("Yamoney gap");
        vevent = vevent.withDtStart(start);
        vevent = vevent.withDtEnd(start.plusHours(1));

        icsImporter.importIcsFromMailhook(organizer.getUid(), Option.empty(),
                Cf.list(organizer.getParticipantId()), vevent.makeCalendar().withMethod(IcsMethod.REQUEST));

        long mainId = mainEventDao.findMainEventsByExternalId(new ExternalId(vevent.getUid().get())).single().getId();
        long eventId = eventDao.findEventsByMainId(mainId).single().getId();

        Assert.some(Decision.YES, eventUserDao.findEventUserByEventIdAndUid(
                eventId, organizer.getUid()).map(EventUser::getDecision));

        vevent = vevent.withDtStamp(Instant.now().plus(Duration.standardMinutes(1)));

        icsImporter.importIcsFromMailhook(organizer.getUid(), Option.empty(),
                Cf.list(organizer.getParticipantId()), vevent.makeCalendar().withMethod(IcsMethod.CANCEL));

        Assert.some(Decision.NO, eventUserDao.findEventUserByEventIdAndUid(
                eventId, organizer.getUid()).map(EventUser::getDecision));
    }

} //~
