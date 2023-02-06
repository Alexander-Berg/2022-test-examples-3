package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ConsignmentParsingTest extends ParsingTest<Consignment> {

    public ConsignmentParsingTest() {
        super(Consignment.class, "fixture/entities/consignment.xml");
    }

    @Override
    protected void performAdditionalAssertions(Consignment consignment) {
        assertions().assertThat(consignment.getConsignmentId())
            .as("Asserting consignmentId instance")
            .isInstanceOf(ResourceId.class);

        assertions().assertThat(consignment.getItem())
            .as("Asserting item instance")
            .isInstanceOf(Item.class);

        assertions().assertThat(consignment.getManufacturedDate())
            .as("Asserting dateTime instance")
            .isInstanceOf(DateTime.class);
    }
}
