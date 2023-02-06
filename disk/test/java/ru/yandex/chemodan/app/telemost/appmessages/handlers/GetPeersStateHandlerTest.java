package ru.yandex.chemodan.app.telemost.appmessages.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.appmessages.model.GetPeerStateRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.GetPeersStateRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.PeersState;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.repository.dao.UserStateDtoDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserBackData;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.services.PeerStateService;
import ru.yandex.chemodan.app.telemost.services.UserService;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;

public class GetPeersStateHandlerTest extends TelemostBaseContextTest {

    private final User testUser = createTestUserForUid(11133);

    @Autowired
    GetPeersStateHandler peersStateHandler;

    @Autowired
    PeerStateService peerStateService;

    @Autowired
    UserStateDtoDao userStateDao;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ConferenceHelper conferenceHelper;

    @Autowired
    protected UserService userService;

    private static final String TEST_AVATAR = "testAva";
    private static final String TEST_DISPLAY_NAME = "testName";

    @Before
    public void setUp() {
        userService.addUserIfNotExists(testUser.getUid());
        UserData blackboxData = UserData.defaultUser("test", Option.of(TEST_DISPLAY_NAME), Option.of(TEST_AVATAR), Cf.map());
        long uid = testUser.getUid().getPassportUid().getUid();
        addUser(uid, blackboxData);
    }

    @Test
    public void validateGetPeerStateResponseForLoginUser() {
        PeersState response = getPeersState(testUser);
        Option<UserBackData> backData = peerStateService.getBackData(response.getPeers().get(0).getState());
        Assert.assertTrue(backData.isPresent());
        Assert.assertEquals(backData.get().getRole().get(), UserRole.OWNER);
        SetPeerStateHandlerTest.validatePassportData(testUser.getUid().asString(), TEST_DISPLAY_NAME, TEST_AVATAR, false, backData.get());
    }

    @Test
    public void validateGetPeerStateResponseForAnonymousUser() {
        PeersState response = getPeersState();
        Option<UserBackData> backData = peerStateService.getBackData(response.getPeers().get(0).getState());
        Assert.assertTrue(backData.isPresent());
        Assert.assertEquals(backData.get().getRole().get(), UserRole.MEMBER);
        SetPeerStateHandlerTest.validatePassportData(null, TEST_DISPLAY_NAME, null, true, backData.get());
    }

    private PeersState getPeersState(User peerUser) {
        Conference conference = conferenceHelper.createConference(testUser);
        RoomConnectionInfo roomInfo = conferenceHelper.joinConference(conference, peerUser);
        GetPeerStateRequest peerRequest = new GetPeerStateRequest(roomInfo.getUserId(), null);
        GetPeersStateRequest request = new GetPeersStateRequest(Cf.list(peerRequest), null);
        return (PeersState) peersStateHandler.processMessage(roomInfo.getRoomId(), roomInfo.getUserId(),
                request);
    }

    private PeersState getPeersState() {
        Conference conference = conferenceHelper.createConference(testUser);
        RoomConnectionInfo roomInfo = conferenceHelper.joinConference(conference, TEST_DISPLAY_NAME);
        GetPeerStateRequest peerRequest = new GetPeerStateRequest(roomInfo.getUserId(), null);
        GetPeersStateRequest request = new GetPeersStateRequest(Cf.list(peerRequest), null);
        return (PeersState) peersStateHandler.processMessage(roomInfo.getRoomId(), roomInfo.getUserId(),
                request);
    }
}
