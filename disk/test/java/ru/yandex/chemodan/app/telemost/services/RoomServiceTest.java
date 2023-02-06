package ru.yandex.chemodan.app.telemost.services;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.exceptions.ActiveSessionNotFoundTelemostException;
import ru.yandex.chemodan.app.telemost.exceptions.InvalidPeerTokenTelemostException;
import ru.yandex.chemodan.app.telemost.mock.TelemostAuditLogMockConfiguration;
import ru.yandex.chemodan.app.telemost.mock.mediator.RoomGrpcMockService;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferencePeerDao;
import ru.yandex.chemodan.app.telemost.repository.dao.MediaSessionDao;
import ru.yandex.chemodan.app.telemost.repository.model.ConferencePeerDto;
import ru.yandex.chemodan.app.telemost.repository.model.MediaSessionDto;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.services.model.XMPPLimitType;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.time.MoscowTime;

public class RoomServiceTest extends TelemostBaseContextTest {

    @Autowired
    @Qualifier("roomServiceBlockingV2")
    private RoomService roomServiceBlockingV2;

    @Autowired
    @Qualifier("roomServiceAsyncV1")
    private RoomService roomServiceAsyncV1;

    @Autowired
    private TelemostAuditLogMockConfiguration telemostAuditLogMockConfiguration;

    @Autowired
    private ConferencePeerDao conferencePeerDao;

    @Autowired
    private MediaSessionDao mediaSessionDao;

    @Autowired
    private RoomGrpcMockService roomGrpcMockService;

    @Test
    public void testRoomCreationForAnonymV1() {
        testRoomCreationForAnonym(roomServiceAsyncV1);
    }

    @Test
    public void testRoomCreationForAnonymV2() {
        testRoomCreationForAnonym(roomServiceBlockingV2);
    }

    @Test
    public void testRoomCreationForUidV1() throws URISyntaxException {
        testRoomCreationForUid(roomServiceAsyncV1);
    }

    @Test
    public void testRoomCreationForUidV2() throws URISyntaxException {
        testRoomCreationForUid(roomServiceBlockingV2);
    }

    @Test
    public void testJoinToConferenceV1() throws URISyntaxException {
        testJoinToConference(roomServiceAsyncV1);
    }

    @Test
    public void testJoinToConferenceV2() throws URISyntaxException {
        testJoinToConference(roomServiceBlockingV2);
    }

    @Test
    public void testRoomCreationWithClientInstanceIdV1() {
        testRoomCreationWithClientInstanceId(roomServiceAsyncV1);
    }

    @Test
    public void testRoomCreationWithClientInstanceIdV2() {
        testRoomCreationWithClientInstanceId(roomServiceBlockingV2);
    }

    @Test
    public void testJoiningUserWithDifferentUidAndClientInstanceIdV1() {
        testJoiningUserWithDifferentUidAndClientInstanceId(roomServiceAsyncV1);
    }

    @Test
    public void testJoiningUserWithDifferentUidAndClientInstanceIdV2() {
        testJoiningUserWithDifferentUidAndClientInstanceId(roomServiceBlockingV2);
    }

    @Test
    public void testJoiningUserSameUidAndDifferentClientInstanceIdV1() {
        testJoiningUserSameUidAndDifferentClientInstanceId(roomServiceAsyncV1);
    }

    @Test
    public void testJoiningUserSameUidAndDifferentClientInstanceIdV2() {
        testJoiningUserSameUidAndDifferentClientInstanceId(roomServiceBlockingV2);
    }

    @Test
    public void testJoiningUserSameUidAndClientInstanceIdV1() {
        testJoiningUserSameUidAndClientInstanceId(roomServiceAsyncV1);
    }

    @Test
    public void testJoiningUserSameUidAndClientInstanceIdV2() {
        testJoiningUserSameUidAndClientInstanceId(roomServiceBlockingV2);
    }

    @Test
    public void testJoiningUserDifferentUidAndSameClientInstanceIdV1() {
        testJoiningUserDifferentUidAndSameClientInstanceId(roomServiceAsyncV1);
    }

    @Test
    public void testJoiningUserDifferentUidAndSameClientInstanceIdV2() {
        testJoiningUserDifferentUidAndSameClientInstanceId(roomServiceBlockingV2);
    }

    @Test
    public void testSuccessfulDisconnectV1() {
        testSuccessfulDisconnect(roomServiceAsyncV1);
    }

    @Test
    public void testSuccessfulDisconnectV2() {
        testSuccessfulDisconnect(roomServiceBlockingV2);
    }

