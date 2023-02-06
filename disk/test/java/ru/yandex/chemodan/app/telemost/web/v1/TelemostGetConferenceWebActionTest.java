package ru.yandex.chemodan.app.telemost.web.v1;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class TelemostGetConferenceWebActionTest extends TelemostWebActionBaseTest {

    private final static String UID = "1234";

    private final static String DEFAULT_CREATE_CONFERENCE_URL = "/v1/conferences";

    private final static String YA_TEAM_UID = "yt:1234";

    private final static String YA_TEAM_CREATE_CONFERENCE_URL = DEFAULT_CREATE_CONFERENCE_URL + "?uid=" + YA_TEAM_UID;

    @Before
    public void initUser() {
        super.before();
        addUsers(Cf.map(Long.parseLong(UID), UserData.staff("test", Option.of("test"), Option.empty(), Cf.map())));
    }

    @Test
    public void testGetWithUidConference() throws IOException {
        testByUrlTemplate(s -> String.format("/v1/conferences/%s/connection?uid=" + UID, s), DEFAULT_CREATE_CONFERENCE_URL);
    }

    @Test
    public void testGetWithoutUidConference() throws IOException {
        testByUrlTemplate(s -> String.format("/v1/conferences/%s/connection", s), DEFAULT_CREATE_CONFERENCE_URL);
    }

    @Test
    public void testConnectionWithParameters() throws IOException {
        testByUrlTemplate(s -> String.format("/v1/conferences/%s/connection", s + "%3Ffrom%3Ddisk"), DEFAULT_CREATE_CONFERENCE_URL);
    }

    @Test
    public void testGetWithUidYtConference() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse createConferenceResponse = helper.post(YA_TEAM_CREATE_CONFERENCE_URL);
        Assert.equals(HttpStatus.SC_CREATED, createConferenceResponse.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> creationResult =
                mapper.readValue(helper.getResult(createConferenceResponse), hashMapTypeReference);
        Assert.isTrue(creationResult.containsKey("uri"));
        Object uriObject = creationResult.get("uri");
        Assert.isTrue(uriObject instanceof String);
        Assert.isTrue(creationResult.containsKey("conf_pwd"));
        Assert.isTrue(creationResult.containsKey("conf_id"));
        Assert.isTrue(creationResult.containsKey("limit_type"));
        String uri = (String) uriObject;
        String encodedUri = URLEncoder.encode(uri, "UTF-8");
        HttpResponse authorizationResponse = helper.post(
                String.format("/v1/yandex-team/conferences/%s/authorized-uri?uid=%s", encodedUri, "yt:12345"));
        Assert.equals(HttpStatus.SC_OK, authorizationResponse.getStatusLine().getStatusCode());
        Map<String, Object> authorizationResult = mapper.readValue(helper.getResult(authorizationResponse), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(authorizationResult.containsKey("uri"));
        Object authorizedUri = authorizationResult.get("uri");
        HttpResponse getConferenceResponse = helper.get(String.format("/v1/conferences/%s/connection?uid=%s", URLEncoder.encode(authorizedUri.toString(), "UTF-8"), UID));
        Assert.equals(HttpStatus.SC_OK, getConferenceResponse.getStatusLine().getStatusCode());
        Map<String, Object> gettingResult =
                mapper.readValue(helper.getResult(getConferenceResponse), hashMapTypeReference);
        Assert.isTrue(gettingResult.containsKey("uri"));
        Assert.isTrue(gettingResult.containsKey("conf_pwd"));
        Assert.isTrue(gettingResult.containsKey("conf_id"));
        Assert.isTrue(gettingResult.containsKey("client_configuration"));
        Assert.assertEquals(uri, gettingResult.get("uri"));
        Assert.equals(creationResult.get("conf_pwd"), gettingResult.get("conf_pwd"));
        Assert.equals(creationResult.get("conf_id"), gettingResult.get("conf_id"));
        Assert.assertEquals(creationResult.get("limit_type"), gettingResult.get("limit_type"));
    }

    @Test
    public void testShortLinks() throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse createConferenceResponse = helper.post("/v1/conferences");
        Assert.equals(HttpStatus.SC_CREATED, createConferenceResponse.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> creationResult =
                mapper.readValue(helper.getResult(createConferenceResponse), hashMapTypeReference);
        Assert.isTrue(creationResult.containsKey("uri"));
        Object uriObject = creationResult.get("uri");
        Assert.isTrue(uriObject instanceof String);
        String sourceUri = ((String) uriObject);
        String uri = sourceUri.replaceAll("https:\\/\\/telemost\\.yandex\\.ru\\/", "telemost://");
        String encodedUri = URLEncoder.encode(uri, "UTF-8");
        HttpResponse getConferenceResponse = helper.get(String.format("/v1/conferences/%s/connection", encodedUri));
        Assert.equals(HttpStatus.SC_OK, getConferenceResponse.getStatusLine().getStatusCode());
        Map<String, Object> gettingResult =
                mapper.readValue(helper.getResult(getConferenceResponse), hashMapTypeReference);
        Assert.isTrue(gettingResult.containsKey("uri"));
        Assert.isTrue(gettingResult.containsKey("conf_pwd"));
        Assert.isTrue(gettingResult.containsKey("conf_id"));
        Assert.isTrue(gettingResult.containsKey("client_configuration"));
        Assert.equals(sourceUri, gettingResult.get("uri"));
        Assert.equals(creationResult.get("conf_pwd"), gettingResult.get("conf_pwd"));
        Assert.equals(creationResult.get("conf_id"), gettingResult.get("conf_id"));
        Assert.assertEquals(creationResult.get("limit_type"), gettingResult.get("limit_type"));
    }

    private void testByUrlTemplate(Function<String, String> connectionUrlBuilder, String createConferenceUrl) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse createConferenceResponse = helper.post(createConferenceUrl);
        Assert.equals(HttpStatus.SC_CREATED, createConferenceResponse.getStatusLine().getStatusCode());
        TypeReference<HashMap<String, Object>> hashMapTypeReference = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> creationResult =
                mapper.readValue(helper.getResult(createConferenceResponse), hashMapTypeReference);
        Assert.isTrue(creationResult.containsKey("uri"));
        Object uriObject = creationResult.get("uri");
        Assert.isTrue(uriObject instanceof String);
        Assert.isTrue(creationResult.containsKey("conf_pwd"));
        Assert.isTrue(creationResult.containsKey("conf_id"));
        Assert.isTrue(creationResult.containsKey("limit_type"));
        String uri = (String) uriObject;
        String encodedUri = URLEncoder.encode(uri, "UTF-8");
        HttpResponse getConferenceResponse = helper.get(connectionUrlBuilder.apply(encodedUri));
        Assert.equals(HttpStatus.SC_OK, getConferenceResponse.getStatusLine().getStatusCode());
        Map<String, Object> gettingResult =
                mapper.readValue(helper.getResult(getConferenceResponse), hashMapTypeReference);
        Assert.isTrue(gettingResult.containsKey("uri"));
        Assert.isTrue(gettingResult.containsKey("conf_pwd"));
        Assert.isTrue(gettingResult.containsKey("conf_id"));
        Assert.isTrue(gettingResult.containsKey("client_configuration"));
        Assert.equals(uri, gettingResult.get("uri"));
        Assert.equals(creationResult.get("conf_pwd"), gettingResult.get("conf_pwd"));
        Assert.equals(creationResult.get("conf_id"), gettingResult.get("conf_id"));
        Assert.assertEquals(creationResult.get("limit_type"), gettingResult.get("limit_type"));
    }
}
