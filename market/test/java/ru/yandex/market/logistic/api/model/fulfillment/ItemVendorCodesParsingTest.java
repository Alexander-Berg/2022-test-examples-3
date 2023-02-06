package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemVendorCodesParsingTest extends ParsingTest<Item> {

    public ItemVendorCodesParsingTest() {
        super(Item.class, "fixture/entities/item_vendor_codes.xml");
    }
}
