package ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class MarschrouteProductsResponseDataParsingTest
    extends MarschrouteJsonParsingTest<MarschrouteProductsResponseData> {

    MarschrouteProductsResponseDataParsingTest() {
        super(MarschrouteProductsResponseData.class, "get_stock/data.json");
    }
}
