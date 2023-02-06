package ru.yandex.calendar.logic.event;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.SingleRecipientType;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.EventUserFields;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.meeting.UpdateMode;
import ru.yandex.calendar.logic.event.model.EventInvitationData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.event.model.ParticipantsOrInvitationsData;
import ru.yandex.calendar.logic.event.model.WebReplyData;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.ics.exp.EventInstanceParameters;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sending.EventSendingInfo;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.EventParticipantsChangesInfo;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.participant.UserParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.YandexUserParticipantInfo;
import ru.yandex.calendar.logic.user.SettingsInfo;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author Stepan Koltsov
 */
public class EventInvitationManagerTest extends AbstractConfTest {

    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private RepetitionRoutines repetitionRoutines;

    @Before
    public void setup() {
        mailSender.clear();
    }

    @Test
    public void fixParticipants() {
        PassportLogin user1 = new PassportLogin("yandex-team-mm-10571");
        PassportLogin user2 = new PassportLogin("yandex-team-mm-10572");

        PassportUid uid1 = userManager.getUidByLoginForTest(user1);
        PassportUid uid2 = userManager.getUidByLoginForTest(user2);

        testManager.cleanUser(uid1);
        testManager.cleanUser(uid2);

        Event event = testManager.createDefaultEvent(uid1, "dfdf");

        Assert.assertFalse(eventInvitationManager.getParticipantsByEventId(event.getId()).isMeeting());

        testManager.addUserParticipantToEvent(event.getId(), user2, Decision.YES, false);

        // make sure testManager hasn't created organizer
        Assert.A.hasSize(1, eventUserDao.findEventUsers(EventUserFields.EVENT_ID.eq(event.getId())));

        Participants participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        Assert.assertTrue(participants.isInconsistent());

        eventInvitationManager.fixParticipants(event.getId(), ActionInfo.webTest());
        participants = eventInvitationManager.getParticipantsByEventId(event.getId());
        Assert.assertTrue(participants.isMeeting());
    }

    @Test
    public void handleYaUserInvitationYesDecision() {
        handleYaUserInvitationDecision(Decision.YES);
    }

    @Test
    public void handleYaUserInvitationWithNoDecision() {
        handleYaUserInvitationDecision(Decision.NO);
    }

    @Test
    public void handleYaUserInvitationWithMaybeDecision() {
        handleYaUserInvitationDecision(Decision.MAYBE);
    }

