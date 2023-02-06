package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.utils.ParsingTest;

public class OrderStatusParsingTest extends ParsingTest<OrderStatus> {

    public OrderStatusParsingTest() {
        super(OrderStatus.class, "fixture/response/entities/order_status.xml");
    }

    @Override
    protected void performAdditionalAssertions(OrderStatus orderStatus) {
        assertions().assertThat(orderStatus.getStatusCode())
            .as("Asserting status code value")
            .isEqualTo(OrderStatusType.ORDER_CREATED_FF);

        assertions().assertThat(orderStatus.getSetDate().getFormattedDate())
            .as("Asserting set date value")
            .isEqualTo("2017-09-10T10:16:00+03:00");

        assertions().assertThat(orderStatus.getMessage())
            .as("Asserting message value")
            .isEqualTo("MyMessage");
    }
}
