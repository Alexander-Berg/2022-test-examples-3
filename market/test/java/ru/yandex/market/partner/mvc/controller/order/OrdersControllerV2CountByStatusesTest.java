package ru.yandex.market.partner.mvc.controller.order;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link OrdersControllerV2#getOrdersCountByStatuses}
 */
class OrdersControllerV2CountByStatusesTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2CountByStatusesTest.before.csv")
    void getOrdersCountByStatuses_nullDates() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/2/orders/by-status/count");
        //language=json
        String expected = "{" +
                "\"PROCESSING\":0," +
                "\"RESERVED\":0," +
                "\"PICKUP\":0," +
                "\"CANCELLED_IN_PROCESSING\":0," +
                "\"PARTIALLY_RETURNED\":3," +
                "\"PARTIALLY_UNREDEEMED\":0," +
                "\"DELIVERED\":0," +
                "\"DELIVERY\":2," +
                "\"RETURNED\":0," +
                "\"UNKNOWN\":0," +
                "\"CANCELLED_IN_DELIVERY\":0," +
                "\"UNPAID\":0," +
                "\"UNREDEEMED\":1," +
                "\"PENDING\":0," +
                "\"CANCELLED_BEFORE_PROCESSING\":0," +
                "\"READY_TO_TRANSFER\":0," +
                "\"TRANSFERRED_TO_YOU\":0" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2CountByStatusesTest.before.csv")
    void getOrdersCountByStatuses_dateFrom() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/2/orders/by-status/count?dateFrom=2020-04-01");
        //language=json
        String expected = "{" +
                "\"PROCESSING\":0," +
                "\"RESERVED\":0," +
                "\"PICKUP\":0," +
                "\"CANCELLED_IN_PROCESSING\":0," +
                "\"PARTIALLY_RETURNED\":2," +
                "\"PARTIALLY_UNREDEEMED\":0," +
                "\"DELIVERED\":0," +
                "\"DELIVERY\":1," +
                "\"RETURNED\":0," +
                "\"UNKNOWN\":0," +
                "\"CANCELLED_IN_DELIVERY\":0," +
                "\"UNPAID\":0," +
                "\"UNREDEEMED\":0," +
                "\"PENDING\":0," +
                "\"CANCELLED_BEFORE_PROCESSING\":0," +
                "\"READY_TO_TRANSFER\":0," +
                "\"TRANSFERRED_TO_YOU\":0" +                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "OrdersControllerV2CountByStatusesTest.before.csv")
    void getOrdersCountByStatuses_dateTo() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "v2/campaigns/2/orders/by-status/count?dateTo=2020-04-01");
        //language=json
        String expected = "{" +
                "\"PROCESSING\":0," +
                "\"RESERVED\":0," +
                "\"PICKUP\":0," +
                "\"CANCELLED_IN_PROCESSING\":0," +
                "\"PARTIALLY_RETURNED\":1," +
                "\"PARTIALLY_UNREDEEMED\":0," +
                "\"DELIVERED\":0," +
                "\"DELIVERY\":1," +
                "\"RETURNED\":0," +
                "\"UNKNOWN\":0," +
                "\"CANCELLED_IN_DELIVERY\":0," +
                "\"UNPAID\":0," +
                "\"UNREDEEMED\":1," +
                "\"PENDING\":0," +
                "\"CANCELLED_BEFORE_PROCESSING\":0," +
                "\"READY_TO_TRANSFER\":0," +
                "\"TRANSFERRED_TO_YOU\":0" +                "}";

        JsonTestUtil.assertEquals(response, expected);
    }
}
