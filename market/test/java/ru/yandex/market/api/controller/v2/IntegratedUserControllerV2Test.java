package ru.yandex.market.api.controller.v2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.sec.AuthorizationType;
import ru.yandex.market.api.server.sec.oauth.OAuthSecurityConfig;
import ru.yandex.market.api.util.httpclient.clients.BlackBoxTestClient;
import ru.yandex.market.api.util.httpclient.clients.HistoryTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * @author dimkarp93
 */
@ActiveProfiles(IntegratedUserControllerV2Test.PROFILE)
public class IntegratedUserControllerV2Test extends BaseTest {
    static final String PROFILE = "IntegratedUserControllerV2Test";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Primary
        @Bean
        public OAuthSecurityConfig localConfig() {
            Map<String, Collection<AuthorizationType>> types = Maps.newHashMap();
            types.put("103", Collections.singleton(AuthorizationType.OAuth));
            return new OAuthSecurityConfig(types);
        }
    }

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private HistoryTestClient historyTestClient;

    @Inject
    private BlackBoxTestClient blackBoxTestClient;

    @Test
    public void historyAdditionalOffersTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");
        headers.add("X-User-Authorization", "OAuth: 1o");

        blackBoxTestClient.postUserByOAuth("1o", "blackbox_oauth-with-additional-offers.xml");

        historyTestClient.getHistory(1L, PageInfo.DEFAULT, "pers_history-with-additional-offers.json");

        reportTestClient.skus(
                Collections.singletonList("100131944741"),
                "sku_offers-with-additional-offers.json"
        );


        String url = baseUrl + "/v2/user/history/blue?geo_id=213";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();

        JSONObject result = new JSONObject(body).
                getJSONArray("promoOffers").
                getJSONObject(0);

        JSONObject expected = new JSONObject();
        expected.put("id",
                "yDpJekrrgZEt4E6gSZh0qCu0MY3RVigcQUZuugiZP0b2_rbJ1CS15s6TRy9_i-vm_FmvI_JUOwt1nAys6QIovhOXsG7fHGLWW" +
                        "-TMQXcn0Cg"
        );
        expected.put("wareMd5", "Kj1tbMS153I4wfwbts3WgQ");
        expected.put("sku", "15");
        JSONAssert.assertEquals(expected, result, false);
    }
}
