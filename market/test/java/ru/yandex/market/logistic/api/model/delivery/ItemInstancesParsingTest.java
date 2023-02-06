package ru.yandex.market.logistic.api.model.delivery;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemInstancesParsingTest extends ParsingTest<ItemInstances> {
    public ItemInstancesParsingTest() {
        super(ItemInstances.class, "fixture/entities/delivery/items_instances.xml");
    }
}
