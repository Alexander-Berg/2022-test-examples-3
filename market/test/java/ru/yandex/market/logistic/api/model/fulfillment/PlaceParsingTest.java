package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class PlaceParsingTest extends ParsingTest<Place> {

    public PlaceParsingTest() {
        super(Place.class, "fixture/entities/place.xml");
    }
}
