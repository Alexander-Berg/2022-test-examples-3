package ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.List;
import java.util.Map;

class TransportResponseEmptyResponseParsingTest extends ParsingTest<TransportResponse> {
    TransportResponseEmptyResponseParsingTest() {
        super(new ObjectMapper(), TransportResponse.class, "transport/empty_response.json");
    }

    @Override
    protected void performAdditionalAssertions(TransportResponse response) {
        softly.assertThat(response.isSuccess())
            .as("Asserting success value")
            .isEqualTo(true);

        List<TransportResponseData> data = response.getData();
        softly.assertThat(data)
            .as("Asserting that data is not null")
            .isNotNull();

        softly.assertThat(data)
            .as("Asserting data size")
            .isEmpty();

        softly.assertThat(response.getParams())
            .as("Asserting that params is not null")
            .isNotNull();

        softly.assertThat(response.getParams().getTotal())
            .as("Asserting params total")
            .isEqualTo(0);
    }
}
