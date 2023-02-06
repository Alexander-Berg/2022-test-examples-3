package ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class MarschrouteOrderTrackingResponseParsingTest extends ParsingTest<TrackingResponse> {

    MarschrouteOrderTrackingResponseParsingTest() {
        super(new ObjectMapper(), TrackingResponse.class, "order_tracking/response.json");
    }

    @Override
    protected void performAdditionalAssertions(TrackingResponse response) {
        softly.assertThat(response.isSuccess())
                .as("Asserting success value")
                .isEqualTo(true);

        TrackingResponseData data = response.getData();
        softly.assertThat(data)
                .as("Asserting that data is not null")
                .isNotNull();

        softly.assertThat(data.getOrderId())
                .as("Asserting order id value")
                .isEqualTo("EXT40238858");

        softly.assertThat(data.getCity())
                .as("Asserting order city value")
                .isEqualTo("Казань");

        softly.assertThat(data.getDateCreate().getValue())
                .as("Asserting date create value")
                .isEqualTo("04.09.2017 16:04:34");

        softly.assertThat(data.getTracking())
                .as("Asserting order tracking info size")
                .hasSize(3);
    }
}
