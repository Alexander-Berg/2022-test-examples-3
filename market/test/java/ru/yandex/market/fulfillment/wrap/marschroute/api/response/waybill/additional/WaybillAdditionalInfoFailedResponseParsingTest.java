package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.additional;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

class WaybillAdditionalInfoFailedResponseParsingTest extends ParsingTest<WaybillAdditionalInfoResponse> {

    WaybillAdditionalInfoFailedResponseParsingTest() {
        super(new ObjectMapper(),
                WaybillAdditionalInfoResponse.class,
                "marschroute/api/waybill/additional/failed_response.json"
        );
    }

    @Override
    protected void performAdditionalAssertions(WaybillAdditionalInfoResponse response) {
        softly.assertThat(response.isSuccessful())
                .as("Asserting that response was not successful")
                .isFalse();

        softly.assertThat(response.getData())
                .as("Asserting that data is empty")
                .isEmpty();

        softly.assertThat(response.getCode())
                .as("Asserting response code")
                .isEqualTo(401);

        softly.assertThat(response.getComment())
                .as("Asserting comment")
                .isEqualTo("Накладая не найдена!");
    }
}
