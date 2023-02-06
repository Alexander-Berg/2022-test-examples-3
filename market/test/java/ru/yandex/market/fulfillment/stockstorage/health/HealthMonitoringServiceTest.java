package ru.yandex.market.fulfillment.stockstorage.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.stockstorage.service.health.CrossdockSkuDivergenceChecker;
import ru.yandex.market.fulfillment.stockstorage.service.health.HealthCheckerResultFormatterMonrun;
import ru.yandex.market.fulfillment.stockstorage.service.health.HealthMonitoringServiceImpl;
import ru.yandex.market.fulfillment.stockstorage.service.health.StockFreezeChecker;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.LmsStocksSettingsSyncChecker;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.WarehouseDesyncChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class HealthMonitoringServiceTest {

    private static final String ERROR_MESSAGE = "ERROR";

    @Mock
    private WarehouseDesyncChecker warehouseDesyncChec;
    @Mock
    private StockFreezeChecker stockFreezeChecker;
    @Mock
    private LmsStocksSettingsSyncChecker lmsStocksSettingsSyncChecker;
    @Mock
    private CrossdockSkuDivergenceChecker crossdockSkuDivergenceChecker;
    @Spy
    private HealthCheckerResultFormatterMonrun resultFormatter;
    @InjectMocks
    private HealthMonitoringServiceImpl healthMonitoringService;

    @Test
    public void checkLmsStockSyncWhenOkTest() {
        when(lmsStocksSettingsSyncChecker.check()).thenReturn(CheckResult.OK);
        String status = healthMonitoringService.checkLmsStockSync();
        assertEquals("0;OK", status);
    }

    @Test
    public void checkLmsStockSyncWhenCRITTest() {
        when(lmsStocksSettingsSyncChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, ERROR_MESSAGE));
        String status = healthMonitoringService.checkLmsStockSync();
        assertEquals("2;" + ERROR_MESSAGE, status);
    }

    @Test
    public void checkCrossdockSkuDivergenceWhenOkTest() {
        when(crossdockSkuDivergenceChecker.check()).thenReturn(CheckResult.OK);
        String status = healthMonitoringService.checkCrossdockSkuDivergence();
        assertEquals("0;OK", status);
    }

    @Test
    public void checkCrossdockSkuDivergenceWhenCRITTest() {
        when(crossdockSkuDivergenceChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, ERROR_MESSAGE));
        String status = healthMonitoringService.checkCrossdockSkuDivergence();
        assertEquals("2;" + ERROR_MESSAGE, status);
    }

    @Test
    public void checkWarehousesDesyncWhenOkTest() {
        when(warehouseDesyncChec.check()).thenReturn(CheckResult.OK);
        String status = healthMonitoringService.checkWarehousesDesync();
        assertEquals("0;OK", status);
    }

    @Test
    public void checkWarehousesDesyncWhenCRITTest() {
        when(warehouseDesyncChec.check()).thenReturn(new CheckResult(CheckResult.Level.CRITICAL, ERROR_MESSAGE));
        String status = healthMonitoringService.checkWarehousesDesync();
        assertEquals("2;" + ERROR_MESSAGE, status);
    }

    @Test
    public void checkStockFreezeWhenOkTest() {
        when(stockFreezeChecker.check()).thenReturn(CheckResult.OK);
        String status = healthMonitoringService.checkStockFreeze();
        assertEquals("0;OK", status);
    }

    @Test
    public void checkStockFreezeWhenCRITTest() {
        when(stockFreezeChecker.check()).thenReturn(new CheckResult(CheckResult.Level.CRITICAL, ERROR_MESSAGE));
        String status = healthMonitoringService.checkStockFreeze();
        assertEquals("2;" + ERROR_MESSAGE, status);
    }

}
