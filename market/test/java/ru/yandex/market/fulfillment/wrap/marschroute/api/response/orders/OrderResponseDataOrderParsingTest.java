package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class OrderResponseDataOrderParsingTest extends MarschrouteJsonParsingTest<OrderResponseDataOrder> {

    OrderResponseDataOrderParsingTest() {
        super(OrderResponseDataOrder.class, "marschroute/api/orders/data/order.json");
    }
}
