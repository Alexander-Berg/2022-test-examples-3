package ru.yandex.market.fulfillment.stockstorage.domain.converter;

import java.time.LocalDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;

import static org.junit.Assert.assertEquals;

public class StockConverterTest {

    @Test
    public void singleToStock() {
        Sku sku = getSku();

        Stock fitStock = Stock.createStock(StockType.FIT, sku);
        setStockAmounts(fitStock);

        ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock testEntity =
                StockConverter.toStockDto(fitStock);

        assertEquals(1000, testEntity.getAmount());
        assertEquals(10, testEntity.getFreezeAmount());
        assertEquals(990, testEntity.getAvailableAmount());
        assertEquals("FIT", testEntity.getType());
    }

    @Test
    public void multipleToStock() {
        Sku sku = getSku();

        Stock fitStock = Stock.createStock(StockType.FIT, sku);
        setStockAmounts(fitStock);
        Stock fitStock2 = Stock.createStock(StockType.FIT, sku);
        setStockAmounts(fitStock2);


        List<ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock> testEntities
                = StockConverter.toStockDto(Lists.newArrayList(fitStock, fitStock2));

        for (ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock testEntity : testEntities) {
            assertEquals(1000, testEntity.getAmount());
            assertEquals(10, testEntity.getFreezeAmount());
            assertEquals(990, testEntity.getAvailableAmount());
            assertEquals("FIT", testEntity.getType());
        }
    }

    @Test
    public void toSimpleStock() {
        Sku sku = getSku();

        Stock fitStock = Stock.createStock(StockType.FIT, sku);
        setStockAmounts(fitStock);
        Stock fitStock2 = Stock.createStock(StockType.FIT, sku);
        setStockAmounts(fitStock2);


        List<SimpleStock> testEntities = StockConverter.toSimpleStocks(Lists.newArrayList(fitStock, fitStock2));

        for (SimpleStock testEntity : testEntities) {
            assertEquals(990, testEntity.getQuantity().intValue());
            assertEquals(1L, testEntity.getVendorId().longValue());
            assertEquals(1, testEntity.getWarehouseId().intValue());
            assertEquals("Sku", testEntity.getSku());
        }
    }

    private void setStockAmounts(Stock fitStock) {
        fitStock.setAmount(1000);
        fitStock.setFreezeAmount(10);
        fitStock.setFfUpdated(getLocalDateTime());
    }

    private Sku getSku() {
        Sku sku = new Sku();
        sku.setUnitId(new UnitId("Sku", 1L, 1));
        return sku;
    }

    private LocalDateTime getLocalDateTime() {
        return LocalDateTime.of(2000, 12, 1, 1, 1);
    }

}
