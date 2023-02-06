package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class TelemostGetUserWebActionTest extends AbstractConferenceWebActionTest {

    @Test
    public void testGetUser() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.get(
                String.format("/v2/users/%s", TEST_FEELGOOD_UID)
        );

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        Assert.assertEquals(TEST_FEELGOOD_UID, result.get("uid"));
        Assert.assertEquals("feelgood", result.get("display_name"));
        Assert.assertEquals("https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-200", result.get("avatar_url"));
        Assert.assertTrue(result.containsKey("is_yandex_staff"));
        Assert.assertTrue(result.containsKey("broadcast_allowed"));
        Assert.assertTrue(result.containsKey("broadcast_feature_enabled"));
    }
}
