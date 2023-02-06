package ru.yandex.market.fulfillment.wrap.marschroute.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.order.MarschrouteCreateOrderRequest;

class CreateOrderRequestParsingTest extends ParsingTest<MarschrouteCreateOrderRequest> {
    CreateOrderRequestParsingTest() {
        super(new ObjectMapper(), MarschrouteCreateOrderRequest.class, "create_order_request.json");
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteCreateOrderRequest object) {
        softly.assertThat(object.getOrder())
            .as("Assert that order object is filled")
            .isNotNull()
            .hasNoNullFieldsOrPropertiesExcept("pickupPointCode", "dimensions", "deliveryServiceId");

        softly.assertThat(object.getCustomer())
            .as("Assert that customer object is filled")
            .isNotNull()
            .hasNoNullFieldsOrProperties();

        softly.assertThat(object.getItems())
            .as("Assert that items are filled")
            .isNotNull()
            .hasSize(2);

        softly.assertThat(object.getItems().get(0))
            .hasNoNullFieldsOrProperties();

        softly.assertThat(object.getItems().get(1))
            .hasNoNullFieldsOrProperties();
    }
}
