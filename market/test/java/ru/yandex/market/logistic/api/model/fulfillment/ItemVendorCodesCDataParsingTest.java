package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemVendorCodesCDataParsingTest extends ParsingTest<Item> {

    public ItemVendorCodesCDataParsingTest() {
        super(Item.class, "fixture/entities/item_vendor_codes_cdata.xml");
    }
}
