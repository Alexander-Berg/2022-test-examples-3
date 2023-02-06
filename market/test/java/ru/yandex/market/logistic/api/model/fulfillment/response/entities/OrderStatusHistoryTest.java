package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class OrderStatusHistoryTest extends ParsingTest<OrderStatusHistory> {

    public OrderStatusHistoryTest() {
        super(OrderStatusHistory.class, "fixture/response/entities/ff_order_status_history.xml");
    }

    @Override
    protected void performAdditionalAssertions(OrderStatusHistory orderStatusHistory) {
        assertions().assertThat(orderStatusHistory.getHistory())
            .as("Asserting history size")
            .hasSize(3);
    }
}
