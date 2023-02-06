package ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class TrackingInfoParsingTest extends ParsingTest<TrackingInfo> {

    TrackingInfoParsingTest() {
        super(new ObjectMapper(), TrackingInfo.class, "tracking_info.json");
    }

    @Override
    protected void performAdditionalAssertions(TrackingInfo trackingInfo) {
        softly.assertThat(trackingInfo.getStatus())
                .as("Asserting status value")
                .isEqualTo("Оформлен");

        softly.assertThat(trackingInfo.getDate().getValue())
                .as("Asserting date value")
                .isEqualTo("04.09.2017 16:04:34");

        softly.assertThat(trackingInfo.getTrackingStatus())
                .as("Asserting status id value")
                .isEqualTo(TrackingStatus.PROCESSED);
    }
}
