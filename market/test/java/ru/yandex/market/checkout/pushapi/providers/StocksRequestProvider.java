package ru.yandex.market.checkout.pushapi.providers;

import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;

import java.util.Collections;

public abstract class StocksRequestProvider {

    public static final long WAREHOUSE_ID = 123456L;
    public static final String SKU = "asdasd";

    private StocksRequestProvider() {
        throw new UnsupportedOperationException();
    }

    public static StocksRequest buildStocksRequest() {
        StocksRequest request = new StocksRequest();
        request.setWarehouseId(WAREHOUSE_ID);
        request.setPartnerWarehouseId(Long.toString(WAREHOUSE_ID));
        request.setSkus(Collections.singletonList(SKU));
        return request;
    }
}
