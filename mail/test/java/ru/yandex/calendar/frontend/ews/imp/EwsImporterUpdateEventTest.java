package ru.yandex.calendar.frontend.ews.imp;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import lombok.val;
import org.joda.time.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.exp.EventToCalendarItemConverter;
import ru.yandex.calendar.frontend.ews.exp.EwsModifyingItemId;
import ru.yandex.calendar.frontend.ews.exp.OccurrenceId;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.frontend.ews.proxy.EwsActionLogData;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.frontend.ews.sync.IgnoredEventDao;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.RdateFields;
import ru.yandex.calendar.logic.event.*;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.base.Binary;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import static ru.yandex.calendar.logic.event.ActionInfo.exchangeTest;
import static ru.yandex.calendar.test.auto.db.util.TestManager.NEXT_YEAR;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class,
        EwsPurgeTestConfiguration.class
})
public class EwsImporterUpdateEventTest extends AbstractConfTest {
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
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventToCalendarItemConverter eventToCalendarItemConverter;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private IgnoredEventDao ignoredEventDao;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private EventDbManager eventDbManager;

    @Test
    public void updateEventWithExdates() throws Exception {
        val user = new PassportLogin("tester11");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer = testManager.createDefaultLayerForUser(uid, now.minus(Duration.standardDays(1)));

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR + 1, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(dateTime, "Exchange event");
        TestCalItemFactory.addDailyRecurrence(calItem);

        val exchangeId = calItem.getItemId().getId();
        val subjectId = UidOrResourceId.user(uid);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, exchangeTest(now), false);
        val event = eventRoutines.findEventByExchangeId(exchangeId).get();

