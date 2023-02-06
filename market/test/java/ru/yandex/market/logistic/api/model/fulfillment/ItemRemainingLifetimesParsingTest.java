package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemRemainingLifetimesParsingTest extends ParsingTest<Item> {

    public ItemRemainingLifetimesParsingTest() {
        super(Item.class, "fixture/entities/item_remaining_lifetimes.xml");
    }
}
