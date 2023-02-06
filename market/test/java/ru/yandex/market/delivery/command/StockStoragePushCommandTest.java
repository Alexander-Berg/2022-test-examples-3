package ru.yandex.market.delivery.command;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.supplier.state.StockStorageStateAction;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalRestClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.terminal.TestTerminal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "StockStorageStateListener.before.csv")
public class StockStoragePushCommandTest extends FunctionalTest {
    private static final String FULL_SYNC_JOB = "FullSync";
    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final String STOCK_STORAGE_FF_INTERVALS_PERIOD = "stock.storage.ff.intervals.interval";

    private static final TestTerminal TEST_TERMINAL = new TestTerminal();

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private DeliveryInfoService deliveryInfoService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private EnvironmentService environmentService;

    @Mock
    private StockStorageFFIntervalRestClient intervalRestClient;

    private StockStoragePushCommand stockStoragePushCommand;


    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("refresh-stock-storage-intervals", args, Collections.emptyMap());
    }

    @BeforeEach
    void init() {
        intervalRestClient = mock(StockStorageFFIntervalRestClient.class);
        stockStoragePushCommand =
                new StockStoragePushCommand(new StockStorageStateAction(partnerTypeAwareService, deliveryInfoService,
                        featureService, paramService, environmentService, intervalRestClient));
    }

    @Test
    @DisplayName("Обновление интервалов - обновляем интервалы. поскольку проходим через правило " +
            "STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL (входит ли текущий поставщик в список разрешенных)")
    @DbUnitDataSet(before = "StockStorageStateListener.withoutAllSuppliers.before.csv")
    public void testRefreshStockStorageIntervals() {
        long supplierId = 3L, intervalId = 3L;
        int warehouseId = 103;
        int intervalInMinutes = environmentService.getIntValue(STOCK_STORAGE_FF_INTERVALS_PERIOD, 15);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        CommandInvocation commandInvocation = commandInvocation(String.valueOf(supplierId));
        stockStoragePushCommand.executeCommand(commandInvocation, TEST_TERMINAL);
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
        ArgumentCaptor<FFIntervalDto> captor = ArgumentCaptor.forClass(FFIntervalDto.class);
        verify(intervalRestClient).updateSyncJobInterval(eq(intervalId), captor.capture());
        FFIntervalDto capturedDto = captor.getValue();
        Assertions.assertEquals(intervalInMinutes, capturedDto.getInterval());
    }
}
