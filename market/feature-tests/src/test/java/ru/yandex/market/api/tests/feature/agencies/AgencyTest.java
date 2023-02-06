package ru.yandex.market.api.tests.feature.agencies;

import com.google.common.base.Charsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import ru.yandex.market.api.listener.Listener;
import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.ResourceHelpers;
import ru.yandex.market.api.tests.feature.clients.BlackboxTestClient;
import ru.yandex.market.api.tests.feature.clients.VendorApiTestClient;
import ru.yandex.market.api.tests.feature.rules.ListenerRule;
import ru.yandex.market.api.util.Urls;

import static ru.yandex.market.api.tests.feature.util.Parameters.TEST_LISTENER_ENABLE_OVERWRITE_URI_PARAM;
import static ru.yandex.market.api.tests.feature.util.TestConstants.*;

/**
 * @author dimkarp93
 */
public class AgencyTest extends BaseTest {
    private static final String VENDOR_SECRET = "sulim-test-vendor-1-secret";
    private static final long VENDOR_API_UID = 67282295L;

    @Rule
    public ListenerRule rule = new ListenerRule(httpExpectations, PORT);

    //добавил в ignore, пока не научися поднимать автоматом, пока такие тесты можно гонять локально
    @Ignore
    @Test
    public void requestByIdTest() throws Exception {
        long agencyId = 2L;
        long requestId = 110350L;


        URIBuilder uriBuilder = Urls
            .builder(
            API_DEVLOCAL + "v2/agencies/models/requests/" + requestId
        )
            .addParameter("secret", VENDOR_SECRET)
            .addParameter("oauth_token", TEST_OAUTH)
            .addParameter("x_agency_id", String.valueOf(agencyId))
            .addParameter(TEST_LISTENER_ENABLE_OVERWRITE_URI_PARAM, "1");

        BlackboxTestClient blackboxClient = new BlackboxTestClient(httpExpectations);
        blackboxClient.userByOAuth(
            TEST_OAUTH,
            "blackbox_api_request.xml"
        );

        VendorApiTestClient vendorApiClient = new VendorApiTestClient(httpExpectations);

        vendorApiClient.agencyComments(
            agencyId,
            requestId,
            VENDOR_API_UID,
            "agency_comments.json"
        );

        vendorApiClient.agencyRequest(
            agencyId,
            requestId,
            VENDOR_API_UID,
            "vendor_api_request.json"
        );

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            CloseableHttpResponse resp = client.execute(new HttpGet(Urls.toUri(uriBuilder)));

            String expected = new String(
                ResourceHelpers.getResource("content_api_request.json"),
                Charsets.UTF_8
            );
            String actual = EntityUtils.toString(
                resp.getEntity(),
                Charsets.UTF_8
            );

            JSONAssert.assertEquals(
                new JSONObject(expected),
                new JSONObject(actual).getJSONObject("request"),
                JSONCompareMode.NON_EXTENSIBLE
            );
        }
    }
}
