package ru.yandex.market.fulfillment.stockstorage.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezeData;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockFreeze;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.RefreezeDiffsProvider;

import static com.google.common.collect.ImmutableMap.of;

public class RefreezeDiffsProviderTest {

    private final RefreezeDiffsProvider provider = new RefreezeDiffsProvider();

    @Test
    public void sameStocksDifferentQuantity() {
        UnitId s1 = new UnitId("sku1", 1L, 1);
        UnitId s2 = new UnitId("sku2", 2L, 1);

        Map<UnitId, FreezeData> refreezeDiffs = provider.getRefreezeDiffs(
                Arrays.asList(createReserveStock(s1, 10, true),
                        createReserveStock(s2, 5, false)
                ), of(s1, FreezeData.of(5, false), s2, FreezeData.of(10, false))
        );

        Assert.assertEquals(of(
                s1, FreezeData.of(-5, false),
                s2, FreezeData.of(5, false)), refreezeDiffs);
    }

    @Test
    public void differentStocks() {
        UnitId s1 = new UnitId("sku1", 1L, 1);
        UnitId s2 = new UnitId("sku2", 2L, 1);
        UnitId s3 = new UnitId("sku3", 3L, 1);

        List<StockFreeze> storedStocks = Arrays.asList(
                createReserveStock(s1, 10, false),
                createReserveStock(s2, 5, true)
        );
        ImmutableMap<UnitId, FreezeData> newStocks = of(
                s2, FreezeData.of(5, false),
                s3, FreezeData.of(10, true));

        Map<UnitId, FreezeData> refreezeDiffs = provider.getRefreezeDiffs(storedStocks, newStocks);

        Assert.assertEquals(of(
                s1, FreezeData.of(-10, false),
                s2, FreezeData.of(0, false),
                s3, FreezeData.of(10, true)), refreezeDiffs);
    }

    private StockFreeze createReserveStock(UnitId id, int amount, boolean backorder) {
        StockFreeze stockFreeze = new StockFreeze();
        Sku sku = new Sku();
        sku.setUnitId(id);
        Stock stock = new Stock();
        stock.setSku(sku);

        stockFreeze.setAmount(amount);
        stockFreeze.setStock(stock);
        stockFreeze.setBackorder(backorder);
        return stockFreeze;
    }
}
