package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

public class TelemostJoinConferenceWebActionTest extends AbstractConferenceWebActionTest {

    private static final String TEST_YA_TEAM_UID = "yt:123456";

    private static final String UID = "11133";

    public static final Pattern YA_TEAM_URL_PATTERN = Pattern.compile("https://telemost.yandex-team.ru/j/\\d{38}");

    @Before
    public void initUser() {
        super.before();
        addUsers(Cf.map(Long.parseLong(UID), UserData.staff("test", Option.of("test"), Option.empty(), Cf.map())));
    }

    @Test
    public void testJoinWithoutUid() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
    }

    @Test
    public void testJoinWithUid() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?uid=11133", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
    }

    @Test
    public void testJoinWithYaTeamUid() throws IOException {
        Assert.equals(userService.upsert(PassportOrYaTeamUid.parseUid(TEST_YA_TEAM_UID), true).isBroadcastEnabled(),
                true);
        String conferenceUri = testCreateConferenceByLink("/v2/conferences?uid=" + TEST_YA_TEAM_UID, TelemostCreateConferenceWebActionTest.YA_TEAM_URL_PATTERN,
                true);
        String encodedUri = URLEncoder.encode(conferenceUri, "UTF-8");
        A3TestHelper helper = getA3TestHelper();
        HttpResponse authorizationResponse = helper.post(
                String.format("/v1/yandex-team/conferences/%s/authorized-uri?uid=%s", encodedUri, "yt:12345"));
        Assert.equals(HttpStatus.SC_OK, authorizationResponse.getStatusLine().getStatusCode());
        Map<String, Object> authorizationResult = mapper.readValue(helper.getResult(authorizationResponse), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(authorizationResult.containsKey("uri"));
        Object authorizedUri = authorizationResult.get("uri");
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(authorizedUri.toString(), "UTF-8") + "/connection?uid=" + UID, YA_TEAM_URL_PATTERN,
                true);
    }

    @Test
    public void testReJoinWithUid() throws IOException {
        String conferenceUri = testCreateConferenceByLink("/v2/conferences", TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?uid=" + UID, TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
        testJoinConferenceByLink("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/connection?uid=" + UID, TelemostCreateConferenceWebActionTest.URL_PATTERN,
                false);
    }

}
