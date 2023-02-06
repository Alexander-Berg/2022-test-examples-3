package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class CancelWaybillResponseParsingTest extends MarschrouteJsonParsingTest<CancelWaybillResponse> {

    CancelWaybillResponseParsingTest() {
        super(CancelWaybillResponse.class, "marschroute/api/waybill/cancel_response.json");
    }
}
