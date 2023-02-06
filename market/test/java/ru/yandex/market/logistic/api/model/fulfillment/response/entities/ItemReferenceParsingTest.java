package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemReferenceParsingTest extends ParsingTest<ItemReference> {

    public ItemReferenceParsingTest() {
        super(ItemReference.class, "fixture/response/entities/item_reference.xml");
    }
}
