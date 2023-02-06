package ru.yandex.calendar.logic.event.meeting;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActorId;
import ru.yandex.calendar.logic.event.ChangedEventInfoForMails;
import ru.yandex.calendar.logic.event.EventChangesInfoForMails;
import ru.yandex.calendar.logic.event.EventInstanceForUpdate;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.EventInstanceStatusInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.sending.EventSendingInfo;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.param.InvitationMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.EventParticipantsChangesInfo;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.ParticipantChangesInfo;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.UserParticipantInfo;
import ru.yandex.calendar.logic.sharing.perm.LayerInfoForPermsCheck;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.UserGroupsDao;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * Details: http://wiki.yandex-team.ru/Calendar/sharing/messages
 * @author akirakozov
 * @author shinderuk
 */
@AbstractEwsExportedLoginsTest.WantsEws
public class UpdateMeetingHandlerTest extends AbstractEwsExportedLoginsTest {

    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private MailSenderMock mailSenderMock;
    @Autowired
    private UpdateMeetingHandler updateMeetingHandler;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private UserGroupsDao userGroupsDao;
    @Autowired
    private EventRoutines eventRoutines;

    private TestUserInfo organizer;

    private PassportUid organizerUid;
    private PassportUid yesAttendeeUid;
    private PassportUid noAttendeeUid;
    private PassportUid maybeAttendeeUid;
    private PassportUid undecidedAttendeeUid;
    private PassportUid superUserUid;
    private PassportUid subscriberUid;

    private Email organizerEmail;
    private Email yesAttendeeEmail;
    private Email noAttendeeEmail;
    private Email maybeAttendeeEmail;
    private Email undecidedAttendeeEmail;
    private Email subscriberEmail;
    private final Email invitingAttendeeEmail = new Email("cal-dev@yandex-team.ru");

    private Event event;

    private final ActionInfo actionInfo = ActionInfo.webTest(TestDateTimes.moscow(2011, 11, 21, 21, 28));

    public UpdateMeetingHandlerTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void setUpAll() {
        super.setUpAll();
        mailSenderMock.clear();
        createParticipants();
        createEvent();
        setIsEwserIfNeeded(organizer);
        setIsExportedWithEwsIfNeeded(event);
    }

    private void createParticipants() {
        organizer = testManager.prepareRandomYaTeamUser(10571);

        organizerUid = organizer.getUid();
        yesAttendeeUid = testManager.prepareRandomYaTeamUser(10572).getUid();
        noAttendeeUid = testManager.prepareRandomYaTeamUser(10573).getUid();
        maybeAttendeeUid = testManager.prepareRandomYaTeamUser(10574).getUid();
        undecidedAttendeeUid = testManager.prepareRandomYaTeamUser(10575).getUid();

        superUserUid = testManager.prepareRandomYaTeamUser(10576).getUid();
        userGroupsDao.addGroup(Either.left(superUserUid), Group.SUPER_USER);

        subscriberUid = testManager.prepareRandomYaTeamSuperUser(10577).getUid();

        organizerEmail = userManager.getEmailByUid(organizerUid).get();
        yesAttendeeEmail = userManager.getEmailByUid(yesAttendeeUid).get();
        noAttendeeEmail = userManager.getEmailByUid(noAttendeeUid).get();
        maybeAttendeeEmail =  userManager.getEmailByUid(maybeAttendeeUid).get();
        undecidedAttendeeEmail = userManager.getEmailByUid(undecidedAttendeeUid).get();
        subscriberEmail = userManager.getEmailByUid(subscriberUid).get();
    }

