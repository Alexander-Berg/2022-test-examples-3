package ru.yandex.market.checkout.pushapi.providers;

import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.client.entity.stock.Stock;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockItem;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockType;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;

public abstract class StocksResponseProvider {

    private StocksResponseProvider() {
        throw new UnsupportedOperationException();
    }

    public static StocksResponse buildResponse(Clock clock) {
        StockItem stockItem = new StockItem();
        stockItem.setType(StockType.FIT);
        stockItem.setUpdatedAt(OffsetDateTime.now(clock));
        stockItem.setCount(1);

        Stock stock = new Stock();
        stock.setSku(StocksRequestProvider.SKU);
        stock.setWarehouseId(StocksRequestProvider.WAREHOUSE_ID);
        stock.setItems(Collections.singletonList(stockItem));

        StocksResponse stocksResponse = new StocksResponse();
        stocksResponse.setSkus(Collections.singletonList(stock));
        return stocksResponse;
    }
}
