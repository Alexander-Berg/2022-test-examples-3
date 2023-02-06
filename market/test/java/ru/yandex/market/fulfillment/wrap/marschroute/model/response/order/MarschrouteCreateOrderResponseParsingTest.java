package ru.yandex.market.fulfillment.wrap.marschroute.model.response.order;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

import static org.assertj.core.api.Assertions.assertThat;

class MarschrouteCreateOrderResponseParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteCreateOrderResponse> {

    MarschrouteCreateOrderResponseParsingTest() {
        super(MarschrouteCreateOrderResponse.class, "order/create/create_order_response_with_errors.json");
    }

    @Test
    public void testSerializationAndDeserialization() throws Exception {
        String expected = extractFileContent(fileName);
        MarschrouteCreateOrderResponse response = getMapper().readValue(expected, type);

        assertThat(response.getErrors().get("product_shortage").getMessage())
            .as("Asserting that response was deserialized successfully")
            .isEqualTo("[{\"item_id\":\"334005452\",\"cnt\":\"500\",\"cnt_reserved\":\"78\"}]");
    }
}
