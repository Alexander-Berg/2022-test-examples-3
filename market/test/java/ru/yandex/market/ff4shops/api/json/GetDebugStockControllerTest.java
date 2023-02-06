package ru.yandex.market.ff4shops.api.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockType;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.model.ErrorSubCode;
import ru.yandex.market.ff4shops.delivery.stocks.StocksRequestStatusService;
import ru.yandex.market.ff4shops.offer.model.PartnerOffer;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FfAsserts;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.ff4shops.util.JsonTestUtil;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Тесты для {@link GetDebugStockController}
 */
@DbUnitDataSet(before = "DebugGetStockControllerTest.before.csv")
class GetDebugStockControllerTest extends AbstractJsonControllerFunctionalTest {

    @Autowired
    private StocksRequestStatusService stocksRequestStatusService;

    private ResponseEntity<String> getDebugStocks(int supplierId) {
        String referenceUrl = FF4ShopsUrlBuilder.getDebugStockUrl(randomServerPort, supplierId);
        return FunctionalTestHelper.getForEntity(referenceUrl, FunctionalTestHelper.jsonHeaders());
    }

    private ResponseEntity<String> getStocksDebugStatus(int supplierId) {
        String referenceUrl = FF4ShopsUrlBuilder.getStocksDebugStatusUrl(randomServerPort, supplierId);
        return FunctionalTestHelper.getForEntity(referenceUrl, FunctionalTestHelper.jsonHeaders());
    }

