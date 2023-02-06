package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.ParsingTest;

import static ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundStatusType.ARRIVED;

public class InboundStatusParsingTest extends ParsingTest<InboundStatus> {

    public InboundStatusParsingTest() {
        super(InboundStatus.class, "fixture/response/entities/inbound_status.xml");
    }

    @Override
    protected void performAdditionalAssertions(InboundStatus inboundStatus) {
        assertions().assertThat(inboundStatus.getInboundId())
            .as("Asserting status code value")
            .isEqualTo(new ResourceId("123", "321"));

        assertions().assertThat(inboundStatus.getDate().getFormattedDate())
            .as("Asserting set date value")
            .isEqualTo("2012-12-21T11:59:59+03:00");

        assertions().assertThat(inboundStatus.getStatusCode())
            .as("Asserting message value")
            .isEqualTo(ARRIVED);
    }
}
