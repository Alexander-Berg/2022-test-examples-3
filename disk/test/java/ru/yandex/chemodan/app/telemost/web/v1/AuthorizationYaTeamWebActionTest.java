package ru.yandex.chemodan.app.telemost.web.v1;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.telemost.services.ConferenceUriService;
import ru.yandex.chemodan.app.telemost.services.model.ConferenceUriData;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class AuthorizationYaTeamWebActionTest extends TelemostWebActionBaseTest {

    private static final Pattern AUTHORIZED_URI_PATTERN = Pattern.compile("^https:\\/\\/telemost\\.yandex\\.ru\\/j\\/\\d{38}\\?yt-token=.+$");

    @Autowired
    private ConferenceUriService conferenceUriService;

    @Test
    public void testAuthorization() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post("/v1/conferences?uid=yt:123");
        Assert.equals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        Map<String, Object> result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        HttpResponse authorizationResponse = helper.post(String.format("/v1/yandex-team/conferences/%s/authorized-uri?uid=%s",
                URLEncoder.encode(uri.toString(), "UTF-8"), "yt:12345"));
        Assert.equals(HttpStatus.SC_OK, authorizationResponse.getStatusLine().getStatusCode());
        Map<String, Object> authorizationResult = mapper.readValue(helper.getResult(authorizationResponse), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(authorizationResult.containsKey("uri"));
        Object authorizedUri = authorizationResult.get("uri");
        Assert.assertTrue(AUTHORIZED_URI_PATTERN.matcher(authorizedUri.toString()).matches());
    }

    @Test
    public void testAuthorizationWithShortenedLinks() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post("/v1/conferences?uid=yt:123");
        Assert.equals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        Map<String, Object> result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        ConferenceUriData uriData = conferenceUriService.getYaTeamConferenceUriData(uri.toString());
        String uriToCheck = String.format("telemost://yandex-team/%s", uriData.getShortUrlId());
        HttpResponse authorizationResponse = helper.post(String.format("/v1/yandex-team/conferences/%s/authorized-uri?uid=%s",
                URLEncoder.encode(uriToCheck, "UTF-8"), "yt:12345"));
        Assert.equals(HttpStatus.SC_OK, authorizationResponse.getStatusLine().getStatusCode());
        Map<String, Object> authorizationResult = mapper.readValue(helper.getResult(authorizationResponse), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(authorizationResult.containsKey("uri"));
        Object authorizedUri = authorizationResult.get("uri");
        Assert.assertTrue(AUTHORIZED_URI_PATTERN.matcher(authorizedUri.toString()).matches());
    }
}
