package ru.yandex.market.fulfillment.wrap.marschroute.entity;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class DeliveryServiceMetaParsingTest extends MarschrouteJsonParsingTest<DeliveryServiceMeta> {

    DeliveryServiceMetaParsingTest() {
        super(DeliveryServiceMeta.class, "entity/delivery_service_meta.json");
    }
}
