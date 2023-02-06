package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteProductsRequestParsingTest extends MarschrouteJsonParsingTest<MarschrouteProductsRequest> {

    MarschrouteProductsRequestParsingTest() {
        super(MarschrouteProductsRequest.class, "products_request.json");
    }
}
