package ru.yandex.market.fulfillment.stockstorage.service.stocks;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.StocksState;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.SkuAdditionalParams;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuSyncAuditRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuAdditionalParamsRepository;
import ru.yandex.market.fulfillment.stockstorage.service.SkuJpaLockService;
import ru.yandex.market.fulfillment.stockstorage.service.audit.SkuEventAuditService;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeAnyStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.queue.PushStocksEventExecutionQueueProducer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.DefaultStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.validator.StockUpdateValidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.FIT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.PREORDER;

@ExtendWith(SpringExtension.class)
public class StockUpdatingStrategyTest {

    @Mock
    private StockService stockService;
    @Mock
    private SkuService skuService;
    @Mock
    private SkuJpaLockService skuJpaLockService;
    @Mock
    private StockUpdatesChecker updatesChecker;
    @Mock
    private SkuEventAuditService skuEventAuditService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private JdbcSkuSyncAuditRepository jdbcSkuSyncAuditRepository;
    @Mock
    private SkuAdditionalParamsRepository skuAdditionalParamsRepository;
    @Mock
    private SkuChangeAvailabilityMessageProducer skuChangeAvailabilityMessageProducer;
    @Mock
    private SkuChangeStocksAmountMessageProducer skuChangeStocksAmountMessageProducer;
    @Mock
    private SkuChangeAnyStocksAmountMessageProducer skuChangeAnyStocksAmountMessageProducer;
    @Mock
    private PushStocksEventExecutionQueueProducer pushStocksEventExecutionQueueProducer;
    @Mock
    private SystemPropertyService systemPropertyService;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1), ZoneId.of("Europe/Moscow"));

    private StockUpdatingStrategy service;

    @BeforeEach
    public void setUp() {
        service = new DefaultStockUpdatingStrategy(
                stockService,
                skuService,
                skuJpaLockService,
                updatesChecker,
                Mockito.mock(StockUpdateValidator.class),
                skuEventAuditService,
                transactionTemplate,
                transactionTemplate,
                jdbcSkuSyncAuditRepository,
                skuAdditionalParamsRepository,
                skuChangeAvailabilityMessageProducer,
                skuChangeStocksAmountMessageProducer,
                skuChangeAnyStocksAmountMessageProducer,
                pushStocksEventExecutionQueueProducer,
                systemPropertyService,
                clock,
                false
        );

        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.FULL_SYNC_THREAD_COUNT)).thenReturn(10);
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.PUSH_STOCK_SAVE_BATCH_SIZE))
                .thenReturn(10);
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.ALLOW_DISAPPEAR_DATE_WRITING)).thenReturn(true);
        when(transactionTemplate.execute(any()))
                .then(e ->
                        ((TransactionCallback) e.getArgument(0)).doInTransaction(null)
                );
    }

    @Test
    public void pushStockNewSkuCreated() {
        UnitId unitId = new UnitId("SKU", 100L, 100);
        LocalDateTime now = LocalDateTime.now();
        Map<StockType, StockAmount> stockToUpdate = ImmutableMap.of(FIT, new StockAmount(10, now));

        StocksState stocksState = createStock(unitId, stockToUpdate);
        when(skuService.findSku(unitId)).thenReturn(Optional.empty());
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.of(now));
        when(updatesChecker.getStockDisappearDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.isPreorderAvailabilityChanged(any(), any())).thenReturn(true);
        when(updatesChecker.isAvailabilityChanged(any(), any())).thenReturn(true);


        service.pushStock(List.of(stocksState));

        verify(skuEventAuditService).logNewStockAmountPushed(any());
        verify(skuEventAuditService, never()).logNewStockAmountSynced(any());
        verify(skuEventAuditService).logSkuCreated(any(), any());

        verify(skuJpaLockService).lockByUnitIds(Collections.singleton(unitId));
        verify(skuService).saveAll(any());
        verify(stockService).updateStockState(any());

    }

    private StocksState createStock(UnitId unitId, Map<StockType, StockAmount> stockToUpdate) {
        return new StocksState(
                unitId,
                stockToUpdate,
                10
        );
    }

    @Test
    public void pushStockNewExistingSkuFound() {
        UnitId unitId = new UnitId("SKU", 100L, 100);
        Sku sku = new Sku() {{
            id = 1L;
        }};
        sku.setUnitId(unitId);

        LocalDateTime now = LocalDateTime.now();
        Map<StockType, StockAmount> stockToUpdate = ImmutableMap.of(
                FIT, new StockAmount(10, now));

        StocksState stocksState = createStock(unitId, stockToUpdate);

        when(skuService.findSku(unitId)).thenReturn(Optional.of(sku));
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.getStockDisappearDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.isPreorderAvailabilityChanged(any(), any())).thenReturn(false);
        when(updatesChecker.isAvailabilityChanged(any(), any())).thenReturn(false);


        service.pushStock(List.of(stocksState));

        verify(skuEventAuditService).logNewStockAmountPushed(any());
        verify(skuEventAuditService, never()).logNewStockAmountSynced(any());

        verify(skuJpaLockService).lockByUnitIds(Collections.singleton(unitId));
        verify(skuService).saveAll(any());
        verify(stockService).updateStockState(any());

    }

    @Test
    public void pushPreorderStockNewExistingSkuFound() {
        UnitId unitId = new UnitId("SKU", 100L, 100);
        Sku sku = new Sku() {{
            id = 1L;
        }};
        sku.setUnitId(unitId);

        LocalDateTime now = LocalDateTime.now();
        Map<StockType, StockAmount> stockToUpdate = ImmutableMap.of(
                PREORDER, new StockAmount(10, now));

        StocksState stocksState = createStock(unitId, stockToUpdate);

        when(skuService.findSku(unitId)).thenReturn(Optional.of(sku));
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.getStockDisappearDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.isPreorderAvailabilityChanged(any(), any())).thenReturn(true);
        when(updatesChecker.isAvailabilityChanged(any(), any())).thenReturn(false);


        service.pushStock(List.of(stocksState));

        verify(skuEventAuditService).logNewStockAmountPushed(any());
        verify(skuEventAuditService, never()).logNewStockAmountSynced(any());

        verify(skuJpaLockService).lockByUnitIds(Collections.singleton(unitId));
        verify(skuService).saveAll(any());
        verify(stockService).updateStockState(any());

    }

    @Test
    public void pushStockNewExistingSkuFoundAndStockRefilled() {
        UnitId unitId = new UnitId("SKU", 100L, 100);
        Sku sku = new Sku() {{
            id = 1L;
        }};
        sku.setUnitId(unitId);

        LocalDateTime now = LocalDateTime.now();
        Map<StockType, StockAmount> stockToUpdate = ImmutableMap.of(
                FIT, new StockAmount(10, now));

        StocksState stocksState = createStock(unitId, stockToUpdate);

        when(skuService.findSkusByUnitIds(Collections.singleton(unitId))).thenReturn(List.of(sku));
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.of(now));
        when(updatesChecker.getStockDisappearDate(any(), any())).thenReturn(Optional.empty());
        when(updatesChecker.isPreorderAvailabilityChanged(any(), any())).thenReturn(false);
        when(updatesChecker.isAvailabilityChanged(any(), any())).thenReturn(false);


        service.pushStock(List.of(stocksState));

        Assert.assertEquals("If refilled date changed, we must update refilled field in sku", now, sku.getRefilled());

        verify(skuEventAuditService).logNewStockAmountPushed(any());
        verify(skuEventAuditService, never()).logNewStockAmountSynced(any());

        verify(skuJpaLockService).lockByUnitIds(Collections.singleton(unitId));
        verify(skuService).saveAll(any());
        verify(stockService).updateStockState(any());
    }

    @Test
    public void logNewStockAmountSynced() {
        UnitId unitId = new UnitId("SKU", 100L, 100);
        Sku sku = new Sku() {{
            id = 1L;
        }};
        sku.setUnitId(unitId);

        LocalDateTime now = LocalDateTime.now();
        Map<StockType, StockAmount> stockToUpdate = ImmutableMap.of(
                FIT, new StockAmount(10, now));

        StocksState stocksState = createStock(unitId, stockToUpdate);

        when(skuService.findSkusByUnitIds(Collections.singleton(unitId))).thenReturn(List.of(sku));
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.of(now));
        when(updatesChecker.getStockDisappearDate(any(), any())).thenReturn(Optional.of(LocalDateTime.now()));
        when(updatesChecker.isPreorderAvailabilityChanged(any(), any())).thenReturn(false);
        when(updatesChecker.isAvailabilityChanged(any(), any())).thenReturn(false);
        when(skuAdditionalParamsRepository.findBySkuId(anyLong()))
                .thenReturn(Optional.of(new SkuAdditionalParams(sku.getId(), null)));

        service.syncStock(List.of(stocksState), Collections.emptyMap());

        verify(skuEventAuditService).logNewStockAmountSynced(any());
        verify(skuEventAuditService, never()).logNewStockAmountPushed(any());
        verify(skuAdditionalParamsRepository).saveAll(any());
    }

}
