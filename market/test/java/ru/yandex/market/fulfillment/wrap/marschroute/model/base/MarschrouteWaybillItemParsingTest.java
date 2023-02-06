package ru.yandex.market.fulfillment.wrap.marschroute.model.base;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteWaybillItemParsingTest extends MarschrouteJsonParsingTest<MarschrouteWaybillItem> {

    MarschrouteWaybillItemParsingTest() {
        super(MarschrouteWaybillItem.class, "model/base/marschroute_waybill_item.json");
    }
}
