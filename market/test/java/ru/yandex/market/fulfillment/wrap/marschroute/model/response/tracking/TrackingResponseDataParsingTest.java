package ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class TrackingResponseDataParsingTest extends MarschrouteJsonParsingTest<TrackingResponseData> {

    TrackingResponseDataParsingTest() {
        super(TrackingResponseData.class, "order_tracking/data.json");
    }
}
