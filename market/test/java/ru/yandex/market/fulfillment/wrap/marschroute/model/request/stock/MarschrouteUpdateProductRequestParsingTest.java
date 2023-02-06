package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteUpdateProductRequestParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteUpdateProductRequest> {

    MarschrouteUpdateProductRequestParsingTest() {
        super(MarschrouteUpdateProductRequest.class, "update_product_request.json");
    }
}
