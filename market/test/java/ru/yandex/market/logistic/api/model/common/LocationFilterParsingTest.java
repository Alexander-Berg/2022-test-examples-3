package ru.yandex.market.logistic.api.model.common;

import ru.yandex.market.logistic.api.utils.ParsingTest;

class LocationFilterParsingTest extends ParsingTest<LocationFilter> {

    LocationFilterParsingTest() {
        super(LocationFilter.class, "fixture/entities/location_filter.xml");
    }
}
