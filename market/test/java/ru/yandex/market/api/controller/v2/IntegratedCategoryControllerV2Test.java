package ru.yandex.market.api.controller.v2;

import javax.inject.Inject;

import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.json.Jsoner;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * @author dimkarp93
 */
public class IntegratedCategoryControllerV2Test extends BaseTest {
    @Inject
    private ReportTestClient reportTestClient;

    @Test
    public void categoryPopularProductsWithPromoOffers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");

        int categoryId = 91491;

        reportTestClient.popularProducts("category_controller_popular_products.json");

        String url = baseUrl +
                String.format("/v2/categories/%d/populars/products", categoryId)
                + "?geo_id=213";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();
        JSONObject promoOffersResult = new JSONObject(body).
                getJSONArray("promoOffers").
                getJSONObject(0);

        JSONObject promoOffersExpected = new JSONObject();
        promoOffersExpected.put("id",
                "yDpJekrrgZEt4E6gSZh0qCu0MY3RVigcQUZuugiZP0b2_rbJ1CS15s6TRy9_i-vm_FmvI_JUOwt1nAys6QIovhOXsG7fHGLWW-TMQXcn0Cg"
        );
        promoOffersExpected.put("wareMd5", "Kj1tbMS153I4wfwbts3WgQ");
        promoOffersExpected.put("sku", "15");

        JSONAssert.assertEquals(promoOffersExpected, promoOffersResult, false);

        JSONObject optionsResult = new JSONObject(body)
                .getJSONObject("context")
                .getJSONObject("processingOptions");
        JSONObject optionsExpected = new JSONObject()
                .put("adult", true);
        JSONAssert.assertEquals(optionsExpected, optionsResult, false);

    }
}
