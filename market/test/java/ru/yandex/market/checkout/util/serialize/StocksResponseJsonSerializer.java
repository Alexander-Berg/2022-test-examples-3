package ru.yandex.market.checkout.util.serialize;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.client.entity.stock.Stock;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockItem;

import java.util.List;
import java.util.Objects;

public abstract class StocksResponseJsonSerializer {
    private StocksResponseJsonSerializer() {
        throw new UnsupportedOperationException();
    }

    public static String serializeJson(StocksResponse stocksResponse) {
        Objects.requireNonNull(stocksResponse);

        JSONObject response = new JSONObject();
        if (stocksResponse.getSkus() != null) {
            JSONArray skus = writeSkus(stocksResponse.getSkus());
            response.put("skus", skus);
        }

        return response.toString();

    }

    private static JSONArray writeSkus(List<Stock> stocks) {
        JSONArray array = new JSONArray();

        for (Stock stock : stocks) {
            array.put(writeStock(stock));
        }

        return array;
    }

    private static JSONObject writeStock(Stock stock) {
        JSONObject result = new JSONObject();
        result.put("sku", stock.getSku());
        result.put("warehouseId", String.valueOf(stock.getWarehouseId()));

        if (CollectionUtils.isNonEmpty(stock.getItems())) {
            result.put("items", writeStockItems(stock.getItems()));
        }

        return result;
    }

    private static JSONArray writeStockItems(List<StockItem> items) {
        JSONArray result = new JSONArray();

        for (StockItem item : items) {
            JSONObject object = new JSONObject();
            object.put("type", item.getType().name());
            object.put("count", String.valueOf(item.getCount()));
            object.put("updatedAt", String.valueOf(item.getUpdatedAt()));
            result.put(object);
        }

        return result;
    }
}
