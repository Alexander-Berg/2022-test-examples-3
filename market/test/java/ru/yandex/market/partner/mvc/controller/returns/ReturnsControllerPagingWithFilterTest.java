package ru.yandex.market.partner.mvc.controller.returns;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

class ReturnsControllerPagingWithFilterTest extends FunctionalTest {

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "ReturnsControllerPagingWithFilterTest.before.csv")
    void testPaging() {
        when(clock.instant()).thenReturn(Instant.parse("2020-04-01T12:00:00.000Z"));

        ResponseEntity<String> response1p = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns?limit=2");
        //language=json
        String expectedFirstPage = "{\n" +
                "    \"returns\": [\n" +
                "        {\n" +
                "            \"orderId\": 5555,\n" +
                "            \"returnId\": 695055,\n" +
                "            \"createdAt\": \"2021-02-27T22:08:53.71163+03:00\",\n" +
                "            \"updatedAt\": \"2021-04-10T20:34:23.037181+03:00\",\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"returnStatus\": \"STARTED_BY_USER\",\n" +
                "            \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695755.pdf\",\n" +
                "            \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "            \"fastReturn\": true,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 72450555,\n" +
                "                    \"shopSku\": \"sku21\",\n" +
                "                    \"marketSku\": 21,\n" +
                "                    \"title\": \"sku21\",\n" +
                "                    \"count\": 1,\n" +
                "                    \"resupplyCount\": 0,\n" +
                "                    \"resupplyGoodCount\": 0,\n" +
                "                    \"resupplyDefectCount\": 0,\n" +
                "                    \"photoUrls\": [],\n" +
                "                    \"moneyAmount\": 50,\n" +
                "                    \"returnReason\": \"order5555\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "                    \"supplierCompensation\": 0.0,\n" +
                "                    \"attributes\": {}\n" +
                "                }\n" +
                "            ],\n" +
                "            \"trackCode\": \"pp6ppp5\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"orderId\": 4444,\n" +
                "            \"returnId\": 695744,\n" +
                "            \"createdAt\": \"2021-02-26T22:08:53.71163+03:00\",\n" +
                "            \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"returnStatus\": \"COMPLETED\",\n" +
                "            \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695744.pdf\",\n" +
                "            \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "            \"fastReturn\": true,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 72450444,\n" +
                "                    \"shopSku\": \"sku21\",\n" +
                "                    \"marketSku\": 21,\n" +
                "                    \"title\": \"sku21\",\n" +
                "                    \"count\": 1,\n" +
                "                    \"resupplyCount\": 0,\n" +
                "                    \"resupplyGoodCount\": 0,\n" +
                "                    \"resupplyDefectCount\": 0,\n" +
                "                    \"photoUrls\": [],\n" +
                "                    \"moneyAmount\": 50,\n" +
                "                    \"returnReason\": \"order4444\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "                    \"supplierCompensation\": 0.0,\n" +
                "                    \"attributes\": {}\n" +
                "                }\n" +
                "            ],\n" +
                "            \"trackCode\": \"66mkmm\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"total\": 4,\n" +
                "    \"paging\": {\n" +
                "        \"prevPageToken\": " +
                "\"eyJvcCI6IjwiLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTEwVDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTA1NX0sInNraXAiOjB9\",\n" +
                "        \"nextPageToken\": " +
                "\"eyJvcCI6Ij4iLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTc0NH0sInNraXAiOjB9\"\n" +
                "    }\n" +
                "}";

        JsonTestUtil.assertEquals(response1p, expectedFirstPage);

        ResponseEntity<String> response2p = FunctionalTestHelper.get(
                baseUrl + "campaigns/2/returns?page_token" +
                        "=eyJvcCI6Ij4iLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTc0NH0sInNraXAiOjB9&limit=2");
        //language=json
        String expectedSecondPage = "{\n" +
                "    \"returns\": [\n" +
                "        {\n" +
                "            \"orderId\": 33,\n" +
                "            \"returnId\": 695700,\n" +
                "            \"createdAt\": \"2021-02-25T22:08:53.71163+03:00\",\n" +
                "            \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"returnStatus\": \"COMPLETED\",\n" +
                "            \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695700.pdf\",\n" +
                "            \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "            \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "            \"fastReturn\": true,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 72450004,\n" +
                "                    \"shopSku\": \"sku21\",\n" +
                "                    \"marketSku\": 21,\n" +
                "                    \"title\": \"sku21\",\n" +
                "                    \"count\": 1,\n" +
                "                    \"resupplyCount\": 0,\n" +
                "                    \"resupplyGoodCount\": 0,\n" +
                "                    \"resupplyDefectCount\": 0,\n" +
                "                    \"photoUrls\": [\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f14b14cf6344f8eba423f661/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/200370/2a00000177d9f0be1e7cbb5cf0ce92fa7403/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f0ed775315fd02d236101f73/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f02b8821e14022acbe875dff/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/474703/2a00000177d9f0a15a9b21259b8e6a01bf1f/orig\"\n" +
                "                    ],\n" +
                "                    \"moneyAmount\": 50,\n" +
                "                    \"returnReason\": \"Ошиблась и заказала неправильное средство, которое дублирует" +
                " мусс Кора из заказа. Поэтому не открывала и хочу оформить возврат.\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "                    \"supplierCompensation\": 0.0,\n" +
                "                    \"attributes\": {}\n" +
                "                }\n" +
                "            ],\n" +
                "            \"trackCode\": \"123m1k23m\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"orderId\": 33,\n" +
                "            \"returnId\": 695802,\n" +
                "            \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "            \"updatedAt\": \"2019-05-28T17:14:23.939+03:00\",\n" +
                "            \"moneyAmount\": 500,\n" +
                "            \"supplierCompensation\": 77700.0,\n" +
                "            \"returnStatus\": \"PARTIALLY_RETURNED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "            \"applicationUrl\": \"http://application.url\",\n" +
                "            \"resupplyStatus\": \"RESUPPLIED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "            \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "            \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "            \"fastReturn\": false,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 9568212,\n" +
                "                    \"shopSku\": \"sku22\",\n" +
                "                    \"marketSku\": 22,\n" +
                "                    \"title\": \"sku22\",\n" +
                "                    \"count\": 10,\n" +
                "                    \"resupplyCount\": 4,\n" +
                "                    \"resupplyGoodCount\": 1,\n" +
                "                    \"resupplyDefectCount\": 3,\n" +
                "                    \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "                    \"photoUrls\": [\n" +
                "                        \"single url\"\n" +
                "                    ],\n" +
                "                    \"moneyAmount\": 500,\n" +
                "                    \"returnReason\": \"нет возможности быстро печатать\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"NOT_WORKING\",\n" +
                "                    \"supplierCompensation\": 77700.0,\n" +
                "                    \"attributes\": {\n" +
                "                        \"WAS_USED\": 2,\n" +
                "                        \"MISSING_PARTS\": 1,\n" +
                "                        \"WRONG_OR_DAMAGED_PAPERS\": 1\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "            \"complainTimeLeft\": \"PT-22H\",\n" +
                "            \"trackCode\": \"345jjj35k\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"total\": 4,\n" +
                "    \"paging\": {\n" +
                "        \"prevPageToken\": " +
                "\"eyJvcCI6IjwiLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTcwMH0sInNraXAiOjB9\",\n" +
                "        \"nextPageToken\": " +
                "\"eyJvcCI6Ij4iLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDE5LTA1LTI4VDE3OjE0OjIzLjkzOSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTgwMn0sInNraXAiOjB9\"\n" +
                "    }\n" +
                "}";

        JsonTestUtil.assertEquals(response2p, expectedSecondPage);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerPagingWithFilterTest.before.csv")
    void testCreatedDateTimeFilter() {
        URI uri = URI.create(baseUrl + "/campaigns/2/returns?" +
                "created_since=2021-02-25T22:00:00.123456%2B03:00" +
                "&created_until=2021-02-26T22:10:00.123456%2B03:00");

        ResponseEntity<String> response = FunctionalTestHelper.get(uri, String.class);

        //language=json
        String expected = "{\n" +
                "    \"returns\": [\n" +
                "        {\n" +
                "            \"orderId\": 4444,\n" +
                "            \"returnId\": 695744,\n" +
                "            \"createdAt\": \"2021-02-26T22:08:53.71163+03:00\",\n" +
                "            \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"returnStatus\": \"COMPLETED\",\n" +
                "            \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695744.pdf\",\n" +
                "            \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "            \"fastReturn\": true,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 72450444,\n" +
                "                    \"shopSku\": \"sku21\",\n" +
                "                    \"marketSku\": 21,\n" +
                "                    \"title\": \"sku21\",\n" +
                "                    \"count\": 1,\n" +
                "                    \"resupplyCount\": 0,\n" +
                "                    \"resupplyGoodCount\": 0,\n" +
                "                    \"resupplyDefectCount\": 0,\n" +
                "                    \"photoUrls\": [],\n" +
                "                    \"moneyAmount\": 50,\n" +
                "                    \"returnReason\": \"order4444\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "                    \"supplierCompensation\": 0.0,\n" +
                "                    \"attributes\": {}\n" +
                "                }\n" +
                "            ],\n" +
                "            \"trackCode\": \"66mkmm\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"orderId\": 33,\n" +
                "            \"returnId\": 695700,\n" +
                "            \"createdAt\": \"2021-02-25T22:08:53.71163+03:00\",\n" +
                "            \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"returnStatus\": \"COMPLETED\",\n" +
                "            \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695700.pdf\",\n" +
                "            \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "            \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "            \"fastReturn\": true,\n" +
                "            \"returnItems\": [\n" +
                "                {\n" +
                "                    \"orderItemId\": 72450004,\n" +
                "                    \"shopSku\": \"sku21\",\n" +
                "                    \"marketSku\": 21,\n" +
                "                    \"title\": \"sku21\",\n" +
                "                    \"count\": 1,\n" +
                "                    \"resupplyCount\": 0,\n" +
                "                    \"resupplyGoodCount\": 0,\n" +
                "                    \"resupplyDefectCount\": 0,\n" +
                "                    \"photoUrls\": [\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f14b14cf6344f8eba423f661/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/200370/2a00000177d9f0be1e7cbb5cf0ce92fa7403/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f0ed775315fd02d236101f73/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f02b8821e14022acbe875dff/orig\",\n" +
                "                        \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/474703/2a00000177d9f0a15a9b21259b8e6a01bf1f/orig\"\n" +
                "                    ],\n" +
                "                    \"moneyAmount\": 50,\n" +
                "                    \"returnReason\": \"Ошиблась и заказала неправильное средство, которое дублирует" +
                " мусс Кора из заказа. Поэтому не открывала и хочу оформить возврат.\",\n" +
                "                    \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "                    \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "                    \"supplierCompensation\": 0.0,\n" +
                "                    \"attributes\": {}\n" +
                "                }\n" +
                "            ],\n" +
                "            \"trackCode\": \"123m1k23m\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"total\": 2,\n" +
                "    \"paging\": {\n" +
                "        \"prevPageToken\": " +
                "\"eyJvcCI6IjwiLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTc0NH0sInNraXAiOjB9\",\n" +
                "        \"nextPageToken\": " +
                "\"eyJvcCI6Ij4iLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTcwMH0sInNraXAiOjB9\"\n" +
                "    }\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerPagingWithFilterTest.before.csv")
    void testOrdersFilter() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/2/returns?order_id=4444&order_id=5555");

        //language=json
        String expected = "{\n" +
                "  \"returns\": [\n" +
                "    {\n" +
                "      \"orderId\": 5555,\n" +
                "      \"returnId\": 695055,\n" +
                "      \"createdAt\": \"2021-02-27T22:08:53.71163+03:00\",\n" +
                "      \"updatedAt\": \"2021-04-10T20:34:23.037181+03:00\",\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"supplierCompensation\": 0,\n" +
                "      \"returnStatus\": \"STARTED_BY_USER\",\n" +
                "      \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695755.pdf\",\n" +
                "      \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "      \"fastReturn\": true,\n" +
                "      \"returnItems\": [\n" +
                "        {\n" +
                "          \"orderItemId\": 72450555,\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"resupplyCount\": 0,\n" +
                "          \"resupplyGoodCount\": 0,\n" +
                "          \"resupplyDefectCount\": 0,\n" +
                "          \"photoUrls\": [],\n" +
                "          \"moneyAmount\": 50,\n" +
                "          \"returnReason\": \"order5555\",\n" +
                "          \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "          \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "          \"supplierCompensation\": 0,\n" +
                "          \"attributes\": {}\n" +
                "        }\n" +
                "      ],\n" +
                "      \"trackCode\": \"pp6ppp5\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"orderId\": 4444,\n" +
                "      \"returnId\": 695744,\n" +
                "      \"createdAt\": \"2021-02-26T22:08:53.71163+03:00\",\n" +
                "      \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "      \"moneyAmount\": 50,\n" +
                "      \"supplierCompensation\": 0,\n" +
                "      \"returnStatus\": \"COMPLETED\",\n" +
                "      \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex" +
                ".net/return-application-695744.pdf\",\n" +
                "      \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "      \"fastReturn\": true,\n" +
                "      \"returnItems\": [\n" +
                "        {\n" +
                "          \"orderItemId\": 72450444,\n" +
                "          \"shopSku\": \"sku21\",\n" +
                "          \"marketSku\": 21,\n" +
                "          \"title\": \"sku21\",\n" +
                "          \"count\": 1,\n" +
                "          \"resupplyCount\": 0,\n" +
                "          \"resupplyGoodCount\": 0,\n" +
                "          \"resupplyDefectCount\": 0,\n" +
                "          \"photoUrls\": [],\n" +
                "          \"moneyAmount\": 50,\n" +
                "          \"returnReason\": \"order4444\",\n" +
                "          \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "          \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "          \"supplierCompensation\": 0,\n" +
                "          \"attributes\": {}\n" +
                "        }\n" +
                "      ],\n" +
                "      \"trackCode\": \"66mkmm\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"total\": 2,\n" +
                "  \"paging\": {\n" +
                "    \"prevPageToken\": " +
                "\"eyJvcCI6IjwiLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTEwVDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTA1NX0sInNraXAiOjB9\",\n" +
                "    \"nextPageToken\": " +
                "\"eyJvcCI6Ij4iLCJrZXkiOnsic3RhcnRSZXR1cm5VcGRhdGVkQXQiOiIyMDIxLTA0LTA5VDIwOjM0OjIzLjAzNzE4MSswMzowMCIsInN0YXJ0UmV0dXJuSWQiOjY5NTc0NH0sInNraXAiOjB9\"\n" +
                "  }\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerPagingWithFilterTest.before.csv")
    void testEmptyListNoReturnsWithThisStatusFilter() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "campaigns/2/returns?statuses=READY_FOR_PICKUP_COMPLAIN_PERIOD_EXPIRED");

        //language=json
        String expected = "{\n" +
                "  \"returns\": [],\n" +
                "  \"total\": 0,\n" +
                "  \"paging\": {}\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

}
