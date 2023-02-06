package ru.yandex.market.partner.mvc.controller.order;

import java.util.List;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static ru.yandex.devtools.test.Shared.GSON;

/**
 * Тесты для {@link OrdersControllerV2#getOrders}
 */
class OrdersControllerV2PagingWithFilterTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2PagingWithFilterTest.before.csv")
    void testPaging() {
        ResponseEntity<String> response1 = FunctionalTestHelper.get(baseUrl + "v2/campaigns/2/orders?limit=2");
        //language=json
        String expected1 = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 33,\n" +
                "      \"returnIds\": [18802,18803],\n" +
                "      \"returnedSkuCount\":2," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 2,\n" +
                "          \"initialCount\": 2\n" +
                "        },\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\":1\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 150,\n" +
                "      \"initialMoneyAmount\": 150,\n" +
                "      \"createdAt\": \"2020-03-03T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"CANCELLED_IN_PROCESSING\",\n" +
                "      \"orderSubstatus\": \"DELIVERY_PROBLEMS\"\n," +
                "      \"additionalInfo\": {\"buyerType\":\"BUSINESS\"}\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 3,\n" +
                "      \"returnIds\": [],\n" +
                "      \"returnedSkuCount\":0," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 4\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"initialMoneyAmount\": 200,\n" +
                "      \"createdAt\": \"2020-03-02T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"DELIVERY\",\n" +
                "      \"orderSubstatus\":\"SHOP_FAILED\"\n," +
                "      \"additionalInfo\": {\n" +
                "                               \"buyerType\":\"PERSON\"\n," +
                "                               \"statusExpiryDate\": \"2020-03-03T16:30:00\"\n" +
                "                           }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 2,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjMzLCJza2lwIjowfQ\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjMsInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";

        ResponseEntity<String> response2 = FunctionalTestHelper
                .get(baseUrl + "v2/campaigns/2/orders?page_token=eyJvcCI6Ij4iLCJrZXkiOjMsInNraXAiOjB9&limit=2");
        //language=json
        String expected2 = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"returnIds\": [],\n" +
                "      \"returnedSkuCount\":0," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 2\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"initialMoneyAmount\": 100,\n" +
                "      \"createdAt\": \"2020-03-01T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"PROCESSING\",\n" +
                "      \"orderSubstatus\": \"PACKAGING\"\n," +
                "      \"additionalInfo\": {\n" +
                "                               \"buyerType\":\"PERSON\"\n," +
                "                               \"statusExpiryDate\": \"2020-03-03T16:30:00\"\n" +
                "                           }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 1,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjIsInNraXAiOjB9\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjIsInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";

        ResponseEntity<String> response3 = FunctionalTestHelper.get(
                baseUrl + "v2/campaigns/2/orders?page_token=eyJvcCI6Ij4iLCJrZXkiOjIsInNraXAiOjB9&limit=2"
        );
        //language=json
        String expected3 = "{\"orders\": [], \"total\": 0, \"paging\": {}}";
        assertResponse(response1, expected1);
        assertResponse(response2, expected2);
        assertResponse(response3, expected3);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2PagingWithFilterTest.before.csv")
    void testFilterByDate() {
        ResponseEntity<String> response = FunctionalTestHelper
                .get(baseUrl + "v2/campaigns/2/orders?dateFrom=2020-03-03&dateTo=2020-03-03&limit=2");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 33,\n" +
                "      \"returnIds\": [18802,18803],\n" +
                "      \"returnedSkuCount\":2," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 2,\n" +
                "          \"initialCount\": 2\n" +
                "        },\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 1\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 150,\n" +
                "      \"initialMoneyAmount\": 150,\n" +
                "      \"createdAt\": \"2020-03-03T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"CANCELLED_IN_PROCESSING\",\n" +
                "      \"orderSubstatus\": \"DELIVERY_PROBLEMS\"\n," +
                "      \"additionalInfo\": {\"buyerType\":\"BUSINESS\"}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 1,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjMzLCJza2lwIjowfQ\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjMzLCJza2lwIjowfQ\"\n" +
                "  }\n" +
                "}";
        assertResponse(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2FilterByStatusTest.before.csv")
    void testFilterByStatus() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "v2/campaigns/2/orders?statuses=UNREDEEMED&limit=2");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"returnIds\": [],\n" +
                "      \"returnedSkuCount\":0," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 2\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"initialMoneyAmount\": 100,\n" +
                "      \"createdAt\": \"2020-03-01T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"UNREDEEMED\",\n" +
                "      \"orderSubstatus\": \"PACKAGING\"\n," +
                "      \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 1,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjIsInNraXAiOjB9\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjIsInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";
        assertResponse(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2PagingWithFilterTest.before.csv")
    void testLikeOrderId() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "v2/campaigns/2/orders?q=3&limit=2");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 33,\n" +
                "      \"returnIds\": [18802,18803],\n" +
                "      \"returnedSkuCount\":2," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 2,\n" +
                "          \"initialCount\": 2\n" +
                "        },\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 1\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 150,\n" +
                "      \"initialMoneyAmount\": 150,\n" +
                "      \"createdAt\": \"2020-03-03T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"CANCELLED_IN_PROCESSING\",\n" +
                "      \"orderSubstatus\": \"DELIVERY_PROBLEMS\"\n," +
                "      \"additionalInfo\": {\"buyerType\":\"BUSINESS\"}\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 3,\n" +
                "      \"returnIds\": [],\n" +
                "      \"returnedSkuCount\":0," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 4\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"initialMoneyAmount\": 200,\n" +
                "      \"createdAt\": \"2020-03-02T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"DELIVERY\",\n" +
                "      \"orderSubstatus\":\"SHOP_FAILED\"\n," +
                "      \"additionalInfo\": {\n" +
                "                               \"buyerType\":\"PERSON\"\n," +
                "                               \"statusExpiryDate\": \"2020-03-03T16:30:00\"\n" +
                "                           }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 2,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjMzLCJza2lwIjowfQ\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjMsInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";
        assertResponse(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2PagingWithFilterTest.before.csv")
    void testLikeShopSku() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "v2/campaigns/2/orders?q=sku2&limit=2");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orders\": [\n" +
                "    {\n" +
                "      \"id\": 33,\n" +
                "      \"returnIds\": [18802,18803],\n" +
                "      \"returnedSkuCount\":2," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 2,\n" +
                "          \"initialCount\": 2\n" +
                "        },\n" +
                "        {\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 1\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 150,\n" +
                "      \"initialMoneyAmount\": 150,\n" +
                "      \"createdAt\": \"2020-03-03T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"CANCELLED_IN_PROCESSING\",\n" +
                "      \"orderSubstatus\": \"DELIVERY_PROBLEMS\",\n" +
                "      \"additionalInfo\": {\"buyerType\":\"BUSINESS\"}\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 3,\n" +
                "      \"returnIds\": [],\n" +
                "      \"returnedSkuCount\":0," +
                "      \"orderItems\": [\n" +
                "        {\n" +
                "          \"shopSku\": \"sku22\",\n" +
                "          \"marketSku\": 22,\n" +
                "          \"title\": \"sku22\",\n" +
                "          \"count\": 1,\n" +
                "          \"initialCount\": 4\n" +
                "        }\n" +
                "      ],\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"initialMoneyAmount\": 200,\n" +
                "      \"createdAt\": \"2020-03-02T00:00:00+03:00\",\n" +
                "      \"orderStatus\": \"DELIVERY\",\n" +
                "      \"orderSubstatus\":\"SHOP_FAILED\",\n" +
                "      \"additionalInfo\": {\n" +
                "                             \"buyerType\":\"PERSON\",\n" +
                "                             \"statusExpiryDate\": \"2020-03-03T16:30:00\"\n" +
                "                           }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 2,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": \"eyJvcCI6IjwiLCJrZXkiOjMzLCJza2lwIjowfQ\",\n" +
                "    \"nextPageToken\": \"eyJvcCI6Ij4iLCJrZXkiOjMsInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";
        assertResponse(response, expected);
    }

    private void assertResponse(ResponseEntity<String> response, String expected) {
        ArrayValueMatcher<Object> arrValMatcher = new ArrayValueMatcher<>(
                new CustomComparator(
                        JSONCompareMode.NON_EXTENSIBLE,
                        new Customization("orders[*].createdAt", (c1, c2) -> true)
                )
        );
        Customization arrValMatchCustomization = new Customization("orders", arrValMatcher);

        MbiAsserts.assertJsonEquals(
                expected,
                GSON.fromJson(response.getBody(), JsonObject.class).get("result").toString(),
                List.of(arrValMatchCustomization)
        );
    }
}
