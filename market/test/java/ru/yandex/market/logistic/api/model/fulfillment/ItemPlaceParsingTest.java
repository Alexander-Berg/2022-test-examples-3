package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemPlaceParsingTest extends ParsingTest<ItemPlace> {

    public ItemPlaceParsingTest() {
        super(ItemPlace.class, "fixture/entities/item_place.xml");
    }
}
