package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class OrderItemParsingTest extends MarschrouteJsonParsingTest<OrderItem> {

    OrderItemParsingTest() {
        super(OrderItem.class, "marschroute/api/orders/item.json");
    }
}
