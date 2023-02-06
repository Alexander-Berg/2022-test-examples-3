package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class CourierParsingTest extends ParsingTest<Courier> {

    public CourierParsingTest() {
        super(Courier.class, "fixture/entities/courier.xml");
    }

    @Override
    protected void performAdditionalAssertions(Courier courier) {
        assertions().assertThat(courier.getCar())
            .as("Check Car.class proper deserialization")
            .isInstanceOf(Car.class);
        assertions().assertThat(courier.getPersons().get(0))
            .as("Check Person.class proper deserialization")
            .isInstanceOf(Person.class);
    }
}
