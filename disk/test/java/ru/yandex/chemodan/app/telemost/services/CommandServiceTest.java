package ru.yandex.chemodan.app.telemost.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.appmessages.AppMessage;
import ru.yandex.chemodan.app.telemost.appmessages.AppMessageSender;
import ru.yandex.chemodan.app.telemost.appmessages.model.commands.MuteMicrophoneCommand;
import ru.yandex.chemodan.app.telemost.appmessages.model.events.EventMessage;
import ru.yandex.chemodan.app.telemost.appmessages.model.events.RoleChangedEvent;
import ru.yandex.chemodan.app.telemost.exceptions.CommandNotAllowedException;
import ru.yandex.chemodan.app.telemost.exceptions.PeerNotFoundException;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferencePeerDao;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceUserDao;
import ru.yandex.chemodan.app.telemost.repository.dao.UserStateDtoDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferencePeerDto;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.repository.model.UserStateDto;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.inside.passport.PassportUid;

public class CommandServiceTest extends TelemostBaseContextTest {

    private final AppMessageSender appMessageSenderMock = Mockito.mock(AppMessageSender.class);
    private final ChatService chatServiceMock = Mockito.mock(ChatService.class);
    private User owner;
    private User member;
    private User testUser;
    private Conference conference;
    private String memberPeer;
    private String guestPeer;

    private CommandService commandService;

    @Autowired
    private ConferenceService conferenceService;

    @Autowired
    private ConferencePeerDao conferencePeerDao;

    @Autowired
    @Qualifier("roomServiceBlockingV2")
    private RoomService roomService;

    @Autowired
    private ConferenceUserDao conferenceUserDao;

    @Autowired
    private UserStateDtoDao userStateDtoDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        commandService = new CommandService(
                conferencePeerDao, userStateDtoDao, appMessageSenderMock,
                conferenceUserDao, conferenceService, chatServiceMock);

        owner = createTestUserForUid(PassportOrYaTeamUid.passportUid(new PassportUid(1000)));
        member = createTestUserForUid(PassportOrYaTeamUid.passportUid(new PassportUid(2000)));
        testUser = createTestUserForUid(PassportOrYaTeamUid.passportUid(new PassportUid(11133)));

        userService.addUserIfNotExists(owner.getUid());
        userService.addUserIfNotExists(member.getUid());
        userService.addUserIfNotExists(testUser.getUid());

        conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(owner))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        conferenceService.joinConference(
                Option.of(member), conferenceService.getConferenceUriId(conference.getUri()), Option.empty(),
                Option.empty());
        memberPeer = roomService.joinToConference(
                conference, Option.of(member), Option.empty(), Option.empty(), Option.empty(), ParticipantType.USER).getUserId();
        guestPeer = roomService.joinToConference(
                conference, Option.empty(), Option.empty(), Option.empty(), Option.empty(), ParticipantType.USER).getUserId();
    }

    @Test
    public void muteTest() {
        commandService.sendCommand(conference.getConferenceDto(), memberPeer, owner.getUid(),
                new MuteMicrophoneCommand());
        Mockito.verify(appMessageSenderMock).sendMessageAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test(expected = PeerNotFoundException.class)
    public void sendCommandFailIfPeerNotInConference() {
        String peerId = "randomPeer";
        commandService.sendCommand(conference.getConferenceDto(), peerId, owner.getUid(), new MuteMicrophoneCommand());
    }

    @Test(expected = CommandNotAllowedException.class)
    public void sendCommandFailIfSenderIsNotInConference() {
        commandService.sendCommand(conference.getConferenceDto(), memberPeer, testUser.getUid(),
                new MuteMicrophoneCommand());
        Mockito.verify(appMessageSenderMock).sendMessageAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test(expected = CommandNotAllowedException.class)
    public void sendCommandFailIfSenderIsNotAdmin() {
        commandService.sendCommand(conference.getConferenceDto(), guestPeer, member.getUid(),
                new MuteMicrophoneCommand());
        Mockito.verify(appMessageSenderMock).sendMessageAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void setRoleChangesRole() {
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.ADMIN);
        Assert.assertEquals(UserRole.ADMIN,
                conferenceUserDao.findByConferenceAndUid(conference.getDbId(), member.getUid()).get().getRole());
    }

    @Test
    public void setRoleChangesChatRole() {
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.ADMIN);
        Mockito.verify(chatServiceMock).setRole(conference.getConferenceDto(), member.getUid(), UserRole.ADMIN.getChatRole());
    }

    @Test
    public void setRoleSendsEvent() {
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.ADMIN);
        ArgumentCaptor<AppMessage> captor = ArgumentCaptor.forClass(AppMessage.class);
        Mockito.verify(appMessageSenderMock).sendMessageToAll(Mockito.eq(conference.getRoomId()), captor.capture());
        EventMessage eventMessage = (EventMessage) captor.getValue();
        RoleChangedEvent event = (RoleChangedEvent) eventMessage.getEvent();
        Assert.assertEquals(UserRole.ADMIN, event.getUserRole());
        Assert.assertEquals(memberPeer, event.getPeerId());
    }

    @Test
    public void setRoleIncrementsUserState() {
        ConferencePeerDto conferencePeer = conferencePeerDao.findByConferenceAndUid(conference.getConferenceId(), member.getUid()).get(0);
        UserStateDto userState = userStateDtoDao.updatePeerState(conferencePeer.getId(),
                objectMapper.valueToTree(new Object())).get();
        long version = userState.getVersion();
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.ADMIN);
        Assert.assertEquals(version + 1, userStateDtoDao.findById(userState.getId()).getVersion());
    }

    @Test(expected = CommandNotAllowedException.class)
    public void changeOwnerRoleIsNotAllowed() {
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), owner.getUid(), UserRole.ADMIN);
    }

    @Test(expected = CommandNotAllowedException.class)
    public void changeOwnRoleIsNotAllowed() {
        try {
            commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.ADMIN);
        } catch (Exception e) {
            Assert.fail("Exception here is not expected");
        }
        commandService.setRole(conference.getConferenceDto(), member.getUid(), member.getUid(), UserRole.MEMBER);
    }

    @Test(expected = CommandNotAllowedException.class)
    public void setRoleToOwnerIsNotAllowed() {
        commandService.setRole(conference.getConferenceDto(), owner.getUid(), member.getUid(), UserRole.OWNER);
    }
}
