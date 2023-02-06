package ru.yandex.chemodan.app.telemost.appmessages.handlers;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.appmessages.AppMessage;
import ru.yandex.chemodan.app.telemost.appmessages.AppMessageSender;
import ru.yandex.chemodan.app.telemost.appmessages.model.PeersState;
import ru.yandex.chemodan.app.telemost.appmessages.model.SetPeerStateRequest;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.repository.model.UserBackData;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.repository.model.UserStateDto;
import ru.yandex.chemodan.app.telemost.services.PeerStateService;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;

@ContextConfiguration(classes = SetPeerStateHandlerTest.Context.class)
public class SetPeerStateHandlerTest extends TelemostBaseContextTest {

    @Autowired
    private SetPeerStateHandler setPeerStateHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PeerStateService peerStateService;

    @Autowired
    private AppMessageSender senderMock;

    @Autowired
    private ConferenceHelper conferenceHelper;

    private final User testUser = createTestUserForUid(11133);
    private static final String TEST_AVATAR = "testAva";
    private static final String TEST_DISPLAY_NAME = "testName";

    @Before
    public void setUp() {
        Mockito.reset(senderMock);
        userService.addUserIfNotExists(testUser.getUid());
    }

    @Test
    public void setPeerStateHandlerReturnsRole() {
        Conference conference = createConference();
        RoomConnectionInfo roomConnectionInfo = conferenceHelper.joinConference(conference, testUser, null);
        setPeerStateHandler.processMessage(roomConnectionInfo.getRoomId(), roomConnectionInfo.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));
        validateRole(UserRole.OWNER, captureBackData());
    }

    @Test
    public void peerStateContainsPassportData() {
        UserData blackboxData = UserData.defaultUser("test", Option.of(TEST_DISPLAY_NAME), Option.of(TEST_AVATAR), Cf.map());
        long uid = testUser.getUid().getPassportUid().getUid();
        addUser(uid, blackboxData);
        Conference conference = createConference();
        RoomConnectionInfo roomConnectionInfo = conferenceHelper.joinConference(conference, testUser, null);
        setPeerStateHandler.processMessage(roomConnectionInfo.getRoomId(), roomConnectionInfo.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));
        validatePassportData(testUser.getUid().asString(), TEST_DISPLAY_NAME, TEST_AVATAR, false, captureBackData().get());
    }

    @Test
    public void peerStateForAnonymousContainsDisplayName() {
        Conference conference = createConference();
        RoomConnectionInfo roomConnectionInfo = conferenceHelper.joinConference(conference, TEST_DISPLAY_NAME, null);
        setPeerStateHandler.processMessage(roomConnectionInfo.getRoomId(), roomConnectionInfo.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));
        validatePassportData(null, TEST_DISPLAY_NAME, null, true, captureBackData().get());
    }

    @Test
    public void setPeerStateReturnsDefaultRoleUnregistered() {
        Conference conference = createConference();
        RoomConnectionInfo roomConnectionInfo = conferenceHelper.joinConference(conference, (JsonNode) null);
        setPeerStateHandler.processMessage(roomConnectionInfo.getRoomId(), roomConnectionInfo.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));
        validateRole(UserRole.MEMBER, captureBackData());
    }

    @Test
    public void setPeerStateOverrideInvalidRole() {
        Conference conference = createConference();
        RoomConnectionInfo roomConnectionInfo = conferenceHelper.joinConference(conference);
        String peerId = roomConnectionInfo.getUserId();
        Map<String, UserBackData> state = Cf.map(UserStateDto.BACK_DATA_FIELD,
                new UserBackData(Option.of("1"), Option.empty(), Option.empty(), Option.empty(),
                        Option.of(UserRole.ADMIN)));
        setPeerStateHandler.processMessage(roomConnectionInfo.getRoomId(), peerId,
                new SetPeerStateRequest(objectMapper.valueToTree(state)));
        validateRole(UserRole.MEMBER, captureBackData());
    }

    private Conference createConference() {
        return conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(testUser))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
    }

    private Option<UserBackData> captureBackData() {
        ArgumentCaptor<AppMessage> captor = ArgumentCaptor.forClass(AppMessage.class);
        Mockito.verify(senderMock).sendMessageToAllAsync(Mockito.any(), captor.capture(), Mockito.any());
        PeersState message = (PeersState) captor.getValue();
        return peerStateService.getBackData(message.getPeers().get(0).getState());
    }

    private void validateRole(UserRole expectedRole, Option<UserBackData> backData) {
        Assert.assertTrue(backData.isPresent());
        Assert.assertEquals(expectedRole, backData.get().getRole().get());
    }



    static void validatePassportData(String uid, String displayName, String avatar, Boolean isDefaultAvatar,
                                     UserBackData backData) {
        if (avatar == null)
            Assert.assertTrue(backData.getAvatarUrl() == null || backData.getAvatarUrl().isEmpty());
        else
            Assert.assertTrue(backData.getAvatarUrl().get().contains(avatar));
        if (uid == null)
            Assert.assertNull(backData.getUid());
        else
            Assert.assertEquals(uid, backData.getUid().getOrNull());
        if (displayName == null)
            Assert.assertNull(backData.getDisplayName());
        else
            Assert.assertEquals(displayName, backData.getDisplayName().getOrNull());
        if (isDefaultAvatar == null)
            Assert.assertNull(backData.getIsDefaultAvatar());
        else
            Assert.assertEquals(isDefaultAvatar, backData.getIsDefaultAvatar().getOrNull());
    }

    @Configuration
    public static class Context {

        @Bean
        @Primary
        AppMessageSender appMessageSender() {
            return Mockito.mock(AppMessageSender.class);
        }
    }
}
