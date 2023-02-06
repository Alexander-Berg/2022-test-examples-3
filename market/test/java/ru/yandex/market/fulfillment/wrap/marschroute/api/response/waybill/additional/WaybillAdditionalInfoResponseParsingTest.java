package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.additional;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class WaybillAdditionalInfoResponseParsingTest extends ParsingTest<WaybillAdditionalInfoResponse> {

    WaybillAdditionalInfoResponseParsingTest() {
        super(new ObjectMapper(),
                WaybillAdditionalInfoResponse.class,
                "marschroute/api/waybill/additional/response.json"
        );
    }

    @Override
    protected void performAdditionalAssertions(WaybillAdditionalInfoResponse response) {
        softly.assertThat(response.isSuccessful())
                .as("Checking that response is successful")
                .isTrue();

        Map<String, WaybillAdditionalInfoResponseData> data = response.getData();
        softly.assertThat(data)
                .as("Checking that response contains two items")
                .hasSize(2);

        assertDataContent("311158128.76616", data, 4, 5, 6);
        assertDataContent("311191128.76616", data, 1, 2, 3);
    }

    private void assertDataContent(String key,
                                   Map<String, WaybillAdditionalInfoResponseData> data,
                                   int quantity,
                                   int quantityOrig,
                                   int quantityDefect) {

        WaybillAdditionalInfoResponseData firstData = data.get(key);

        softly.assertThat(firstData.getItemId())
                .as("Checking that itemId is identical to map key")
                .isEqualTo(key);

        softly.assertThat(firstData.getQty())
                .as("Asserting quantity")
                .isEqualTo(quantity);

        softly.assertThat(firstData.getQtyOrig())
                .as("Asserting original quantity")
                .isEqualTo(quantityOrig);

        softly.assertThat(firstData.getQtyDefect())
                .as("Asserting defect quantity")
                .isEqualTo(quantityDefect);
    }

}
