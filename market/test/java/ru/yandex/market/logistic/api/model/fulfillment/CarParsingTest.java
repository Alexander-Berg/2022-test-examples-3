package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class CarParsingTest extends ParsingTest<Car> {
    public CarParsingTest() {
        super(Car.class, "fixture/entities/car.xml");
    }

    @Override
    protected void performAdditionalAssertions(Car car) {
        assertions().assertThat(car.getNumber())
            .as("Asserting number value")
            .isEqualTo("z777zz77");
        assertions().assertThat(car.getDescription())
            .as("Asserting description value")
            .isEqualTo("bldzhad");
    }
}
