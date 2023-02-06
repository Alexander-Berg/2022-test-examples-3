package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.additional;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class WaybillAdditionalInfoEmptyResponseParsingTest extends ParsingTest<WaybillAdditionalInfoResponse> {

    WaybillAdditionalInfoEmptyResponseParsingTest() {
        super(new ObjectMapper(),
                WaybillAdditionalInfoResponse.class,
                "marschroute/api/waybill/additional/empty_response.json"
        );
    }

    @Override
    protected void performAdditionalAssertions(WaybillAdditionalInfoResponse response) {
        softly.assertThat(response.isSuccessful())
                .as("Asserting that response is successful")
                .isTrue();

        softly.assertThat(response.getData())
                .as("Asserting that data is empty")
                .isEmpty();
    }
}