    @Test(expected = InvalidPeerTokenTelemostException.class)
    public void testIncorrectTokenDisconnectV1() {
        testIncorrectTokenDisconnect(roomServiceAsyncV1);
    }

    @Test(expected = InvalidPeerTokenTelemostException.class)
    public void testIncorrectTokenDisconnectV2() {
        testIncorrectTokenDisconnect(roomServiceBlockingV2);
    }

    @Test(expected = ActiveSessionNotFoundTelemostException.class)
    public void testIncorrectSessionIdDisconnectV1() {
        testIncorrectSessionIdDisconnect(roomServiceAsyncV1);
    }

    @Test(expected = ActiveSessionNotFoundTelemostException.class)
    public void testIncorrectSessionIdDisconnectV2() {
        testIncorrectSessionIdDisconnect(roomServiceBlockingV2);
    }

    @Test(expected = ActiveSessionNotFoundTelemostException.class)
    public void testIncorrectPeerIdDisconnectV1() {
        testIncorrectPeerIdDisconnect(roomServiceAsyncV1);
    }

    @Test(expected = ActiveSessionNotFoundTelemostException.class)
    public void testIncorrectPeerIdDisconnectV2() {
        testIncorrectPeerIdDisconnect(roomServiceBlockingV2);
    }

    public void testAlreadyDisconnectedDisconnectV1() {
        testAlreadyDisconnectedDisconnect(roomServiceAsyncV1);
    }

    public void testAlreadyDisconnectedDisconnectV2() {
        testAlreadyDisconnectedDisconnect(roomServiceBlockingV2);
    }

    @Test
    public void testSavePeerIdIntentionOnRetryV2() {
        testSavePeerIdIntentionOnRetry(roomServiceBlockingV2);
    }

