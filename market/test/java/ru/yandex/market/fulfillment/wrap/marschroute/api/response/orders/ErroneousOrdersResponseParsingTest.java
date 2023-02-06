package ru.yandex.market.fulfillment.wrap.marschroute.api.response.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class ErroneousOrdersResponseParsingTest extends ParsingTest<OrdersResponse> {

    ErroneousOrdersResponseParsingTest() {
        super(new ObjectMapper(), OrdersResponse.class, "marschroute/api/orders/erroneous_response.json");
    }

    @Override
    protected void performAdditionalAssertions(OrdersResponse ordersResponse) {
        softly.assertThat(ordersResponse.getData())
                .as("Asserting that data contains no elements")
                .isEmpty();

        softly.assertThat(ordersResponse.isSuccess())
                .as("Asserting that response success flag is false")
                .isFalse();

        softly.assertThat(ordersResponse.getComment())
                .as("Asserting comment value")
                .isEqualTo("Некорректный формат параметров запроса");

        softly.assertThat(ordersResponse.getCode())
                .as("Asserting code value")
                .isEqualTo(104);
    }
}
