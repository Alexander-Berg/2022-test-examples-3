package ru.yandex.market.core.supplier.state;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.common.transaction.TransactionListener;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalRestClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageUnexpectedBehaviourRuntimeException;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "StockStorageStateListener.before.csv")
public class StockStorageStateListenerTest extends FunctionalTest {

    private static final String FULL_SYNC_JOB = "FullSync";
    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final String STOCK_STORAGE_FF_INTERVALS_PERIOD = "stock.storage.ff.intervals.interval";

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

    @Autowired
    private StockStorageFFIntervalClient intervalRestClient;

    @Autowired
    private StockStorageStateListener stockStorageStateListener;


    private LocalTransactionListener localTransactionListener() {
        LocalTransactionListener localTransactionListener = mock(LocalTransactionListener.class);
        doAnswer(invocation -> {
            TransactionListener listener = invocation.getArgument(0);
            listener.onBeforeCommit(mock(TransactionStatus.class));
            return null;
        }).when(localTransactionListener).addListener(any());
        return localTransactionListener;
    }

    @BeforeEach
    void init() {
        intervalRestClient = mock(StockStorageFFIntervalRestClient.class);
        stockStorageStateListener = new StockStorageStateListener(
                new StockStorageStateAction(partnerTypeAwareService, deliveryInfoService,
                        featureService, paramService, environmentService, intervalRestClient),
                localTransactionListener());
    }

    @DisplayName("Обновление интервалов - обновляем интервалы. поскольку проходим через правило " +
            "STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL (входит ли текущий поставщик в список разрешенных)")
    @DbUnitDataSet(before = "StockStorageStateListener.withoutAllSuppliers.before.csv")
    @Test
    public void doNotUpdateIntervalZero() {
        long supplierId = 0L, intervalId = 0L;
        int warehouseId = 104;
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
    }

    @DisplayName("Обновление интервалов - не обновляем интервалы, поскольку статус " +
            "поставщика - FAIL")
    @DbUnitDataSet(before = "StockStorageStateListener.withAllSuppliers.before.csv")
    @Test
    public void doNotUpdateIntervalOne() {
        long supplierId = 1L, intervalId = 1L;
        int warehouseId = 104;
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
    }

    @DisplayName("Обновление интервалов - не обновляем интервалы, поскольку отсутствует признак участия поставщика " +
            "в программе работы по прямым поставкам, а также CUTOFF (не CROSSDOCK)" +
            "поставщика - FAIL")
    @DbUnitDataSet(before = "StockStorageStateListener.withAllSuppliers.before.csv")
    @Test
    public void doNotUpdateIntervalTwo() {
        long supplierId = 2L, intervalId = 2L;
        int warehouseId = 104;
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
    }

    @DisplayName("Обновление интервалов - обновляем интервалы, поскольку попадаем " +
            "в STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_LIST")
    @DbUnitDataSet(before = "StockStorageStateListener.withoutAllSuppliers.before.csv")
    @Test
    public void updateInterval() {
        long supplierId = 3L, intervalId = 3L;
        int warehouseId = 103;
        int intervalInMinutes = environmentService.getIntValue(STOCK_STORAGE_FF_INTERVALS_PERIOD, 15);
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
        ArgumentCaptor<FFIntervalDto> captor = ArgumentCaptor.forClass(FFIntervalDto.class);
        verify(intervalRestClient).updateSyncJobInterval(eq(intervalId), captor.capture());
        FFIntervalDto capturedDto = captor.getValue();
        Assertions.assertEquals(intervalInMinutes, capturedDto.getInterval());
    }

    @DisplayName("Обновление интервалов -  создаем интервалы, поскольку попадаем " +
            "в STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_LIST и интервала нет")
    @DbUnitDataSet(before = "StockStorageStateListener.withoutAllSuppliers.before.csv")
    @Test
    public void createInterval() {
        long supplierId = 4L, intervalId = 4L;
        int warehouseId = 104;
        int intervalInMinutes = environmentService.getIntValue(STOCK_STORAGE_FF_INTERVALS_PERIOD, 15);
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        HttpStatusCodeException httpStatusCodeException = mock(HttpStatusCodeException.class);
        doReturn(HttpStatus.NOT_FOUND)
                .when(httpStatusCodeException).getStatusCode();
        StockStorageUnexpectedBehaviourRuntimeException stockStorageUnexpectedBehaviourRuntimeException =
                new StockStorageUnexpectedBehaviourRuntimeException(httpStatusCodeException);
        doThrow(stockStorageUnexpectedBehaviourRuntimeException)
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
        ArgumentCaptor<FFIntervalDto> captor = ArgumentCaptor.forClass(FFIntervalDto.class);
        verify(intervalRestClient).createSyncJobInterval(captor.capture());
        FFIntervalDto capturedDto = captor.getValue();
        Assertions.assertNull(capturedDto.getId());
        Assertions.assertEquals(DEFAULT_BATCH_SIZE, capturedDto.getBatchSize());
        Assertions.assertEquals(intervalInMinutes, capturedDto.getInterval());
    }

    @DisplayName("Обновление интервалов - ничего не делаем, поскольку CPA_IS_PARTNER_INTERFACE=false и интервала нет")
    @DbUnitDataSet(before = "StockStorageStateListener.withAllSuppliers.before.csv")
    @Test
    public void doNothing() {
        long supplierId = 5L, intervalId = 5L;
        int warehouseId = 103;
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        HttpStatusCodeException httpStatusCodeException = mock(HttpStatusCodeException.class);
        doReturn(HttpStatus.NOT_FOUND)
                .when(httpStatusCodeException).getStatusCode();
        StockStorageUnexpectedBehaviourRuntimeException stockStorageUnexpectedBehaviourRuntimeException =
                new StockStorageUnexpectedBehaviourRuntimeException(httpStatusCodeException);
        doThrow(stockStorageUnexpectedBehaviourRuntimeException)
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient, times(0)).deleteSyncJobInterval(eq(intervalId));
    }

    @DisplayName("Обновление интервалов - удаляем интервал, поскскольку " +
            "CPA_IS_PARTNER_INTERFACE=false и интервал есть")
    @DbUnitDataSet(before = "StockStorageStateListener.withAllSuppliers.before.csv")
    @Test
    public void deleteInterval() {
        long supplierId = 6L, intervalId = 6L;
        int warehouseId = 104;
        PartnerStateChangedEvent event = new PartnerStateChangedEvent(supplierId);
        doReturn(new FFIntervalDto(intervalId, warehouseId, FULL_SYNC_JOB, 0, true, DEFAULT_BATCH_SIZE))
                .when(intervalRestClient).getSyncJobInterval(FULL_SYNC_JOB, warehouseId);
        stockStorageStateListener.onApplicationEvent(event);
        verify(intervalRestClient, times(0)).updateSyncJobInterval(eq(intervalId), any());
        verify(intervalRestClient, times(0)).createSyncJobInterval(any());
        verify(intervalRestClient).deleteSyncJobInterval(eq(intervalId));
    }
}
