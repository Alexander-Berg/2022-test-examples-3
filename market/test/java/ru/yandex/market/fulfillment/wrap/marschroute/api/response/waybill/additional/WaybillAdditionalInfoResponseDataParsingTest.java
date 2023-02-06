package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill.additional;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class WaybillAdditionalInfoResponseDataParsingTest
    extends MarschrouteJsonParsingTest<WaybillAdditionalInfoResponseData> {

    WaybillAdditionalInfoResponseDataParsingTest() {
        super(WaybillAdditionalInfoResponseData.class, "marschroute/api/waybill/additional/data.json");
    }
}
