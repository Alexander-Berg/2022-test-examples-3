package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.chemodan.app.telemost.appmessages.AppMessageSender;
import ru.yandex.chemodan.app.telemost.repository.dao.UserStateDtoDao;
import ru.yandex.chemodan.app.telemost.repository.model.UserBackData;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.repository.model.UserStateDto;
import ru.yandex.chemodan.app.telemost.room.proto.MediatorOuterClass;
import ru.yandex.chemodan.app.telemost.services.ConferenceParticipantsService;
import ru.yandex.chemodan.app.telemost.services.ParticipantType;
import ru.yandex.chemodan.app.telemost.services.RoomService;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceClientParameters;
import ru.yandex.chemodan.app.telemost.services.model.RoomConnectionInfo;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static ru.yandex.bolts.collection.Tuple3.tuple;
import static ru.yandex.chemodan.app.telemost.appmessages.AppMessageProcessorTest.SET_STATUS_MESSAGE;
import static ru.yandex.chemodan.app.telemost.appmessages.AppMessageProcessorTest.STATE_JSON;

public class TelemostHandleAppMessageWebActionTest extends TelemostWebActionBaseTest {

    @Autowired
    @Qualifier("roomServiceBlockingV2")
    private RoomService roomServiceBlockingV2;
    @Autowired
    private AppMessageSender appMessageSender;
    @Autowired
    private UserStateDtoDao userStateDtoDao;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ConferenceParticipantsService conferenceParticipantsService;