    private void createEvent() {
        Event eventData = testManager.createDefaultEventData(organizerUid, "meeting").getEvent();
        eventData.setParticipantsInvite(true);

        event = testManager.createDefaultEvent(organizerUid, "meeting", eventData);

        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), yesAttendeeUid, Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), noAttendeeUid, Decision.NO, false);
        testManager.addUserParticipantToEvent(event.getId(), maybeAttendeeUid, Decision.MAYBE, false);
        testManager.addUserParticipantToEvent(event.getId(), undecidedAttendeeUid, Decision.UNDECIDED, false);

        testManager.addSubscriberToEvent(event.getId(), subscriberUid);
    }

    @Test
    public void receiveEmailsWithoutEventLayer() {
        eventLayerDao.deleteEventLayerByEventIdAndLayerCreatorUid(event.getId(), organizerUid);
        eventLayerDao.deleteEventLayerByEventIdAndLayerCreatorUid(event.getId(), noAttendeeUid);
        updateMeetingByAttendee();
    }

    @Test
    public void updateMeetingByOrganizer() {
        EventChangesInfoForMails changes = EventChangesInfoForMails.timeOrRepetitionChanged();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);
    }

    @Test
    public void updateMeetingByAttendee() {
        EventChangesInfoForMails changes = EventChangesInfoForMails.timeOrRepetitionChanged();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(yesAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);
    }

    @Test
    public void updateMeetingByNoAttendee() {
        EventChangesInfoForMails changes = EventChangesInfoForMails.timeOrRepetitionChanged();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(noAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);
    }

    @Test
    public void updateMeetingMakeAttendeesUndecided() {
        EventChangesInfoForMails changes = EventChangesInfoForMails.timeOrRepetitionChanged();
        MeetingMailRecipients recipients = MeetingMailRecipients.ALL_PARTICIPANTS_AND_SUBSCRIBERS;

        eventLayerDao.deleteEventLayerByEventIdAndLayerCreatorUid(event.getId(), noAttendeeUid);

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                noAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);

        Option<EventUser> noEu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), noAttendeeUid);
        Option<EventUser> yesEu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), yesAttendeeUid);
        Option<EventUser> organizerEu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), organizerUid);
        Option<EventUser> subscriberEu = eventUserDao.findEventUserByEventIdAndUid(event.getId(), subscriberUid);

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), noAttendeeUid));

        Assert.some(Decision.UNDECIDED, noEu.map(EventUser.getDecisionF()));
        Assert.some(Decision.UNDECIDED, yesEu.map(EventUser.getDecisionF()));
        Assert.some(Decision.YES, organizerEu.map(EventUser.getDecisionF()));
        Assert.some(Decision.YES, subscriberEu.map(EventUser.getDecisionF()));
    }

    @Test
    public void removeAttendeeAndInviteNewByOrganizer() {
        EventChangesInfoForMails changes = removeUndecidedAttendeeAndInviteNewChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.INVITED_AND_REMOVED;

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                invitingAttendeeEmail, MailType.EVENT_INVITATION,
                undecidedAttendeeEmail, MailType.EVENT_CANCEL);
    }

    @Test
    public void removeAttendeeAndInviteNewByAttendee() {
        EventChangesInfoForMails changes = removeUndecidedAttendeeAndInviteNewChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.INVITED_AND_REMOVED;

        handleUpdateAndAssertSendingMailTypesAre(yesAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                invitingAttendeeEmail, MailType.EVENT_INVITATION,
                undecidedAttendeeEmail, MailType.EVENT_CANCEL);
    }

    @Test
    public void removeAttendeeAndInviteNewBySuperUser() {
        EventChangesInfoForMails changes = removeUndecidedAttendeeAndInviteNewChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.INVITED_AND_REMOVED;

        handleUpdateAndAssertSendingMailTypesAre(superUserUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                invitingAttendeeEmail, MailType.EVENT_INVITATION,
                undecidedAttendeeEmail, MailType.EVENT_CANCEL);
    }

    @Test
    public void inviteNewOrganizerAndRemoveOldByOrganizer() {
        EventChangesInfoForMails changes = inviteNewOrganizerAndRemoveOldChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_CANCEL,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                invitingAttendeeEmail, MailType.EVENT_INVITATION,
                subscriberEmail, MailType.EVENT_UPDATE);
    }

    @Test
    public void inviteNewOrganizerAndRemoveOldByAttendee() {
        EventChangesInfoForMails changes = inviteNewOrganizerAndRemoveOldChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(yesAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_CANCEL,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                invitingAttendeeEmail, MailType.EVENT_INVITATION,
                subscriberEmail, MailType.EVENT_UPDATE);
    }

    @Test
    public void makeYesAttendeeOrganizerByOrganizer() {
        EventChangesInfoForMails changes = makeYesAttendeeOrganizerChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);

        Assert.equals(yesAttendeeUid, findPrimaryLayer().getCreatorUid());
    }

    @Test
    public void makeYesAttendeeOrganizerByAttendee() {
        EventChangesInfoForMails changes = makeYesAttendeeOrganizerChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(yesAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);

        Assert.equals(yesAttendeeUid, findPrimaryLayer().getCreatorUid());
    }

    @Test
    public void makeYesAttendeeOrganizerAndRemoveOldByOrganizer() {
        EventChangesInfoForMails changes = makeYesAttendeeOrganizerAndRemoveOldChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(organizerUid, changes, recipients,
                organizerEmail, MailType.EVENT_CANCEL,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);

        Assert.equals(yesAttendeeUid, findPrimaryLayer().getCreatorUid());
    }

    @Test
    public void makeYesAttendeeOrganizerAndRemoveOldByAttendee() {
        EventChangesInfoForMails changes = makeYesAttendeeOrganizerAndRemoveOldChangesInfo();
        MeetingMailRecipients recipients = MeetingMailRecipients.NOT_REJECTED_PARTICIPANTS_AND_SUBSCRIBERS;

        handleUpdateAndAssertSendingMailTypesAre(yesAttendeeUid, changes, recipients,
                organizerEmail, MailType.EVENT_CANCEL,
                yesAttendeeEmail, MailType.EVENT_UPDATE,
                maybeAttendeeEmail, MailType.EVENT_UPDATE,
                undecidedAttendeeEmail, MailType.EVENT_UPDATE,
                subscriberEmail, MailType.EVENT_UPDATE);

        Assert.equals(yesAttendeeUid, findPrimaryLayer().getCreatorUid());
    }

    @Test
    public void changeToNotMeetingByOrganizer() {
        changeToNotMeeting(organizerUid);
    }

    @Test
    public void changeToNotMeetingByAttendee() {
        changeToNotMeeting(yesAttendeeUid);
    }

    private void changeToNotMeeting(PassportUid actor) {
        EventChangesInfoForMails changes = new EventChangesInfoForMails(false, false, false, false,
                EventParticipantsChangesInfo.changedToNotMeeting(
                        Cf.list(organizerEmail, yesAttendeeEmail, maybeAttendeeEmail, undecidedAttendeeEmail)
                                .map(this::getParticipant)));

        handleUpdateAndAssertSendingMailTypesAre(
                actor, changes, MeetingMailRecipients.INVITED_AND_REMOVED,
                organizerEmail, MailType.EVENT_UPDATE,
                yesAttendeeEmail, MailType.EVENT_CANCEL,
                maybeAttendeeEmail, MailType.EVENT_CANCEL,
                undecidedAttendeeEmail, MailType.EVENT_CANCEL);
    }

    private void handleUpdateAndAssertSendingMailTypesAre(
            PassportUid actorUid, EventChangesInfoForMails changes, MeetingMailRecipients recipients,
            Email email1, MailType expectedMailType1, Object... others)
    {
        handleUpdateAndAssertSendingMailTypesAre(actorUid, changes, recipients,
                Tuple2List.<Email, MailType>fromPairs(others).plus1(email1, expectedMailType1));
    }

    private void handleUpdateAndAssertSendingMailTypesAre(
            PassportUid actorUid, EventChangesInfoForMails changes,
            MeetingMailRecipients recipients, Tuple2List<Email, MailType> expectedMailTypes)
    {
        boolean organizerIsEwsLogin = isEwser(organizer);

        expectedMailTypes =
                expectedMailTypes.filter((email, type) -> email.equalsIgnoreCase(subscriberEmail) || type == MailType.EVENT_INVITATION || !organizerIsEwsLogin);

        EventInstanceForUpdate instance = eventRoutines.getEventInstanceForModifier(
                Option.empty(), Option.empty(), event.getId(), Option.empty(), ActionInfo.webTest());

        ListF<EventSendingInfo> sendingInfo = updateMeetingHandler.handleMeetingUpdate(
                event, ActorId.user(actorUid),
                new ChangedEventInfoForMails(instance, changes), recipients, false, actionInfo,
                EventInstanceStatusInfo.needToUpdate(event.getId()),
                UpdateMode.NORMAL, organizerIsEwsLogin).getSendingInfos();

        eventInvitationManager.createAndSendEventInvitationOrCancelMails(
                ActorId.user(actorUid), sendingInfo, actionInfo);

        ListF<Email> actualRecipients = mailSenderMock.getEventMessageParameters()
                .map(InvitationMessageParameters.getRecipientEmailF());

        Assert.unique(actualRecipients);
        Assert.unique(expectedMailTypes.get1());

        MapF<Email, MailType> expected = expectedMailTypes.toMap();
        MapF<Email, MailType> actual = mailSenderMock.getEventMessageParameters().toMap(toEmailMailTypeF());

        Assert.equals(expected, actual);
    }

    private EventChangesInfoForMails removeUndecidedAttendeeAndInviteNewChangesInfo() {
        ParticipantInfo removingAttendeeParticipant = getParticipant(undecidedAttendeeEmail);
        ParticipantData newAttendeeData = new ParticipantData(invitingAttendeeEmail, "", Decision.UNDECIDED, true, false, false);

        return new EventChangesInfoForMails(false, false, false, false, EventParticipantsChangesInfo.changes(
                Tuple2List.fromPairs(ParticipantId.invitationIdForExternalUser(invitingAttendeeEmail), newAttendeeData),
                Tuple2List.<ParticipantId, ParticipantChangesInfo>tuple2List(),
                Cf.list(removingAttendeeParticipant)));
    }

    private EventChangesInfoForMails makeYesAttendeeOrganizerAndRemoveOldChangesInfo() {
        ParticipantInfo removingOrganizerParticipant = getParticipant(organizerEmail);
        UserParticipantInfo yesAttendeeParticipant = getParticipant(yesAttendeeEmail);

        ParticipantChangesInfo makeOrganizerChange = new ParticipantChangesInfo(
                yesAttendeeParticipant, yesAttendeeParticipant.getName(), yesAttendeeParticipant.getDecision(), true, yesAttendeeParticipant.isOptional());

        return new EventChangesInfoForMails(false, false, false, false, EventParticipantsChangesInfo.changes(
                Tuple2List.<ParticipantId, ParticipantData>tuple2List(),
                Tuple2List.fromPairs(ParticipantId.yandexUid(yesAttendeeUid), makeOrganizerChange),
                Cf.list(removingOrganizerParticipant)));
    }

    private EventChangesInfoForMails makeYesAttendeeOrganizerChangesInfo() {
        UserParticipantInfo organizerParticipant = getParticipant(organizerEmail);
        UserParticipantInfo yesAttendeeParticipant = getParticipant(yesAttendeeEmail);

        ParticipantChangesInfo makeOrganizerChange = new ParticipantChangesInfo(
                yesAttendeeParticipant, yesAttendeeParticipant.getName(), yesAttendeeParticipant.getDecision(), true, yesAttendeeParticipant.isOptional());

        ParticipantChangesInfo makeNotOrganizerChange = new ParticipantChangesInfo(
                organizerParticipant, organizerParticipant.getName(), organizerParticipant.getDecision(), false, organizerParticipant.isOptional());

        return new EventChangesInfoForMails(false, false, false, false, EventParticipantsChangesInfo.changes(
                Tuple2List.<ParticipantId, ParticipantData>tuple2List(),
                Tuple2List.fromPairs(
                        ParticipantId.yandexUid(yesAttendeeUid), makeOrganizerChange,
                        ParticipantId.yandexUid(organizerUid), makeNotOrganizerChange),
                Cf.<ParticipantInfo>list()));
    }

    private EventChangesInfoForMails inviteNewOrganizerAndRemoveOldChangesInfo() {
        ParticipantInfo removingOrganizerParticipant = getParticipant(organizerEmail);
        ParticipantData newOrganizerData = new ParticipantData(invitingAttendeeEmail, "", Decision.UNDECIDED, true, true, false);

        return new EventChangesInfoForMails(false, false, false, false, EventParticipantsChangesInfo.changes(
                Tuple2List.fromPairs(ParticipantId.invitationIdForExternalUser(invitingAttendeeEmail), newOrganizerData),
                Tuple2List.<ParticipantId, ParticipantChangesInfo>tuple2List(),
                Cf.list(removingOrganizerParticipant)));
    }

    private UserParticipantInfo getParticipant(Email email) {
        return (UserParticipantInfo) eventInvitationManager.getParticipantByEventIdAndEmail(event.getId(), email).get();
    }

    private static Function<EventMessageParameters, Tuple2<Email, MailType>> toEmailMailTypeF() {
        return new Function<EventMessageParameters, Tuple2<Email, MailType>>() {
            public Tuple2<Email, MailType> apply(EventMessageParameters x) {
                return Tuple2.tuple(x.getRecipientEmail(), x.mailType());
            }
        };
    }

    private LayerInfoForPermsCheck findPrimaryLayer() {
        return eventLayerDao.findPrimaryLayersForPersCheckByEventIds(Cf.list(event.getId())).get2().single();
    }
}
