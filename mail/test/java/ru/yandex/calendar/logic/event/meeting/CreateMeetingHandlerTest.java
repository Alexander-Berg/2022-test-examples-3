package ru.yandex.calendar.logic.event.meeting;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.param.InvitationMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * Details: http://wiki.yandex-team.ru/Calendar/sharing/messages
 * @author ssytnik
 */
public class CreateMeetingHandlerTest extends AbstractEwsExportedLoginsTest{
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private SettingsRoutines settingsRoutines;

    public CreateMeetingHandlerTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void mockEwsProxyWrapper(){
        setMockEwsProxyWrapper();
    }

    @Test
    @WantsEws
    public void organizerReceivesEmailFromWebYt() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12600);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(12601);

        setIsEwserIfNeeded(organizer);

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        EventData eventData = testManager.createDefaultEventData(organizer.getUid(), "organizerReceivesEmailFromWeb");
        eventData.setInvData(testManager.createParticipantsData(organizer.getEmail(), attendee.getEmail()));

        createEvent(organizer.getUid(), eventData, ActionInfo.webTest());

        ListF<InvitationMessageParameters> invList = mailSenderMock.getInvitationMessageParameterss();

        if (isEwser(organizer)) {
            Assert.hasSize(1, invList);
        } else {
            Assert.hasSize(2, invList);

            InvitationMessageParameters organizerInvitation =
                    invList.find(EventMessageParameters.getRecipientEmailF().andThenEquals(organizer.getEmail())).get();
            InvitationMessageParameters attendeeInvitation =
                    invList.find(EventMessageParameters.getRecipientEmailF().andThenEquals(attendee.getEmail())).get();

            Assert.equals(attendeeInvitation.mailType(), MailType.EVENT_INVITATION);
            Assert.equals(organizerInvitation.mailType(), MailType.EVENT_INVITATION);
        }
    }

    @Test
    @WantsEws
    public void organizerDoesNotReceiveEmailFromCaldav() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-12610");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-12611");

        setIsEwserIfNeeded(organizer);

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        EventData eventData = testManager.createDefaultEventData(organizer.getUid(), "organizerDoesNotReceiveEmailFromCaldav");
        eventData.setInvData(testManager.createParticipantsData(organizer.getEmail(), attendee.getEmail()));

        createEvent(organizer.getUid(), eventData, new ActionInfo(ActionSource.CALDAV, "?", Instant.now()));

        ListF<InvitationMessageParameters> invList = mailSenderMock.getInvitationMessageParameterss();

        if (isEwser(organizer)) {
            Assert.hasSize(0, invList);
        } else {
            Assert.hasSize(1, invList);

            Option<InvitationMessageParameters> organizerInvitation =
                    invList.find(EventMessageParameters.getRecipientEmailF().andThenEquals(organizer.getEmail()));
            Option<InvitationMessageParameters> attendeeInvitation =
                    invList.find(EventMessageParameters.getRecipientEmailF().andThenEquals(attendee.getEmail()));

            Assert.none(organizerInvitation);
            Assert.some(attendeeInvitation);
            Assert.equals(attendeeInvitation.get().mailType(), MailType.EVENT_INVITATION);
        }
    }

    @Test
    @WantsEws
    public void icsMethod() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(306);;
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(307);

        setIsEwserIfNeeded(organizer);

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        EventData eventData = testManager.createDefaultEventData(organizer.getUid(), "organizerReceivesREQUEST");
        eventData.setInvData(testManager.createParticipantsData(organizer.getEmail(), attendee.getEmail()));

        createEvent(organizer.getUid(), eventData, ActionInfo.webTest());

        ListF<InvitationMessageParameters> invitations = mailSenderMock.getInvitationMessageParameterss();
        if (isEwser(organizer)) {
            Assert.sizeIs(1, invitations);
        } else {
            Assert.sizeIs(2, invitations);
            Assert.equals(IcsMethod.REQUEST, invitations.first().getIcs().get().getMethod());
            Assert.equals(IcsMethod.REQUEST, invitations.last().getIcs().get().getMethod());
        }
    }

    private CreateInfo createEvent(PassportUid creatorUid, EventData eventData, ActionInfo actionInfo) {

        return eventRoutines.createUserOrFeedEvent(UidOrResourceId.user(creatorUid), EventType.USER,
                eventRoutines.createMainEvent(creatorUid, eventData, actionInfo),
                eventData, NotificationsData.createEmpty(),
                InvitationProcessingMode.SAVE_ATTACH_SEND, actionInfo);
    }
}
