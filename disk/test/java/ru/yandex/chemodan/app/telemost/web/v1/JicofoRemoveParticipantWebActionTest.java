package ru.yandex.chemodan.app.telemost.web.v1;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class JicofoRemoveParticipantWebActionTest extends TelemostWebActionBaseTest {

    @Test
    public void testRemoving() throws IOException {
        Map<String, Object> conferenceData = createConferenceV1(Cf.map());
        A3TestHelper helper = getA3TestHelper();
        String peerId = "peer-id-value";
        String roomId = conferenceData.get("conf_id").toString();
        helper.put(String.format("/v1/jicofo/rooms/%s/participants/%s", roomId, peerId), null);
        HttpResponse response = helper.delete(String.format("/v1/jicofo/rooms/%s/participants/%s", roomId, peerId));
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testNotExistingPeer() throws IOException {
        Map<String, Object> conferenceData = createConferenceV1(Cf.map());
        A3TestHelper helper = getA3TestHelper();
        String peerId = "peer-id-value";
        String roomId = conferenceData.get("conf_id").toString();
        HttpResponse response = helper.delete(String.format("/v1/jicofo/rooms/%s/participants/%s", roomId, peerId));
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }
}
