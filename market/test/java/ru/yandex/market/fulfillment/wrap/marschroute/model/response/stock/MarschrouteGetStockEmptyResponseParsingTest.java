package ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class MarschrouteGetStockEmptyResponseParsingTest extends ParsingTest<MarschrouteProductsResponse> {

    MarschrouteGetStockEmptyResponseParsingTest() {
        super(new ObjectMapper(), MarschrouteProductsResponse.class, "get_stock/empty_response.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of("success", true);
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteProductsResponse response) {
        softly.assertThat(response.getData())
                .as("Asserting that data is empty array")
                .isEmpty();

        softly.assertThat(response.getParams().getTotal())
                .as("Asserting that param.total is equal to 0")
                .isEqualTo(0);

        softly.assertThat(response.getComment())
                .as("Asserting that error info has null comment")
                .isNull();

        softly.assertThat(response.getCode())
                .as("Asserting that error info has null code")
                .isNull();
    }
}
