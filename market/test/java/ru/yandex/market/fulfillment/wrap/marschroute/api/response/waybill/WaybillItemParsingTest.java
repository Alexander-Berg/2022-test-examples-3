package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class WaybillItemParsingTest extends MarschrouteJsonParsingTest<WaybillItem> {

    WaybillItemParsingTest() {
        super(WaybillItem.class, "marschroute/api/waybill/item.json");
    }
}
