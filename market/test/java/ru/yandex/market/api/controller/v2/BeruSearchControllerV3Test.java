package ru.yandex.market.api.controller.v2;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.XmlUtil;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.util.httpclient.clients.BukerTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ParametersAreNonnullByDefault
public class BeruSearchControllerV3Test extends BaseTest {

    private static final String BERU_MODELS_PATH = "/v3/affiliate/beru/search";
    private static final String BERU_OFFERS_PATH = "/v3/affiliate/beru/models/{id}/offers";
    private static final String BERU_CATEGORIES_PATH = "/v3/affiliate/beru/categories/{id}/search";
    private static final String BERU_CATEGORIES_HOT_OFFERS_PATH = "/v3/affiliate/beru/categories/{id}/hot-offers";

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private BukerTestClient bukerTestClient;

    @Inject
    private BlueRule blueRule;

    @Test
    public void testBeruSearchFieldsSetAndReturned() {
        final HttpHeaders headers = new HttpHeaders();
        mockSearchResponse(headers);

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_MODELS_PATH + "?text=iPhone&geo_id=213&fields=MODEL_CATEGORY,MODEL_DEFAULT_OFFER,MODEL_MEDIA,MODEL_OFFERS,MODEL_PHOTO," +
                        "MODEL_PHOTOS,MODEL_PRICE,MODEL_RATING,MODEL_VENDOR,OFFER_PHOTO,OFFER_DELIVERY&count=1&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_search_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruSearchFieldsSetExactMatchAndReturned() {
        final HttpHeaders headers = new HttpHeaders();
        mockSearchResponseExactMatch(headers);

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_MODELS_PATH + "?text=Смартфон Apple iPhone SE 16GB&exact-match=1&geo_id=213&fields=MODEL_CATEGORY,MODEL_DEFAULT_OFFER,MODEL_MEDIA,MODEL_OFFERS,MODEL_PHOTO," +
                        "MODEL_PHOTOS,MODEL_PRICE,MODEL_RATING,MODEL_VENDOR,OFFER_PHOTO,OFFER_DELIVERY&count=1&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_search_exact_match_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruSearchFieldsWithoutDefaultOffer() {
        final HttpHeaders headers = new HttpHeaders();
        mockSearchResponse(headers);

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_MODELS_PATH + "?text=iPhone&geo_id=213&fields=MODEL_CATEGORY,MODEL_MEDIA,MODEL_OFFERS,MODEL_PHOTO," +
                        "MODEL_PHOTOS,MODEL_PRICE,MODEL_RATING,MODEL_VENDOR,OFFER_PHOTO,OFFER_DELIVERY&count=1&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_search_without_offer_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testClidAndVidSetAndReturned() {
        final HttpHeaders headers = new HttpHeaders();
        mockSearchResponse(headers, "123456", "789");

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_MODELS_PATH + "?text=iPhone&geo_id=213&fields=MODEL_CATEGORY,MODEL_DEFAULT_OFFER,MODEL_MEDIA,MODEL_OFFERS,MODEL_PHOTO," +
                        "MODEL_PHOTOS,MODEL_PRICE,MODEL_RATING,MODEL_VENDOR,OFFER_PHOTO,OFFER_DELIVERY&count=1&clid=123456&vid=789&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_search_clid_vid_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruOffersFieldsSetAndReturned() {
        final long modelId = 175941311L;
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/beru/models/{}/offers")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "prime",
                t -> t.param("cpa", "real")
                        .param("allow-collapsing", "1")
                        .param("allow-ungrouping", "1")
                        .param("use-default-offers", "1")
                        .param("hyperid", String.valueOf(modelId))
                        .param("pp", "930")
                        .param("rids", "213")
                        .param("currency", "RUR")
                        .param("clid", "8888888")
                        .param("mclid", "1003"),
                "blue_report_offers_response.json"
        );


        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_OFFERS_PATH + "?geo_id=213&fields=OFFER_DELIVERY,OFFER_PHOTO&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                modelId
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_model_offers_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruCheckoutLinkAndSku() {
        final long modelId = 175941311L;
        final String msku = "123";
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/beru/models/{}/offers")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "prime",
                t -> t.param("cpa", "real")
                        .param("allow-collapsing", "1")
                        .param("allow-ungrouping", "1")
                        .param("use-default-offers", "1")
                        .param("market-sku", msku)
                        .param("pp", "930")
                        .param("rids", "213")
                        .param("currency", "RUR")
                        .param("clid", "8888888")
                        .param("mclid", "1003"),
                "blue_report_offers_checkout_link_response.json"
        );


        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_OFFERS_PATH + "?geo_id=213&fields=OFFER_CHECKOUT_LINK&sku=123&touch=0&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                modelId
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_model_offers_checkout_link_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruCategoriesFieldsSetAndReturned() {
        final long parentCategoryId = 91461L; // категории 91072 и 91491 - дочерние для указанной
        final HttpHeaders headers = new HttpHeaders();

        mockCategoriesResponse(headers);

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_CATEGORIES_PATH + "?geo_id=213&fields=MODEL_CATEGORY,MODEL_DEFAULT_OFFER,MODEL_MEDIA,MODEL_OFFERS,MODEL_PHOTO," +
                        "MODEL_PHOTOS,MODEL_PRICE,MODEL_RATING,MODEL_VENDOR,OFFER_PHOTO,OFFER_DELIVERY&count=2&page=6&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                parentCategoryId
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_categories_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test
    public void testBeruCategoriesHotOffersFieldsSetAndReturned() {
        final long categoryId = 91491L;
        final HttpHeaders headers = new HttpHeaders();

        mockCategoriesHotOffersResponse(headers, "blue_report_categories_hot_offers_response.json");

        ResponseEntity<String> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_CATEGORIES_HOT_OFFERS_PATH + "?geo_id=213&fields=OFFER_DELIVERY,OFFER_PHOTO&count=2&page=1&format=xml",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                categoryId
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        XmlUtil.assertEqual(
                BeruSearchControllerV3Test.class,
                "data/assert/beru_categories_hot_offers_expected.xml",
                responseEntity.getBody(),
                attr -> !"time".equals(attr.getName())
                        && !("id".equals(attr.getName()) && "context".equals(attr.getOwnerElement().getNodeName()))
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void testBeruSearchThrowsWithoutGeoId() {
        REST_TEMPLATE.exchange(
                baseUrl + BERU_MODELS_PATH + "?text=iPhone&cpa=real&format=xml",
                HttpMethod.GET,
                null,
                String.class
        );
    }

    @Test(expected = HttpClientErrorException.class)
    public void testBeruOffersThrowsWithoutGeoId() {
        final long modelId = 175941311L;
        REST_TEMPLATE.exchange(
                baseUrl + BERU_OFFERS_PATH + "?format=xml",
                HttpMethod.GET,
                null,
                String.class,
                modelId
        );
    }

    @Test
    public void testBlueRuleAppliedForBeruSearch() {
        prepareContext("V3/affiliate/beru/search");
        assertFalse(blueRule.test(context));
    }

    @Test
    public void testBlueRuleNotAppliedForNotBeruSearch() {
        prepareContext("V2/search");
        assertFalse(blueRule.test(context));
    }


    private void mockSearchResponse(final HttpHeaders headers) {
        mockSearchResponse(headers, "8888888", null);
    }

    private void mockSearchResponseExactMatch(final HttpHeaders headers) {
        mockSearchResponseExactMatch(headers, "8888888", null);
    }

    private void mockSearchResponse(final HttpHeaders headers, final String clid, final String vid) {
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/search")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "prime",
                t -> applyVid(
                        applyClid(
                                t.param("cpa", "real")
                                        .param("allow-collapsing", "1")
                                        .param("allow-ungrouping", "1")
                                        .param("use-default-offers", "1")
                                        .param("text", "iPhone")
                                        .param("rids", "213")
                                        .param("currency", "RUR")
                                        .param("pp", "929")
                                        .param("mclid", "1003"),
                                clid
                        ),
                        vid
                ),
                "blue_report_response.json"
        );

        bukerTestClient.getModelRatingCards(
                Arrays.asList(13584121L, 1759344314L, 169117444L),
                "beru_search_buker_response.xml"
        );

        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");
    }

    private void mockSearchResponseExactMatch(final HttpHeaders headers, final String clid, final String vid) {
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/search")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "prime",
                t -> applyVid(
                        applyClid(
                                t.param("cpa", "real")
                                        .param("allow-collapsing", "1")
                                        .param("allow-ungrouping", "1")
                                        .param("use-default-offers", "1")
                                        .param("text", "Смартфон Apple iPhone SE 16GB")
                                        .param("rids", "213")
                                        .param("currency", "RUR")
                                        .param("pp", "929")
                                        .param("mclid", "1003")
                                        .param("exact-match", "1"),
                                clid
                        ),
                        vid
                ),
                "blue_report_exact_match_response.json"
        );

        bukerTestClient.getModelRatingCards(
                Arrays.asList(13584121L),
                "beru_search_exact_match_buker_response.xml"
        );

        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");
    }

    private void mockCategoriesResponse(final HttpHeaders headers) {
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/beru/categories/{}/search")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "prime",
                t -> t.param("cpa", "real")
                        .param("allow-collapsing", "1")
                        .param("allow-ungrouping", "1")
                        .param("use-default-offers", "1")
                        .param("pp", "929")
                        .param("rids", "213")
                        .param("currency", "RUR")
                        .param("clid", "8888888")
                        .param("mclid", "1003")
                        .param("hid", "91461"),
                "blue_report_categories_response.json"
        );

        bukerTestClient.getModelRatingCards(
                Arrays.asList(142798000L, 71307936L),
                "beru_categories_buker_response.xml"
        );

        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");
    }

