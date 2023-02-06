package ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class MarschrouteDeliveryCityEmptyResponseParsingTest extends ParsingTest<MarschrouteDeliveryCityResponse> {

    MarschrouteDeliveryCityEmptyResponseParsingTest() {
        super(new ObjectMapper(), MarschrouteDeliveryCityResponse.class, "delivery_city/empty.json");
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteDeliveryCityResponse response) {
        softly.assertThat(response.isSuccess())
                .as("Asserting that response was successful")
                .isTrue();

        MarschrouteDeliveryCity responseData = response.getData().iterator().next();

        softly.assertThat(responseData.getDeliveryOptions())
                .as("Asserting that delivery options are empty")
                .isEmpty();
    }
}
