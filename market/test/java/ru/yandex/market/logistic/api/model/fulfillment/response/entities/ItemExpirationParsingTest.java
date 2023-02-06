package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemExpirationParsingTest extends ParsingTest<ItemExpiration> {

    public ItemExpirationParsingTest() {
        super(ItemExpiration.class, "fixture/response/entities/item_expiration.xml");
    }
}
