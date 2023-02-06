package ru.yandex.market.partner.mvc.controller.order;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link OrdersControllerV2#getOrderSummary}
 */
class OrdersControllerV2SummaryTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/1898847" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 1898847,\n" +
                "  \"createdAt\": \"2017-11-27\",\n" +
                "  \"updatedAt\": \"2019-05-29\",\n" +
                "  \"orderStatus\": \"CANCELLED_IN_DELIVERY\",\n" +
                "  \"returnIds\": [18802,18803],\n" +
                "  \"returnedSkuCount\":1," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"cbb2282b-eea7-11e6-810b-00155d000405\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Attento\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"CANCELLED_IN_DELIVERY\",\n" +
                "      \"changedAt\": \"2019-05-29\",\n" +
                "      \"orderPrice\": 4171.00,\n" +
                "      \"orderSubsidy\": 371.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"cbb2282b-eea7-11e6-810b-00155d000405\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"offer\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"CANCELLED_IN_DELIVERY\",\n" +
                "      \"changedAt\": \"2019-05-29\",\n" +
                "      \"orderPrice\": 1.00,\n" +
                "      \"orderSubsidy\": 0.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_whenHasHandlingTime() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/7006286" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 7006286,\n" +
                "  \"createdAt\": \"2020-11-25\",\n" +
                "  \"updatedAt\": \"2017-11-27\",\n" +
                "  \"orderStatus\": \"DELIVERY\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\":0," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"status.100530200240\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Утюг Philips qwe543\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 5670.00,\n" +
                "      \"orderSubsidy\": 404.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"bugs-princess-14-sku\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Многоразовые пеленки Daisy хлопок 75x120 белый/розовый 1 шт.\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 2000.00,\n" +
                "      \"orderSubsidy\": 142.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"try2.MARKETPARTNER-11673\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Сухой корм для рыб Зоомир Рыбята Золотая рыбка в хлопьях 10 г\",\n" +
                "      \"count\": 0,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 777.00,\n" +
                "      \"orderSubsidy\": 54.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_whenNoHandlingTime() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/7006287" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 7006287,\n" +
                "  \"createdAt\": \"2020-11-25\",\n" +
                "  \"updatedAt\": \"2017-11-27\",\n" +
                "  \"orderStatus\": \"DELIVERY\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\":0," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"status.100530200240\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Утюг Philips qwe543\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 5670.00,\n" +
                "      \"orderSubsidy\": 404.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"bugs-princess-14-sku\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Многоразовые пеленки Daisy хлопок 75x120 белый/розовый 1 шт.\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 2000.00,\n" +
                "      \"orderSubsidy\": 142.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"try2.MARKETPARTNER-11673\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Сухой корм для рыб Зоомир Рыбята Золотая рыбка в хлопьях 10 г\",\n" +
                "      \"count\": 0,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 777.00,\n" +
                "      \"orderSubsidy\": 54.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_shouldShowPaymentForDeliveryPrePaid() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/7006288" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 7006288,\n" +
                "  \"createdAt\": \"2020-11-25\",\n" +
                "  \"updatedAt\": \"2017-11-27\",\n" +
                "  \"orderStatus\": \"DELIVERY\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\":0," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"status.100530200240\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Утюг Philips qwe543\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 5670.00,\n" +
                "      \"orderSubsidy\": 404.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"bugs-princess-14-sku\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Многоразовые пеленки Daisy хлопок 75x120 белый/розовый 1 шт.\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 2000.00,\n" +
                "      \"orderSubsidy\": 142.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"try2.MARKETPARTNER-11673\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Сухой корм для рыб Зоомир Рыбята Золотая рыбка в хлопьях 10 г\",\n" +
                "      \"count\": 0,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2017-11-27\",\n" +
                "      \"orderPrice\": 777.00,\n" +
                "      \"orderSubsidy\": 54.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_noItems() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245733/orders/1898847/summary")
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testOrderSummary_notFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245733/orders/1898847/summary")
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }


    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_spasiboAndCashbackInOrders() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/7009000" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 7009000,\n" +
                "  \"createdAt\": \"2020-11-25\",\n" +
                "  \"updatedAt\": \"2020-05-05\",\n" +
                "  \"orderStatus\": \"DELIVERY\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\":0," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"pepelac\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Пепелац\",\n" +
                "      \"count\": 2,\n" +
                "      \"initialCount\": 2,\n" +
                "      \"itemStatus\": \"DELIVERY\",\n" +
                "      \"changedAt\": \"2020-05-05\",\n" +
                "      \"orderPrice\": 8700.00,\n" +
                "      \"orderSubsidy\": 0.00,\n" +
                "      \"orderSpasibo\": 250.00,\n" +
                "      \"orderCashback\": 100.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTestControlEnabled.before.csv")
    @DisplayName("Получить заказ по новой схеме управления выплатами.")
    void testOrderSummary_newMoneyFlowTriggerTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/1898848" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 1898848,\n" +
                "  \"createdAt\": \"2017-11-27\",\n" +
                "  \"updatedAt\": \"2017-12-27\",\n" +
                "  \"orderStatus\": \"CANCELLED_IN_DELIVERY\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\": 0,\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"SOME-SKU\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"offer\",\n" +
                "      \"count\": 1,\n" +
                "      \"initialCount\": 1,\n" +
                "      \"itemStatus\": \"CANCELLED_IN_DELIVERY\",\n" +
                "      \"changedAt\": \"2017-12-27\",\n" +
                "      \"orderPrice\": 1.00,\n" +
                "      \"orderSubsidy\": 0.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\"buyerType\":\"PERSON\"}\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2SummaryTest.before.csv")
    void testOrderSummary_partiallyReturnedStatus() {
        //один товар доставлен, другой полностью возращен
        //ожидаем, что статус всего заказа - частичный возврат
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "v2/campaigns/10245732/orders/7009600" +
                "/summary");
        //language=json
        String expected = "" +
                "{\n" +
                "  \"orderId\": 7009600,\n" +
                "  \"createdAt\": \"2020-11-25\",\n" +
                "  \"updatedAt\": \"2020-11-26\",\n" +
                "  \"orderStatus\": \"PARTIALLY_RETURNED\",\n" +
                "  \"returnIds\": [],\n" +
                "  \"returnedSkuCount\":0," +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"shopSku\": \"gravitsapa\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Гравицапа\",\n" +
                "      \"count\": 2,\n" +
                "      \"initialCount\": 2,\n" +
                "      \"itemStatus\": \"DELIVERED\",\n" +
                "      \"changedAt\": \"2020-11-25\",\n" +
                "      \"orderPrice\": 4000.00,\n" +
                "      \"orderSubsidy\": 0.00\n" +
                "    },\n" +
                "    {\n" +
                "      \"shopSku\": \"tranklukator\",\n" +
                "      \"marketSku\": 0,\n" +
                "      \"title\": \"Транклюкатор\",\n" +
                "      \"count\": 2,\n" +
                "      \"initialCount\": 2,\n" +
                "      \"itemStatus\": \"RETURNED\",\n" +
                "      \"changedAt\": \"2020-11-26\",\n" +
                "      \"orderPrice\": 8000.00,\n" +
                "      \"orderSubsidy\": 0.00\n" +
                "    }\n" +
                "  ],\n" +
                "  \"additionalInfo\": {\n" +
                "                          \"buyerType\":\"PERSON\",\n" +
                "                          \"statusExpiryDate\": \"2020-11-27T16:30:00\"\n" +
                "                       }\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }
}
