package ru.yandex.market.api.controller.v2;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.clients.ShopInfoTestClient;

/**
 * @author dimkarp93
 */
public class IntegratedModelControllerV2Test extends BaseTest {
    @Inject
    private ReportTestClient reportTestClient;
    @Inject
    private PersStaticTestClient persStaticTestClient;
    @Inject
    private ShopInfoTestClient shopInfoTestClient;

    @Test
    public void modelsAccessroiesWithPromoOffers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");

        long modelId = 678L;

        reportTestClient.getProductAccessories(
                modelId,
                "product_accessroies-with-additional-offers.json"
        );

        reportTestClient.getModelInfoById(modelId, "modelinfo_678.json");

        String url = baseUrl +
                String.format("/v2/models/%d/accessories", modelId)
                + "?geo_id=213";

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
                "yDpJekrrgZEt4E6gSZh0qCu0MY3RVigcQUZuugiZP0b2_rbJ1CS15s6TRy9_i-vm_FmvI_JUOwt1nAys6QIovhOXsG7fHGLWW-TMQXcn0Cg"
        );
        expected.put("wareMd5", "Kj1tbMS153I4wfwbts3WgQ");
        expected.put("sku", "15");
        JSONAssert.assertEquals(expected, result, false);

    }

    @Test
    public void modelsOfferBatchCount10() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&count=10";

        reportTestClient.getModelInfoById(ids, "modelnfo_modeloffers_batch.json");
        reportTestClient.getModelOffers(ids, "productoffers_modeloffers_batch_2x10.json");

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String offersRaw = ApiStrings.valueOf(ResourceHelpers.getResource("result-model-offers-batch.json"));

        String body = response.getBody();

        JSONArray offers = new JSONObject(body).getJSONArray("offers");
        JSONAssert.assertEquals(offersRaw, offers, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void modelsOfferBatchLastOfferFieldsAll() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&count=3&fields=ALL";

        Collection<Long> shopIds = Arrays.asList(10278030L, 17677L, 10275457L, 10278039L);

        reportTestClient.getModelInfoById(ids, "modelnfo_modeloffers_batch.json");
        reportTestClient.getModelOffers(ids, "productoffers_modeloffers_batch_2x3.json");
        reportTestClient.getShopsRatings(shopIds, "shopinfo_modeloffers_batch.json");
        persStaticTestClient.getShopRatingDistribution(shopIds, "pers_static_modeloffers_batch.json");
        shopInfoTestClient.supplier(10278030L, "shopinfo_partner-id_10278030.json");
        shopInfoTestClient.supplier(17677L, "shopinfo_partner-id_10278030.json");
        shopInfoTestClient.supplier(10275457L, "shopinfo_partner-id_10278030.json");
        shopInfoTestClient.supplier(10278039L, "shopinfo_partner-id_10278030.json");

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String offersRaw = ApiStrings.valueOf(ResourceHelpers.getResource("result-last-model-offers-batch.json"));

        String body = response.getBody();

        JSONArray offers = new JSONObject(body).getJSONArray("offers");
        JSONAssert.assertEquals(offersRaw, offers, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test(expected = HttpClientErrorException.class)
    public void modelsOfferBatchCount20FieldsFilterIncorrect() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&page=1&count=20&fields=FILTERS";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void modelsOfferBatchCount20FieldsSortsIncorrect() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&page=1&count=20&fields=SORTS";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void modelsOfferBatchCount20FieldsStandardIncorrect() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&page=1&count=20&fields=STANDARD";

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void modelsOfferBatchLastOfferListMore20ElementsIncorrect() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = LongStream.range(0, 21).boxed().collect(Collectors.toList());

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&page=20&count=1&fields=ALL";
        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

    }

    @Test
    public void modelsOfferBatchEmptyOffers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        Collection<Long> ids = Arrays.asList(14206636L, 12299057L);

        String url = baseUrl +
                "/v2.1.6/models/offers?ids=" + ApiStrings.COMMA_JOINER.join(ids)
                + "&geo_id=213&page=1&count=20";

        reportTestClient.getModelInfoById(ids, "modelnfo_modeloffers_batch.json");
        reportTestClient.getModelOffers(ids, "productoffers_modeloffers_batch_NPE.json");

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String offersRaw = ApiStrings.valueOf(ResourceHelpers.getResource("result-model-offers-batch-NPE.json"));

        String body = response.getBody();

        JSONArray offers = new JSONObject(body).getJSONArray("offers");
        JSONAssert.assertEquals(offersRaw, offers, JSONCompareMode.NON_EXTENSIBLE);
    }
}
