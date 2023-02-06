package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemNullLifetimeParsingTest extends ParsingTest<Item> {

    public ItemNullLifetimeParsingTest() {
        super(Item.class, "fixture/entities/item_null_lifetime.xml");
    }
}
