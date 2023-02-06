package ru.yandex.direct.libs.oauth.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.libs.oauth.client.model.ApplicationInfo;
import ru.yandex.direct.test.utils.MockedHttpWebServerRule;

import static java.util.Arrays.asList;

public class OauthClientTest {
    @Rule
    public MockedHttpWebServerRule server = new MockedHttpWebServerRule(ContentType.APPLICATION_JSON);

    AsyncHttpClient asyncHttpClient;
    OauthClient client;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Config httpCfg = ConfigFactory.load("ru/yandex/direct/libs/oauth/client/http_mock.conf");
        for (Config req : httpCfg.getConfigList("requests")) {
            server.addResponse(req.getString("url"), req.getString("response"));
        }

        asyncHttpClient = new DefaultAsyncHttpClient();
        client = new OauthClient(server.getServerURL() + "/", asyncHttpClient, new FetcherSettings());
    }

    @After
    public void tearDown() throws Exception {
        asyncHttpClient.close();
    }

    @Test
    public void getGeoData_GetExactOneResult() {
        String validId = "2f60fbb45f034943a0e2846a91b957a3";
        Map<String, Result<ApplicationInfo>> res = client.getApplicationsInfo(asList("xxx", validId));
        System.out.println(res);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(res).containsOnlyKeys("xxx", validId);
        softly.assertThat(res.get("xxx").getSuccess()).isNull();
        ApplicationInfo appInfo = res.get(validId).getSuccess();
        softly.assertThat(appInfo).isNotNull();
        if (appInfo != null) {
            softly.assertThat(appInfo.id()).isEqualTo(validId);
            softly.assertThat(appInfo.scope()).containsExactly("direct:api");
            softly.assertThat(appInfo.name()).isEqualTo("zhur-direct-test");
        }

        softly.assertAll();
    }

}
