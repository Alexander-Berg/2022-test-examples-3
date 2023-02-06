package ru.yandex.calendar.logic.ics.imp;

import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Minutes;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsOrganizer;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class IcsImporterNonparticipantUpdatesMeetingTest extends AbstractConfTest {
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;

    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventUserRoutines eventUserRoutines;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private EventRoutines eventRoutines;

    @Before
    public void setup() {
        mailSender.clear();
    }

    @Test
    public void uninvitedUserImportsIcsWhereHeIsNotAttendee() {
        PassportLogin organizer = new PassportLogin("yandex-team-mm-10301");
        PassportLogin invitedGuest = new PassportLogin("yandex-team-mm-10302");
        PassportLogin uninvitedImporter = new PassportLogin("yandex-team-mm-10303");

        PassportUid organizerUid = userManager.getUidByLoginForTest(organizer);
        PassportUid uninvitedImporterUid = userManager.getUidByLoginForTest(uninvitedImporter);

        testManager.cleanUser(organizer);
        testManager.cleanUser(invitedGuest);
        testManager.cleanUser(uninvitedImporter);

        Instant now = TestDateTimes.moscow(2021, 1, 14, 20, 57);
        Event event = testManager.createDefaultMeeting(organizerUid, "Квартальная презентация");
        event.setStartTs(now.plus(Duration.standardMinutes(45)));
        event.setEndTs(now.plus(Duration.standardHours(1)));

        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), invitedGuest, Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.withOrganizer(new Email(organizer.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addAttendee(new Email(invitedGuest.getNormalizedValue() + "@yandex.ru"), IcsPartStat.TENTATIVE);

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(uninvitedImporterUid, calendar, IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.A.isTrue(icsImportStats.getNewEventIds().isEmpty());

        Assert.A.equals(event.getId(), icsImportStats.getUpdatedEventIds().single());

        ListF<EventLayerWithRelations> eventLayers = eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), uninvitedImporterUid);
        Assert.A.hasSize(1, eventLayers);

        Option<EventUser> eventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), uninvitedImporterUid);
        Assert.A.isFalse(eventUser.get().getIsAttendee());
        Assert.A.isFalse(eventUser.get().getIsOrganizer());

        testStatusChecker.checkEventLastUpdateIsPreserved(event);

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void uninvitedUserJoinsMeeting() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10311");
        TestUserInfo uninvitedGuest = testManager.prepareUser("yandex-team-mm-10312");

        Instant now = TestDateTimes.moscow(2021, 1, 14, 20, 57);
        Event event = testManager.createDefaultEvent(organizer.getUid(), "testInternal");
        event.setStartTs(now.plus(Duration.standardHours(1)));
        event.setEndTs(now.plus(Duration.standardHours(2)));
        testManager.addUserParticipantToEvent(
                event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addExternalUserParticipantToEvent(
                event.getId(), new Email("sanek-tester@yandex.ru"), Decision.UNDECIDED, false);
        testManager.addExternalUserParticipantToEvent(
                event.getId(), new Email("akirakozov@yandex-team.ru"), Decision.UNDECIDED, false);

        eventUserDao.updateEventUserSetOrganizerByEventIdAndUid(event.getId(), organizer.getUid(), true, ActionInfo.webTest());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addProperty(new IcsAttendee(uninvitedGuest.getEmail()));
        vevent = vevent.withSequenece(1);

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REQUEST);
        IcsImportStats stats = icsImporter.importIcsStuff(uninvitedGuest.getUid(), calendar,
                IcsImportMode.incomingEmailFromMailhook(now).withActionInfoFreezedNowForTest());
        Assert.A.hasSize(0, stats.getNewEventIds());

        eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(uninvitedGuest.getUid())).get();

        eventUserDao.findEventUserByEventIdAndUid(event.getId(), uninvitedGuest.getUid());
        Assert.A.hasSize(1, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), uninvitedGuest.getUid()));

        Participants participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        Assert.assertContains(participants.getParticipants().map(ParticipantInfo.getIdF()), ParticipantId.yandexUid(uninvitedGuest.getUid()));

        testStatusChecker.checkEventLastUpdateIsPreserved(event);

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void userImportsMeetingCloneWithAnotherUidAndResourceConflict() { // Lightning mode
        PassportLogin importer = new PassportLogin("yandex-team-mm-10321");
        PassportLogin organizer = new PassportLogin("yandex-team-mm-10322");

        PassportUid importerUid = userManager.getUidByLoginForTest(importer);
        PassportUid organizerUid = userManager.getUidByLoginForTest(organizer);

        testManager.cleanUser(importerUid);
        testManager.cleanUser(organizerUid);

        Resource threeLittlePigs = testManager.cleanAndCreateThreeLittlePigs();

        Instant start = new DateTime(2021, 1, 19, 1, 9, 0, 0, dateTimeManager.getTimeZoneForUid(importerUid)).toInstant();

        Event meeting = new Event();
        meeting.setType(EventType.USER);
        meeting.setStartTs(start);
        meeting.setEndTs(start.plus(Duration.standardHours(1)));
        meeting = testManager.createDefaultEvent(organizerUid, "Квартальная презентация", meeting);

        long meetingId = meeting.getId();
        testManager.addResourceParticipantToEvent(meetingId, threeLittlePigs);
        testManager.addUserParticipantToEvent(meetingId, organizer, Decision.UNDECIDED, true);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid("not_" + eventDao.findExternalIdByEventId(meetingId));
        vevent = vevent.withSummary(meeting.getName());
        vevent = vevent.withDtStart(meeting.getStartTs());
        vevent = vevent.withDtEnd(meeting.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.withOrganizer(new Email(organizer.getNormalizedValue() + "@yandex-team.ru"));
        vevent = vevent.addProperty(new IcsAttendee(ResourceRoutines.getResourceEmail(threeLittlePigs), IcsPartStat.ACCEPTED));

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                importerUid, calendar, IcsImportMode.importFile(
                        LayerReference.defaultLayer(),
                        start.minus(Duration.standardHours(1))).withActionInfoFreezedNowForTest());

        Assert.A.hasSize(1, icsImportStats.getNewEventIds());

        ListF<EventLayerWithRelations> eventLayers = eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(meetingId, importerUid);
        Assert.A.hasSize(0, eventLayers);

        Option<EventUser> eventUser = eventUserDao.findEventUserByEventIdAndUid(meetingId, importerUid);
        Assert.A.none(eventUser);

        testStatusChecker.checkEventLastUpdateIsPreserved(meeting);

        long importedEventId = icsImportStats.getNewEventIds().single();
        Event importedEvent = eventDao.findEventById(importedEventId);

        Assert.notEquals(meeting.getMainEventId(), importedEvent.getMainEventId());
        Assert.equals(layerRoutines.getDefaultLayerId(importerUid).get(),
                eventLayerDao.findEventLayersByEventId(importedEventId).single().getLayerId());
        Assert.equals(importerUid,
                eventUserDao.findEventUsersByEventId(importedEventId).single().getUid());

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void userWithEventUserAndWithoutEventLayerInportsIcsWhereHeIsNotAttendee() {
        PassportLogin organizer = new PassportLogin("yandex-team-mm-10331");
        PassportLogin user = new PassportLogin("yandex-team-mm-10332");

        PassportUid organizerUid = userManager.getUidByLoginForTest(organizer);
        PassportUid userUid = userManager.getUidByLoginForTest(user);

        testManager.cleanUser(organizer);
        testManager.cleanUser(user);

        Instant now = TestDateTimes.moscow(2021, 1, 14, 20, 57);
        Event event = testManager.createDefaultMeeting(organizerUid, "Квартальная презентация");
        event.setStartTs(now.plus(Duration.standardHours(3)));
        event.setEndTs(now.plus(Duration.standardHours(4)));

        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.UNDECIDED, true);

        eventUserRoutines.saveEventUser(userUid, event.getId(), new EventUser(), Cf.<Notification>list(),
                new ActionInfo(ActionSource.UNKNOWN, "test", new Instant()));

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.withOrganizer(new Email(organizer.getNormalizedValue() + "@yandex.ru"));

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportStats icsImportStats = icsImporter.importIcsStuff(userUid, calendar, IcsImportMode.importFile(LayerReference.defaultLayer()));

        Assert.A.isTrue(icsImportStats.getNewEventIds().isEmpty());

        Assert.A.equals(event.getId(), icsImportStats.getUpdatedEventIds().single());

        ListF<EventLayerWithRelations> eventLayers = eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), userUid);
        Assert.A.hasSize(1, eventLayers);

        Option<EventUser> eventUserAfterImport = eventUserDao.findEventUserByEventIdAndUid(event.getId(), userUid);
        Assert.A.isFalse(eventUserAfterImport.get().getIsAttendee());
        Assert.A.isFalse(eventUserAfterImport.get().getIsOrganizer());

        testStatusChecker.checkEventLastUpdateIsPreserved(event);

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    /**
     * @see IcsImporterAttendeeUpdatesMeetingTest#attendeeImportsStaleEventAndChangesAreIgnored()
     */
    @Test
    public void uninvitedUserImportsStaleEventAndBecomesParticipant() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10341");
        TestUserInfo uninvitedGuest = testManager.prepareUser("yandex-team-mm-10342");

        Instant now = TestDateTimes.moscow(2021, 1, 14, 20, 57);
        Event event = testManager.createDefaultEventWithEventLayerAndEventUserInFuture(organizer.getUid(), "testInternal");

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(event.getLastUpdateTs().minus(Duration.standardHours(1)));

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REQUEST);
        IcsImportStats stats = icsImporter.importIcsStuff(uninvitedGuest.getUid(), calendar,
                IcsImportMode.incomingEmailFromMailhook(now).withActionInfoFreezedNowForTest());

        Assert.A.hasSize(0, stats.getNewEventIds());
        Assert.A.hasSize(1, stats.getUpdatedEventIds());

        Assert.A.hasSize(1, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), uninvitedGuest.getUid()));
        Assert.A.some(eventUserDao.findEventUserByEventIdAndUid(event.getId(), uninvitedGuest.getUid()));

        testStatusChecker.checkEventLastUpdateIsPreserved(event);

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void attendeesAddThemselvesToMeetingOrganizedByResourceMasterAttendeeIsCreator() {
        attendeesAddThemselvesToMeetingOrganizedByResourceMaster(true);
    }

    @Test
    public void attendeesAddThemselvesToMeetingOrganizedByResourceMaster() {
        attendeesAddThemselvesToMeetingOrganizedByResourceMaster(false);
    }

    private void attendeesAddThemselvesToMeetingOrganizedByResourceMaster(boolean attendeeIsCreator) {
        val attendee1 = testManager.prepareUser("yandex-team-mm-10351");
        val attendee2 = testManager.prepareUser("yandex-team-mm-10352");
        val resourceMaster = testManager.prepareUser("yandex-team-mm-10353");

        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        Event event;
        if (attendeeIsCreator) {
            event = testManager.createDefaultEvent(attendee1.getUid(), "commonNewAttendeesAddedToMeetingOrganizedByResourceMaster " + attendee1.getUid());
        } else {
            event = testManager.createDefaultEvent(resourceMaster.getUid(), "commonNewAttendeesAddedToMeetingOrganizedByResourceMaster " + resourceMaster.getUid());
        }

        Email resourceEmail = ResourceRoutines.getResourceEmail(r);
        testManager.addUserParticipantToEvent(event.getId(), resourceMaster, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withDtStart(TestDateTimes.moscow(2021, 1, 28, 7, 0));
        vevent = vevent.withDtEnd(vevent.getStart().plus(Minutes.minutes(30)));
        vevent = vevent.withSequenece(1);
        vevent = vevent.addProperty(new IcsOrganizer(resourceEmail));
        vevent = vevent.addAttendee(attendee1.getEmail());
        vevent = vevent.addAttendee(attendee2.getEmail());
        IcsCalendar calendar = vevent.makeCalendar();

        icsImporter.importIcsStuff(attendee1.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook());
        icsImporter.importIcsStuff(attendee2.getUid(), calendar, IcsImportMode.incomingEmailFromMailhook());

        SetF<ParticipantId> expectedEmails = Cf.set(
                ParticipantId.resourceId(r.getId()),
                ParticipantId.yandexUid(attendee1.getUid()),
                ParticipantId.yandexUid(attendee2.getUid()),
                ParticipantId.yandexUid(resourceMaster.getUid()));
        Participants participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        SetF<ParticipantId> actualEmails = participants.getAllAttendees().map(ParticipantInfo.getIdF()).unique();
        Assert.A.equals(expectedEmails, actualEmails);

        Assert.isEmpty(mailSender.getEventMessageParameters());
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
        vevent = vevent.withDtStart(TestDateTimes.moscow(2023, 2, 4, 22, 30));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2023, 2, 4, 23, 0));

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                notAttendee.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        long updatedEventId = icsImportStats.getUpdatedEventIds().single();
        EventUser eu = eventUserDao.findEventUserByEventIdAndUid(updatedEventId, notAttendee.getUid()).get();

        Assert.isFalse(eu.getIsAttendee());
        Assert.equals(Availability.MAYBE, eu.getAvailability());
        Assert.equals(Decision.YES, eu.getDecision());

        Assert.isEmpty(mailSender.getEventMessageParameters());
    }

    @Test
    public void reattachByWebIcs() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(926);
        TestUserInfo notAttendee = testManager.prepareRandomYaTeamUser(923);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(new Email("top@yandex-team.ru"));

        vevent = vevent.withSummary("reattachByWebIcs");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2023, 2, 4, 22, 30));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2023, 2, 4, 23, 0));

        IcsImportMode mode = IcsImportMode.importFileForTest(
                LayerReference.id(notAttendee.getDefaultLayerId()),
                vevent.getStart(IcsVTimeZones.fallback(MoscowTime.TZ)));

        IcsImportStats stats = icsImporter.importIcsStuff(notAttendee.getUid(), vevent.makeCalendar(), mode);
        Assert.hasSize(1, stats.getNewEventIds());

        long eventId = stats.getNewEventIds().single();

        Assert.some(eventRoutines.detachEventsFromLayerByMainEventId(
                notAttendee.getUserInfo(), mainEventDao.findMainEventByEventId(eventId),
                notAttendee.getDefaultLayerId(), ActionInfo.webTest()).getLayers().getEvents().single().get2().getDetachedId());

        Assert.hasSize(1, icsImporter.importIcsStuff(notAttendee.getUid(), vevent.makeCalendar(), mode).getUpdatedEventIds());

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, notAttendee.getDefaultLayerId()));
        Assert.some(Decision.YES, eventUserDao.findEventUserByEventIdAndUid(eventId, notAttendee.getUid()).map(EventUser::getDecision));
    }
}
