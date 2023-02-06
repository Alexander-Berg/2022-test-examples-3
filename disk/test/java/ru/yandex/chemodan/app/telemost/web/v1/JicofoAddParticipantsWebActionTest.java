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

public class JicofoAddParticipantsWebActionTest extends TelemostWebActionBaseTest {

    @Test
    public void testSuccessful() throws IOException {
        Map<String, Object> conferenceData = createConferenceV1(Cf.map());
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.put(String.format("/v1/jicofo/rooms/%s/participants/peer-id-value", conferenceData.get("conf_id")), null);
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testConferenceNotFound() {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.put(String.format("/v1/jicofo/rooms/%s/participants/peer-id-value", "not_exists"), null);
        Assert.equals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }
}
