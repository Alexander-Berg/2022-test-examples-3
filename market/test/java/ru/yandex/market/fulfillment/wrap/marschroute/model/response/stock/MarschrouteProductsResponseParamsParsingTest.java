package ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteProductsResponseParamsParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteProductsResponseParams> {

    MarschrouteProductsResponseParamsParsingTest() {
        super(MarschrouteProductsResponseParams.class, "get_stock/params.json");
    }
}
