package ru.yandex.market.monitoring.stocks;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link StocksUpdateMonitoringExecutor}.
 *
 * @author Zvorygin Andrey don-dron@yandex-team.ru
 */
public class StocksUpdateMonitoringExecutorTest extends FunctionalTest {

    @Spy
    @Autowired
    private StocksUpdateMonitoringHelpService stocksUpdateMonitoringHelpService;

    private StocksUpdateMonitoringExecutor stocksUpdateMonitoringExecutor;

    private static final long LAST_UPDATE = 0L;
    private static final long TEST_BUSINESS_ID = 1L;
    private static final long TEST_SUPPLIER_ID = 2L;
    private static final long TEST_FEED_ID = 3L;
    private static final long TEST_WAREHOUSE_ID = 4L;
    private static final String TEST_URL = "testUrl";
    private static final String TEST_OFFER_ID = "testOffer";

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        stocksUpdateMonitoringExecutor = new StocksUpdateMonitoringExecutor(stocksUpdateMonitoringHelpService);
    }

    @DisplayName("Проверяем получение ошибки при НЕ обновлении оффера и вызов метода на обновление фида.")
    @Test
    void doJobFailUpdate() {
        Mockito.doReturn(Optional.of(TEST_URL))
                .when(stocksUpdateMonitoringHelpService)
                .getUrlFileStock();
        Mockito.doReturn(Optional.of(TEST_URL))
                .when(stocksUpdateMonitoringHelpService)
                .getUrlFileStockAlter();
        Mockito.doReturn(Optional.of(TEST_FEED_ID))
                .when(stocksUpdateMonitoringHelpService)
                .getFeedId();
        Mockito.doReturn(Optional.of(TEST_WAREHOUSE_ID))
                .when(stocksUpdateMonitoringHelpService)
                .getWarehouseId();
        Mockito.doReturn(TEST_SUPPLIER_ID)
                .when(stocksUpdateMonitoringHelpService)
                .getSupplierId(Mockito.anyLong());
        Mockito.doReturn(Optional.of(TEST_OFFER_ID))
                .when(stocksUpdateMonitoringHelpService)
                .getOfferId();
        Mockito.doReturn(TEST_BUSINESS_ID)
                .when(stocksUpdateMonitoringHelpService)
                .getBusinessId(Mockito.anyLong());

        Mockito.doReturn(LAST_UPDATE).when(stocksUpdateMonitoringHelpService)
                .getLastFeedUpdateTimestamp(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> stocksUpdateMonitoringExecutor.doJob(null));

        assertTrue(runtimeException.getMessage().contains("Wrong update timeout for test feed."));
        Mockito.verify(stocksUpdateMonitoringHelpService)
                .updateSuppliersStocks(TEST_SUPPLIER_ID, TEST_URL, TEST_WAREHOUSE_ID);
    }
}
