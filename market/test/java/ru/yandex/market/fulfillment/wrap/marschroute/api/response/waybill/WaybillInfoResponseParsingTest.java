package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class WaybillInfoResponseParsingTest extends MarschrouteJsonParsingTest<WaybillInfoResponse> {

    WaybillInfoResponseParsingTest() {
        super(WaybillInfoResponse.class, "marschroute/api/waybill/info_response.json");
    }
}
