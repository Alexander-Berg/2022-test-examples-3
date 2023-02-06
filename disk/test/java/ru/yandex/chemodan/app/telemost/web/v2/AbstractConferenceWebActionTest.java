package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.services.ParticipantType;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public abstract class AbstractConferenceWebActionTest extends TelemostWebActionBaseTest {

    protected static final String TEST_FEELGOOD_UID = "3000185708";

    @Before
    public void initFeelgoodUser() {
        super.before();
        addUsers(Cf.map(Long.parseLong(TEST_FEELGOOD_UID), UserData.staff("feelgood", Option.of("feelgood"), Option.of("0/0-0"), Cf.map())));;
    }

    @SuppressWarnings("unchecked")
    protected String testCreateConferenceByLink(String link, Pattern urlPattern,
                                                boolean broadcastFeature) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post(link);
        Assert.equals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        Map<String, Object>
                result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        Assert.isTrue(urlPattern.matcher((String) uri).matches());
        Assert.isTrue(result.containsKey("ws_uri"));
        Assert.isTrue(result.containsKey("room_id"));
        Assert.isTrue(result.containsKey("safe_room_id"));
        Assert.isTrue(result.containsKey("peer_id"));

        Assert.isTrue(result.containsKey("conference_state"));

        Map<String, Object> conferenceState = (Map<String, Object>)result.get("conference_state");
        Assert.isTrue(conferenceState.containsKey("local_recording_allowed"));
        Assert.isTrue(conferenceState.containsKey("cloud_recording_allowed"));
        Assert.isTrue(conferenceState.containsKey("chat_allowed"));
        Assert.isTrue(conferenceState.containsKey("control_allowed"));
        Assert.isTrue(conferenceState.containsKey("broadcast_allowed"));
        Assert.isTrue(conferenceState.containsKey("broadcast_feature_enabled"));

        Assert.equals(broadcastFeature, conferenceState.get("broadcast_feature_enabled"));

        return uri.toString();
    }

    @SuppressWarnings("unchecked")
    protected String testJoinConferenceByLink(String link, Pattern urlPattern,
                                              boolean broadcastFeature) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.get(link);
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Map<String, Object>
                result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        Assert.isTrue(urlPattern.matcher((String) uri).matches());
        Assert.isTrue(result.containsKey("ws_uri"));
        Assert.isTrue(result.containsKey("room_id"));
        Assert.isTrue(result.containsKey("peer_id"));

        Assert.isTrue(result.containsKey("conference_state"));

        Map<String, Object> conferenceState = (Map<String, Object>)result.get("conference_state");
        Assert.isTrue(conferenceState.containsKey("local_recording_allowed"));
        Assert.isTrue(conferenceState.containsKey("cloud_recording_allowed"));
        Assert.isTrue(conferenceState.containsKey("chat_allowed"));
        Assert.isTrue(conferenceState.containsKey("control_allowed"));
        Assert.isTrue(conferenceState.containsKey("broadcast_allowed"));
        Assert.isTrue(conferenceState.containsKey("broadcast_feature_enabled"));

        Assert.equals(broadcastFeature, conferenceState.get("broadcast_feature_enabled"));

        return result.get("peer_id").toString();
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> joinToConferenceAndGetState(String link, ParticipantType participantType) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.get(link);
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Map<String, Object> result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {});

        Assert.isTrue(result.containsKey("conference_state"));
        Assert.isTrue(result.containsKey("peer_id"));

        String peerId = (String)result.get("peer_id");

        if (participantType == ParticipantType.USER) {
            Assert.isFalse(peerId.startsWith("broadcaster-"));
        } else {
            Assert.isTrue(peerId.startsWith("broadcaster-"));
        }

        return (Map<String, Object>)result.get("conference_state");
    }
}
