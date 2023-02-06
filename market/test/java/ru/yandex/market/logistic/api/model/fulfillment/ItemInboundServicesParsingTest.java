package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemInboundServicesParsingTest extends ParsingTest<Item> {

    public ItemInboundServicesParsingTest() {
        super(Item.class, "fixture/entities/item_inbound_services.xml");
    }
}
