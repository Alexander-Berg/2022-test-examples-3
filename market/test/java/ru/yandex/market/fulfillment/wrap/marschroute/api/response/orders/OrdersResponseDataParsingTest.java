package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class OrdersResponseDataParsingTest extends ParsingTest<OrdersResponseData> {

    OrdersResponseDataParsingTest() {
        super(new ObjectMapper(), OrdersResponseData.class, "marschroute/api/orders/data/orders_response.json");
    }

    @Override
    protected void performAdditionalAssertions(OrdersResponseData ordersResponseData) {
        softly.assertThat(ordersResponseData.getOrderId())
                .as("Asserting order id value")
                .isEqualTo("EXT51030432");

        softly.assertThat(ordersResponseData.getType())
                .as("Asserting type")
                .isEqualTo(4);
    }
}