    private void testRoomCreationWithClientInstanceId(RoomService roomService) {
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.empty())
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-10";
        RoomConnectionInfo room =
                roomService.createRoomAndUserForConference(conference, Option.empty(), Option.empty(),
                        Option.of(clientInstanceId));
        Assert.assertNotNull(room);
        Assert.assertEquals(conference.getRoomId(), room.getRoomId());
        ListF<ConferencePeerDto> activeConferencePeers = conferencePeerDao.findActivePeers(conference.getRoomId());
        Assert.assertEquals(1, activeConferencePeers.size());
        ConferencePeerDto conferencePeer = activeConferencePeers.first();
        Assert.assertEquals(room.getUserId(), conferencePeer.getPeerId());
        Assert.assertTrue(conferencePeer.getPeerToken().isPresent());
        Assert.assertEquals(room.getPeerToken(), conferencePeer.getPeerToken().get());
        Option<MediaSessionDto> mediaSessionDto = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.empty());
        Assert.assertTrue(mediaSessionDto.isPresent());
        Assert.assertEquals(room.getMediaSessionId(), mediaSessionDto.get().getMediaSessionId());
    }

    private void testJoiningUserWithDifferentUidAndClientInstanceId(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(21L));
        userService.addUserIfNotExists(passportUser);

        User user1 = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user1))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId1 = "client-instance-11";
        RoomConnectionInfo roomData1 =
                roomService.createRoomAndUserForConference(conference, Option.of(user1), Option.empty(),
                        Option.of(clientInstanceId1));

        PassportOrYaTeamUid passportUser2 = PassportOrYaTeamUid.passportUid(PassportUid.cons(31L));
        userService.addUserIfNotExists(passportUser2);

        User user2 = createTestUserForUid(passportUser2);

        String clientInstanceId2 = "client-instance-21";
        RoomConnectionInfo roomData2 =
                roomService.joinToConference(conference, Option.of(user2), Option.empty(),
                        Option.empty(), Option.of(clientInstanceId2), ParticipantType.USER);
        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findActivePeers(roomData2.getRoomId());
        Assert.assertEquals(2, conferencePeers.size());
        Assert.assertEquals(roomData1.getRoomId(), roomData2.getRoomId());
        Assert.assertNotEquals(roomData1.getUserId(), roomData2.getUserId());
        Assert.assertTrue(conferencePeers.exists(user -> roomData1.getUserId().equals(user.getPeerId()) &&
                roomData1.getPeerToken().equals(user.getPeerToken().get())));
        Assert.assertTrue(conferencePeers.exists(user -> roomData2.getUserId().equals(user.getPeerId()) &&
                roomData2.getPeerToken().equals(user.getPeerToken().get())));
        Option<MediaSessionDto> mediaSessionDto1 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId1, Option.of(user1.getUid().asString()));
        Assert.assertTrue(mediaSessionDto1.isPresent());
        Assert.assertEquals(roomData1.getMediaSessionId(), mediaSessionDto1.get().getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDto2 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId2, Option.of(user2.getUid().asString()));
        Assert.assertTrue(mediaSessionDto2.isPresent());
        Assert.assertEquals(roomData2.getMediaSessionId(), mediaSessionDto2.get().getMediaSessionId());
    }

    private void testJoiningUserSameUidAndDifferentClientInstanceId(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(22L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId1 = "client-instance-12";
        RoomConnectionInfo roomData1 =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId1));
        String clientInstanceId2 = "client-instance-22";
        RoomConnectionInfo roomData2 =
                roomService.joinToConference(conference, Option.of(user), Option.empty(),
                        Option.empty(), Option.of(clientInstanceId2), ParticipantType.USER);
        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findActivePeers(roomData2.getRoomId());
        Assert.assertEquals(2, conferencePeers.size());
        Assert.assertEquals(roomData1.getRoomId(), roomData2.getRoomId());
        Assert.assertNotEquals(roomData1.getUserId(), roomData2.getUserId());
        Assert.assertTrue(conferencePeers.exists(userDto -> roomData1.getUserId().equals(userDto.getPeerId()) &&
                roomData1.getPeerToken().equals(userDto.getPeerToken().get())));
        Assert.assertTrue(conferencePeers.exists(userDto -> roomData2.getUserId().equals(userDto.getPeerId()) &&
                roomData2.getPeerToken().equals(userDto.getPeerToken().get())));
        Option<MediaSessionDto> mediaSessionDto1 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId1, Option.of(user.getUid().asString()));
        Assert.assertTrue(mediaSessionDto1.isPresent());
        Assert.assertEquals(roomData1.getMediaSessionId(), mediaSessionDto1.get().getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDto2 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId2, Option.of(user.getUid().asString()));
        Assert.assertTrue(mediaSessionDto2.isPresent());
        Assert.assertEquals(roomData2.getMediaSessionId(), mediaSessionDto2.get().getMediaSessionId());
    }

    private void testJoiningUserSameUidAndClientInstanceId(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(23L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-13";
        RoomConnectionInfo roomData1 =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        RoomConnectionInfo roomData2 =
                roomService.joinToConference(conference, Option.of(user), Option.empty(),
                        Option.empty(), Option.of(clientInstanceId), ParticipantType.USER);
        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findActivePeers(roomData2.getRoomId());
        Assert.assertEquals(1, conferencePeers.size());
        Assert.assertEquals(roomData1.getRoomId(), roomData2.getRoomId());
        Assert.assertEquals(roomData1.getUserId(), roomData2.getUserId());
        Assert.assertEquals(roomData1.getPeerToken(), roomData2.getPeerToken());
        ConferencePeerDto conferencePeer = conferencePeers.first();
        Assert.assertEquals(roomData1.getUserId(), conferencePeer.getPeerId());
        Assert.assertEquals(roomData1.getPeerToken(), conferencePeer.getPeerToken().get());
        Assert.assertNotEquals(roomData1.getMediaSessionId(), roomData2.getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDto = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.of(user.getUid().asString()));
        Assert.assertTrue(mediaSessionDto.isPresent());
        Assert.assertEquals(roomData2.getMediaSessionId(), mediaSessionDto.get().getMediaSessionId());
    }

    private void testJoiningUserDifferentUidAndSameClientInstanceId(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(24L));
        userService.addUserIfNotExists(passportUser);

        User user1 = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user1))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-14";
        RoomConnectionInfo roomData1 =
                roomService.createRoomAndUserForConference(conference, Option.of(user1), Option.empty(),
                        Option.of(clientInstanceId));

        PassportOrYaTeamUid passportUser2 = PassportOrYaTeamUid.passportUid(PassportUid.cons(34L));
        userService.addUserIfNotExists(passportUser2);

        User user2 = createTestUserForUid(passportUser2);

        RoomConnectionInfo roomData2 =
                roomService.joinToConference(conference, Option.of(user2), Option.empty(),
                        Option.empty(), Option.of(clientInstanceId), ParticipantType.USER);
        ListF<ConferencePeerDto> conferencePeers = conferencePeerDao.findActivePeers(roomData2.getRoomId());
        Assert.assertEquals(2, conferencePeers.size());
        Assert.assertEquals(roomData1.getRoomId(), roomData2.getRoomId());
        Assert.assertNotEquals(roomData1.getUserId(), roomData2.getUserId());
        Assert.assertTrue(conferencePeers.exists(user -> roomData1.getUserId().equals(user.getPeerId()) &&
                roomData1.getPeerToken().equals(user.getPeerToken().get())));
        Assert.assertTrue(conferencePeers.exists(user -> roomData2.getUserId().equals(user.getPeerId()) &&
                roomData2.getPeerToken().equals(user.getPeerToken().get())));
        Option<MediaSessionDto> mediaSessionDto1 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.of(user1.getUid().asString()));
        Assert.assertTrue(mediaSessionDto1.isPresent());
        Assert.assertEquals(roomData1.getMediaSessionId(), mediaSessionDto1.get().getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDto2 = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.of(user2.getUid().asString()));
        Assert.assertTrue(mediaSessionDto2.isPresent());
        Assert.assertEquals(roomData2.getMediaSessionId(), mediaSessionDto2.get().getMediaSessionId());
    }

    private void testSuccessfulDisconnect(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(4L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-2";
        RoomConnectionInfo roomData =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        roomService.disconnectUserFromConference(conference, roomData.getUserId(), roomData.getPeerToken(),
                roomData.getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDto = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.of(user.getUid().asString()));
        Assert.assertFalse(mediaSessionDto.isPresent());
    }

    private void testIncorrectTokenDisconnect(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(5L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-6";
        RoomConnectionInfo roomData =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        roomService.disconnectUserFromConference(conference, roomData.getUserId(), roomData.getPeerToken() + "-",
                roomData.getMediaSessionId());
    }

    private void testIncorrectSessionIdDisconnect(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(7L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-7";
        RoomConnectionInfo roomData =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        roomService.disconnectUserFromConference(conference, roomData.getUserId(), roomData.getPeerToken(),
                roomData.getMediaSessionId() + "-");
    }

    private void testIncorrectPeerIdDisconnect(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(8L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-8";
        RoomConnectionInfo roomData =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        roomService.disconnectUserFromConference(conference, roomData.getUserId() + "-", roomData.getPeerToken(),
                roomData.getMediaSessionId());
    }

    private void testAlreadyDisconnectedDisconnect(RoomService roomService) {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(PassportUid.cons(9L));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        String clientInstanceId = "client-instance-9";
        RoomConnectionInfo roomData =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(),
                        Option.of(clientInstanceId));
        MediaSessionDto mediaSessionDto = mediaSessionDao.getActiveMediaSession(conference.getDbId(), clientInstanceId,
                Option.of(user.getUid().asString())).getOrThrow(IllegalStateException::new);
        mediaSessionDao.deactivateSession(mediaSessionDto);
        roomService.disconnectUserFromConference(conference, roomData.getUserId(), roomData.getPeerToken(),
                roomData.getMediaSessionId());
        Option<MediaSessionDto> mediaSessionDtoO = mediaSessionDao.getActiveMediaSession(conference.getDbId(),
                clientInstanceId, Option.of(user.getUid().asString()));
        Assert.assertFalse(mediaSessionDtoO.isPresent());
    }

    private void testRoomCreationForAnonym(RoomService roomService) {
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.empty())
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        telemostAuditLogMockConfiguration.reset();
        RoomConnectionInfo room = roomService.createRoomAndUserForConference(conference, Option.empty(), Option.empty(), Option.empty());
        telemostAuditLogMockConfiguration.assertConferenceLogged(conference.getRoomId(), conference.getConferenceDto().getShortUrlId());
        Assert.assertNotNull(room);
        Assert.assertEquals(conference.getRoomId(), room.getRoomId());
    }

    private void testRoomCreationForUid(RoomService roomService) throws URISyntaxException {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(new PassportUid(11133));
        userService.addUserIfNotExists(passportUser);

        User user = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        RoomConnectionInfo room =
                roomService.createRoomAndUserForConference(conference, Option.of(user), Option.empty(), Option.empty());
        Assert.assertNotNull(room);
        Assert.assertEquals(conference.getRoomId(), room.getRoomId());
        ListF<ConferencePeerDto> conferencePeersO = conferencePeerDao.findByConferenceAndUid(room.getRoomId(), user.getUid());
        Assert.assertFalse(conferencePeersO.isEmpty());
        ConferencePeerDto conferencePeer = conferencePeersO.get(0);
        Assert.assertEquals(user.getUid().asString(), conferencePeer.getUid().get());
        Assert.assertEquals(conference.getDbId(), conferencePeer.getConferenceId());
        MapF<String, String> wsQueryParameters = getQueryParametersFromWebsocketUri(room.getWebsocketUri());
        Assert.assertEquals(wsQueryParameters.getTs("user"), conferencePeer.getPeerId());
        Assert.assertEquals(wsQueryParameters.getTs("session"), room.getMediaSessionId());
    }

    private void testJoinToConference(RoomService roomService) throws URISyntaxException {
        PassportOrYaTeamUid passportUser = PassportOrYaTeamUid.passportUid(new PassportUid(11133));
        userService.addUserIfNotExists(passportUser);

        User user1 = createTestUserForUid(passportUser);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user1))
                .permanent(Option.of(Boolean.FALSE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.empty()).build());
        RoomConnectionInfo room1 =
                roomService.createRoomAndUserForConference(conference, Option.of(user1), Option.empty(), Option.empty());
        Assert.assertNotNull(room1);
        Assert.assertEquals(conference.getRoomId(), room1.getRoomId());

        PassportOrYaTeamUid passportUser2 = PassportOrYaTeamUid.passportUid(new PassportUid(3000185708L));
        userService.addUserIfNotExists(passportUser2);

        User user2 = new User(passportUser2,
                Option.of("feelgood"), Option.empty(), Option.empty(), false, false, MoscowTime.TZ, "ru");
        Option<String> name = Option.of("Увася");
        RoomConnectionInfo room2 =
                roomService.joinToConference(conference, Option.of(user2), name,
                        Option.empty(), Option.empty(), ParticipantType.USER);
        Assert.assertNotNull(room2);
        Assert.assertEquals(conference.getRoomId(), room2.getRoomId());

        ListF<ConferencePeerDto> conferencePeerO = conferencePeerDao.findByConferenceAndUid(room2.getRoomId(), user2.getUid());
        Assert.assertFalse(conferencePeerO.isEmpty());
        ConferencePeerDto conferencePeer = conferencePeerO.get(0);
        Assert.assertEquals(user2.getUid().asString(), conferencePeer.getUid().get());
        Assert.assertEquals(conference.getDbId(), conferencePeer.getConferenceId());
        MapF<String, String> wsQueryParameters = getQueryParametersFromWebsocketUri(room2.getWebsocketUri());
        Assert.assertEquals(wsQueryParameters.getTs("user"), conferencePeer.getPeerId());
        Assert.assertEquals(wsQueryParameters.getTs("session"), room2.getMediaSessionId());
        Assert.assertEquals("feelgood", conferencePeer.getDisplayName());
    }


    private MapF<String, String> getQueryParametersFromWebsocketUri(String websocketUri) throws URISyntaxException {
        URI uri = new URI(websocketUri);
        return Cf.list(uri.getQuery().split("&"))
                .map(part -> part.split("=")).toMap(parts -> parts[0], parts -> parts[1]);
    }

    private void testSavePeerIdIntentionOnRetry(RoomService roomService) {
        String clientInstanceId = "clientInstanceId" + Random2.R.nextAlnum(6);
        PassportOrYaTeamUid uid = PassportOrYaTeamUid.passportUid(PassportUid.cons(1806L));
        userService.addUserIfNotExists(uid);

        User user = createTestUserForUid(uid);
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.of(user))
                .limitType(XMPPLimitType.DEFAULT)
                .staffOnly(Option.of(Boolean.FALSE))
                .permanent(Option.of(Boolean.FALSE))
                .externalMeeting(Option.of(Boolean.FALSE))
                .eventId(Option.empty()).build());
        RoomConnectionInfo initialConnectionInfo = roomService
                .createRoomAndUserForConference(conference, Option.of(user), Option.empty(), Option.of(clientInstanceId));
        roomGrpcMockService.makePeerIdFailToAddOnce(initialConnectionInfo.getUserId());
        try {
            roomService.joinToConference(conference, Option.of(user), Option.empty(), Option.empty(),
                    Option.of(clientInstanceId), ParticipantType.USER);
            Assert.fail();
        } catch (Exception e) {
            System.out.println("Catched: " + e);
        }
        RoomConnectionInfo resultConnectionInfo =
                roomService.joinToConference(conference, Option.of(user), Option.empty(), Option.empty(),
                        Option.of(clientInstanceId), ParticipantType.USER);
        Assert.assertEquals(initialConnectionInfo.getUserId(), resultConnectionInfo.getUserId());
    }

    @Test
    public void testCalendarEventData() {
        Conference conference = conferenceService.generateConference(ConferenceClientParameters.builder()
                .user(Option.empty())
                .permanent(Option.of(Boolean.TRUE))
                .staffOnly(Option.of(Boolean.FALSE))
                .externalMeeting(Option.empty())
                .eventId(Option.of("12345")).build());
        Assert.assertEquals(conference.getConferenceDto().getEventId().get(), "12345");
    }
}
