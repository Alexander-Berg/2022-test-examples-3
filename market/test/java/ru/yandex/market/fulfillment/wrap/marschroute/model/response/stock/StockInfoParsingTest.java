package ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock;

import ru.yandex.market.fulfillment.wrap.marschroute.MarschrouteJsonParsingTest;

class StockInfoParsingTest extends MarschrouteJsonParsingTest<StockInfo> {

    StockInfoParsingTest() {
        super(StockInfo.class, "get_stock/stock_info.json");
    }
}
