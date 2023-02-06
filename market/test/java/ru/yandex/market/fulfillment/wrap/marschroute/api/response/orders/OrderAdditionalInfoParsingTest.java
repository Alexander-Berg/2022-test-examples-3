package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders.additional.OrderAdditionalInfoResponse;

class OrderAdditionalInfoParsingTest extends MarschrouteJsonParsingTest<OrderAdditionalInfoResponse> {

    OrderAdditionalInfoParsingTest() {
        super(OrderAdditionalInfoResponse.class, "marschroute/api/orders/data/order_additional_info_response.json");
    }
}
