package ru.yandex.chemodan.app.telemost.web.v1;

import java.io.IOException;
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
import ru.yandex.chemodan.app.telemost.services.model.XMPPLimitType;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.test.Assert;

public class TelemostCreateConferenceWebActionTest extends TelemostWebActionBaseTest {

    public static final Pattern URL_PATTERN = Pattern.compile("https://telemost.yandex.ru/j/\\d{14}");

    public static final Pattern YA_TEAM_URL_PATTERN = Pattern.compile("https://telemost.yandex-team.ru/j/\\d{38}");

    private final static String TEST_UID = "123";

    @Before
    public void initUser() {
        super.before();
        addUsers(Cf.map(Long.parseLong(TEST_UID), UserData.staff("test", Option.of("test"), Option.empty(), Cf.map())));
    }

    @Test
    public void testCreateConferenceWithUid() throws IOException {
        testByLink("/v1/conferences?uid=" + TEST_UID, URL_PATTERN);
    }

    @Test
    public void testCreateConferenceWithoutUid() throws IOException {
        testByLink("/v1/conferences", URL_PATTERN);
    }

    @Test
    public void testCreateConferenceWithYaTeamUid() throws IOException {
        testByLink("/v1/conferences?uid=yt:123", YA_TEAM_URL_PATTERN);
    }

    @Test
    public void testCreateConferenceWithYaTeamUidAndExternal() throws IOException {
        testByLink("/v1/conferences?uid=yt:123&external_meeting=true", URL_PATTERN);
    }

    private void testByLink(String link, Pattern urlPattern) throws IOException {
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.post(link);
        Assert.equals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        Map<String, Object> result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>(){});
        Assert.isTrue(result.containsKey("uri"));
        Object uri = result.get("uri");
        Assert.isTrue(uri instanceof String);
        Assert.isTrue(urlPattern.matcher((String) uri).matches());
        Assert.isTrue(result.containsKey("conf_pwd"));
        Assert.isTrue(result.containsKey("conf_id"));
        Assert.isTrue(result.containsKey("client_configuration"));
        Assert.assertEquals(XMPPLimitType.STAFF.name(), result.get("limit_type"));
    }
}
