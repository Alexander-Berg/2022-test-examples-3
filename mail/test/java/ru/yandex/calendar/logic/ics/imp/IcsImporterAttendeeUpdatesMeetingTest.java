package ru.yandex.calendar.logic.ics.imp;

import lombok.val;
import net.fortuna.ical4j.model.Property;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVAlarm;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsRelated;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAction;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsExDate;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsProperty;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTransp;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsTrigger;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvAcceptingType;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
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

/**
 * See http://wiki.yandex-team.ru/calendar/unittests at your own risk :)
 */
public class IcsImporterAttendeeUpdatesMeetingTest extends AbstractConfTest {
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
    private LayerRoutines layerRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private SettingsRoutines settingsRoutines;

    @Before
    public void setup() {
        mailSender.clear();
    }

    @Test
    public void attendeeChangesDecisionToYesInExistingEvent() { // test 1
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11301");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11302");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2000, 12, 1, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "sample event 1");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addProperty(new IcsAttendee(organizer.getEmail()));
        vevent = vevent.addProperty(new IcsAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED));
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        // newer version (by dtstamp)
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(event.getLastUpdateTs().plus(Duration.standardHours(1)));

        EventUser eu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();
        Assert.notEquals(Availability.BUSY, eu.getAvailability());

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(), importMode);

        eu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();

        Assert.equals(Decision.YES, eu.getDecision());
        Assert.equals(Availability.BUSY, eu.getAvailability());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(attendee.getUid(), event, vevent, importMode, true);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
    }

    @Test
    public void attendeeChangesDecisionToNoInExistingEvent() { // test 2
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11303");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11304");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2000, 12, 1, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "sample event 2");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        ActionInfo lastUpdatedActionInfo = ActionInfo.webTest(beforeNow);
        // XXX is it needed? Test passes without it. Anyway, we need to update event's last_update afterwards.
        eventDao.updateEventLastUpdateTsAndEventModificationInfo(
                event.getId(), lastUpdatedActionInfo.getNow(), lastUpdatedActionInfo);
        event = eventDao.findEventById(event.getId()); // refresh last-update

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addProperty(new IcsAttendee(organizer.getEmail()));
        vevent = vevent.addProperty(new IcsAttendee(attendee.getEmail(), IcsPartStat.DECLINED)); // was updateAttendee()
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        // newer version (by sequence)
        vevent = vevent.withSequenece(1);
        vevent = vevent.withDtStamp(event.getLastUpdateTs());

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(), importMode);

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), attendee.getUid()));

        EventUser eventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();
        Assert.A.equals(Decision.NO, eventUser.getDecision());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(attendee.getUid(), event, vevent, importMode, true);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
    }

    @Test
    public void organizerDeletesExistingEventWithCancel() { // test 7
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(11311);
        TestUserInfo attendee1 = testManager.prepareRandomYaTeamUser(11312);
        TestUserInfo attendee2 = testManager.prepareRandomYaTeamUser(11313);

        deleteExistingEventWithCancel(true, organizer, attendee1, attendee2);
    }

    @Test
    public void attendeeDeletesExistingEventWithCancel() { // test 8
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(11321);
        TestUserInfo attendee1 = testManager.prepareRandomYaTeamUser(11322);
        TestUserInfo attendee2 = testManager.prepareRandomYaTeamUser(11323);

        deleteExistingEventWithCancel(false, organizer, attendee1, attendee2);
    }

    private void deleteExistingEventWithCancel(
            boolean isByOrganizer, TestUserInfo organizer, TestUserInfo attendee1, TestUserInfo attendee2)
    {

        IcsImportMode importMode = IcsImportMode.incomingEmailFromMailhook(
                Option.empty(), Cf.list(attendee1.getParticipantId()),
                TestDateTimes.moscow(2011, 11, 21, 21, 28)).withActionInfoFreezedNowForTest();

        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendee1Layer = testManager.createDefaultLayerForUser(attendee1.getUid(), beforeNow);
        long attendee2Layer = testManager.createDefaultLayerForUser(attendee2.getUid(), beforeNow);

        String actor = isByOrganizer ? "organizer" : "attendee";
        Event event = testManager.createDefaultMeeting(
                organizer.getUid(), "This event will be deleted. Received by " + actor);
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee1.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee2.getUid(), Decision.MAYBE, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        // RFC: status should be CANCELLED
        vevent = vevent.withOrganizer(organizer.getEmail());
        // RFC: all attendees for which event should be deleted
        vevent = vevent.addProperty(new IcsAttendee(attendee1.getEmail()));
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withSequenece(1);
        vevent = vevent.withDtStamp(event.getLastUpdateTs().plus(Duration.standardHours(4)));

        IcsCalendar calendar = new IcsCalendar(Cf.list(vevent), Cf.list(IcsMethod.CANCEL));
        PassportUid actorUid = isByOrganizer ? organizer.getUid() : attendee1.getUid();
        icsImporter.importIcsStuff(actorUid, calendar, importMode);

        EventUser organizerEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), organizer.getUid()).get();
        eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), organizer.getUid()).single();

        EventUser attendee1EventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee1.getUid()).get();
        Assert.A.hasSize(0, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(
                event.getId(), attendee1.getUid()));

        EventUser attendee2EventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee2.getUid()).get();
        eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(event.getId(), attendee2.getUid()).single();

        Assert.A.equals(Decision.YES, organizerEventUser.getDecision());
        Assert.A.isFalse(attendee1EventUser.getIsAttendee());
        Assert.A.isTrue(attendee2EventUser.getIsAttendee());
        Assert.A.equals(Decision.MAYBE, attendee2EventUser.getDecision());

        Option<ParticipantInfo> att1Invitation =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                        event.getId(), ParticipantId.yandexUid(attendee1.getUid()));
        Assert.assertEmpty(att1Invitation);

        if (!isByOrganizer) {
            testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(actorUid, event, vevent, importMode, true);
        }

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendee1Layer, attendee2Layer), importMode.getActionInfo().getNow());
    }

    @Test
    public void attendeeUpdatesExistingEvent() { // test 9
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11331");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11332");

        IcsImportMode importMode = IcsImportMode
                        .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "Event for update");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.addProperty(new IcsAttendee(attendee.getEmail(), IcsPartStat.TENTATIVE));
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        Instant newStartTs = event.getStartTs().plus(Duration.standardDays(2));
        vevent = vevent.withDtStart(newStartTs);
        Instant newEndTs = newStartTs.plus(Duration.standardHours(4));
        vevent = vevent.withDtEnd(newEndTs);
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(event.getLastUpdateTs().plus(Duration.standardHours(1)));

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(), importMode);

        Option<Event> orgEventO = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(organizer.getUid()), eventDao.findExternalIdByEventId(event.getId()));
        Option<Event> attEventO = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(attendee.getUid()), eventDao.findExternalIdByEventId(event.getId()));
        Assert.assertNotEmpty(orgEventO);
        // XXX ssytnik: Sasha, please verify. Should we preserve event-layer on YES-to-MAYBE decision change?
        Assert.assertNotEmpty(attEventO); // XXX ssytnik: assertEmpty?

        ParticipantInfo participant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(attendee.getUid())).get();
        Assert.A.equals(Decision.MAYBE, participant.getDecision());
        Assert.A.notEquals(newStartTs, orgEventO.first().getStartTs());
        Assert.A.notEquals(newEndTs, orgEventO.first().getEndTs());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(attendee.getUid(), event, vevent, importMode, true);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());
    }

    @Test
    public void evilAttendeeTriesToUpdateMeeting() {
        PassportLogin host = new PassportLogin("yandex-team-mm-11361");
        PassportLogin guest = new PassportLogin("yandex-team-mm-11362");
        PassportLogin guestsFriend = new PassportLogin("yandex-team-mm-11363");

        PassportUid hostUid = userManager.getUidByLoginForTest(host);
        PassportUid guestUid = userManager.getUidByLoginForTest(guest);
        PassportUid guestsFriendUid = userManager.getUidByLoginForTest(guestsFriend);

        Event event = testManager.createDefaultMeeting(hostUid, "very important event");

        testManager.addUserParticipantToEvent(event.getId(), host, Decision.UNDECIDED, true);
        testManager.addUserParticipantToEvent(event.getId(), guest, Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withSummary(event.getName() + " bla bla");
        vevent = vevent.withDtStart(event.getStartTs().plus(Duration.standardHours(1)));
        vevent = vevent.withDtEnd(event.getEndTs().plus(Duration.standardHours(1)));
        vevent = vevent.withSequenece(1);
        vevent = vevent.withOrganizer(new Email(host.getNormalizedValue() + "@yandex.ru"));
        vevent = vevent.addProperty(new IcsAttendee(new Email(guest.getNormalizedValue() + "@yandex.ru"), IcsPartStat.ACCEPTED));
        vevent = vevent.addProperty(new IcsAttendee(new Email(guestsFriend.getNormalizedValue() + "@yandex.ru"), IcsPartStat.ACCEPTED));

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportMode mode = IcsImportMode.importFile(LayerReference.defaultLayer(), event.getStartTs())
                .withActionInfoFreezedNowForTest();

        icsImporter.importIcsStuff(guestUid, calendar, mode);
        Event eventAfterImport = eventDao.findEventById(event.getId());

        ParticipantInfo guestParticipant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(guestUid)).get();
        Assert.A.equals(Decision.UNDECIDED, guestParticipant.getDecision());

        ParticipantInfo hostParticipant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(hostUid)).get();
        Assert.A.equals(Decision.UNDECIDED, hostParticipant.getDecision());

        Option<ParticipantInfo> guestFriendParticipant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(guestsFriendUid));
        Assert.A.none(guestFriendParticipant);
        Assert.A.equals(event.getName(), eventAfterImport.getName());
        Assert.A.equals(event.getStartTs(), eventAfterImport.getStartTs());
        Assert.A.equals(event.getEndTs(), eventAfterImport.getEndTs());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(guestUid, event, vevent, mode, true);
    }

    @Test
    public void updateMeetingByIcsWithoutOrganizer() {
        PassportUid uid1 = testManager.prepareUser("yandex-team-mm-11341").getUid();
        PassportUid uid2 = testManager.prepareUser("yandex-team-mm-11342").getUid();
        PassportUid uid3 = testManager.prepareUser("yandex-team-mm-11343").getUid();

        Event event = testManager.createDefaultEvent(uid1, "updateMeetingByIcsWithoutOrganizer");
        testManager.addUserParticipantToEvent(event.getId(), uid1, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), uid2, Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(event.getId(), uid3, Decision.UNDECIDED, false);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withDtStamp(TestDateTimes.moscow(2009, 1, 16, 22, 7));
        vevent = vevent.withSummary("fgdfgdfg");
        vevent = vevent.withDtStart(event.getStartTs().plus(Duration.standardHours(1)));
        vevent = vevent.withDtEnd(event.getEndTs().plus(Duration.standardHours(1)));
        vevent = vevent.withSequenece(1);
        // no organizer
        vevent = vevent.addAttendee(userManager.getEmailByUid(uid2).get(), IcsPartStat.DECLINED);
        vevent = vevent.addAttendee(userManager.getEmailByUid(uid3).get(), IcsPartStat.ACCEPTED);

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        icsImporter.importIcsStuff(uid3, vevent.makeCalendar(), mode);

        EventUser eventUser1 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), uid1).get();
        EventUser eventUser2 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), uid2).get();
        EventUser eventUser3 = eventUserDao.findEventUserByEventIdAndUid(event.getId(), uid3).get();

        Assert.A.equals(Decision.YES, eventUser1.getDecision());
        Assert.A.equals(Decision.UNDECIDED, eventUser2.getDecision());
        Assert.A.equals(Decision.YES, eventUser3.getDecision());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(uid3, event, vevent, mode, true);
    }


    @Test
    public void attendeeChangesNotification() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11381");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11382");
        val orgUserInfo = organizer.getUserInfo();
        val attendeeUserInfo = attendee.getUserInfo();

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "event");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.MAYBE, false);

        IcsVAlarm valarm = new IcsVAlarm();
        valarm = valarm.withAction(IcsAction.DISPLAY);
        valarm = valarm.withTrigger(IcsTrigger.createDuration("-PT45M", true, Option.of(IcsRelated.START)));
        valarm = valarm.withDescription("Achtung!");

        IcsVEvent vevent = new IcsVEvent(Cf.<IcsProperty>list(), Cf.list(valarm));
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 20, 10, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 20, 13, 0));

        IcsCalendar calendar = vevent.makeCalendar();
        icsImporter.importIcsStuff(attendee.getUid(), calendar, importMode);

        EventWithRelations eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        val eventAuthInfoForOrg = authorizer.loadEventInfoForPermsCheck(orgUserInfo, eventWithRelations);
        val eventAuthInfoForAttendee = authorizer.loadEventInfoForPermsCheck(attendeeUserInfo, eventWithRelations);
        Assert.A.isTrue(authorizer.canEditEvent(orgUserInfo, eventAuthInfoForOrg, ActionSource.WEB));
        Assert.A.isTrue(!authorizer.canEditEvent(attendeeUserInfo, eventAuthInfoForAttendee, ActionSource.WEB));

        long eventUser1Id = eventUserDao.findEventUserByEventIdAndUid(event.getId(), organizer.getUid()).get().getId();
        ListF<Notification> user1Notification = notificationDbManager.getNotificationsByEventUserId(eventUser1Id).getNotifications();

        long eventUser2Id = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get().getId();
        ListF<Notification> user2Notification = notificationDbManager.getNotificationsByEventUserId(eventUser2Id).getNotifications();

        ListF<Notification> expectedUser2Notification = Cf.list(Notification.display(Duration.standardMinutes(-45)));

        Assert.notEquals(user1Notification.unique(), user2Notification.unique());
        Assert.equals(expectedUser2Notification.unique(), user2Notification.unique());

        testStatusChecker.checkForAttendeeOnVEventUpdateOrDelete(attendee.getUid(), event, vevent, importMode, true);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(attendeeLayer), importMode.getActionInfo().getNow());
    }

    @Test
    public void meetingToEventWithParticipantsFlagsCheck() {
        PassportUid organizer = testManager.prepareUser("yandex-team-mm-11307").getUid();
        PassportUid attendee = testManager.prepareUser("yandex-team-mm-11308").getUid();

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer, beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee, beforeNow);

        final String oldEventName = "Meeting => Event";
        Event oldEvent = testManager.createDefaultMeeting(organizer, oldEventName);
        testManager.addUserParticipantToEvent(oldEvent.getId(), organizer, Decision.YES, true);
        testManager.addUserParticipantToEvent(oldEvent.getId(), attendee, Decision.YES, false);

        final long eventId = oldEvent.getId();

        // Now, import ics without participants, so the meeting degrades to just an event
        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(eventId));
        final String eventNameAfterIcsImport = "Meeting => Event (ics: no organizer, no attendees)";
        vevent = vevent.withSummary(eventNameAfterIcsImport);
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 21, 16, 20));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 21, 16, 40));
        vevent = vevent.withSequenece(1);
        icsImporter.importIcsStuff(organizer, vevent.makeCalendar(), importMode);

        final Event event = eventDao.findEventById(eventId);
        Assert.A.equals(eventNameAfterIcsImport, event.getName());
        Assert.assertTrue(eventInvitationManager.getParticipantsByEventId(event.getId()).isNotMeetingOrIsInconsistent());

        final EventUser orgEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, organizer).get();
        Assert.assertFalse(orgEventUser.getIsOrganizer());
        Assert.assertFalse(orgEventUser.getIsAttendee());

        final EventUser attEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, attendee).get();
        Assert.assertFalse(attEventUser.getIsOrganizer());
        Assert.assertFalse(attEventUser.getIsAttendee());

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());
    }

    /**
     *  @see IcsImporterNonparticipantUpdatesMeetingTest#uninvitedUserImportsStaleEventAndBecomesParticipant()
     */
    @Test
    public void attendeeImportsStaleEventAndChangesAreIgnored() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11351");
        TestUserInfo participant = testManager.prepareUser("yandex-team-mm-11352");

        String eventName = "attendeeImportsStaleEventAndChangesAreIgnored";
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(organizer.getUid(), eventName);

        testManager.addUserParticipantToEvent(event.getId(), participant.getUid(), Decision.YES, false);
        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        EventUser oldEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), participant.getUid()).get();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(participant.getEmail(), IcsPartStat.DECLINED);
        vevent = vevent.withSequenece(0);
        vevent = vevent.withDtStamp(event.getLastUpdateTs().minus(Duration.standardHours(1)));
        vevent = vevent.withSummary(eventName + " - this update should not apply");

        IcsCalendar calendar = vevent.makeCalendar().withMethod(IcsMethod.REQUEST);
        Instant now = TestDateTimes.moscow(2011, 1, 14, 20, 57);
        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest(now);
        IcsImportStats stats = icsImporter.importIcsStuff(participant.getUid(), calendar, mode);

        Assert.A.hasSize(0, stats.getNewEventIds());
        // Assert.A.hasSize(0, stats.getUpdatedEventIds()); // only participation is ignored now based on version (why?)// gutman@
        Assert.A.hasSize(1, stats.getUpdatedEventIds());

        Event foundEvent = eventDao.findEventById(event.getId());
        Option<EventUser> eventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), participant.getUid());

        Assert.equals(Decision.NO, eventUser.get().getDecision()); // changed
        Assert.equals(eventName, foundEvent.getName());            // not changed

        testStatusChecker.checkEventLastUpdateIsPreserved(event);
        testStatusChecker.checkMainEventLastUpdateIsUpdated(event, mode.getActionInfo());

        testStatusChecker.checkUserSequenceAndDtStampArePreserved(participant.getUid(), event.getId(), oldEventUser);
    }

    @Test
    public void attendeeAcceptsRecurrence() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11391");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11392");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2000, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "attendeeAcceptsRecurrence");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        Instant recurrenceStartTs = event.getStartTs().plus(Duration.standardDays(1));

        Tuple2<IcsVEvent, IcsVEvent> masterAndRecurrence = createMasterAndRecurrenceAcceptedByAttendee(
                organizer, attendee, event, externalId, recurrenceStartTs);
        // XXX: add alarm

        IcsCalendar calendar = new IcsCalendar()
            .addComponent(masterAndRecurrence._1)
            .addComponent(masterAndRecurrence._2)
            ;

        icsImporter.importIcsStuff(attendee.getUid(), calendar, importMode);

        ListF<Event> eventsFromDb = eventDao.findEventsByMainId(event.getMainEventId());
        Assert.A.hasSize(2, eventsFromDb);
        Event masterFromDb = eventsFromDb.find(EventFields.RECURRENCE_ID.getF().andThenEquals(null)).get();
        Event recurrFromDb = eventsFromDb.find(EventFields.RECURRENCE_ID.getF().andThenEquals(null).notF()).get();

        Assert.A.equals(recurrenceStartTs, recurrFromDb.getStartTs());

        // don't do checkUserSequenceAndDtStampArePreserved as master event is also handled by the attendee
        testStatusChecker.checkForAttendeeOnVEventCreateRecurrence(
                attendee.getUid(), recurrFromDb.getId(), masterAndRecurrence._2, importMode);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
        Assert.equals(recurrFromDb.getId(), mailSender.getEventMessageParameters().single().getEventId());
    }

    @Test
    public void attendeeDeclinesRecurrence() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11398");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11399");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2000, 11, 21, 21, 28));
        Instant beforeNow = importMode.getActionInfo().getNow().minus(Duration.standardDays(1));

        long organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), beforeNow);
        long attendeeLayer = testManager.createDefaultLayerForUser(attendee.getUid(), beforeNow);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "attendeeAcceptsRecurrence");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        Instant recurrenceStartTs = event.getStartTs().plus(Duration.standardDays(1));

        Tuple2<IcsVEvent, IcsVEvent> masterAndRecurrence = createMasterAndRecurrenceAcceptedByAttendee(
                organizer, attendee, event, externalId, recurrenceStartTs);

        IcsVEvent master = masterAndRecurrence._1;
        IcsVEvent recurrence = masterAndRecurrence._2;

        recurrence = recurrence
                .removeAttendee(attendee.getEmail())
                .addAttendee(attendee.getEmail(), IcsPartStat.DECLINED);

        IcsCalendar calendar = new IcsCalendar()
                .addComponent(master)
                .addComponent(recurrence)
                ;

        icsImporter.importIcsStuff(attendee.getUid(), calendar, importMode);

        ListF<Event> eventsFromDb = eventDao.findEventsByMainId(event.getMainEventId());
        Event recurrFromDb = eventsFromDb.find(EventFields.RECURRENCE_ID.getF().andThenEquals(null).notF()).get();

        EventUser recurrenceAttendeeEventUser = eventUserDao
                .findEventUserByEventIdAndUid(recurrFromDb.getId(), attendee.getUid()).get();
        Assert.equals(Decision.NO, recurrenceAttendeeEventUser.getDecision());

        Option<EventLayer> recurrenceAttendeeEventLayer = eventLayerDao
                .findEventLayerByEventIdAndLayerCreatorUid(recurrFromDb.getId(), attendee.getUid());
        Assert.some(recurrenceAttendeeEventLayer);

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizerLayer, attendeeLayer), importMode.getActionInfo().getNow());

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
        Assert.equals(recurrFromDb.getId(), mailSender.getEventMessageParameters().single().getEventId());
    }

    @Test
    public void attendeeAcceptsRecurrenceOfEventExistingInExchange() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-13901");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-13902");

        Resource resource = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = ResourceRoutines.getResourceEmail(resource);

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "attendeeAcceptsRecurrence");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 3);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);

        ewsProxyWrapper.cancelMeetings(
                resourceRoutines.getExchangeEmail(resource), event.getStartTs(), dueTs.plus(Duration.standardHours(1)));

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().minus(Duration.standardHours(1))));

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        Instant recurrenceStartTs = event.getStartTs().plus(Duration.standardDays(1));

        Tuple2<IcsVEvent, IcsVEvent> masterAndRecurrence = createMasterAndRecurrenceAcceptedByAttendee(
                organizer, attendee, event, externalId, recurrenceStartTs);

        IcsCalendar calendar = new IcsCalendar()
            .addComponent(masterAndRecurrence._1.addAttendee(resourceEmail, IcsPartStat.ACCEPTED))
            .addComponent(masterAndRecurrence._2.addAttendee(resourceEmail, IcsPartStat.ACCEPTED))
            ;

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest(event.getStartTs());
        icsImporter.importIcsStuff(attendee.getUid(), calendar, mode);

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
    }

    // CAL-5682
    @Test
    public void attendeeAcceptsInstanceOfEventWithExdate() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(358);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(359);

        DateTime start = new DateTime(2012, 11, 2, 16, 0, MoscowTime.TZ);
        Event master = testManager.createDefaultEvent(organizer.getUid(), "Event with exdate", start.toInstant());

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.createExdate(start.plusDays(1).toInstant(), master.getId());

        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        String externalId = mainEventDao.findExternalIdByMainEventId(master.getMainEventId());
        Instant recurrenceStartTs = start.plusDays(2).toInstant();

        Tuple2<IcsVEvent, IcsVEvent> masterAndRecurrence = createMasterAndRecurrenceAcceptedByAttendee(
                organizer, attendee, master, externalId, recurrenceStartTs);

        IcsCalendar calendar = new IcsCalendar().addComponents(Cf.list(masterAndRecurrence._1, masterAndRecurrence._2));
        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest(master.getStartTs());
        IcsImportStats stats = icsImporter.importIcsStuff(attendee.getUid(), calendar, mode);

        Assert.hasSize(1, stats.getNewEventIds());
        Assert.hasSize(1, mailSender.getEventMessageParameters());

        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
        Assert.equals(stats.getNewEventIds().single(), mailSender.getEventMessageParameters().single().getEventId());
    }

    // CAL-5630
    @Test
    public void attendeeCanNotAddResourceFromMailhook() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(928);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(929);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());

        vevent = vevent.withSummary("Attendee can not add resource from mailhook");
        vevent = vevent.withDtStart(TestDateTimes.moscow(2012, 10, 17, 17, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2012, 10, 17, 17, 30));
        vevent = vevent.withSequenece(0);

        icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());

        vevent = vevent.addAttendee(Cf.list(resource).map(resourceRoutines.getResourceEmailF()).single());
        vevent = vevent.withSequenece(1);

        IcsImportStats icsImportStats = icsImporter.importIcsStuff(
                attendee.getUid(), vevent.makeCalendar(), IcsImportMode.incomingEmailFromMailhook());
        long eventId = icsImportStats.getUpdatedEventIds().single();

        Participants p = eventInvitationManager.getParticipantsByEventId(eventId);
        Assert.forAll(p.getParticipantIds(), ParticipantId.isResourceF().notF());
    }

    // CAL-10278
    @Test
    public void autoAcceptingAttendeeDeclines() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1028);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(8201);

        Settings settings = new Settings();
        settings.setInvAcceptType(InvAcceptingType.AUTO);
        settingsRoutines.updateSettingsByUid(settings, attendee.getUid());

        Event event = testManager.createDefaultEvent(organizer.getUid(), "Meeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.DECLINED);
        vevent = vevent.withSequenece(0);

        IcsImportStats stats = icsImporter.importIcsStuff(attendee.getUid(),
                vevent.makeCalendar().withMethod(IcsMethod.REQUEST),
                IcsImportMode.caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 1, 14, 20, 57)));

        Assert.A.hasSize(0, stats.getNewEventIds());
        Assert.A.hasSize(1, stats.getUpdatedEventIds());

        Assert.some(Decision.NO, eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid())
                .map(EventUser::getDecision));
    }

    private Tuple2<IcsVEvent, IcsVEvent> createMasterAndRecurrenceAcceptedByAttendee(TestUserInfo organizer, TestUserInfo attendee, Event event, String externalId, Instant recurrenceStartTs) {
        IcsVEvent masterIcsEvent = createVeventWithDailyRepetition(organizer, attendee, event, externalId);

        IcsVEvent recurrIcsEvent = createRecurreneAcceptedByAttendee(organizer, attendee, event, externalId, recurrenceStartTs);

        return Tuple2.tuple(masterIcsEvent, recurrIcsEvent);
    }

    private IcsVEvent createRecurreneAcceptedByAttendee(TestUserInfo organizer, TestUserInfo attendee, Event event, String externalId, Instant recurrenceStartTs) {
        IcsVEvent recurrIcsEvent = new IcsVEvent();
        recurrIcsEvent = recurrIcsEvent.withUid(externalId);
        recurrIcsEvent = recurrIcsEvent.withSummary(event.getName());
        recurrIcsEvent = recurrIcsEvent.withDtStart(recurrenceStartTs);
        recurrIcsEvent = recurrIcsEvent.withDtEnd(recurrenceStartTs.plus(Duration.standardHours(1)));
        recurrIcsEvent = recurrIcsEvent.withDtStamp(event.getCreationTs().plus(Duration.standardDays(1)));
        recurrIcsEvent = recurrIcsEvent.withOrganizer(organizer.getEmail());
        recurrIcsEvent = recurrIcsEvent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        recurrIcsEvent = recurrIcsEvent.withRecurrence(recurrenceStartTs);
        return recurrIcsEvent;
    }

    private IcsVEvent createVeventWithDailyRepetition(TestUserInfo organizer, TestUserInfo attendee, Event event, String externalId) {
        IcsVEvent masterIcsEvent = new IcsVEvent();
        masterIcsEvent = masterIcsEvent.withUid(externalId);
        masterIcsEvent = masterIcsEvent.withDtStart(event.getStartTs());
        masterIcsEvent = masterIcsEvent.withDtEnd(event.getEndTs());
        masterIcsEvent = masterIcsEvent.withDtStamp(event.getCreationTs());
        masterIcsEvent = masterIcsEvent.withOrganizer(organizer.getEmail());
        masterIcsEvent = masterIcsEvent.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        masterIcsEvent = masterIcsEvent.addProperty(new IcsRRule("FREQ=DAILY"));
        return masterIcsEvent;
    }

    // CAL-5601, CAL-6653
    @Test
    public void emailsOnDecliningSingleEvent() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11396");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11397");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "emails");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        event.setPermParticipants(EventActionClass.EDIT); // CAL-6653
        eventDao.updateEvent(event);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(mainEventDao.findExternalIdByMainEventId(event.getMainEventId()));
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.DECLINED);
        vevent = vevent.withSummary(event.getName());

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        icsImporter.importIcsStuff(attendee.getUid(), vevent.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest(event.getStartTs()));

        Assert.hasSize(0, mailSenderMock.getInvitationMessageParameterss());
        Assert.hasSize(0, mailSenderMock.getCancelEventMessageParameterss());
        Assert.equals(organizer.getEmail(), mailSenderMock.getReplyMessageParameterss().single().getRecipientEmail());
    }

    @Test
    public void emailsOnDecliningRecurrence() {
        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11393");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11394");
        TestUserInfo anotherAttendee = testManager.prepareUser("yandex-team-mm-11395");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "emails");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(event.getId(), anotherAttendee.getUid(), Decision.YES, false);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent master = new IcsVEvent();
        master = master.withUid(externalId);
        master = master.withDtStart(event.getStartTs());
        master = master.withDtEnd(event.getEndTs());
        master = master.withOrganizer(organizer.getEmail());
        master = master.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        master = master.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        master = master.addAttendee(anotherAttendee.getEmail(), IcsPartStat.ACCEPTED);
        master = master.withSummary(event.getName());
        master = master.addProperty(new IcsRRule("FREQ=DAILY"));

        Instant recurrenceStartTs = event.getStartTs().plus(Duration.standardDays(1));

        IcsVEvent recurrence = master.withRecurrence(recurrenceStartTs);
        recurrence = recurrence.withDtStart(recurrenceStartTs);
        recurrence = recurrence.withDtEnd(recurrenceStartTs.plus(new Duration(event.getStartTs(), event.getEndTs())));
        recurrence = recurrence.removeProperties(Property.RRULE);

        recurrence = recurrence.updateAttendee(new IcsAttendee(attendee.getEmail(), IcsPartStat.DECLINED));

        IcsImportStats stats = icsImporter.importIcsStuff(attendee.getUid(), new IcsCalendar().addComponent(master).addComponent(recurrence),
                IcsImportMode.caldavPutToDefaultLayerForTest(event.getStartTs()));

        Assert.A.hasSize(1, stats.getNewEventIds());

        Assert.A.hasSize(0, mailSenderMock.getInvitationMessageParameterss());
        Assert.A.hasSize(0, mailSenderMock.getCancelEventMessageParameterss());
        Assert.A.equals(organizer.getEmail(), mailSenderMock.getReplyMessageParameterss().single().getRecipientEmail());
    }

    @Test
    public void singleRecurrenceImportByAttendeeDoesNotDeleteMaster() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12301");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12302");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "singleRecurrenceDoesNotDeleteMaster");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        EventUser oldEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        Instant recurrenceStartTs = event.getStartTs().plus(Duration.standardDays(1));

        IcsVEvent recurrenceIcs = new IcsVEvent();
        recurrenceIcs = recurrenceIcs.withUid(externalId);
        recurrenceIcs = recurrenceIcs.withDtStart(event.getStartTs());
        recurrenceIcs = recurrenceIcs.withDtEnd(event.getEndTs());
        recurrenceIcs = recurrenceIcs.withOrganizer(organizer.getEmail());
        recurrenceIcs = recurrenceIcs.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        recurrenceIcs = recurrenceIcs.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        recurrenceIcs = recurrenceIcs.withSummary(event.getName());
        recurrenceIcs = recurrenceIcs.addProperty(new IcsRRule("FREQ=DAILY"));
        recurrenceIcs = recurrenceIcs.withRecurrence(recurrenceStartTs);

        recurrenceIcs = recurrenceIcs.withDtStart(recurrenceStartTs);
        recurrenceIcs = recurrenceIcs.withDtEnd(recurrenceStartTs.plus(new Duration(event.getStartTs(), event.getEndTs())));
        recurrenceIcs = recurrenceIcs.removeProperties(Property.RRULE);

        recurrenceIcs = recurrenceIcs.updateAttendee(new IcsAttendee(attendee.getEmail(), IcsPartStat.DECLINED));

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        IcsImportStats stats = icsImporter.importIcsStuff(attendee.getUid(), new IcsCalendar().addComponent(recurrenceIcs), mode);

        long recurrenceId = stats.getNewEventIds().single();
        Event recurrence = eventDao.findEventById(recurrenceId);
        Assert.A.equals(event.getMainEventId(), recurrence.getMainEventId());

        testStatusChecker.checkUserSequenceAndDtStampArePreserved(attendee.getUid(), event.getId(), oldEventUser);
        testStatusChecker.checkForAttendeeOnVEventCreateRecurrence(attendee.getUid(), recurrence.getId(), recurrenceIcs, mode);
    }

    @Test
    public void importingOneRecurrenceByAttendeeDoesNotDeleteOthersWithoutMasterVEvent() {
        importingOneRecurrenceByAttendeeDoesNotDeleteOthers(false);
    }

    @Test
    public void importingOneRecurrenceByAttendeeDoesNotDeleteOthersWithMasterVEvent() {
        importingOneRecurrenceByAttendeeDoesNotDeleteOthers(true);
    }

    private void importingOneRecurrenceByAttendeeDoesNotDeleteOthers(boolean masterVEventComes) {
        MailSenderMock mailSenderMock = mailSender;
        mailSenderMock.clear();

        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11393");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11394");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "importingOneRecurrenceDoesNotDeleteOthers");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        Event tomorrowRecurrence = TestManager.createRecurrence(event, Duration.standardDays(1), "tomorrow recurrence");
        long tomorrowRecurrenceId = eventDao.saveEvent(tomorrowRecurrence, ActionInfo.webTest());

        EventUser oldMasterEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent master = new IcsVEvent();
        master = master.withUid(externalId);
        master = master.withDtStart(event.getStartTs());
        master = master.withDtEnd(event.getEndTs());
        master = master.withOrganizer(organizer.getEmail());
        master = master.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        master = master.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        master = master.withSummary(event.getName());
        master = master.addProperty(new IcsRRule("FREQ=DAILY"));

        Instant dayAfterTomorrowRecurrenceStartTs = event.getStartTs().plus(Duration.standardDays(2));

        IcsVEvent dayAfterTomorrowRecurrence = master.withRecurrence(dayAfterTomorrowRecurrenceStartTs);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtStart(dayAfterTomorrowRecurrenceStartTs);
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.withDtEnd(dayAfterTomorrowRecurrenceStartTs.plus(new Duration(event.getStartTs(), event.getEndTs())));
        dayAfterTomorrowRecurrence = dayAfterTomorrowRecurrence.removeProperties(Property.RRULE);

        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();

        IcsCalendar calendar = new IcsCalendar();
        if (masterVEventComes) { calendar = calendar.addComponent(master); }
        calendar = calendar.addComponent(dayAfterTomorrowRecurrence);

        IcsImportStats stats = icsImporter.importIcsStuff(attendee.getUid(), calendar, mode);

        Event masterIsAlive = eventDao.findEventById(event.getId());
        Event tomorrowRecurrenceIsAlive = eventDao.findEventById(tomorrowRecurrenceId);
        Event dayAfterTomorrowRecurrenceCreated = eventDao.findEventById(stats.getNewEventIds().single());

        testStatusChecker.checkEventLastUpdateIsPreserved(event);
        testStatusChecker.checkMainEventLastUpdateIsUpdated(event, mode.getActionInfo());

        testStatusChecker.checkUserSequenceAndDtStampArePreserved(attendee.getUid(), event.getId(), oldMasterEventUser);
        // don't check old event-user for tomorrow recurrence because it does not exist
        testStatusChecker.checkForAttendeeOnVEventCreateRecurrence(attendee.getUid(), dayAfterTomorrowRecurrenceCreated.getId(), dayAfterTomorrowRecurrence, mode);
    }

    @Test
    public void attendeeChangesAvailability() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11395");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11396");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "event");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        eventUserDao.updateEventUserAvailabilityByEventIdAndUserId(
                event.getId(), attendee.getUid(), Availability.AVAILABLE, ActionInfo.webTest());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withDtStampNow();
        vevent = vevent.withUid(eventDao.findExternalIdByEventId(event.getId()));
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail());
        vevent = vevent.withSummary(event.getName());
        vevent = vevent.withDtStart(TestDateTimes.moscow(2010, 11, 20, 10, 0));
        vevent = vevent.withDtEnd(TestDateTimes.moscow(2010, 11, 20, 13, 0));
        vevent = vevent.withTransp(new IcsTransp("OPAQUE"));

        IcsCalendar calendar = vevent.makeCalendar();
        IcsImportMode mode = IcsImportMode.caldavPutToDefaultLayerForTest();
        icsImporter.importIcsStuff(attendee.getUid(), calendar, mode);

        EventUser attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();
        Assert.A.equals(Availability.BUSY, attendeeEventUser.getAvailability());
    }

    @Test
    public void attendeeWithoutEventLayerAcceptsRecurringEvent() {

        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10005");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10006");

        IcsImportMode importMode = IcsImportMode
                .caldavPutToDefaultLayerForTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));

        Event event = testManager.createDefaultMeeting(organizer.getUid(),
                "attendeeWithoutEventLayerAcceptsRecurringEvent");

        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.createEventUser(attendee.getUid(), event.getId(), Decision.UNDECIDED, Option.<Boolean>empty());

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withDtStamp(event.getCreationTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addProperty(new IcsRRule("FREQ=DAILY"));

        icsImporter.importIcsStuff(attendee.getUid(), new IcsCalendar(Cf.list(vevent)), importMode);

        Event eventAfterImport = eventDao.findEventsByMainId(event.getMainEventId()).first();

        Option<EventLayer> eventLayer = eventLayerDao
                .findEventLayerByEventIdAndLayerCreatorUid(eventAfterImport.getId(), attendee.getUid());

        Assert.some(eventLayer);
    }

    // CAL-7067
    @Test
    public void keepNotificationsByMailWidget() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11393");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11394");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "keepNotificationsFromMail");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        String externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(externalId);
        vevent = vevent.withDtStart(event.getStartTs());
        vevent = vevent.withDtEnd(event.getEndTs());
        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.withSummary(event.getName());

        Notification notification = Notification.sms(Duration.standardMinutes(-5));
        ListF<Notification> notifications;

        LayerUser attendeeLayerUser = layerRoutines.getLayerUser(attendee.getDefaultLayerId(), attendee.getUid()).get();
        notificationDbManager.saveLayerNotifications(attendeeLayerUser.getId(), Cf.list(notification));

        ActionInfo actionInfo = ActionInfo.webTest().withActionSource(ActionSource.MAIL);

        IcsImportMode importMode = IcsImportMode.mailWidget(attendee.getUid(), actionInfo);
        icsImporter.importIcsStuff(attendee.getUid(), new IcsCalendar(Cf.list(vevent)), importMode);

        notifications = notificationDbManager.getNotificationsByUidAndEventId(attendee.getUid(), event.getId());
        Assert.in(notification, notifications);

        importMode = IcsImportMode.mailWidget(attendee.getUid(), Decision.YES, actionInfo);
        icsImporter.importIcsStuff(attendee.getUid(), new IcsCalendar(Cf.list(vevent)), importMode);

        notifications = notificationDbManager.getNotificationsByUidAndEventId(attendee.getUid(), event.getId());
        Assert.in(notification, notifications);
    }

    // CAL-9724
    @Test
    public void keepUserEventLayer() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11393");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11394");

        layerRoutines.startNewSharing(organizer.getUid(), attendee.getDefaultLayerId(), LayerActionClass.EDIT);

        IcsVEvent vevent = new IcsVEvent();
        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        vevent = vevent.withDtStart(MoscowTime.instant(2017, 4, 18, 15, 0));
        vevent = vevent.withDtEnd(MoscowTime.instant(2017, 4, 18, 16, 0));

        vevent = vevent.withOrganizer(organizer.getEmail());
        vevent = vevent.addAttendee(organizer.getEmail(), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(attendee.getEmail(), IcsPartStat.NEEDS_ACTION);
        vevent = vevent.withSummary("keepUserEventLayer");

        long eventId = icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest(Instant.now())).getNewEventIds().single();

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, organizer.getDefaultLayerId()));
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, attendee.getDefaultLayerId()));

        Assert.hasSize(1, icsImporter.importIcsStuff(organizer.getUid(), vevent.makeCalendar(),
                IcsImportMode.caldavPut(attendee.getDefaultLayerId(), Instant.now())).getUpdatedEventIds());

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, organizer.getDefaultLayerId()));
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(eventId, attendee.getDefaultLayerId()));
    }

    @Test
    public void ignoreUserMissedExdates() {
        TestUserInfo user1 = testManager.prepareUser("yandex-team-mm-11391");
        TestUserInfo user2 = testManager.prepareUser("yandex-team-mm-11392");

        Event master = testManager.createDefaultMeeting(user1.getUid(), "master");
        testManager.addUserParticipantToEvent(master.getId(), user1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), user2.getUid(), Decision.YES, false);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Instant recurrenceId = master.getStartTs().plus(Duration.standardDays(1));

        Event recurrence = testManager.createDefaultRecurrence(user2.getUid(), master.getId(), recurrenceId);
        testManager.addUserParticipantToEvent(recurrence.getId(), user2.getUid(), Decision.YES, true);

        Tuple2<IcsVEvent, IcsVEvent> masterAndRecurrence = createMasterAndRecurrenceAcceptedByAttendee(
                user1, user2, master, mainEventDao.findExternalIdByMainEventId(master.getMainEventId()), recurrenceId);

        IcsCalendar calendar = new IcsCalendar().addComponent(
                masterAndRecurrence._1.withExdates(Cf.list(new IcsExDate(recurrenceId))));

        icsImporter.importIcsStuff(user1.getUid(), calendar, IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.notEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrence.getId())));
        Assert.isEmpty(eventDao.findRdateRdatesByEventId(master.getId()));

        icsImporter.importIcsStuff(user2.getUid(), calendar, IcsImportMode.caldavPutToDefaultLayerForTest());

        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrence.getId())));
    }


} //~
