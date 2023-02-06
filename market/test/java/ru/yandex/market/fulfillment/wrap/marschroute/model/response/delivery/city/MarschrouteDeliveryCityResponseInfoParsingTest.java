package ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteDeliveryCityResponseInfoParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteDeliveryCityResponseInfo> {

    MarschrouteDeliveryCityResponseInfoParsingTest() {
        super(MarschrouteDeliveryCityResponseInfo.class, "delivery_city/info.json");
    }
}
