package ru.yandex.chemodan.app.telemost.appmessages;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.app.telemost.appmessages.model.SetPeerStateRequest;
import ru.yandex.chemodan.app.telemost.appmessages.model.SetPeerStateResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppMessageProcessorTest extends TelemostBaseContextTest {
    public static final String STATE_JSON = "{\n" +
            "    \"user_data\": {\n" +
            "      \"display_name\": \"Гость\",\n" +
            "      \"is_default_avatar\": true,\n" +
            "      \"role\": \"MEMBER\"\n" +
            "    }," +
            "    \"media_state\": {\n" +
            "      \"tracks\": [\n" +
            "        {\n" +
            "          \"track_id\": \"id1\",\n" +
            "          \"muted\": true,\n" +
            "          \"source\": \"camera\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"track_id\": \"id122\",\n" +
            "          \"muted\": false,\n" +
            "          \"source\": \"microphone\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"client_state\": {\n" +
            "        \"is_local_recording\": true,\n" +
            "        \"is_cloud_recording\": false\n" +
            "      }\n" +
            "    }\n" +
            "  }";

    public static final String SET_STATUS_MESSAGE =
            "{\n" +
                    "  \"type\": \"peers_state_set\",\n" +
                    "  \"payload\": " + STATE_JSON + "\n" +
                    "}\n";

    public static final String PEERS_STATE_SET_RESPONSE =
            "{\n" +
                    "  \"type\" : \"peers_state_set_response\",\n" +
                    "  \"payload\" : { }\n" +
                    "}";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppMessageProcessor appMessageProcessor;

    @Test
    public void testJsonMessageDeserialization() throws IOException {
        TransportMessage message = objectMapper.readValue(SET_STATUS_MESSAGE, TransportMessage.class);

        assertEquals("peers_state_set", message.getType());
        SetPeerStateRequest payload = appMessageProcessor.toAppMessage(message, SetPeerStateRequest.class);
        JsonNode mediaState = payload.getStateContent().get("media_state");
        JsonNode tracksNode = mediaState.get("tracks");
        assertTrue(tracksNode.isArray());
        ArrayList<JsonNode> tracks = Lists.newArrayList(tracksNode.elements());
        assertEquals(2, tracks.size());
        JsonNode featuresNode = mediaState.get("client_state");
        assertEquals(2, featuresNode.size());
    }

    @Test
    public void testJsonMessageSerialization() throws JsonProcessingException {
        TransportMessage message = appMessageProcessor.toJsonMessage(SetPeerStateResponse.INSTANCE);

        String json = objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).writeValueAsString(message);
        assertEquals(PEERS_STATE_SET_RESPONSE, json);
    }

}
