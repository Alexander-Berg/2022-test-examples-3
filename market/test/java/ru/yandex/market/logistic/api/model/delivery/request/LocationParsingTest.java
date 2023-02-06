package ru.yandex.market.logistic.api.model.delivery.request;

import java.math.BigDecimal;

import org.assertj.core.data.Offset;

import ru.yandex.market.logistic.api.model.delivery.Location;
import ru.yandex.market.logistic.api.utils.ParsingTest;

public class LocationParsingTest extends ParsingTest<Location> {

    public LocationParsingTest() {
        super(Location.class, "fixture/entities/location.xml");
    }

    @Override
    protected void performAdditionalAssertions(Location location) {
        assertions().assertThat(location.getCountry())
            .as("Asserting string value")
            .isEqualTo("Россия");
        assertions().assertThat(location.getLat())
            .as("Asserting bignum value")
            .isCloseTo(BigDecimal.valueOf(55.753960), Offset.offset(new BigDecimal(0.0001)));
    }
}
