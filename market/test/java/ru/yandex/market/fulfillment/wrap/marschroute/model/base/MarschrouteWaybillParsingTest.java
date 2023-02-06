package ru.yandex.market.fulfillment.wrap.marschroute.model.base;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteWaybillParsingTest extends MarschrouteJsonParsingTest<MarschrouteWaybill> {

    MarschrouteWaybillParsingTest() {
        super(MarschrouteWaybill.class, "model/base/marschroute_waybill.json");
    }
}
