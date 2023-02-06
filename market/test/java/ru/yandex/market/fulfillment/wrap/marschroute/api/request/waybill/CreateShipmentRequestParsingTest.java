package ru.yandex.market.fulfillment.wrap.marschroute.api.request.waybill;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class CreateShipmentRequestParsingTest extends MarschrouteJsonParsingTest<CreateShipmentRequest> {

    CreateShipmentRequestParsingTest() {
        super(CreateShipmentRequest.class, "api/request/waybill/create_shipment_request.json");
    }
}
