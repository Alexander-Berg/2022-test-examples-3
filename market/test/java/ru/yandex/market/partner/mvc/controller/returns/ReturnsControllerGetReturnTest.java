package ru.yandex.market.partner.mvc.controller.returns;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.delivery.LogisticPointInfoYtDao;
import ru.yandex.market.core.delivery.model.LogisticPointInfo;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

class ReturnsControllerGetReturnTest extends FunctionalTest {

    @Autowired
    private Clock clock;

    @Autowired
    private LogisticPointInfoYtDao logisticPointInfoYtDao;

    @Test
    @DisplayName("Существующий FBY возврат с пустым складом")
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testGetExistingFBYReturn() {
        mockYtDao();
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/695755");

        String expected = "{\n" +
                "    \"orderId\": 5555,\n" +
                "    \"returnId\": 695755,\n" +
                "    \"createdAt\": \"2021-02-27T22:08:53.71163+03:00\",\n" +
                "    \"updatedAt\": \"2021-04-10T20:34:23.037181+03:00\",\n" +
                "    \"moneyAmount\": 50,\n" +
                "    \"supplierCompensation\": 0.0,\n" +
                "    \"returnStatus\": \"COMPLETED\",\n" +
                "    \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex.net/return-application-695755" +
                ".pdf\",\n" +
                "    \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "    \"fastReturn\": true,\n" +
                "    \"returnItems\": [\n" +
                "        {\n" +
                "            \"orderItemId\": 72450555,\n" +
                "            \"shopSku\": \"sku21\",\n" +
                "            \"marketSku\": 21,\n" +
                "            \"title\": \"sku21\",\n" +
                "            \"count\": 1,\n" +
                "            \"resupplyCount\": 0,\n" +
                "            \"resupplyGoodCount\": 0,\n" +
                "            \"resupplyDefectCount\": 0,\n" +
                "            \"photoUrls\": [],\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"returnReason\": \"order5555\",\n" +
                "            \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "            \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"attributes\": {}\n" +
                "        }\n" +
                "    ],\n" +
                "    \"trackCode\": \"1231mklmn\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Существующий FBY возврат без ресапплаев")
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testGetExistingFBYWithoutResuppliesReturn() {
        mockYtDao();
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/2205");

        String expected = "{\n" +
                "    \"orderId\": 3,\n" +
                "    \"returnId\": 2205,\n" +
                "    \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "    \"updatedAt\": \"2019-05-28T17:14:23.939+03:00\",\n" +
                "    \"moneyAmount\": 50,\n" +
                "    \"supplierCompensation\": 111100.0,\n" +
                "    \"returnStatus\": \"PARTIALLY_RETURNED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "    \"applicationUrl\": \"http://application.url\",\n" +
                "    \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "    \"fastReturn\": false,\n" +
                "    \"returnItems\": [\n" +
                "        {\n" +
                "            \"orderItemId\": 4,\n" +
                "            \"shopSku\": \"sku22\",\n" +
                "            \"marketSku\": 22,\n" +
                "            \"title\": \"sku22\",\n" +
                "            \"count\": 1,\n" +
                "            \"resupplyCount\": 0,\n" +
                "            \"resupplyGoodCount\": 0,\n" +
                "            \"resupplyDefectCount\": 0,\n" +
                "            \"photoUrls\": [\n" +
                "                \"single url\"\n" +
                "            ],\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"returnReason\": \"Разбитый\",\n" +
                "            \"returnReasonType\": \"BAD_QUALITY\",\n" +
                "            \"returnSubreason\": \"UNKNOWN\",\n" +
                "            \"supplierCompensation\": 111100.0,\n" +
                "            \"attributes\": {}\n" +
                "        }\n" +
                "    ],\n" +
                "    \"trackCode\": \"j234jln\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("ПВЗ возврат")
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testGetPickupReturn() {

        mockYtDao();
        when(clock.instant()).thenReturn(Instant.parse("2021-04-11T12:00:00.000Z"));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/695744");

        String expected = "{\n" +
                "    \"orderId\": 4444,\n" +
                "    \"returnId\": 695744,\n" +
                "    \"createdAt\": \"2021-02-26T22:08:53.71163+03:00\",\n" +
                "    \"updatedAt\": \"2021-04-09T20:34:23.037181+03:00\",\n" +
                "    \"moneyAmount\": 50,\n" +
                "    \"supplierCompensation\": 0.0,\n" +
                "    \"returnStatus\": \"READY_FOR_PICKUP_COMPLAIN_PERIOD_EXPIRED\",\n" +
                "    \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex.net/return-application-695744" +
                ".pdf\",\n" +
                "    \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "    \"warehouse\": \"Партнерский ПВЗ ЯНДЕКС МАРКЕТА (Moscow, ARBAT street, 5)\",\n" +
                "    \"fastReturn\": false,\n" +
                "    \"returnItems\": [\n" +
                "        {\n" +
                "            \"orderItemId\": 72450444,\n" +
                "            \"shopSku\": \"sku21\",\n" +
                "            \"marketSku\": 21,\n" +
                "            \"title\": \"sku21\",\n" +
                "            \"count\": 1,\n" +
                "            \"resupplyCount\": 0,\n" +
                "            \"resupplyGoodCount\": 0,\n" +
                "            \"resupplyDefectCount\": 0,\n" +
                "            \"photoUrls\": [],\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"returnReason\": \"order4444\",\n" +
                "            \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "            \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "            \"supplierCompensation\": 0.0,\n" +
                "            \"attributes\": {}\n" +
                "        }\n" +
                "    ],\n" +
                "    \"complainTimeLeft\": \"PT674H14M23.939S\",\n" +
                "    \"trackCode\": \"13jkkj123\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("FBS возврат не через ПВЗ")
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testGetFBSReturn() {
        mockYtDao();
        when(clock.instant()).thenReturn(Instant.parse("2021-04-11T12:00:00.000Z"));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "campaigns/3/returns/2204");

        String expected = "{\n" +
                "    \"orderId\": 5556,\n" +
                "    \"returnId\": 2204,\n" +
                "    \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "    \"updatedAt\": \"2019-05-28T17:14:23.939+03:00\",\n" +
                "    \"moneyAmount\": 50,\n" +
                "    \"supplierCompensation\": 111100.0,\n" +
                "    \"returnStatus\": \"COMPLAIN_PERIOD_EXPIRED\",\n" +
                "    \"applicationUrl\": \"http://application.url\",\n" +
                "    \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "    \"warehouse\": \"Ваш склад\",\n" +
                "    \"fastReturn\": false,\n" +
                "    \"returnItems\": [\n" +
                "        {\n" +
                "            \"orderItemId\": 72450556,\n" +
                "            \"shopSku\": \"sku44\",\n" +
                "            \"marketSku\": 44,\n" +
                "            \"title\": \"sku44\",\n" +
                "            \"count\": 1,\n" +
                "            \"resupplyCount\": 0,\n" +
                "            \"resupplyGoodCount\": 0,\n" +
                "            \"resupplyDefectCount\": 0,\n" +
                "            \"photoUrls\": [\n" +
                "                \"single url\"\n" +
                "            ],\n" +
                "            \"moneyAmount\": 50,\n" +
                "            \"returnReason\": \"Разбитый\",\n" +
                "            \"returnReasonType\": \"BAD_QUALITY\",\n" +
                "            \"returnSubreason\": \"UNKNOWN\",\n" +
                "            \"supplierCompensation\": 111100.0,\n" +
                "            \"attributes\": {}\n" +
                "        }\n" +
                "    ],\n" +
                "    \"complainTimeLeft\": \"PT-15693H-45M-36.061S\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testNoReturn() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/666")
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testReturnStatus1000() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "campaigns/2/returns/13928")
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "ReturnsControllerGetReturnTest.before.csv")
    void testNoPartner() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "campaigns/666/returns/695755")
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private void mockYtDao() {
        LogisticPointInfo logisticPointInfo = LogisticPointInfo.builder()
                .setLogisticPointId(10001)
                .setPointName("Партнерский ПВЗ ЯНДЕКС МАРКЕТА")
                .setShipperName("IP logistic")
                .setLocalityName("Moscow")
                .setStreetName("ARBAT street")
                .setPremiseNumber("5")
                .build();
        when(logisticPointInfoYtDao.lookupLogisticPointsInfo(Mockito.any())).thenReturn(List.of(logisticPointInfo));
    }
}
