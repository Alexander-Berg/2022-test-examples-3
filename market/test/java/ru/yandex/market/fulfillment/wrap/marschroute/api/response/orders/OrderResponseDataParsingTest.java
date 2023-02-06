package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class OrderResponseDataParsingTest extends MarschrouteJsonParsingTest<OrderResponseData> {

    OrderResponseDataParsingTest() {
        super(OrderResponseData.class, "marschroute/api/orders/data/order_response.json");
    }
}
