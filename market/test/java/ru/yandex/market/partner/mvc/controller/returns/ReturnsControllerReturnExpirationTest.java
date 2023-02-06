package ru.yandex.market.partner.mvc.controller.returns;

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

class ReturnsControllerReturnExpirationTest extends FunctionalTest {

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "ReturnsControllerReturnExpirationTest.before.csv")
    void fulfillmentReturnComplainPeriodNotExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2020-03-04T12:00:00.000Z"));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/18803");
        String expected = "{\n" +
                "  \"orderId\": 33,\n" +
                "  \"returnId\": 18803,\n" +
                "  \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "  \"updatedAt\": \"2020-03-03T03:14:23.939+03:00\",\n" +
                "  \"moneyAmount\": 500,\n" +
                "  \"supplierCompensation\": 77700,\n" +
                "  \"returnStatus\": \"PARTIALLY_RETURNED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "  \"applicationUrl\": \"http://application.url\",\n" +
                "  \"resupplyStatus\": \"RESUPPLIED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "  \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "  \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "  \"fastReturn\": false,\n" +
                "  \"complainTimeLeft\": \"PT650H\",\n" +
                "  \"returnItems\": [\n" +
                "    {\n" +
                "      \"orderItemId\": 9568213,\n" +
                "      \"shopSku\": \"sku22\",\n" +
                "      \"marketSku\": 22,\n" +
                "      \"title\": \"sku22\",\n" +
                "      \"count\": 10,\n" +
                "      \"resupplyCount\": 4,\n" +
                "      \"resupplyGoodCount\": 1,\n" +
                "      \"resupplyDefectCount\": 3,\n" +
                "      \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "      \"photoUrls\": [\n" +
                "        \"single url\"\n" +
                "      ],\n" +
                "      \"moneyAmount\": 500,\n" +
                "      \"returnReason\": \"нет возможности быстро печатать\",\n" +
                "      \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "      \"returnSubreason\": \"NOT_WORKING\",\n" +
                "      \"supplierCompensation\": 77700.0,\n" +
                "      \"attributes\": {\n" +
                "        \"WAS_USED\": 2,\n" +
                "        \"MISSING_PARTS\": 1,\n" +
                "        \"WRONG_OR_DAMAGED_PAPERS\": 1\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerReturnExpirationTest.before.csv")
    void fulfillmentReturnComplainPeriodExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2020-04-01T12:00:00.000Z"));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/18802");
        String expected = "{\n" +
                "  \"orderId\": 33,\n" +
                "  \"returnId\": 18802,\n" +
                "  \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "  \"updatedAt\": \"2020-03-03T03:14:23.939+03:00\",\n" +
                "  \"moneyAmount\": 500,\n" +
                "  \"supplierCompensation\": 77700,\n" +
                "  \"returnStatus\": \"COMPLAIN_PERIOD_EXPIRED\",\n" +
                "  \"applicationUrl\": \"http://application.url\",\n" +
                "  \"resupplyStatus\": \"RESUPPLIED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "  \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "  \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "  \"fastReturn\": false,\n" +
                "  \"complainTimeLeft\": \"PT-22H\",\n" +
                "  \"returnItems\": [\n" +
                "    {\n" +
                "      \"orderItemId\": 9568212,\n" +
                "      \"shopSku\": \"sku22\",\n" +
                "      \"marketSku\": 22,\n" +
                "      \"title\": \"sku22\",\n" +
                "      \"count\": 10,\n" +
                "      \"resupplyCount\": 4,\n" +
                "      \"resupplyGoodCount\": 1,\n" +
                "      \"resupplyDefectCount\": 3,\n" +
                "      \"resuppliedAt\": \"2020-03-01T17:00:00+03:00\",\n" +
                "      \"photoUrls\": [\n" +
                "        \"single url\"\n" +
                "      ],\n" +
                "      \"moneyAmount\": 500,\n" +
                "      \"returnReason\": \"нет возможности быстро печатать\",\n" +
                "      \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "      \"returnSubreason\": \"NOT_WORKING\",\n" +
                "      \"supplierCompensation\": 77700,\n" +
                "      \"attributes\": {\n" +
                "        \"WAS_USED\": 2,\n" +
                "        \"MISSING_PARTS\": 1,\n" +
                "        \"WRONG_OR_DAMAGED_PAPERS\": 1\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerReturnExpirationTest.before.csv")
    void dropshipReturnComplainPeriodNotExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2019-04-13T12:00:00.000Z"));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/3/returns/13927");
        String expected = "{\n" +
                "  \"orderId\": 6666,\n" +
                "  \"returnId\": 13927,\n" +
                "  \"createdAt\": \"2019-03-27T15:01:03.814+03:00\",\n" +
                "  \"updatedAt\": \"2019-04-12T12:29:48.303+03:00\",\n" +
                "  \"moneyAmount\": 100,\n" +
                "  \"supplierCompensation\": 111100,\n" +
                "  \"returnStatus\": \"APPROVED\",\n" +
                "  \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex.net/return-application-13926" +
                ".pdf\",\n" +
                "  \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "  \"warehouse\": \"Ваш склад\",\n" +
                "  \"fastReturn\": false,\n" +
                "  \"returnItems\": [\n" +
                "    {\n" +
                "      \"orderItemId\": 72450666,\n" +
                "      \"shopSku\": \"sku21\",\n" +
                "      \"marketSku\": 21,\n" +
                "      \"title\": \"sku21\",\n" +
                "      \"count\": 2,\n" +
                "      \"resupplyCount\": 0,\n" +
                "      \"resupplyGoodCount\": 0,\n" +
                "      \"resupplyDefectCount\": 0,\n" +
                "      \"photoUrls\": [],\n" +
                "      \"moneyAmount\": 100,\n" +
                "      \"returnReason\": \"dropship return item\",\n" +
                "      \"returnReasonType\": \"BAD_QUALITY\",\n" +
                "      \"returnSubreason\": \"UNKNOWN\",\n" +
                "      \"supplierCompensation\": 111100,\n" +
                "      \"attributes\": {}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"complainTimeLeft\": \"PT693H\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerReturnExpirationTest.before.csv")
    void dropshipReturnComplainPeriodExpired() {
        when(clock.instant()).thenReturn(Instant.parse("2019-05-13T12:00:00.000Z"));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/3/returns/13928");
        String expected = "{\n" +
                "  \"orderId\": 6667,\n" +
                "  \"returnId\": 13928,\n" +
                "  \"createdAt\": \"2019-03-27T15:01:03.814+03:00\",\n" +
                "  \"updatedAt\": \"2019-04-12T12:29:48.303+03:00\",\n" +
                "  \"moneyAmount\": 100,\n" +
                "  \"supplierCompensation\": 111100,\n" +
                "  \"returnStatus\": \"COMPLAIN_PERIOD_EXPIRED\",\n" +
                "  \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex.net/return-application-13927" +
                ".pdf\",\n" +
                "  \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "  \"warehouse\": \"Ваш склад\",\n" +
                "  \"fastReturn\": false,\n" +
                "  \"returnItems\": [\n" +
                "    {\n" +
                "      \"orderItemId\": 72450667,\n" +
                "      \"shopSku\": \"sku21\",\n" +
                "      \"marketSku\": 21,\n" +
                "      \"title\": \"sku21\",\n" +
                "      \"count\": 2,\n" +
                "      \"resupplyCount\": 0,\n" +
                "      \"resupplyGoodCount\": 0,\n" +
                "      \"resupplyDefectCount\": 0,\n" +
                "      \"photoUrls\": [],\n" +
                "      \"moneyAmount\": 100,\n" +
                "      \"returnReason\": \"dropship return item\",\n" +
                "      \"returnReasonType\": \"BAD_QUALITY\",\n" +
                "      \"returnSubreason\": \"UNKNOWN\",\n" +
                "      \"supplierCompensation\": 111100,\n" +
                "      \"attributes\": {}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"complainTimeLeft\": \"PT-27H\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }
}
