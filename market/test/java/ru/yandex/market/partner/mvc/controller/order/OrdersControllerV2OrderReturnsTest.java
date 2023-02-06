package ru.yandex.market.partner.mvc.controller.order;

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

/**
 * Тесты для {@link OrdersControllerV2#getOrderReturns}
 */
class OrdersControllerV2OrderReturnsTest extends FunctionalTest {

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2OrderReturnsTest.before.csv")
    void testOrderReturns() {
        when(clock.instant()).thenReturn(Instant.parse("2020-03-04T12:00:00.000Z"));

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/2/orders/33/returns");

        String returnReason = "Пришел вскрытый пакет,заклеенный скотчем,заметила сразу после отъезда курьера и " +
                "отправила " +
                "заявление в вашу службу поддержки с фото. У меня 2кота которые отказались есть данный корм,возможно " +
                " в пакет был насыпан некачественный корм и продан 'на дурака' с заклееным разрезом.м";

        //language=json
        String expected = "[\n" +
                "  {\n" +
                "    \"orderId\": 33,\n" +
                "    \"returnId\": 18802,\n" +
                "    \"createdAt\": \"2019-05-16T12:32:56.888+03:00\",\n" +
                "    \"updatedAt\": \"2019-05-28T17:14:23.939+03:00\",\n" +
                "    \"moneyAmount\": 500,\n" +
                "    \"supplierCompensation\":77700.0," +
                "    \"returnStatus\": \"PARTIALLY_RETURNED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "    \"applicationUrl\": \"http://application.url\",\n" +
                "    \"resupplyStatus\": \"RESUPPLIED_TO_BAD_AND_GOOD_STOCK\",\n" +
                "    \"resuppliedAt\":\"2020-03-01T17:00:00+03:00\"," +
                "    \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "    \"fastReturn\":false,\n" +
                "    \"complainTimeLeft\": \"PT650H\",\n" +
                "    \"returnItems\": [\n" +
                "      {\n" +
                "        \"orderItemId\": 9568212,\n" +
                "        \"shopSku\": \"sku22\",\n" +
                "        \"marketSku\": 22,\n" +
                "        \"title\": \"sku22\",\n" +
                "        \"count\": 10,\n" +
                "        \"resupplyCount\": 4,\n" +
                "        \"resupplyGoodCount\": 1,\n" +
                "        \"resupplyDefectCount\": 3,\n" +
                "        \"resuppliedAt\":\"2020-03-01T17:00:00+03:00\"," +
                "        \"photoUrls\": [\n" +
                "          \"single url\"\n" +
                "        ],\n" +
                "        \"moneyAmount\": 500,\n" +
                "        \"returnReason\": \"нет возможности быстро печатать\",\n" +
                "        \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "        \"returnSubreason\": \"NOT_WORKING\",\n" +
                "        \"supplierCompensation\":77700.0,\n" +
                "        \"attributes\": {\"MISSING_PARTS\":1,\"WAS_USED\":2,\"WRONG_OR_DAMAGED_PAPERS\":1}\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"orderId\": 33,\n" +
                "    \"returnId\": 695700,\n" +
                "    \"createdAt\": \"2021-02-25T22:08:53.71163+03:00\",\n" +
                "    \"updatedAt\": \"2021-04-08T20:34:23.037181+03:00\",\n" +
                "    \"moneyAmount\": 50,\n" +
                "    \"supplierCompensation\":0.0," +
                "    \"returnStatus\": \"COMPLETED\",\n" +
                "    \"applicationUrl\": \"https://market-checkouter-prod.s3.mds.yandex.net/return-application-695700" +
                ".pdf\",\n" +
                "    \"resupplyStatus\": \"NOT_RESUPPLIED\",\n" +
                "    \"warehouse\": \"Яндекс.Маркет (Московская область, Томилино)\",\n" +
                "    \"fastReturn\":true,\n" +
                "    \"returnItems\": [\n" +
                "      {\n" +
                "        \"orderItemId\": 72450004,\n" +
                "        \"shopSku\": \"sku21\",\n" +
                "        \"marketSku\": 21,\n" +
                "        \"title\": \"sku21\",\n" +
                "        \"count\": 1,\n" +
                "        \"resupplyCount\": 0,\n" +
                "        \"resupplyGoodCount\": 0,\n" +
                "        \"resupplyDefectCount\": 0,\n" +
                "        \"photoUrls\": [\n" +
                "          \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f14b14cf6344f8eba423f661/orig\",\n" +
                "          \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/200370/2a00000177d9f0be1e7cbb5cf0ce92fa7403/orig\",\n" +
                "          \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f0ed775315fd02d236101f73/orig\",\n" +
                "          \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/2369747/2a00000177d9f02b8821e14022acbe875dff/orig\",\n" +
                "          \"https://avatars.mds.yandex" +
                ".net/get-market-ugc/474703/2a00000177d9f0a15a9b21259b8e6a01bf1f/orig\"\n" +
                "        ],\n" +
                "        \"moneyAmount\": 50,\n" +
                "        \"returnReason\": \"Ошиблась и заказала неправильное средство, которое дублирует мусс Кора " +
                "из заказа. Поэтому не открывала и хочу оформить возврат.\",\n" +
                "        \"returnReasonType\": \"DO_NOT_FIT\",\n" +
                "        \"returnSubreason\": \"USER_CHANGED_MIND\",\n" +
                "        \"supplierCompensation\":0.0,\n" +
                "        \"attributes\": {}\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        JsonTestUtil.assertEquals(response, expected);
    }
}
