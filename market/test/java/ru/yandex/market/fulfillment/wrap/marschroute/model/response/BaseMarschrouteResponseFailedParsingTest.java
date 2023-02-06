package ru.yandex.market.fulfillment.wrap.marschroute.model.response;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class BaseMarschrouteResponseFailedParsingTest extends MarschrouteJsonParsingTest<BaseMarschrouteResponse> {

    BaseMarschrouteResponseFailedParsingTest() {
        super(BaseMarschrouteResponse.class, "base_marschroute_response_failed.json");
    }
}