    private void handleYaUserInvitationDecision(Decision decision) {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10501");

        PassportUid organizerUid = organizer.getUid();
        PassportUid attendeeUid = testManager.prepareUser("yandex-team-mm-10502").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "Apple Event");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendeeUid, Decision.UNDECIDED, false);

        ParticipantInfo participant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(attendeeUid)).get();
        Assert.A.equals(Decision.UNDECIDED, participant.getDecision());
        Instant now = event.getStartTs();

        ParticipantInfo previousInvitationCache =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                    event.getId(), ParticipantId.yandexUid(attendeeUid)).get();

        WebReplyData reply = new WebReplyData(
                decision, Option.of("It's my reason!"), Option.<Long>empty(),
                Option.<Availability>empty(), NotificationsData.createEmpty());

        ActionInfo actionInfo = ActionInfo.webTest(now); // XXX web is wrong // ssytnik@
        eventInvitationManager.handleEventInvitationDecision(
                attendeeUid, (UserParticipantInfo) previousInvitationCache, reply, false, actionInfo);

        ParticipantInfo updatedParticipant = eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(attendeeUid)).get();

        Assert.equals(decision, updatedParticipant.getDecision());
        Assert.some(updatedParticipant.getReason(), reply.getReason());

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), attendeeUid));

        testStatusChecker.checkEventLastUpdateIsPreserved(event);
        testStatusChecker.checkMainEventLastUpdateIsUpdated(event, actionInfo);

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
    }

    @Test
    public void handleYaUserInvitationDecisionToNondefaultLayer() {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-10511").getUid();
        PassportUid attendeeUid = testManager.prepareUser("yandex-team-mm-10512").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "Apple Event");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendeeUid, Decision.UNDECIDED, false);

        long defaultLayerId = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(
                    event.getId(), attendeeUid
                ).get().getLayerId();
        long layerId = layerRoutines.createUserLayer(attendeeUid, Option.of("Non default layer"));
        Assert.A.notEquals(layerId, defaultLayerId);

        ParticipantInfo previousInvitationCache =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                    event.getId(), ParticipantId.yandexUid(attendeeUid)).get();

        WebReplyData reply = new WebReplyData(
                Decision.YES, Option.of("It's my reason!"), Option.of(layerId),
                Option.<Availability>empty(), NotificationsData.createEmpty());

        eventInvitationManager.handleEventInvitationDecision(
                attendeeUid, (UserParticipantInfo) previousInvitationCache, reply, false, ActionInfo.webTest());

        long updatedLayerId =
            eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(
                        event.getId(), attendeeUid
                    ).get().getLayerId();
        Assert.A.equals(updatedLayerId, layerId);
    }

    @Test
    public void handleYaUserInvitationByAnotherYaUser() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10541");

        PassportUid organizerUid = organizer.getUid();
        PassportUid invitedAttendeeUid = testManager.prepareUser("yandex-team-mm-10542").getUid();
        PassportUid actingUid = testManager.prepareUser("yandex-team-mm-10543").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "Apple Event");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), invitedAttendeeUid, Decision.UNDECIDED, false);

        ParticipantInfo previousInvitationCache =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                    event.getId(), ParticipantId.yandexUid(invitedAttendeeUid)).get();
        WebReplyData reply = new WebReplyData(
                Decision.YES, Option.<String>empty(), Option.<Long>empty(),
                Option.<Availability>empty(), NotificationsData.createEmpty());

        eventInvitationManager.handleEventInvitationDecision(
                actingUid, (UserParticipantInfo) previousInvitationCache, reply, false, ActionInfo.webTest(event.getStartTs()));

        ParticipantInfo invitedParticipantAfterHandling =
            eventInvitationManager.getParticipantByEventIdAndParticipantId(
                    event.getId(), ParticipantId.yandexUid(invitedAttendeeUid)).get();
        ParticipantInfo actingParticipantAfterHandling =
            eventInvitationManager.getParticipantByEventIdAndParticipantId(
                    event.getId(), ParticipantId.yandexUid(actingUid)).get();

        Assert.A.equals(invitedParticipantAfterHandling.getDecision(), Decision.UNDECIDED);
        Assert.A.equals(actingParticipantAfterHandling.getDecision(), Decision.YES);

        Assert.A.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), invitedAttendeeUid));
        Assert.A.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), actingUid));

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
        Assert.some(actingUid, mailSender.getEventMessageParameters().single().getSender().getUid());
    }

    // https://jira.yandex-team.ru/browse/CAL-3194
    // Xml creation failed, because event user's private token was not set.
    @Test
    public void createMajorEventInvitationMailXml() {
        PassportUid senderUid = testManager.prepareUser("yandex-team-mm-10545").getUid();
        PassportUid recipientUid = testManager.prepareUser("yandex-team-mm-10546").getUid();

        Event event = testManager.createDefaultEvent(senderUid, "createMajorEventInvitationMailXml");
        testManager.addUserParticipantToEvent(event.getId(), senderUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), recipientUid, Decision.UNDECIDED, false);

        EventUser recipientEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), recipientUid).get();
        SettingsInfo recipientSettings = settingsRoutines.getSettingsByUid(recipientUid);
        UserParticipantInfo invitation = new YandexUserParticipantInfo(recipientEventUser, recipientSettings);

        EventInstanceForUpdate instance = eventRoutines.getEventInstanceForModifier(
                Option.empty(), Option.empty(), event.getId(), Option.empty(), ActionInfo.webTest());

        EventSendingInfo sendingInfo = new EventSendingInfo(
                invitation, MailType.EVENT_UPDATE,
                EventInstanceParameters.fromEvent(event),
                Option.of(new ChangedEventInfoForMails(instance, EventChangesInfoForMails.timeOrRepetitionChanged())));

        eventInvitationManager.createEventInvitationOrCancelMails(
                ActorId.user(senderUid), Cf.list(sendingInfo), ActionInfo.webTest());
    }

    @Test
    public void createNotMeeting() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(575);

        ListF<Email> emails = Cf.list(creator.getEmail(), creator.getEmail());
        EventParticipantsChangesInfo changes = eventInvitationManager.participantsChanges(
                Option.of(creator.getUid()), Participants.notMeeting(),
                ParticipantsOrInvitationsData.eventInvitationData(new EventInvitationsData(emails)));

        Assert.isEmpty(changes.getNewParticipants());
    }

    @Test
    public void participantsChangesToMeeting() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(577);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(775);

        ListF<Email> emails = Cf.list(creator.getEmail(), invitee.getEmail());
        EventParticipantsChangesInfo changes = eventInvitationManager.participantsChanges(
                Option.of(creator.getUid()), Participants.notMeeting(),
                ParticipantsOrInvitationsData.eventInvitationData(new EventInvitationsData(emails)));

        Assert.sizeIs(2, changes.getNewParticipants());
        ParticipantData inviteeData = changes.getNewParticipantsExclOrganizer().single();
        ParticipantData creatorData = changes.getNewParticipants().get2().find(Cf.Object.equalsF(inviteeData).notF()).single();

        Assert.isTrue(inviteeData.isAttendee());
        Assert.isTrue(creatorData.isAttendee());
    }

    @Test
    public void participantsChangesToNotMeeting() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(579);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(975);

        ListF<Email> emails = Cf.list(creator.getEmail());
        Event event = testManager.createDefaultEvent(creator.getUid(), "participantsChangesToNotMeeting");
        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), invitee.getUid(), Decision.MAYBE, false);

        EventParticipantsChangesInfo changes = eventInvitationManager.participantsChanges(
                Option.of(creator.getUid()), eventInvitationManager.getParticipantsByEventId(event.getId()),
                ParticipantsOrInvitationsData.eventInvitationData(new EventInvitationsData(emails)));

        ListF<PassportUid> removedUids = changes.getRemovedParticipants().filterMap(ParticipantInfo.getUidF());
        Assert.equals(Cf.set(creator.getUid(), invitee.getUid()), removedUids.unique());
        Assert.isTrue(changes.isChangedToNotMeeting());
    }

    @Test
    public void participantsChangesOrganizer() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(779);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(977);
        TestUserInfo newOrganizer = testManager.prepareRandomYaTeamUser(797);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "participantsChangesToNotMeeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), invitee.getUid(), Decision.MAYBE, false);

        EventInvitationsData invitations = new EventInvitationsData(
                Option.of(newOrganizer.getEmail()),
                Cf.list(invitee.getEmail()).map(EventInvitationData.consWithNoNameF()));

        EventParticipantsChangesInfo changes = eventInvitationManager.participantsChanges(
                Option.of(organizer.getUid()), eventInvitationManager.getParticipantsByEventId(event.getId()),
                ParticipantsOrInvitationsData.eventInvitationData(invitations));

        Assert.some(ParticipantId.yandexUid(newOrganizer.getUid()), changes.getNewOrganizer());
        Assert.some(ParticipantId.yandexUid(organizer.getUid()), changes.getOldOrganizers().singleO());
    }

    @Test
    public void participantsKeepOrganizer() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(779);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(977);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "participantsKeepOrganizer");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), invitee.getUid(), Decision.MAYBE, false);

        Option<Email> organizerEmail = Option.empty();
        EventInvitationsData invitations = new EventInvitationsData(
                organizerEmail, Cf.list(invitee.getEmail()).map(EventInvitationData.consWithNoNameF()));

        EventParticipantsChangesInfo changes = eventInvitationManager.participantsChanges(
                Option.of(organizer.getUid()), eventInvitationManager.getParticipantsByEventId(event.getId()),
                ParticipantsOrInvitationsData.eventInvitationData(invitations));

        Assert.none(changes.getNewOrganizer());
        Assert.isEmpty(changes.getOldOrganizers());
    }

    @Test
    public void fixParticipantsWithIntersectingEmails() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(579);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(975);
        Email externalUser = new Email("external-user@yandex.ru");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "participantsWithIntersectingEmails");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        testManager.addExternalUserParticipantToEvent(event.getId(), externalUser, Decision.UNDECIDED, false);

        Settings newSettings = new Settings();
        newSettings.setEmail(externalUser);

        settingsRoutines.updateSettingsByUid(newSettings, attendee.getUid());

        ActionInfo actionInfo = ActionInfo.webTest();

        eventInvitationManager.fixParticipants(event.getId(), actionInfo);
    }

    private ListF<Event> createMeetingWithPastAndFutureRecurrences(PassportUid organizer) {
        Event master = testManager.createDefaultEventWithDailyRepetition(organizer, "Event");

        Instant pastRecurrenceId = master.getStartTs().plus(Duration.standardDays(1));

        Instant futureRecurrenceId = testManager.getRecurrenceIdInFuture(master, Instant.now());

        Event pastRecurrence =
                testManager.createDefaultRecurrence(organizer, master.getId(), pastRecurrenceId);
        Event futureRecurrence =
                testManager.createDefaultRecurrence(organizer, master.getId(), futureRecurrenceId);

        ListF<Event> events = Cf.list(master, pastRecurrence, futureRecurrence);

        events.forEach(event -> {
            testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.YES, true);
        });

        return events;
    }

    @Test
    public void rejectMeetingWithPastAndFutureRecurrences() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(579);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(975);

        ListF<Event> events = createMeetingWithPastAndFutureRecurrences(organizer.getUid());

        events.forEach(event -> {
            testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        });

        ParticipantInfo previousInvitationCache =
                eventInvitationManager.getParticipantByEventIdAndParticipantId(
                        events.first().getId(), ParticipantId.yandexUid(attendee.getUid())).get();

        WebReplyData reply = new WebReplyData(
                Decision.NO, Option.empty(), Option.empty(), Option.empty(), NotificationsData.createEmpty());

        ActionInfo actionInfo = ActionInfo.webTest(Instant.now());
        eventInvitationManager.handleEventInvitationDecision(
                attendee.getUid(), (UserParticipantInfo) previousInvitationCache, reply, true, actionInfo);

        ListF<Decision> decisions = events.map(event -> eventInvitationManager.getParticipantByEventIdAndParticipantId(
                event.getId(), ParticipantId.yandexUid(attendee.getUid())).get().getDecision());

        Assert.equals(Cf.list(Decision.NO, Decision.YES, Decision.NO), decisions);
    }

    @Test
    public void addUserToMeetingWithPastAndFutureRecurrences() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(579);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(975);

        ListF<Event> events = createMeetingWithPastAndFutureRecurrences(organizer.getUid());

        ParticipantId participantId = ParticipantId.yandexUid(attendee.getUid());
        ParticipantData participantData =
                new ParticipantData(attendee.getEmail(), "attendee", Decision.UNDECIDED, true, false, false);

        ActionInfo actionInfo = ActionInfo.webTest(Instant.now());

        Event event = eventDbManager.getEventByIdForUpdate(events.first().getId());

        ListF<EventSendingInfo> sendingInfos = eventInvitationManager.createNewEventInvitations(
                ActorId.user(organizer.getUid()),
                Tuple2List.fromPairs(participantId, participantData),
                eventDbManager.getEventWithRelationsByEvent(event),
                eventDbManager.getEventAndRepetitionByEvent(event).getRepetitionInfo(),
                false, UpdateMode.NORMAL, actionInfo).getSendingInfos();

        Assert.hasSize(1, sendingInfos);
        Assert.equals(events.first().getId(), sendingInfos.single().getEventId());
    }

    @Test
    public void sendDecisionFixingMailsTest() throws Exception {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(634);
        TestUserInfo ewsAttendee = testManager.prepareRandomYaTeamUser(635);
        TestUserInfo nonEwsAttendee = testManager.prepareRandomYaTeamUser(636);
        TestUserInfo nonEwsDeclinedAttendee = testManager.prepareRandomYaTeamUser(637);

        testManager.updateIsEwser(organizer, ewsAttendee);

        Event event = testManager.createDefaultEwsExportedEvent(
                organizer.getUid(), "event with two attendees");

        testManager.openEventAndLayer(event.getId(), organizer.getDefaultLayerId());

        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), ewsAttendee, Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), nonEwsAttendee, Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), nonEwsDeclinedAttendee, Decision.NO, false);


        EventWithRelations eventWithRelations = eventDbManager.getEventWithRelationsById(event.getId());
        RepetitionInstanceInfo repetitionInfo = repetitionRoutines.getRepetitionInstanceInfo(eventWithRelations);
        try {
            EwsProxyWrapper mockEwsProxyWrapper = Mockito.mock(EwsProxyWrapper.class);
            CalendarItemType calItem = mockEventForDecisionFixAccept(event.getName(), organizer.getEmail(),
                    Tuple2List.fromPairs(
                            ewsAttendee, Decision.UNDECIDED,
                            nonEwsAttendee, Decision.UNDECIDED,
                            nonEwsDeclinedAttendee, Decision.NO).map1(TestUserInfo::getEmail).toMap());
            Mockito.when(mockEwsProxyWrapper.getEvent(Mockito.anyString())).thenReturn(Option.of(calItem));
            Mockito.when(mockEwsProxyWrapper
                    .findMasterAndSingleOrInstanceEventId(
                            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                    .thenReturn(Option.of("someId"));

            eventInvitationManager.setEwsProxyWrapperForTests(mockEwsProxyWrapper);
            ewsExportRoutines.setEwsProxyWrapperForTest(mockEwsProxyWrapper);

            eventInvitationManager
                    .sendDecisionFixingMailsIfNeeded(eventWithRelations, repetitionInfo, ActionInfo.webTest());
        } finally {
            eventInvitationManager.setEwsProxyWrapperForTests(ewsProxyWrapper);
            ewsExportRoutines.setEwsProxyWrapperForTest(ewsProxyWrapper);
        }

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_REPLY, mailSender.findEventMailType(organizer.getEmail()));
    }

    private CalendarItemType mockEventForDecisionFixAccept(
            String subject, Email organizerEmail, MapF<Email, Decision> attendees) throws Exception
    {
        DateTime dateTime = new DateTime(2010, 5, 18, 16, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(dateTime, subject);
        NonEmptyArrayOfAttendeesType attendeesArray = new NonEmptyArrayOfAttendeesType();

        SingleRecipientType organizer = new SingleRecipientType();
        organizer.setMailbox(EwsUtils.createEmailAddressType(organizerEmail));
        calItem.setOrganizer(organizer);

        for (Tuple2<Email, Decision> attendee : attendees.entries().toList()) {
            AttendeeType attendeeType = new AttendeeType();
            attendeeType.setMailbox(EwsUtils.createEmailAddressType(attendee._1));
            ResponseTypeType response;
            switch (attendee._2) {
                case YES:
                    response = ResponseTypeType.ACCEPT;
                    break;
                case NO:
                    response = ResponseTypeType.DECLINE;
                    break;
                case MAYBE:
                    response = ResponseTypeType.TENTATIVE;
                    break;
                default:
                    response = ResponseTypeType.UNKNOWN;
            }

            attendeeType.setResponseType(response);
            attendeesArray.getAttendee().add(attendeeType);
        }

        calItem.setRequiredAttendees(attendeesArray);

        return calItem;
    }

} //~
