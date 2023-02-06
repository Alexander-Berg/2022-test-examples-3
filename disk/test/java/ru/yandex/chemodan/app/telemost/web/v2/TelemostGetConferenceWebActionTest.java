package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class TelemostGetConferenceWebActionTest extends AbstractConferenceWebActionTest {

    @Test
    public void testGetConference() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);

        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.get(String.format("/v2/conferences/%s", conferenceUri));

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        Assert.isTrue(ru.yandex.chemodan.app.telemost.web.v1.
                TelemostCreateConferenceWebActionTest.URL_PATTERN.matcher((String) uri).matches());
        Assert.isTrue(result.containsKey("room_id"));
        Assert.isTrue(result.containsKey("is_yandex_team"));
    }

}
