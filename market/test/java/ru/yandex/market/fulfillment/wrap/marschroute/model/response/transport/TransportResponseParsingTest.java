package ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.List;

class TransportResponseParsingTest extends ParsingTest<TransportResponse> {
    TransportResponseParsingTest() {
        super(new ObjectMapper(), TransportResponse.class, "transport/response.json");
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
            .hasSize(3);

        softly.assertThat(response.getParams())
            .as("Asserting that params is not null")
            .isNotNull();

        softly.assertThat(response.getParams().getTotal())
            .as("Asserting params total")
            .isEqualTo(3);

        checkTransportResponseData0(data.get(0));
        checkTransportResponseData1(data.get(1));
        checkTransportResponseData2(data.get(2));
    }

    private void checkTransportResponseData0(TransportResponseData item) {
        softly.assertThat(item.getTicketId())
            .as("Asserting ticket id for item 0")
            .isEqualTo(75667);

        softly.assertThat(item.getDateArrive().getValue())
            .as("Asserting date arrive for item 0")
            .isEqualTo("02.10.2018 22:51:29");

        softly.assertThat(item.getNumberPlate())
            .as("Asserting number plate for item 0")
            .isEqualTo("495");

        softly.assertThat(item.getTransportType())
            .as("Asserting transport type for item 0")
            .isEqualTo(3);

        softly.assertThat(item.getPurpose())
            .as("Asserting purpose for item 0")
            .isEqualTo(2);

        softly.assertThat(item.getStatus())
            .as("Asserting transport status for item 0")
            .isEqualTo(30);

        softly.assertThat(item.getDateStatus().getValue())
            .as("Asserting date status for item 0")
            .isEqualTo("03.10.2018 01:16:44");

        softly.assertThat(item.getDateGate().getValue())
            .as("Asserting date gate for item 0")
            .isEqualTo("03.10.2018 00:45:26");

        softly.assertThat(item.getDateComplete().getValue())
            .as("Asserting date complete for item 0")
            .isEqualTo("03.10.2018 01:17:42");

        softly.assertThat(item.getOrdersId())
            .as("Asserting orders id for item 0")
            .hasSize(3);

        softly.assertThat(item.getDocsId())
            .as("Asserting docs id for item 0")
            .isEmpty();

        softly.assertThat(item.getActNumber())
            .as("Asserting act number for item 0")
            .isEqualTo("28611235O - 210141");
    }

    private void checkTransportResponseData1(TransportResponseData item) {
        softly.assertThat(item.getOrdersId())
            .as("Asserting orders id for item 1")
            .isEmpty();

        softly.assertThat(item.getDocsId())
            .as("Asserting docs id for item 1")
            .hasSize(1);

        softly.assertThat(item.getActNumber())
            .as("Asserting act number id for item 1")
            .isNull();
    }

    private void checkTransportResponseData2(TransportResponseData item) {
        softly.assertThat(item.getOrdersId())
            .as("Asserting orders id for item 2")
            .isEmpty();

        softly.assertThat(item.getDocsId())
            .as("Asserting docs id for item 2")
            .isEmpty();

        softly.assertThat(item.getActNumber())
            .as("Asserting act number id for item 2")
            .isNull();
    }
}
