package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class OrderResponseParsingTest extends MarschrouteJsonParsingTest<OrderResponse> {

    OrderResponseParsingTest() {
        super(OrderResponse.class, "marschroute/api/orders/order_response.json");
    }
}
