package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.repository.model.UserRole;
import ru.yandex.chemodan.app.telemost.services.model.Conference;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class TelemostJicofoGetPeerWebActionTest extends AbstractConferenceWebActionTest {

    @Test
    public void testGetPeer() throws IOException {
        A3TestHelper helper = getA3TestHelper();

        String fullUrl = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        String userId1 = testJoinConferenceByLink(
                "/v2/conferences/" + UrlUtils.urlEncode(fullUrl) + "/connection?display_name=oops", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        String userId2 = testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(fullUrl) + "/connection", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);

        ConferenceUriData conferenceUriData = conferenceUriService.getConferenceUriData(fullUrl);
        Conference conference = conferenceService.joinConference(
                Option.empty(), conferenceUriData, conferenceUriData.getUserToken(), Option.empty());

        HttpResponse response = helper.get(
                String.format("/v2/jicofo/conferences/%s/peers/%s", conference.getConferenceId(), userId1)
        );
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> result = mapper.readValue(helper.getResult(response), hashMapTypeReference);

        Assert.assertEquals(userId1, result.get("peer_id"));
        Assert.assertEquals("oops", result.get("display_name"));
        Assert.assertEquals(UserRole.MEMBER.name(), result.get("role"));
        assertEquals(3, result.size());

        response = helper.get(
                String.format("/v2/jicofo/conferences/%s/peers/%s", conference.getConferenceId(), userId2)
        );
        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        result = mapper.readValue(helper.getResult(response), hashMapTypeReference);

        Assert.assertEquals(userId2, result.get("peer_id"));
        Assert.assertEquals("Гость", result.get("display_name"));
        Assert.assertEquals(UserRole.MEMBER.name(), result.get("role"));
        assertEquals(3, result.size());

    }

}
