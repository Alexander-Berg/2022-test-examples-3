package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemTrueLifetimeParsingTest extends ParsingTest<Item> {

    public ItemTrueLifetimeParsingTest() {
        super(Item.class, "fixture/entities/item_true_lifetime.xml");
    }
}
