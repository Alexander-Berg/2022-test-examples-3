package ru.yandex.market.fulfillment.wrap.marschroute.model.request.delivery.city;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteDeliveryCityRequestParsingTest extends MarschrouteJsonParsingTest<MarschrouteDeliveryCityRequest> {

    MarschrouteDeliveryCityRequestParsingTest() {
        super(MarschrouteDeliveryCityRequest.class, "delivery_city_request.json");
    }
}
