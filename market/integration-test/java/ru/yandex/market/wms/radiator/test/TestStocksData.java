package ru.yandex.market.wms.radiator.test;

import java.util.List;

import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;

import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.DATE_TIME;

public class TestStocksData {

    public static ItemStocks mSku100_01(String warehouseId) {
        return itemStocks(warehouseId, TestData.M_SKU_100_01);
    }

    public static ItemStocks mSku100_02(String warehouseId) {
        return itemStocks(warehouseId, TestData.M_SKU_100_02);
    }

    public static ItemStocks mSku100_03(String warehouseId) {
        return itemStocks(warehouseId, TestData.M_SKU_100_03);
    }

    public static ItemStocks mSku100_04(String warehouseId) {
        return itemStocks(warehouseId, TestData.M_SKU_100_04);
    }


    private static ItemStocks itemStocks(String warehouseId, String mSku) {
        return new ItemStocks(
                TestData.unitId(mSku),
                new ResourceId(warehouseId, null),
                List.of(
                        new Stock(StockType.FIT, 0, DATE_TIME),
                        new Stock(StockType.QUARANTINE, 0, DATE_TIME),
                        new Stock(StockType.EXPIRED, 0, DATE_TIME),
                        new Stock(StockType.DEFECT, 0, DATE_TIME),
                        new Stock(StockType.SURPLUS, 0, DATE_TIME)
                )
        );
    }
}
