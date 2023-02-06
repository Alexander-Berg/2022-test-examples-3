package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class OrderParsingTest extends ParsingTest<Order> {
    public OrderParsingTest() {
        super(Order.class, "fixture/entities/order.xml");
    }
}
