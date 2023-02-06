package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class EmptyOrdersResponseParsingTest extends ParsingTest<OrdersResponse> {

    EmptyOrdersResponseParsingTest() {
        super(new ObjectMapper(), OrdersResponse.class, "marschroute/api/orders/empty_response.json");
    }

    @Override
    protected void performAdditionalAssertions(OrdersResponse ordersResponse) {
        softly.assertThat(ordersResponse.getData())
                .as("Asserting that data contains no elements")
                .isEmpty();

        softly.assertThat(ordersResponse.isSuccess())
                .as("Asserting that response success flag is true")
                .isTrue();
    }
}
