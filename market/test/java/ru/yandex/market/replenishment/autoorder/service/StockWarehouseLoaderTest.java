package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.EventTriggeredLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.StockWarehouseLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.Mockito.when;
public class StockWarehouseLoaderTest extends FunctionalTest {

    @Autowired
    StockWarehouseLoader stockWarehouseLoader;

    @Test
    @DbUnitDataSet(before = "StockWarehouseLoaderTest.before.csv",
            after = "StockWarehouseLoaderTest.after.csv")
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
                    "//home/market/production/mstat/dictionaries/stock_sku/1d/2020-12-12",
            },
            csv = "StockWarehouseLoaderTest_import.yql.csv",
            yqlMock = "StockWarehouseLoaderTest.yql.mock"
    )
    public void importStocks() {
        TimeService timeService = Mockito.mock(TimeService.class);
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2020, 12, 12));
        when(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2020, 12, 12, 6, 0));
        ReflectionTestUtils.setField(stockWarehouseLoader, "timeService", timeService);
        ReflectionTestUtils.setField(stockWarehouseLoader, EventTriggeredLoader.class, "timeService", timeService, TimeService.class);
        stockWarehouseLoader.load();
    }
}