    @Test
    void testGetDebugStocks() {
        int supplierId = 1;
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-3",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                )
        );
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);

        assertDebugStatus(getStocksDebugStatus(supplierId), "NO_REQUESTS");
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of((long) supplierId));
        mockGetPartner(deliveryServiceId, false);
        ResponseEntity<String> response = getDebugStocks(1);
        assertResult(3, 3, 2, response);
        assertDebugStatus(getStocksDebugStatus(supplierId), "SUCCESS");
    }

    @Test
    void testGetDebugStocksWhenLmsSyncEnabled() {
        int supplierId = 1;
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-3",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                )
        );
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);

        assertDebugStatus(getStocksDebugStatus(supplierId), "NO_REQUESTS");
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of((long) supplierId));
        mockGetPartner(deliveryServiceId, true);
        ResponseEntity<String> response = getDebugStocks(1);
        assertResult(3, 3, 2, response);
        assertDebugStatus(getStocksDebugStatus(supplierId), "NO_REQUESTS");
    }

    @Test
    void testGetDebugStocksWhenLmsNotFoundError() {
        int supplierId = 1;
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-3",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                )
        );
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);

        assertDebugStatus(getStocksDebugStatus(supplierId), "NO_REQUESTS");

        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of((long) supplierId));
        mockHttpStatusFromLms(deliveryServiceId, HttpStatus.NOT_FOUND);
        ResponseEntity<String> response = getDebugStocks(1);
        assertResult(3, 3, 2, response);
        assertDebugStatus(getStocksDebugStatus(supplierId), "SUCCESS");
    }

    @Test
    void testGetDebugZeroStocks() {
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(0, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-3",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(0, StockType.AVAILABLE))
                )
        );
        int supplierId = 1;
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);
        mockGetPartner(deliveryServiceId, false);

        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(3, 3, 1, response);
    }

    @Test
    void getDebugStocksActualSizeLower() {
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                )
        );
        int supplierId = 1;
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);
        mockGetPartner(deliveryServiceId, false);
        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(3, 2, 2, response);
    }

    @Test
    void testGetDebugStocksNoStocks() {
        int supplierId = 1;
        int deliveryServiceId = 10;
        List<TestStock> mockStocks = Collections.emptyList();
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);

        assertDebugStatus(getStocksDebugStatus(1), "NO_REQUESTS");
        mockGetPartner(deliveryServiceId, false);
        stocksRequestStatusService.updateTimeOfRequestStocks(Set.of((long) supplierId));
        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(3, 0, 0, response);
        assertDebugStatus(getStocksDebugStatus(supplierId), "NO_STOCKS");
    }

    @Test
    void getDebugStocksActualSizeBigger() {
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        10,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        10,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-3",
                        10,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                ),
                TestStock.of(
                        "sku1-4",
                        10,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                )
        );
        mockPushApi(1, 10, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);
        ResponseEntity<String> response = getDebugStocks(1);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.DIFFERENT_SKUS);
    }

    @Test
    void getDebugStocksDifferentSkus() {
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku1-1",
                        10,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku1-2",
                        10,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku1-100500",
                        10,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                )
        );
        mockPushApi(1, 10, Arrays.asList("sku1-1", "sku1-2", "sku1-3"), mockStocks);
        ResponseEntity<String> response = getDebugStocks(1);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.DIFFERENT_SKUS);
    }

    @Test
    void getDebugStocksDoNotReturnFulfillmentServices() {
        int deliveryServiceId = 91;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of("sku9-1", deliveryServiceId,
                        Arrays.asList(
                                TestItem.of(5, StockType.FIT),
                                TestItem.of(4, StockType.AVAILABLE)
                        )
                ),
                TestStock.of("sku9-2", deliveryServiceId, Collections.singletonList(TestItem.of(5, StockType.FIT)))
        );
        int supplierId = 9;
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku9-1", "sku9-2"), mockStocks);
        mockGetPartner(deliveryServiceId, false);
        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(2, 2, 2, response);
    }

    @Test
    void getDebugStocksDoNotReturnFulfillmentServices2() {
        int deliveryServiceId = 100;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of("sku10-1", deliveryServiceId, Collections.singletonList(TestItem.of(1, StockType.FIT))),
                TestStock.of("sku10-2", deliveryServiceId, Collections.singletonList(TestItem.of(2, StockType.FIT)))
        );
        int supplierId = 10;
        mockPushApi(supplierId, deliveryServiceId, Arrays.asList("sku10-1", "sku10-2"), mockStocks);
        mockGetPartner(deliveryServiceId, false);
        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(2, 2, 2, response);
    }

    @Test
    void testNoSkus() {
        ResponseEntity<String> response = getDebugStocks(2);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.NO_SKUS);
    }

    @Test
    void testNoServiceId() {
        ResponseEntity<String> response = getDebugStocks(3);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.NO_DELIVERY_SERVICE);
    }

    @Test
    void testPushApiErrorCodeException() {
        mockPushApi(
                6,
                60,
                Collections.singletonList("sku6-1"),
                new ErrorCodeException("UNKNOWN_ERROR", "error", HttpStatus.INTERNAL_SERVER_ERROR.value())
        );
        ResponseEntity<String> response = getDebugStocks(6);
        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorSubCode.UNKNOWN_ERROR);
    }

    @Test
    void testPushApiShopErrorException() {
        mockPushApi(
                6,
                60,
                Collections.singletonList("sku6-1"),
                new ShopErrorException(
                        ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode.CANT_PARSE_RESPONSE,
                        "error",
                        false
                )
        );
        ResponseEntity<String> response = getDebugStocks(6);
        assertErrorResponse(response, HttpStatus.OK, ErrorSubCode.CANT_PARSE_RESPONSE);
    }

    @Test
    void testAnotherPushApiException() {
        mockPushApi(6, 60, Collections.singletonList("sku6-1"), new IllegalStateException("everything is bad"));
        ResponseEntity<String> response = getDebugStocks(6);
        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorSubCode.PUSH_API_CLIENT_ERROR);
    }

    @Test
    void testNoFitOffers() {
        int deliveryServiceId = 50;
        mockGetPartner(deliveryServiceId, false);
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku5-1",
                        deliveryServiceId,
                        Collections.singletonList(
                                TestItem.of(2, StockType.AVAILABLE)
                        )
                ),
                TestStock.of(
                        "sku5-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.DEFECT))
                )
        );
        mockPushApi(5, deliveryServiceId, Arrays.asList("sku5-1", "sku5-2"), mockStocks);

        ResponseEntity<String> response = getDebugStocks(5);
        assertResult(2, 2, 0, response);
    }

    @Test
    void testOkReponseFormat() {
        int deliveryServiceId = 80;
        mockGetPartner(deliveryServiceId, false);
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku8-1",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "sku8-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                )
        );
        mockPushApi(8, deliveryServiceId, Arrays.asList("sku8-1", "sku8-2"), mockStocks);
        ResponseEntity<String> response = getDebugStocks(8);

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"any\",\n"
                + "  \"timestamp\": 1,\n"
                + "  \"result\": {\n"
                + "    \"requestedOffersCount\": 2,\n"
                + "    \"stocksOfferCount\": 2,\n"
                + "    \"fitOffersCount\": 1\n"
                + "  },\n"
                + "  \"errors\": null\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, response.getBody(), IGNORED_FIELDS);
    }

    @Test
    void testOkWhite() {
        int deliveryServiceId = 999;
        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "offer1001-1",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(1, StockType.FIT))
                ),
                TestStock.of(
                        "offer1001-2",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(3, StockType.AVAILABLE))
                ),
                TestStock.of(
                        "offer1001-3",
                        deliveryServiceId,
                        Collections.singletonList(TestItem.of(4, StockType.AVAILABLE))
                )
        );
        mockPushApi(1001, deliveryServiceId,
                Arrays.asList("offer1001-1", "offer1001-2", "offer1001-3"), mockStocks);
        ResponseEntity<String> response = getDebugStocks(1001);

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"any\",\n"
                + "  \"timestamp\": 1,\n"
                + "  \"result\": {\n"
                + "    \"requestedOffersCount\": 3,\n"
                + "    \"stocksOfferCount\": 3,\n"
                + "    \"fitOffersCount\": 1\n"
                + "  },\n"
                + "  \"errors\": null\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, response.getBody(), IGNORED_FIELDS);
    }

    @Test
    void testErrorResponse() {
        mockPushApi(6, 60, Collections.singletonList("sku6-1"), new IllegalStateException("everything is bad"));
        ResponseEntity<String> response = getDebugStocks(6);

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"any\",\n"
                + "  \"result\": null,\n"
                + "  \"timestamp\": 1,\n"
                + "  \"errors\": [\n"
                + "    {\n"
                + "      \"subCode\": \"PUSH_API_CLIENT_ERROR\",\n"
                + "      \"message\": \"PushAPI client error\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, response.getBody(), IGNORED_FIELDS);
    }


    @Test
    void testGetStocksDebugNoQuerriesStatus() {
        assertDebugStatus(getStocksDebugStatus(1), "NO_REQUESTS");
    }

    @Test
    void testGetStocksDebugSuccessStatus() {
        assertDebugStatus(getStocksDebugStatus(2), "SUCCESS");
    }

    @Test
    void testGetStocksDebugErrorOnNoSupplier() {
        ResponseEntity<String> response = getStocksDebugStatus(404);

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"any\",\n"
                + "  \"result\": null,\n"
                + "  \"timestamp\": 1,\n"
                + "  \"errors\": [\n"
                + "    {\n"
                + "      \"subCode\": \"NO_SUPPLIER\",\n"
                + "      \"message\": \"Partner not found: 404\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, response.getBody(), IGNORED_FIELDS);
    }

    @Test
    void testNotExistedDropship() {
        final int supplierId = 11;
        expectMbi(11).andRespond(
                withSuccess(//language=xml
                        "<partner-state business-id=\"11\" partner-id=\"11\" feature-type=\"112\" " +
                                "feature-status=\"NEW\" cpa-is-partner-interface=\"false\" " +
                                "push-stocks-is-enabled=\"false\">" +
                                "<fulfillment-links>" +
                                "  <fulfillment-link service-id=\"110\" feed-id=\"11101\" partner-feed-id=\"1110\"/>" +
                                "  <fulfillment-link service-id=\"111\" feed-id=\"11111\" partner-feed-id=\"1111\"/>" +
                                "</fulfillment-links>" +
                                "</partner-state>",
                        MediaType.APPLICATION_XML
                )
        );
        mockGetPartnerTypes();

        mockMappings(supplierId, Arrays.asList(
                new PartnerOffer.Builder().setShopSku("sku11-1").setPartnerId(supplierId).setArchived(false).build(),
                new PartnerOffer.Builder().setShopSku("sku11-2").setPartnerId(supplierId).setArchived(false).build(),
                new PartnerOffer.Builder().setShopSku("sku11-3").setPartnerId(supplierId).setArchived(false).build()
        ));

        List<TestStock> mockStocks = Arrays.asList(
                TestStock.of(
                        "sku11-1",
                        110,
                        Arrays.asList(
                                TestItem.of(3, StockType.FIT),
                                TestItem.of(2, StockType.DEFECT)
                        )
                ),
                TestStock.of(
                        "sku11-2",
                        110,
                        Collections.singletonList(TestItem.of(1, StockType.AVAILABLE))
                ),
                TestStock.of(
                        "sku11-3",
                        110,
                        Collections.singletonList(TestItem.of(3, StockType.AVAILABLE))
                )
        );
        mockPushApi(supplierId, 110, Arrays.asList("sku11-1", "sku11-2", "sku11-3"), mockStocks);
        mockGetPartner(110, false);
        ResponseEntity<String> response = getDebugStocks(supplierId);
        assertResult(3, 3, 1, response);
    }

    @Test
    void testSupplierNotFound() {
        expectMbi(11).andRespond(
                withStatus(HttpStatus.NOT_FOUND)
        );

        ResponseEntity<String> response = getDebugStocks(11);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.NO_SUPPLIER);
    }

    @Test
    void testNoFulfillmentsFound() {
        expectMbi(11).andRespond(
                withSuccess(//language=xml
                        "<partner-state business-id=\"11\" partner-id=\"11\" feature-type=\"112\" " +
                                "feature-status=\"NEW\" cpa-is-partner-interface=\"false\" " +
                                "push-stocks-is-enabled=\"false\">" +
                                "<fulfillment-links/>" +
                                "</partner-state>",
                        MediaType.APPLICATION_XML
                )
        );

        ResponseEntity<String> response = getDebugStocks(11);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.NO_FULFILLMENTS);
    }

    @Test
    void testNoActiveMappings() {
        expectMbi(11).andRespond(
                withSuccess(//language=xml
                        "<partner-state business-id=\"11\" partner-id=\"11\" feature-type=\"112\" " +
                                "feature-status=\"NEW\" cpa-is-partner-interface=\"false\" " +
                                "push-stocks-is-enabled=\"false\">" +
                                "<fulfillment-links>" +
                                "  <fulfillment-link service-id=\"110\" feed-id=\"11101\" partner-feed-id=\"1110\"/>" +
                                "  <fulfillment-link service-id=\"111\" feed-id=\"11111\" partner-feed-id=\"1111\"/>" +
                                "</fulfillment-links>" +
                                "</partner-state>",
                        MediaType.APPLICATION_XML
                )
        );
        ResponseEntity<String> response = getDebugStocks(11);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, ErrorSubCode.NO_SKUS);
    }

    private void assertDebugStatus(ResponseEntity<String> response, String expectedDebugStatus) {
        //language=json
        String expected = "" +
                "{" +
                "  \"application\": \"ff4shops\"," +
                "  \"host\": \"any\"," +
                "  \"timestamp\": 1," +
                "  \"result\": \"" + expectedDebugStatus + "\"," +
                "  \"errors\": null" +
                "}";

        FfAsserts.assertJsonEquals(expected, response.getBody(), IGNORED_FIELDS);
    }

    private void assertResult(
            int requestedOffersCount,
            int stockOfferCount,
            int fitOffersCount,
            ResponseEntity<String> response
    ) {
        JsonObject result = JsonTestUtil.parseJson(response.getBody()).getAsJsonObject().getAsJsonObject("result");
        Assertions.assertEquals(requestedOffersCount, result.getAsJsonPrimitive("requestedOffersCount").getAsInt());
        Assertions.assertEquals(stockOfferCount, result.getAsJsonPrimitive("stocksOfferCount").getAsInt());
        Assertions.assertEquals(fitOffersCount, result.getAsJsonPrimitive("fitOffersCount").getAsInt());
    }
}