    private void mockCategoriesHotOffersResponse(HttpHeaders headers, String bodyResponseFile) {
        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName("V3/affiliate/beru/categories/{}/hot-offers")
                    .build();
            ctx.setRequest(request);
        });

        reportTestClient.search(
                "hot_offers",
                t -> t.param("cpa", "real")
                        .param("pp", "929")
                        .param("rids", "213")
                        .param("currency", "RUR")
                        .param("clid", "8888888")
                        .param("mclid", "1003")
                        .param("hid", "91491"),
                bodyResponseFile
        );

        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");
    }

    private void prepareContext(final String method) {
        final Client client = new Client();
        client.setTariff(TestTariffs.CUSTOM);
        client.setType(Client.Type.EXTERNAL);

        final HttpServletRequest request = MockRequestBuilder.start()
                .methodName(method)
                .build();

        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setRequest(request);
        });
    }

    private HttpRequestExpectationBuilder applyClid(HttpRequestExpectationBuilder b, String clid) {
        if (Strings.isNullOrEmpty(clid)) {
            return b;
        }
        return b.param("clid", clid);
    }

    private HttpRequestExpectationBuilder applyVid(HttpRequestExpectationBuilder b, String vid) {
        if (Strings.isNullOrEmpty(vid)) {
            return b;
        }
        return b.param("vid", vid);
    }
}