    @Test
    public void testSetVersion() throws IOException {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.empty())
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        RoomConnectionInfo owner = roomServiceBlockingV2
                .createRoomAndUserForConference(conference, Option.empty(), Option.empty(), Option.empty());
        RoomConnectionInfo participant =
                roomServiceBlockingV2.joinToConference(conference, Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), ParticipantType.USER);

        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post("/v2/app_messages/handle_app_message",
                "{\"room_id\": \"" + conference.getRoomId() + "\"," +
                        "\"peer_id\": \"" + owner.getUserId() + "\"," +
                        "\"payload\": " + SET_STATUS_MESSAGE + "}");
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertMessagePayload(helper.getResult(response),
                "peers_state_set_response", JsonNodeFactory.instance.objectNode());

        appMessageSender.awaitAllProcessed(5, TimeUnit.SECONDS);

        ListF<MediatorOuterClass.SendAppMessageRequest> sendAppRequests = mockRoomGrpcService.getSendAppRequests();
        MediatorOuterClass.SendAppMessageRequest request = sendAppRequests.single();

        assertEquals(conference.getRoomId(), request.getRoomId());
        assertMessagePayload(request.getMessage(),
                "peers_state",
                peerStateResponse(Cf.list(tuple(owner.getUserId(), 1L, objectMapper.readTree(STATE_JSON))),
                        Option.empty()));
        assertEquals(MediatorOuterClass.SendAppMessageRequest.SendToAll.newBuilder()
                        .addExcludePeers(owner.getUserId())
                        .build(),
                request.getSendToAll());

        MapF<String, UserStateDto> states =
                userStateDtoDao.findStates(conference.getDbId(), Cf.list(owner.getUserId()));
        assertEquals(1, states.size());
        UserStateDto state = states.get(owner.getUserId());
        assertEquals(1, state.getVersion());

        // Мы не храним роль в таблице userState поэтому ее не будет UserStateDto
        JsonNode payload = objectMapper.readTree(SET_STATUS_MESSAGE.getBytes()).get("payload");
        ((ObjectNode)payload.get("user_data")).remove("role");
        assertEquals(payload, state.getState());
    }

    @Test
    public void testGetVersionForAll() {
        Conference conference =
                conferenceService.generateConference(ConferenceClientParameters.builder()
                        .user(Option.empty())
                        .permanent(Option.of(Boolean.FALSE))
                        .staffOnly(Option.of(Boolean.FALSE))
                        .externalMeeting(Option.empty())
                        .eventId(Option.empty()).build());
        RoomConnectionInfo participant1 =
                roomServiceBlockingV2
                        .createRoomAndUserForConference(conference, Option.empty(), Option.empty(), Option.empty());
        RoomConnectionInfo participant2 =
                roomServiceBlockingV2.joinToConference(conference, Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), ParticipantType.USER);
        RoomConnectionInfo participant3 =
                roomServiceBlockingV2.joinToConference(conference, Option.empty(), Option.empty(),
                        Option.empty(), Option.empty(), ParticipantType.USER);

        UUID userId1 = conferenceParticipantsService.findConferenceParticipant(conference, participant1.getUserId())
                .getDbUserId();
        UUID userId2 = conferenceParticipantsService.findConferenceParticipant(conference, participant2.getUserId())
                .getDbUserId();
        UUID userId3 = conferenceParticipantsService.findConferenceParticipant(conference, participant3.getUserId())
                .getDbUserId();

        ObjectNode state11 = randomNode();
        ObjectNode state21 = randomNode();
        ObjectNode state22 = randomNode();
        ObjectNode state23 = randomNode();
        ObjectNode state31 = randomNode();

        Assert.some(userStateDtoDao.updatePeerState(userId1, state11));
        Assert.some(userStateDtoDao.updatePeerState(userId2, state21));
        Assert.some(userStateDtoDao.updatePeerState(userId2, state22));
        Assert.some(userStateDtoDao.updatePeerState(userId2, state23));
        Assert.some(userStateDtoDao.updatePeerState(userId3, state31));
        Assert.none(userStateDtoDao.updatePeerState(userId3, state31));
        UserBackData userBackData = new UserBackData(Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.of(UserRole.MEMBER));
        Stream.of(state11, state21, state22, state23, state31)
                .forEach(node -> node.set("user_data", objectMapper.valueToTree(userBackData)));

        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post("/v2/app_messages/handle_app_message",
                "{\"room_id\": \"" + conference.getRoomId() + "\"," +
                        "\"peer_id\": \"" + participant1.getUserId() + "\"," +
                        "\"payload\": " + getVersionsRequest(Cf.map(
                        participant1.getUserId(), 1L,
                        participant2.getUserId(), 2L,
                        participant3.getUserId(), null
                ), Option.empty()) + "}");
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertMessagePayload(helper.getResult(response),
                "peers_state",
                peerStateResponse(Cf.list(
                        tuple(participant2.getUserId(), 3L, state23),
                        tuple(participant3.getUserId(), 1L, state31)),
                        Option.of(0L)
                ));

        response = helper.post("/v2/app_messages/handle_app_message",
                "{\"room_id\": \"" + conference.getRoomId() + "\"," +
                        "\"peer_id\": \"" + participant1.getUserId() + "\"," +
                        "\"payload\": " + getVersionsRequest(Cf.map(
                        participant1.getUserId(), 1L,
                        participant2.getUserId(), 2L,
                        participant3.getUserId(), null
                ), Option.of(-1L)) + "}");
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertMessagePayload(helper.getResult(response),
                "peers_state",
                peerStateResponse(Cf.list(
                        tuple(participant2.getUserId(), 3L, state23),
                        tuple(participant3.getUserId(), 1L, state31)),
                        Option.of(0L)
                ));

        response = helper.post("/v2/app_messages/handle_app_message",
                "{\"room_id\": \"" + conference.getRoomId() + "\"," +
                        "\"peer_id\": \"" + participant1.getUserId() + "\"," +
                        "\"payload\": " + getVersionsRequest(Cf.map(
                        participant1.getUserId(), 1L,
                        participant2.getUserId(), 2L,
                        participant3.getUserId(), null
                ), Option.of(0L)) + "}");
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertMessagePayload(helper.getResult(response),
                "peers_state",
                peerStateResponse(Cf.list(
                        tuple(participant2.getUserId(), 3L, state23),
                        tuple(participant3.getUserId(), 1L, state31)),
                        Option.empty()
                ));
    }


    private static ObjectNode randomNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put(Random2.threadLocal().nextString(30), Random2.threadLocal().nextString(30));
        return node;
    }

    private static JsonNode getVersionsRequest(MapF<String, Long> peersToVersion, Option<Long> stateVersion) {
        ArrayNode peers = JsonNodeFactory.instance.arrayNode();
        peersToVersion.forEach((k, v) -> {
            ObjectNode peer = JsonNodeFactory.instance.objectNode();
            peer.put("peer_id", k);
            if (v != null) {
                peer.put("version", v);
            }
            peers.add(peer);
        });

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("peers", peers);
        if (stateVersion.isPresent()) {
            ObjectNode c = JsonNodeFactory.instance.objectNode();
            c.put("version", stateVersion.get());
            payload.put("conference", c);
        }

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("type", "peers_state_get");
        root.set("payload", payload);
        return root;
    }

    @SneakyThrows
    private void assertMessagePayload(String message, String messageType, JsonNode payload) {
        JsonNode messageAsNode = objectMapper.readTree(message);
        JsonNode appMessagePayload = messageAsNode.get("payload");

        assertEquals(messageType, appMessagePayload.get("type").asText());
        JSONAssert.assertEquals(payload.toString(), appMessagePayload.get("payload").toString(), false);
    }

    @SneakyThrows
    private void assertMessagePayload(ByteString message, String messageType, JsonNode payload) {
        JsonNode messageAsNode = objectMapper.readTree(message.toByteArray());
        assertEquals(messageType, messageAsNode.get("type").asText());
        JSONAssert.assertEquals(payload.toString(), messageAsNode.get("payload").toString(), false);
    }

    private JsonNode peerStateResponse(ListF<Tuple3<String, Long, JsonNode>> peers, Option<Long> stateVersion) {
        ArrayNode peersNode = JsonNodeFactory.instance.arrayNode();
        peers.forEach(t -> {
            ObjectNode peer = JsonNodeFactory.instance.objectNode();
            peer.put("peer_id", t._1);
            peer.put("version", t._2);
            peer.put("state", t._3);
            peersNode.add(peer);
        });

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("peers", peersNode);

        if (stateVersion.isPresent()) {
            ObjectNode c = JsonNodeFactory.instance.objectNode();
            c.put("version", stateVersion.get());
            ObjectNode s = JsonNodeFactory.instance.objectNode();
            s.put("local_recording_allowed", true);
            s.put("cloud_recording_allowed", true);
            s.put("chat_allowed", true);
            s.put("control_allowed", true);
            s.put("broadcast_allowed", true);
            s.put("broadcast_feature_enabled", false);
            c.put("state", s);
            payload.put("conference", c);
        }

        return payload;
    }

}
