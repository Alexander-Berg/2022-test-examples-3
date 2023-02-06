package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class OrdersResponseParsingTest extends ParsingTest<OrdersResponse> {

    OrdersResponseParsingTest() {
        super(new ObjectMapper(), OrdersResponse.class, "marschroute/api/orders/orders_response.json");
    }
    
    @Override
    protected void performAdditionalAssertions(OrdersResponse ordersResponse) {
        softly.assertThat(ordersResponse.getData())
                .as("Asserting that data contains two elements")
                .hasSize(2);

        softly.assertThat(ordersResponse.isSuccess())
                .as("Asserting that response success flag is true")
                .isTrue();
    }
}
