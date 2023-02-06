package ru.yandex.market.fulfillment.wrap.marschroute.api.request.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class CreateInboundRequestParsingTest extends MarschrouteJsonParsingTest<CreateInboundRequest> {

    CreateInboundRequestParsingTest() {
        super(CreateInboundRequest.class, "api/request/waybill/create_inbound_request.json");
    }
}
