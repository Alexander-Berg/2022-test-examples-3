package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class WaybillInfoParsingTest extends MarschrouteJsonParsingTest<WaybillInfo> {

    WaybillInfoParsingTest() {
        super(WaybillInfo.class, "marschroute/api/waybill/info.json");
    }
}
