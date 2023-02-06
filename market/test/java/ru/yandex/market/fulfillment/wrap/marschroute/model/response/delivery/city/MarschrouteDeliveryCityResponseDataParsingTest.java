package ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteDeliveryCityResponseDataParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteDeliveryCity> {

    MarschrouteDeliveryCityResponseDataParsingTest() {
        super(MarschrouteDeliveryCity.class, "delivery_city/data.json");
    }
}
