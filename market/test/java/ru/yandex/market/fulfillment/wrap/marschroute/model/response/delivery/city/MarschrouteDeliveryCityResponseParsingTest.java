package ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Collection;

class MarschrouteDeliveryCityResponseParsingTest extends ParsingTest<MarschrouteDeliveryCityResponse> {

    MarschrouteDeliveryCityResponseParsingTest() {
        super(new ObjectMapper(), MarschrouteDeliveryCityResponse.class, "delivery_city/value.json");
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteDeliveryCityResponse subject) {
        softly.assertThat(subject.isSuccess())
                .as("Asserting that success value is true")
                .isEqualTo(true);

        softly.assertThat(subject.getCode())
                .as("Asserting code value")
                .isEqualTo(105);

        softly.assertThat(subject.getComment())
                .as("Asserting comment value")
                .isEqualTo("Параметры запроса переданы не в полном объеме");

        Collection<MarschrouteDeliveryCity> dataCollection = subject.getData();
        softly.assertThat(dataCollection)
                .as("Asserting that data has 1 item only")
                .hasSize(1);

        MarschrouteDeliveryCity data = dataCollection.iterator().next();

        softly.assertThat(data)
                .as("Asserting that data is not null")
                .isNotNull();

        softly.assertThat(data.getKladr())
                .as("Asserting kladr value")
                .isEqualTo("52000001000");

        softly.assertThat(data.getDeliveryOptions())
                .as("Asserting that there are 3 delivery options")
                .hasSize(3);
    }
}
