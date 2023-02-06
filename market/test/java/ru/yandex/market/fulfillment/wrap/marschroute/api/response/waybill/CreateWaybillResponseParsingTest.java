package ru.yandex.market.fulfillment.wrap.marschroute.api.response.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class CreateWaybillResponseParsingTest extends MarschrouteJsonParsingTest<CreateWaybillResponse> {

    CreateWaybillResponseParsingTest() {
        super(CreateWaybillResponse.class, "marschroute/api/waybill/create_response.json");
    }
}