        val startOfExdates = Cf.hashSet();
        // Add one exdate and make update of event
        val firstExdateTs = dateTime.plusDays(1).toInstant();
        startOfExdates.add(firstExdateTs);
        val calItemWithOneExdate = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "Exchange event", event.getLastUpdateTs().plus(1000));
        TestCalItemFactory.addDailyRecurrence(calItemWithOneExdate);
        TestCalItemFactory.addExdate(calItemWithOneExdate, firstExdateTs);
        calItemWithOneExdate.getItemId().setId(exchangeId);
        calItemWithOneExdate.setAppointmentSequenceNumber(1);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItemWithOneExdate, exchangeTest(now), false);
        val actualStartOfExdates1 =
                eventDao.findRdatesByEventId(event.getId()).map(RdateFields.START_TS.getF()).unique();
        Assert.A.equals(startOfExdates, actualStartOfExdates1);

        // Add another one exdate and make update of event
        val secondExdateTs = dateTime.plusDays(2).toInstant();
        startOfExdates.add(secondExdateTs);
        val calItemWithTwoExdates = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "Exchange event", event.getLastUpdateTs().plus(3000));
        TestCalItemFactory.addDailyRecurrence(calItemWithTwoExdates);
        TestCalItemFactory.addExdate(calItemWithTwoExdates, firstExdateTs);
        TestCalItemFactory.addExdate(calItemWithTwoExdates, secondExdateTs);
        calItemWithTwoExdates.getItemId().setId(exchangeId);
        calItemWithTwoExdates.setAppointmentSequenceNumber(2);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItemWithTwoExdates, exchangeTest(now), false);
        val actualStartOfExdates2 =
            eventDao.findRdatesByEventId(event.getId()).map(RdateFields.START_TS.getF()).unique();
        Assert.A.equals(startOfExdates, actualStartOfExdates2);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);
    }

    @Test
    public void updateWithRecurrenceInstanceFromUser() throws Exception {
        updateWithRecurrenceInstance();
    }

    private void updateWithRecurrenceInstance() throws Exception {
        userManager.registerYandexUserForTest(TestManager.createResourceMaster());

        val yaUser1 = testManager.prepareRandomYaTeamUser(11110);
        val yaUser2 = testManager.prepareRandomYaTeamUser(11111);

        val r = testManager.cleanAndCreateThreeLittlePigs();
        val resourceEmail = ResourceRoutines.getResourceEmail(r);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer1 = testManager.createDefaultLayerForUser(yaUser1.getUid(), now.minus(Duration.standardDays(1)));
        val layer2 = testManager.createDefaultLayerForUser(yaUser2.getUid(), now.minus(Duration.standardDays(1)));

        // Create new event
        val dateTime = new DateTime(NEXT_YEAR +1, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(dateTime, "Master event");
        TestCalItemFactory.addDailyRecurrence(calItem);
        TestCalItemFactory.setOrganizer(calItem, resourceEmail);
        TestCalItemFactory.addAttendee(calItem, yaUser1.getEmail(), ResponseTypeType.ACCEPT);
        TestCalItemFactory.addAttendee(calItem, yaUser2.getEmail(), ResponseTypeType.ACCEPT);
        calItem.setUID(Random2.R.nextAlnum(10));

        val subjectId1 = UidOrResourceId.user(yaUser1.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId1, calItem, exchangeTest(now), false);

        val masterEventExchangeId = calItem.getItemId().getId();
        val masterEventO = eventRoutines.findEventByExchangeId(masterEventExchangeId);

        Assert.isTrue(masterEventO.isPresent());

        // Create recurrence item
        calItem.setRecurrenceId(calItem.getStart());
        calItem.setSubject("Recurrence event");
        calItem.setItemId(EwsUtils.createItemId(Random2.R.nextAlnum(8)));
        calItem.setRecurrence(null);
        TestCalItemFactory.increaseDtstamp(calItem, Hours.ONE.toStandardDuration());

        ewsImporter.createOrUpdateEventForTest(subjectId1, calItem, exchangeTest(now), false);

        val recurrenceEventExchangeId = calItem.getItemId().getId();
        val recurrenceEventO = eventRoutines.findEventByExchangeId(recurrenceEventExchangeId);

        Assert.some(recurrenceEventO);

        Assert.A.notEquals(masterEventO.get().getId(), recurrenceEventO.get().getId());

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer1, layer2), now);
    }

    @Test
    public void updateOnAnyChannelChangesUndecidedAndDoesNotOverwriteExistingDecisionsByOrganizer() throws Exception {
        updateOnAnyChannelChangesUndecidedAndDoesNotOverwriteExistingDecisions(true);
    }

    @Test
    public void updateOnAnyChannelChangesUndecidedAndDoesNotOverwriteExistingDecisionsByAttendee() throws Exception {
        updateOnAnyChannelChangesUndecidedAndDoesNotOverwriteExistingDecisions(false);
    }

    // https://jira.yandex-team.ru/browse/CAL-2898
    private void updateOnAnyChannelChangesUndecidedAndDoesNotOverwriteExistingDecisions(boolean isByOrganizer)
            throws Exception
    {
        val organizer = testManager.prepareRandomYaTeamUser(11114);
        val attendee1 = testManager.prepareRandomYaTeamUser(11115);
        val attendee2 = testManager.prepareRandomYaTeamUser(11116);
        val organizerEmail = organizer.getEmail();
        val attendee1Email = attendee1.getEmail();
        val attendee2Email = attendee2.getEmail();

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), now.minus(Duration.standardDays(1)));
        val attendee1Layer = testManager.createDefaultLayerForUser(attendee1.getUid(), now.minus(Duration.standardDays(1)));
        val attendee2Layer = testManager.createDefaultLayerForUser(attendee2.getUid(), now.minus(Duration.standardDays(1)));

        // create event with initial decisions state
        val subjectId = UidOrResourceId.user((isByOrganizer ? organizer : attendee2).getUid());
        val subjectExchangeId = Random2.R.nextAlnum(8);
        val commonNamePrefix = "updateFromOrganizerDoesNotOverwriteAttendeeDecision(" + isByOrganizer + ")";
        val event = testManager.createDefaultEventInFuture(organizer.getUid(), commonNamePrefix + " - before");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee1.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee2.getUid(), Decision.UNDECIDED, false);
        eventUserDao.saveUpdateExchangeId(subjectId.getUid(), event.getId(), subjectExchangeId, exchangeTest());

        val subjectEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), subjectId.getUid()).get();

        // import event from exchange with updated decisions
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                commonNamePrefix + " - after", event.getLastUpdateTs().plus(1000));
        calItem.setItemId(EwsUtils.createItemId(subjectExchangeId));
        calItem.setIsMeeting(true);
        TestCalItemFactory.setOrganizer(calItem, organizerEmail);
        TestCalItemFactory.addAttendee(calItem, attendee1Email, Decision.UNDECIDED.getRespType());
        TestCalItemFactory.addAttendee(calItem, attendee2Email, Decision.MAYBE.getRespType());

        calItem.setMyResponseType(isByOrganizer ? ResponseTypeType.ACCEPT : ResponseTypeType.TENTATIVE);

        val actionInfo = exchangeTest(now);
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, actionInfo, false);

        Assert.A.equals(event.getId(), eventUserDao.findEventIdByUserExchangeId(subjectExchangeId).get());
        val participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        // attendee1 had made his decision before, so it should not have been overwritten now
        Assert.A.equals(Decision.YES, participants.getParticipantByEmail(attendee1Email).get().getDecision());
        // however, decision for attendee2 should have been changed
        Assert.A.equals(Decision.MAYBE, participants.getParticipantByEmail(attendee2Email).get().getDecision());

        if (isByOrganizer) {
            testStatusChecker
                    .checkForEventUpdaterOnEwsUpdate(subjectId.getUid(), event, subjectEventUser, calItem, actionInfo);
        } else {
            testStatusChecker
                    .checkForAttendeeOnEwsCreateRecurrenceOrUpdateDecision(subjectId.getUid(), event.getId(), calItem);
        }

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer, attendee1Layer, attendee2Layer), now);
    }

    @Test
    public void updateWithoutExchangeIdWithUserOrganizerFromUser() throws Exception {
        updateWithoutExchangeId();
    }

    private void updateWithoutExchangeId() throws Exception {
        val yaUser = testManager.prepareRandomYaTeamUser(11120);

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val layer = testManager.createDefaultLayerForUser(yaUser.getUid(), now.minus(Duration.standardDays(1)));

        val r = testManager.cleanAndCreateThreeLittlePigs();

        val event = testManager.createDefaultMeetingInFuture(yaUser.getUid(), "updateWithoutExchangeId");
        testManager.addUserParticipantToEvent(
                event.getId(), yaUser.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        val externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                event.getName(), event.getLastUpdateTs().plus(100));
        calItem.setUID(externalId);
        val organizerEmail = yaUser.getEmail();
        val attendeeEmail = ResourceRoutines.getResourceEmail(r);
        TestCalItemFactory.setOrganizer(calItem, organizerEmail);
        TestCalItemFactory.addAttendee(calItem, attendeeEmail, ResponseTypeType.ACCEPT);

        val subjectId = UidOrResourceId.user(yaUser.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, exchangeTest(now), false);

        val eventByExternalId = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(yaUser.getUid()), externalId).single();

        val eventByExchangeId = eventRoutines.findEventByExchangeId(calItem.getItemId().getId()).get();
        Assert.A.equals(eventByExternalId.getId(), eventByExchangeId.getId());

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(layer), now);
    }

    @Test
    public void mergeFromDifferentChannelsImporterParticipates() throws Exception {
        mergeFromDifferentChannels(true);
    }

    @Test
    public void mergeFromDifferentChannelsImporterDoesNotParticipate() throws Exception {
        mergeFromDifferentChannels(false);
    }

    private void mergeFromDifferentChannels(boolean importerParticipates) throws Exception {
        val organizer = testManager.prepareRandomYaTeamUser(11130);
        val uninvitedImporter = testManager.prepareRandomYaTeamUser(11131);
        val topYandexRu = new Email("top@yandex.ru");

        val now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);
        val organizerLayer = testManager.createDefaultLayerForUser(organizer.getUid(), now.minus(Duration.standardDays(1)));
        val uninvitedImporterLayer = testManager.createDefaultLayerForUser(uninvitedImporter.getUid(), now.minus(Duration.standardDays(1)));

        val event = testManager.createDefaultMeetingInFuture(organizer.getUid(), "mergeFromDifferentChannels");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.UNDECIDED, true);
        testManager.addExternalUserParticipantToEvent(event.getId(), topYandexRu, Decision.UNDECIDED, false);

        val exchangeId = EwsUtils.createItemId(Random2.R.nextAlnum(8));

        val externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                event.getName(), event.getLastUpdateTs().plus(100));
        calItem.setUID(externalId);
        calItem.setItemId(exchangeId);
        val organizerEmail = organizer.getEmail();
        val attendeeEmail = uninvitedImporter.getEmail();
        TestCalItemFactory.setOrganizer(calItem, organizerEmail);
        TestCalItemFactory.addAttendee(calItem, topYandexRu, ResponseTypeType.ACCEPT);
        if (importerParticipates) {
            TestCalItemFactory.addAttendee(calItem, attendeeEmail, ResponseTypeType.ACCEPT);
        }

        val uninvitedGuestSubjectId = UidOrResourceId.user(uninvitedImporter.getUid());
        val actionInfo = exchangeTest(now);
        ewsImporter.createOrUpdateEventForTest(uninvitedGuestSubjectId, calItem, actionInfo, false);

        val mainEvent = mainEventDao.findMainEventsByExternalId(new ExternalId(externalId)).single();
        val gotEvent = eventDao.findEventsByMainId(mainEvent.getId()).single();
        val participants = eventInvitationManager.getParticipantsByEventId(gotEvent.getId());
        Assert.A.isTrue(participants.isMeeting());
        Assert.A.equals(organizer.getUid(), participants.getOrganizer().getUid().get());
        val attendeeEmails = participants.getAllAttendeesButNotOrganizer().map(ParticipantInfo.getEmailF());
        if (importerParticipates) {
            Assert.assertContains(attendeeEmails, uninvitedImporter.getEmail());
        } else {
            Assert.A.isFalse(attendeeEmails.containsTs(uninvitedImporter.getEmail()));
        }

        val guestEventUser = eventUserDao.findEventUserByEventIdAndUid(
                event.getId(), uninvitedImporter.getUid()).single();
        Assert.A.equals(importerParticipates, guestEventUser.getIsAttendee());
        Assert.A.equals(exchangeId.getId(), guestEventUser.getExchangeId().get());
        if (importerParticipates) {
            Assert.A.hasSize(1, eventLayerDao.findEventLayersWithRelationsByEventIdAndLayerCreatorUid(
                    event.getId(), uninvitedImporter.getUid()));
        }

        Assert.assertContains(attendeeEmails, topYandexRu);

        if (importerParticipates) {
            testStatusChecker.checkForAttendeeOnEwsUpdateOrDelete(
                    uninvitedGuestSubjectId.getUid(), event, calItem, actionInfo, true);
            testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer, uninvitedImporterLayer), now);
        } else {
            testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer), now);
        }

    }

    @Test
    public void updateByUserOrganizerAndExternalId() throws Exception {
        userManager.registerYandexUserForTest(TestManager.createResourceMaster());
        val r = testManager.cleanAndCreateThreeLittlePigs();

        val user = new PassportLogin("yandex-team-mm-11140");
        val uid = userManager.getUidByLoginForTest(user);
        testManager.cleanUser(uid);

        val eventId = testManager.createDefaultEventInFuture(uid, "updateByUserOrganizerAndExternalId").getId();
        val event = eventDao.findEventById(eventId);
        val externalId = eventDao.findExternalIdByEventId(eventId);
        testManager.addResourceParticipantToEvent(eventId, r);
        testManager.addUserParticipantToEvent(eventId, uid, Decision.YES, true);

        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), tz), event.getName(), event.getLastUpdateTs().plus(1000));
        TestCalItemFactory.setOrganizer(calendarItem, userManager.getLdEmailByUid(uid));
        TestCalItemFactory.addAttendee(calendarItem, ResourceRoutines.getResourceEmail(r), ResponseTypeType.ACCEPT);
        calendarItem.setUID(externalId);
        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(uid), calendarItem, exchangeTest(), false);

        // just check, that new event wasn't created
        Assert.hasSize(1, mainEventDao.findMainEventsByResourceIds(
                Cf.list(r.getId()), mainEventDao.findEventIdsByExternalId(new ExternalId(externalId))));
    }

    @Test
    public void updateOccurrenceFromNotInvitedUserSubscription() throws Exception {
        val user1 = testManager.prepareUser("yandex-team-mm-11150");
        val user2 = testManager.prepareUser("yandex-team-mm-11151");

        val eventId = testManager.createDefaultEventInFuture(
                user1.getUid(), "updateOccurrenceFromNotInvitedUserSubscription").getId();
        testManager.createDailyRepetitionAndLinkToEvent(eventId);
        testManager.addUserParticipantToEvent(eventId, user1.getLogin(), Decision.YES, true);
        val event = eventDao.findEventById(eventId);
        val externalId = eventDao.findExternalIdByEventId(eventId);

        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val recurCalItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), tz), "Recurrence event name", event.getLastUpdateTs().plus(1000));
        TestCalItemFactory.setOrganizer(recurCalItem, user1.getEmail());
        TestCalItemFactory.addAttendee(recurCalItem, user1.getEmail(), ResponseTypeType.ACCEPT);
        recurCalItem.setUID(externalId);
        val recurrenceId = event.getStartTs();
        recurCalItem.setRecurrenceId(EwsUtils.instantToXMLGregorianCalendar(recurrenceId, tz));
        val actionInfo = exchangeTest(TestDateTimes.moscow(NEXT_YEAR, 12, 25, 2, 28));
        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(user2.getUid()), recurCalItem, actionInfo, false);

        Assert.A.hasSize(1, mainEventDao.findMainEventsByExternalId(new ExternalId(externalId)));
        val masterEvent1 = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(user1.getUid()), externalId).get();
        val masterEvent2 = eventRoutines.findMasterEventBySubjectIdAndExternalId(
                UidOrResourceId.user(user2.getUid()), externalId).get();
        Assert.A.equals(masterEvent1.getId(), masterEvent2.getId());

        // due to absence of initial user2's event-user at master event, it's pointless to check version like in
        // IcsImporterAttendeeUpdatesMeetingTest#importingOneRecurrenceByAttendeeDoesNotDeleteOthers()
        testStatusChecker.checkEventLastUpdateIsPreserved(event);
        testStatusChecker.checkMainEventLastUpdateIsUpdated(event, actionInfo);

        val recurEvent2 = eventRoutines
                .findEventByExternalIdAndRecurrence(externalId, Option.of(recurrenceId)).get();
        testStatusChecker.checkForAttendeeOnEwsCreateRecurrenceOrUpdateDecision(
                user2.getUid(), recurEvent2.getId(), recurCalItem);
    }

    @Test
    public void updateFromNonOrganizer() throws Exception {
        val organizer = testManager.prepareUser("yandex-team-mm-11160");
        val attendee1 = testManager.prepareUser("yandex-team-mm-11161");
        val attendee2 = testManager.prepareUser("yandex-team-mm-11162");
        val attendee3 = testManager.prepareUser("yandex-team-mm-11163");

        val eventId = testManager.createDefaultEventInFuture(organizer.getUid(), "Meeting").getId();
        val event = eventDao.findEventById(eventId);
        val externalId = eventDao.findExternalIdByEventId(eventId);
        testManager.addUserParticipantToEvent(eventId, organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, attendee1.getUid(), Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(eventId, attendee2.getUid(), Decision.NO, false);
        testManager.addUserParticipantToEvent(eventId, attendee3.getUid(), Decision.YES, false);

        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), tz), "Update meeting name");
        TestCalItemFactory.setOrganizer(calendarItem, organizer.getEmail());
        TestCalItemFactory.addAttendee(calendarItem, attendee1.getEmail(), ResponseTypeType.ACCEPT);
        TestCalItemFactory.addAttendee(calendarItem, attendee2.getEmail(), ResponseTypeType.UNKNOWN);
        calendarItem.setMyResponseType(ResponseTypeType.ACCEPT);
        calendarItem.setUID(externalId);
        calendarItem.setDateTimeStamp(
                EwsUtils.instantToXMLGregorianCalendar(event.getLastUpdateTs().plus(1000), tz));
        val actionInfo = exchangeTest();
        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(attendee1.getUid()), calendarItem, actionInfo, false);

        val participants =
                eventInvitationManager.getParticipantsByEventId(eventId).getParticipants();
        Assert.A.hasSize(4, participants);
        MapF<Email, Decision> decisionMap = Cf.hashMap();
        decisionMap.put(attendee1.getEmail(), Decision.YES);
        decisionMap.put(attendee2.getEmail(), Decision.NO);
        decisionMap.put(attendee3.getEmail(), Decision.YES);
        for (Email email : decisionMap.keys()) {
            val actualDecision =
                    participants.find(ParticipantInfo.getEmailF().andThenEquals(email))
                    .get().getDecision();
            Assert.A.equals(decisionMap.getTs(email), actualDecision, "Failed for email " + email);
        }
        val updatedEvent = eventDao.findEventById(eventId);
        Assert.A.equals(event.getName(), updatedEvent.getName());

        testStatusChecker
                .checkForAttendeeOnEwsCreateRecurrenceOrUpdateDecision(attendee1.getUid(), eventId, calendarItem);
    }

    @Test
    public void organizerCanNotDeleteParticipants() throws Exception {
        val organizer = testManager.prepareUser("yandex-team-mm-11171");
        val attendee = testManager.prepareUser("yandex-team-mm-11172");

        val eventId = testManager.createDefaultEventInFuture(organizer.getUid(), "organizerCanNotDeleteParticipants").getId();
        val event = eventDao.findEventById(eventId);
        val externalId = eventDao.findExternalIdByEventId(eventId);

        testManager.addUserParticipantToEvent(eventId, organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(eventId, attendee.getUid(), Decision.YES, false);

        val tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
        val newLastUpdate = event.getLastUpdateTs().plus(1000);
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), tz), "organizerCantDeleteParticipants", newLastUpdate);
        TestCalItemFactory.setOrganizer(calendarItem, organizer.getEmail());
        calendarItem.setUID(externalId);
        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(organizer.getUid()), calendarItem, exchangeTest(), false);

        Assert.A.some(
                eventLayerDao.findEventLayerWithLayerByEventIdAndLayerCreatorUid(event.getId(), attendee.getUid()));

        val attendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, attendee.getUid()).get();
        Assert.A.isTrue(attendeeEventUser.getIsAttendee());
        Assert.A.equals(Decision.YES, attendeeEventUser.getDecision());

        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(eventId, newLastUpdate);
    }

    /**
     * see hack for CAL-3207 in EwsImporter#getEventSynchronizationData(...)
     * @see #sameEventVersionByExternalIdCausesUpdate()
     * @see #staleEventVersionByExchangeIdDoesNotCauseUpdate()
     */
    @Test
    public void staleEventVersionByExternalIdCausesUpdate() throws Exception {
        val creator = testManager.prepareUser("yandex-team-mm-11173");

        val eventId = testManager.createDefaultEventWithEventLayerAndEventUserInFuture(
                creator.getUid(), "staleEventVersionByExternalIdCausesUpdate").getId();
        val event = eventDao.findEventById(eventId);
        val externalId = eventDao.findExternalIdByEventId(eventId);

        val staleLastUpdate = event.getLastUpdateTs().minus(1000);
        val newName = event.getName() + " - updated";
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                newName, staleLastUpdate);
        calendarItem.setUID(externalId);
        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(creator.getUid()), calendarItem, exchangeTest(), false);

        val updatedEvent = eventDao.findEventById(eventId);
        Assert.A.equals(newName, updatedEvent.getName());

        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(eventId, staleLastUpdate);
    }

    /**
     * @see #staleEventVersionByExternalIdCausesUpdate()
     */
    @Test
    public void staleEventVersionByExchangeIdDoesNotCauseUpdate() throws Exception {
        val creator = testManager.prepareUser("yandex-team-mm-11174");

        val eventId = testManager.createDefaultEventWithEventLayerAndEventUser(
                creator.getUid(), "staleEventVersionByExchangeIdDoesNotCauseUpdate").getId();
        val event = eventDao.findEventById(eventId);
        val exchangeId = Random2.R.nextAlnum(8);
        val actionInfo = exchangeTest().withActionSource(ActionSource.EXCHANGE_ASYNCH);
        eventUserDao.saveUpdateExchangeId(creator.getUid(), event.getId(), exchangeId, actionInfo);

        val staleLastUpdate = event.getLastUpdateTs().minus(1000);
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                event.getName() + " - updated", staleLastUpdate);
        calendarItem.setItemId(EwsUtils.createItemId(exchangeId));
        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(creator.getUid()), calendarItem, actionInfo, false);

        val notUpdatedEvent = eventDao.findEventById(eventId);
        Assert.A.equals(event.getName(), notUpdatedEvent.getName());

        testStatusChecker.checkEventLastUpdateIsPreserved(event);
    }

    // http://calendar-web.calendar-back01e.tools.yandex.net:81/z/event?q=723
    // (but organizer is user, not a resource here)
    @Test
    public void organizerUserUpdatesEventCreatedByAttendeeUser() throws Exception {
        val organizerUser = testManager.prepareUser("yandex-team-mm-11180");
        val attendeeUser = testManager.prepareUser("yandex-team-mm-11181");

        organizerUpdatesEventCreatedByAttendeeUser(
                UidOrResourceId.user(organizerUser.getUid()), organizerUser.getEmail(), attendeeUser);
    }

    private void organizerUpdatesEventCreatedByAttendeeUser(
            UidOrResourceId organizerSubjectId, Email organizerEmail, TestUserInfo attendeeUserInfo) throws Exception {

        val event = testManager.createDefaultEventWithEventLayerAndEventUserInFuture(
                attendeeUserInfo.getUid(), "organizerUpdatesEventCreatedByAttendeeUser");
        val externalId = eventDao.findExternalIdByEventId(event.getId());

        val newLastUpdate = event.getLastUpdateTs().plus(1000);
        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                new DateTime(event.getStartTs(), TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                "organizerUpdatesEventCreatedByAttendeeUser - updated",
                newLastUpdate);
        TestCalItemFactory.setOrganizer(calendarItem, organizerEmail);
        TestCalItemFactory.addAttendee(calendarItem, attendeeUserInfo.getEmail(), ResponseTypeType.ACCEPT);
        calendarItem.setUID(externalId);
        ewsImporter.createOrUpdateEventForTest(organizerSubjectId, calendarItem, exchangeTest(), false);

        Assert.A.hasSize(1, eventDao.findEventsByExternalIdAndRecurrenceId(externalId, Option.<Instant>empty()));
        Assert.A.equals(1, mainEventDao.findCountOfMainIds(new ExternalId(externalId)));

        testStatusChecker.checkEventLastUpdateIsUpdatedToIncomingValue(event.getId(), newLastUpdate);
    }

    @Test
    public void findUpdatedEventByUidAndFillMissingExchangeId() throws Exception {
        findByUidThenWatchExchangeId(false, false);
    }

    @Test
    public void findUpdatedEventByUidAndKeepExistingExchangeId() throws Exception {
        findByUidThenWatchExchangeId(false, true);
    }

    @Test
    public void findEventByUidThenUpdateAndFillMissingExchangeId() throws Exception {
        findByUidThenWatchExchangeId(true, false);
    }

    @Test
    public void findEventByUidThenUpdateAndRewriteExistingExchangeId() throws Exception {
        findByUidThenWatchExchangeId(true, true);
    }

    private void findByUidThenWatchExchangeId(boolean needToUpdate, boolean exchangeIdWasSet) throws Exception {
        val eventName = "findByUidThenFillMissingExchangeId(" + needToUpdate + ", " + exchangeIdWasSet + ")";
        val userOffset = 11190 + Binary.toLong(needToUpdate) * 2 + Binary.toLong(exchangeIdWasSet);
        val organizer = testManager.prepareRandomYaTeamUser(userOffset);
        val organizerId = UidOrResourceId.user(organizer.getUid());

        val event = testManager.createDefaultEventWithEventLayerAndEventUserInFuture(organizer.getUid(), eventName);
        val externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        Option<String> oldExchangeIdO;
        if (exchangeIdWasSet) {
            oldExchangeIdO = Option.of(Random2.R.nextAlnum(8));
            eventRoutines.saveUpdateExchangeId(organizerId, event.getId(), oldExchangeIdO.get(), exchangeTest());
        }

        val newExchangeId = Random2.R.nextAlnum(8);
        val lastModifiedTs = needToUpdate ?
                event.getLastUpdateTs().plus(1000) : event.getLastUpdateTs().minus(1000);
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                eventName, lastModifiedTs);
        calItem.setUID(externalId);
        calItem.setItemId(EwsUtils.createItemId(newExchangeId));
        ewsImporter.createOrUpdateEventForTest(organizerId, calItem, exchangeTest(), false);

        // even if 'exchangeIdWasSet && !needToUpdate', we rewrite old exchange id,
        // because old event doesn't exist in exchange
        val expectedExchangeId = newExchangeId;
        val foundExchangeIdO =
                eventRoutines.existsExchangeIdBySubjectIdAndEventId(organizerId, event.getId());
        Assert.A.some(foundExchangeIdO);
        Assert.A.equals(expectedExchangeId, foundExchangeIdO.get());
    }

    /**
     * test is broken to temporarily workaround CAL-3207
     * @see #staleEventVersionByExternalIdCausesUpdate()
     */
    @Test
    public void sameEventVersionByExternalIdCausesUpdate() throws Exception {
        val user = testManager.prepareRandomYaTeamUser(11195);

        val initialName = "sameEventVersionByExternalIdCausesUpdate - initial";
        val event = testManager.createDefaultEventWithEventLayerAndEventUserInFuture(user.getUid(), initialName);
        val externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());

        val updatingName = "sameEventVersionByExternalIdCausesUpdate - update attempt";
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(DateTimeZone.UTC), updatingName, event.getLastUpdateTs());
        calItem.setUID(externalId);
        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(user.getUid()), calItem, exchangeTest(), false);

        val newEvent = eventDao.findEventsByExternalIdAndRecurrenceId(externalId, Option.<Instant>empty()).single();
        Assert.A.equals(updatingName, newEvent.getName());
    }

    // https://jira.yandex-team.ru/browse/CAL-3057
    @Test
    public void noPointlessIsACopyMarking() throws Exception {
        val user = testManager.prepareRandomYaTeamUser(11196);
        val subjectId = UidOrResourceId.user(user.getUid());

        val deletedPhantomName = "noPointlessIsACopyMarking - deleted phantom event";
        val event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), deletedPhantomName);
        val externalId = mainEventDao.findExternalIdByMainEventId(event.getMainEventId());
        val oldExchangeId = Random2.R.nextAlnum(8);
        eventRoutines.saveUpdateExchangeId(subjectId, event.getId(), oldExchangeId, exchangeTest());

        val realNotACopyName = "noPointlessIsACopyMarking - real, not-a-copy, event";
        val calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                event.getStartTs().toDateTime(DateTimeZone.UTC), realNotACopyName, event.getLastUpdateTs());
        calItem.setUID(externalId);
        val newExchangeId = Random2.R.nextAlnum(8);
        calItem.setItemId(EwsUtils.createItemId(newExchangeId));
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, exchangeTest(), false);

        val ignoredEventO = ignoredEventDao.findIgnoredEventByExchangeId(newExchangeId);
        Assert.A.none(ignoredEventO); // neither is-a-copy, nor anything else
    }

    @Test
    public void updateEventWithResourceFromUser() throws Exception {
        val organizer = testManager.prepareUser("yandex-team-mm-11201");
        val resource = testManager.cleanAndCreateSmolny();

        val mailboxOwner = testManager.prepareUser("yandex-team-mm-11202");

        val event = testManager.createDefaultEventInFuture(organizer.getUid(), "updateEventWithResourceFromUser");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        val start = event.getStartTs().toDateTime(MoscowTime.TZ);
        val updateIgnoredStart = start.plusHours(3);

        val calendarItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                updateIgnoredStart, "updateEventWithResourceFromUser", event.getLastUpdateTs());

        TestCalItemFactory.setOrganizer(calendarItem, organizer.getEmail());
        TestCalItemFactory.addAttendee(calendarItem, ResourceRoutines.getResourceEmail(resource), ResponseTypeType.ACCEPT);
        calendarItem.setUID(eventDao.findExternalIdByEventId(event.getId()));

        Assert.none(eventUserDao.findEventUserByEventIdAndUid(event.getId(), mailboxOwner.getUid()));
        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), mailboxOwner.getUid()));

        ewsImporter.createOrUpdateEventForTest(
                UidOrResourceId.user(mailboxOwner.getUid()), calendarItem, exchangeTest(), false);

        Assert.notEquals(updateIgnoredStart.toInstant(), eventDao.findEventById(event.getId()).getStartTs());
        Assert.some(eventUserDao.findEventUserByEventIdAndUid(event.getId(), mailboxOwner.getUid()));
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), mailboxOwner.getUid()));
    }

    // CAL-7436
    @Test
    public void updateRecurrencesByOccurrences() {
        val organizer = testManager.prepareUser("yandex-team-mm-11202");
        Event master = testManager.createDefaultEventInFuture(organizer.getUid(), "Repeating");
        master.setRepetitionId(testManager.createDailyRepetitionAndLinkToEvent(master.getId()));
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);

        master = eventDao.findEventById(master.getId());
        CalendarItemType item = eventToCalendarItemConverter.convertToCalendarItem(
                eventDbManager.getEventWithRelationsByEvent(master),
                eventDbManager.getEventAndRepetitionByEvent(master).getRepetitionInfo());

        val outlooker = prepareOutlooker();

        val exchangeId = ewsProxyWrapper.createEvent(outlooker.getEmail(), item, EwsActionLogData.test());
        item = ewsProxyWrapper.getEvent(exchangeId).get();

        val recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        ewsImporter.createOrUpdateEventWithRecurrences(
                UidOrResourceId.user(outlooker.getUid()), item, exchangeTest(), false);

        Assert.some(eventUserDao.findEventUserByEventIdAndUid(master.getId(), outlooker.getUid()));
        Assert.some(eventUserDao.findEventUserByEventIdAndUid(recurrence.getId(), outlooker.getUid()));
    }

    // CAL-10015
    @Test
    public void processRecurrenceWithoutRecurrenceId() {
        val organizer = testManager.prepareRandomYaTeamUser(173);
        val outlooker = prepareOutlooker();

        val master = testManager.createDefaultEventInFuture(organizer.getUid(), "processRecurrence");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        Event recurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(3)));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);

        recurrence = eventDao.findEventById(recurrence.getId()).copy();
        recurrence.setRecurrenceIdNull();

        CalendarItemType item = eventToCalendarItemConverter.convertToCalendarItem(
                eventDbManager.getEventWithRelationsByEvent(recurrence),
                eventDbManager.getEventAndRepetitionByEvent(recurrence).getRepetitionInfo());

        val exchangeId = ewsProxyWrapper.createEvent(outlooker.getEmail(), item, EwsActionLogData.test());
        item = ewsProxyWrapper.getEvent(exchangeId).get();

        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.user(outlooker.getUid()), item, exchangeTest(), false);

        Assert.none(eventUserDao.findEventUserByEventIdAndUid(master.getId(), outlooker.getUid()));
        Assert.some(eventUserDao.findEventUserByEventIdAndUid(recurrence.getId(), outlooker.getUid()));
    }

    @Test
    public void exdateRecurrence() {
        val outlooker = prepareOutlooker();

        val master = testManager.createDefaultEventWithDailyRepetitionInFuture(outlooker.getUid(), "Repeating");
        testManager.addUserParticipantToEvent(master.getId(), outlooker.getUid(), Decision.YES, true);

        val item = eventToCalendarItemConverter.convertToCalendarItem(
                eventDbManager.getEventWithRelationsById(master.getId()),
                eventDbManager.getEventAndRepetitionByEvent(master).getRepetitionInfo());

        val exchangeId = ewsProxyWrapper.createEvent(outlooker.getEmail(), item, EwsActionLogData.test());

        val recurrence = testManager.createDefaultRecurrence(outlooker.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), outlooker.getUid(), Decision.YES, true);

        ewsProxyWrapper.cancelOrDeclineMeetingOccurrenceSafe(EwsModifyingItemId.fromEmailAndOccurrenceId(
                outlooker.getEmail(), OccurrenceId.fromEvent(item.getUID(), recurrence)), EwsActionLogData.test());

        ewsImporter.createOrUpdateEventWithRecurrences(UidOrResourceId.user(outlooker.getUid()),
                ewsProxyWrapper.getEvent(exchangeId).get(), exchangeTest(), false);

        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrence.getId())));
        Assert.equals(Cf.toList(recurrence.getRecurrenceId()), eventDao.findRdatesByEventId(master.getId()).map(Rdate::getStartTs));
    }

    @Test
    public void declineRecurrence() {
        val organizer = testManager.prepareRandomYaTeamUser(100500);
        val outlooker = prepareOutlooker();

        val master = testManager.createDefaultEventWithDailyRepetitionInFuture(outlooker.getUid(), "Repeating");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), outlooker.getUid(), Decision.YES, false);

        val item = eventToCalendarItemConverter.convertToCalendarItem(
                eventDbManager.getEventWithRelationsById(master.getId()),
                eventDbManager.getEventAndRepetitionByEvent(master).getRepetitionInfo());

        val exchangeId = ewsProxyWrapper.createEvent(outlooker.getEmail(), item, EwsActionLogData.test());

        val recurrence = testManager.createDefaultRecurrence(outlooker.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), outlooker.getUid(), Decision.YES, false);

        val occurrences = Cf.list(
                OccurrenceId.fromEvent(item.getUID(), recurrence),
                OccurrenceId.fromEvent(item.getUID(), recurrence).plusDays(1, MoscowTime.TZ));

        occurrences.forEach(occurrence -> ewsProxyWrapper.cancelOrDeclineMeetingOccurrenceSafe(
                EwsModifyingItemId.fromEmailAndOccurrenceId(outlooker.getEmail(), occurrence), EwsActionLogData.test()));

        ewsImporter.createOrUpdateEventWithRecurrences(UidOrResourceId.user(outlooker.getUid()),
                ewsProxyWrapper.getEvent(exchangeId).get(), exchangeTest(master.getStartTs()), false);

        val eventUsers = eventUserDao.findEventUsersByEventIdsAndUid(
                eventDao.findRecurrenceEventsByMainId(master.getMainEventId()).map(Event::getId), outlooker.getUid());

        Assert.equals(Cf.repeat(Decision.NO, 2), eventUsers.map(EventUser::getDecision));
    }

    private TestUserInfo prepareOutlooker() {
        return testManager.prepareYandexUser(new YandexUser(
                TestUsers.DBRYLEV, PassportLogin.cons(TestManager.testExchangeUserEmail.getLocalPart()),
                Option.empty(), Option.of(TestManager.testExchangeUserEmail), Option.empty(), Option.empty(), Option.empty()));
    }
}
