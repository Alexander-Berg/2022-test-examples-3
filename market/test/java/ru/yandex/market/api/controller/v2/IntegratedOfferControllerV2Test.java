package ru.yandex.market.api.controller.v2;

import javax.inject.Inject;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * @author dimkarp93
 */
public class IntegratedOfferControllerV2Test extends BaseTest {
    @Inject
    private ReportTestClient reportTestClient;

    @Test
    public void cutPriceForInternal() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-4");

        String wareMd5 = "AwxLe1ElAgE2hcjxn4eK6w";

        reportTestClient.getOfferInfo(
                new OfferId(wareMd5, null),
                "offerinfo_cutprice.json"
        );

        String url = baseUrl +
                String.format("/v2/offers/%s", wareMd5)
                + "?geo_id=213";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();

        JSONObject result = new JSONObject(body).getJSONObject("offer");

        Assert.assertEquals("AwxLe1ElAgE2hcjxn4eK6w", result.get("wareMd5"));
        Assert.assertEquals(true, result.get("isCutPrice"));

        JSONObject expected = new JSONObject();
        expected.put("type", "like-new");
        expected.put("reason", "Новый, вскрыта упаковка, снята пленка с экрана, комплект не распакованный.");
        JSONAssert.assertEquals(expected, result.getJSONObject("condition"), false);
    }

    @Test
    public void cutPriceForExternal() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        String wareMd5 = "AwxLe1ElAgE2hcjxn4eK6w";

        reportTestClient.getOfferInfo(
                new OfferId(wareMd5, null),
                "offerinfo_cutprice.json"
        );

        String url = baseUrl +
                String.format("/v2/offers/%s", wareMd5)
                + "?geo_id=213";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();

        JSONObject result = new JSONObject(body).getJSONObject("offer");

        Assert.assertEquals("AwxLe1ElAgE2hcjxn4eK6w", result.get("wareMd5"));
        Assert.assertTrue(result.has("isCutPrice"));
        Assert.assertFalse(result.has("condition"));
    }

}
