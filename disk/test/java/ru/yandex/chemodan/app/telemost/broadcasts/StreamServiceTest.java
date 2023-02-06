package ru.yandex.chemodan.app.telemost.broadcasts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.appmessages.handlers.GetPeersStateHandler;
import ru.yandex.chemodan.app.telemost.appmessages.handlers.SetPeerStateHandler;
import ru.yandex.chemodan.app.telemost.appmessages.model.GetPeerStateRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.GetPeersStateRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.PeersState;
import ru.yandex.chemodan.app.telemost.appmessages.model.SetPeerStateRequest;
import ru.yandex.chemodan.app.telemost.exceptions.CommandNotAllowedException;
import ru.yandex.chemodan.app.telemost.exceptions.ConferenceLinkExpiredException;
import ru.yandex.chemodan.app.telemost.exceptions.InvalidTranslatorTokenTelemostException;
import ru.yandex.chemodan.app.telemost.exceptions.NoSuchBroadcastCreatedException;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.repository.dao.ConferenceStateDao;
import ru.yandex.chemodan.app.telemost.repository.dao.StreamDao;
import ru.yandex.chemodan.app.telemost.repository.model.BroadcastDto;
import ru.yandex.chemodan.app.telemost.repository.model.ConferenceStateDto;
import ru.yandex.chemodan.app.telemost.repository.model.StreamDto;
import ru.yandex.chemodan.app.telemost.services.BroadcastService;
import ru.yandex.chemodan.app.telemost.services.PropertyManager;
import ru.yandex.chemodan.app.telemost.services.StreamService;
import ru.yandex.chemodan.app.telemost.services.model.Broadcast;
import ru.yandex.chemodan.app.telemost.services.model.BroadcastAndConferenceUris;
import ru.yandex.chemodan.app.telemost.services.model.BroadcastUserId;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.services.model.Stream;
import ru.yandex.chemodan.app.telemost.services.model.StreamConnection;
import ru.yandex.chemodan.app.telemost.services.model.User;
import ru.yandex.chemodan.app.telemost.tools.ConferenceHelper;
import ru.yandex.chemodan.app.telemost.translator.TranslatorClient;
import ru.yandex.chemodan.app.telemost.ugcLive.UgcLiveClient;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastInitData;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastStatus;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class StreamServiceTest extends TelemostBaseContextTest {

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private StreamService streamService;

    @Autowired
    private TranslatorClient translatorClient;

    @Autowired
    private StreamDao streamDao;

    @Autowired
    private UgcLiveClient ugcLiveClient;

    @Autowired
    private PropertyManager propertyManager;

    @Autowired
    private ConferenceStateDao conferenceStateDao;

    @Autowired
    private ConferenceHelper conferenceHelper;

    @Autowired
    private GetPeersStateHandler getPeersStateHandler;

    @Autowired
    private SetPeerStateHandler setPeerStateHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private BroadcastUserId userId;
    private Conference conference;
    private Broadcast broadcast;
    private BroadcastAndConferenceUris uris;

    private static final String TEST_DISPLAY_NAME = "testName";

    @Before
    public void init() {
        user = createTestUserForUid(PassportOrYaTeamUid.passportUid(PassportUid.cons(19)));
        userId = BroadcastUserId.user(user.getUid());
        userService.addUserIfNotExists(user.getUid());

        UserData blackboxData = UserData.defaultUser("test", Option.of(TEST_DISPLAY_NAME), Option.empty(), Cf.map());
        long uid = user.getUid().getPassportUid().getUid();
        addUser(uid, blackboxData);

        conference = generateConference(user);
        broadcast = broadcastService.createBroadcast(conference.getDbId(), user.getUid().asString(),
                new BroadcastInitData());
        uris = new BroadcastAndConferenceUris(broadcast.getUri(), conference.getUri());
    }

    @Test
    public void testStartStream() {
        Option<ConferenceStateDto> conferenceStateDto1 = conferenceStateDao.findState(conference.getDbId());

        Stream stream = streamService.startStream(user.getUid(), uris);

        Option<ConferenceStateDto> conferenceStateDto2 = conferenceStateDao.findState(conference.getDbId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(translatorClient).start(any(), eq(uris), keyCaptor.capture(), tokenCaptor.capture());

        Assert.some(keyCaptor.getValue(), stream.getStream().getRtmpKey());
        Assert.some(tokenCaptor.getValue(), stream.getStream().getTranslatorToken());

        Assert.none(stream.getStream().getStoppedAt());
        Assert.some(findActiveStream());

        Assert.some(BroadcastStatus.STARTED, findBroadcastStatus());

        Assert.assertEquals(conferenceStateDto1.get().getVersion() + 1, conferenceStateDto2.get().getVersion());
    }

    @Test
    public void testStartStreamFailedAtTranslator() {
        RuntimeException exception = new RuntimeException();

        doThrow(exception).when(translatorClient)
                .start(any(), any(), anyString(), anyString());

        Assert.assertThrows(
                () -> streamService.startStream(user.getUid(), uris),
                RuntimeException.class, exception::equals
        );
        Assert.none(findActiveStream());
    }

    @Test
    public void testStopStream() {
        Option<ConferenceStateDto> conferenceStateDto1 = conferenceStateDao.findState(conference.getDbId());

        Stream started = streamService.startStream(user.getUid(), uris);
        Assert.some(ugcLiveClient.getStreamState(started.getStream().getUgcLiveSlug()));

        Stream stopped = streamService.stopStream(userId, uris);
        Assert.none(ugcLiveClient.getStreamState(started.getStream().getUgcLiveSlug()));

        Option<ConferenceStateDto> conferenceStateDto2 = conferenceStateDao.findState(conference.getDbId());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(translatorClient).stop(any(), eq(uris), keyCaptor.capture());

        Assert.some(keyCaptor.getValue(), stopped.getStream().getRtmpKey());

        Assert.some(stopped.getStream().getStoppedAt());
        Assert.some(BroadcastStatus.FINISHED, findBroadcastStatus());

        Assert.assertEquals(conferenceStateDto1.get().getVersion() + 2, conferenceStateDto2.get().getVersion());
    }

    @Test
    public void testRestartStream() {
        Option<ConferenceStateDto> conferenceStateDto1 = conferenceStateDao.findState(conference.getDbId());

        streamService.startStream(user.getUid(), uris);
        Stream stopped = streamService.stopStream(userId, uris);
        Stream started = streamService.startStream(user.getUid(), uris);

        Option<ConferenceStateDto> conferenceStateDto2 = conferenceStateDao.findState(conference.getDbId());

        Assert.some(findActiveStream());
        Assert.some(BroadcastStatus.STARTED, findBroadcastStatus());

        Assert.notEquals(stopped.getStream().getUgcLiveSlug(), started.getStream().getUgcLiveSlug());
        Assert.equals(stopped.getStream().getRtmpKey(), started.getStream().getRtmpKey());

        Assert.assertEquals(conferenceStateDto1.get().getVersion() + 3, conferenceStateDto2.get().getVersion());
    }

    @Test
    public void testVerifyBroadcast() {
        streamService.startStream(user.getUid(), uris);

        Assert.assertThrows(
                () -> stopStream(BroadcastUserId.user(PassportOrYaTeamUid.parseUid("666"))),
                CommandNotAllowedException.class
        );
        Assert.assertThrows(
                () -> stopStream(BroadcastUserId.translator("invalid")),
                InvalidTranslatorTokenTelemostException.class
        );
        Assert.assertThrows(
                () -> stopStream(BroadcastUserId.anonymous()),
                CommandNotAllowedException.class
        );

        Conference another = generateConference(user);

        Assert.assertThrows(
                () -> streamService.stopStream(userId, uris.withConferenceUri(another.getUri())),
                NoSuchBroadcastCreatedException.class
        );
    }

    @Test
    public void testStreamConnectionData() {
        Option<String> caption = Option.of("Caption");
        Option<String> description = Option.of("Description");

        Conference conference1 = generateConference(user);
        Broadcast broadcast1 = broadcastService.createBroadcast(conference1.getDbId(), user.getUid().asString(),
                new BroadcastInitData(caption, description));
        StreamConnection streamConnection = streamService.getConnection(broadcast1.getUri());

        Assert.equals(caption, streamConnection.getCaption());
        Assert.equals(description, streamConnection.getDescription());
        Assert.equals(BroadcastStatus.CREATED, streamConnection.getStatus());

        Assert.isTrue(streamConnection.getDisplayName().isPresent());
        Assert.equals(TEST_DISPLAY_NAME, streamConnection.getDisplayName().get());

        Assert.isFalse(streamConnection.getStreamStartedAt().isPresent());
    }

    @Test(expected = ConferenceLinkExpiredException.class)
    public void testNonPermanentTtl() {
        DateTimeUtils.setCurrentMillisOffset(
                Duration.standardSeconds(propertyManager.getConferenceTtl() + 5).getMillis());
        streamService.getConnection(broadcast.getUri());
    }

    @Test
    public void testHiddenBroadcasterPeer() {
        streamService.startStream(user.getUid(), uris);

        RoomConnectionInfo roomInfo1 = conferenceHelper.joinConference(conference, TEST_DISPLAY_NAME);

        RoomConnectionInfo roomInfo2 = conferenceHelper.joinConference(conference,
                ConferenceHelper.BROADCASTER_DISPLAY_NAME);

        broadcastService.updateTranslatorPeerIfNeed(conference.getDbId(), roomInfo2.getUserId());

        ConferenceStateDto conferenceStateDto = conferenceStateDao.findState(conference.getDbId()).get();

        Assert.equals(roomInfo2.getUserId(), conferenceStateDto.getStream().get().getTranslatorPeerId().get());

        setPeerStateHandler.processMessage(roomInfo1.getRoomId(), roomInfo1.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));
        setPeerStateHandler.processMessage(roomInfo2.getRoomId(), roomInfo2.getUserId(),
                new SetPeerStateRequest(objectMapper.createObjectNode()));

        GetPeersStateRequest request = new GetPeersStateRequest(Cf.list(
                new GetPeerStateRequest(roomInfo1.getUserId(), null),
                new GetPeerStateRequest(roomInfo2.getUserId(), null)
        ), null);

        PeersState peersState = (PeersState) getPeersStateHandler.processMessage(roomInfo2.getRoomId(),
                roomInfo2.getUserId(), request);

        Assert.equals(1, peersState.getPeers().length());
        Assert.equals(roomInfo1.getUserId(), peersState.getPeers().get(0).getPeerId());
    }

    private Option<StreamDto> findActiveStream() {
        return streamDao.findActiveByBroadcastKey(broadcast.getBroadcast().getBroadcastKey());
    }

    private Option<BroadcastStatus> findBroadcastStatus() {
        return broadcastService.getByKey(broadcast.getBroadcast().getBroadcastKey()).map(BroadcastDto::getStatus);
    }

    private Stream stopStream(BroadcastUserId uid) {
        return streamService.stopStream(uid, uris);
    }
}
