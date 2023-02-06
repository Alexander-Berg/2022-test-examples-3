package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import static ru.yandex.misc.test.Assert.assertEquals;

public class TelemostGetConferencePeersWebActionTest extends AbstractConferenceWebActionTest {

    @Test
    public void testGetPeers() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        String peerId1 = testJoinConferenceByLink(
                "/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?display_name=oops", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        String peerId2 = testJoinConferenceByLink(
                "/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?uid=" + TEST_FEELGOOD_UID, TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);


        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.put(
                String.format("/v2/conferences/%s/peers", conferenceUri),
                "{ \"peers_ids\": [\"" + peerId1 + "\",\"" + peerId2 + "\"]}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        List items = (List) result.get("items");
        assertEquals(2, items.size());

        Map first = (Map) items.get(0);
        assertEquals(peerId1, first.get("peer_id"));
        assertEquals("oops", first.get("display_name"));
        assertEquals(UserRole.MEMBER.name(), first.get("role"));
        assertEquals(3, first.size());

        Map second = (Map) items.get(1);
        assertEquals(peerId2, second.get("peer_id"));
        assertEquals(TEST_FEELGOOD_UID, second.get("uid"));
        assertEquals("https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-200", second.get("avatar_url"));
        assertEquals("feelgood", second.get("display_name"));
        assertEquals(false, second.get("is_default_avatar"));
        assertEquals(UserRole.OWNER.name(), second.get("role"));
        assertEquals(6, second.size());
    }

    @Test
    public void testGetPeersToMuchData() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);

        ListF<String> randomPeers = randomList(102, () -> Random2.R.nextDigits(10)).map(s -> "\"" + s + "\"");

        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.put(
                String.format("/v2/conferences/%s/peers", conferenceUri),
                "{ \"peers_ids\": [" + randomPeers.mkString(",") + "]}");

        Assert.equals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);
        Map error = (Map) result.get("error");
        assertEquals("TooMuchDataError", error.get("name"));
    }

    private static <T> ListF<T> randomList(int count, Function0<T> valueGenerator) {
        ListF<T> result = Cf.arrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            result.add(valueGenerator.apply());
        }

        return result;
    }
}
