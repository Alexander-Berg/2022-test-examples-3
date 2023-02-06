package ru.yandex.market.fulfillment.stockstorage.cqrs.handler.command;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.client.entity.request.Source;
import ru.yandex.market.fulfillment.stockstorage.domain.converter.SSEntitiesConverter;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.StocksState;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.DataValidationException;
import ru.yandex.market.fulfillment.stockstorage.repository.JdbcSkuSyncAuditRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuAdditionalParamsRepository;
import ru.yandex.market.fulfillment.stockstorage.service.SkuJpaLockService;
import ru.yandex.market.fulfillment.stockstorage.service.audit.SkuEventAuditService;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeAnyStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.SkuService;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.StockService;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.StockUpdatesChecker;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.queue.PushStocksEventExecutionQueueProducer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.DefaultStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.validator.StockUpdateValidator;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MultipleStocksUpdatingServiceTest {

    private static final int DEFAULT_WAREHOUSE_ID = 1;
    private static final Source DEFAULT_SOURCE = Source.of(DEFAULT_WAREHOUSE_ID);

    private final StockUpdatingStrategy stockUpdatingStrategy = mock(StockUpdatingStrategy.class);
    private final StockUpdateValidator stockUpdateValidator = new StockUpdateValidator();
    private final StockUpdateValidator stockUpdateValidatorSpy = spy(stockUpdateValidator);
    private final StockService stockService = mock(StockService.class);
    private final SkuService skuService = mock(SkuService.class);
    private final SkuJpaLockService skuJpaLockService = mock(SkuJpaLockService.class);
    private final StockUpdatesChecker updatesChecker = mock(StockUpdatesChecker.class);
    private final SkuEventAuditService skuEventAuditService = mock(SkuEventAuditService.class);
    private final TransactionTemplate transactionTemplate = mockTransactionTemplate();
    private final JdbcSkuSyncAuditRepository jdbcSkuSyncAuditRepository = mock(JdbcSkuSyncAuditRepository.class);
    private final SkuAdditionalParamsRepository skuAdditionalParamsRepository =
            mock(SkuAdditionalParamsRepository.class);
    private final SkuChangeAvailabilityMessageProducer
            skuChangeAvailabilityMessageProducer = mock(SkuChangeAvailabilityMessageProducer.class);
    private final SkuChangeStocksAmountMessageProducer
            skuChangeStocksAmountMessageProducer = mock(SkuChangeStocksAmountMessageProducer.class);
    private final SkuChangeAnyStocksAmountMessageProducer
            skuChangeAnyStocksAmountMessageProducer = mock(SkuChangeAnyStocksAmountMessageProducer.class);
    private final PushStocksEventExecutionQueueProducer
            pushStocksEventExecutionQueueProducer = mock(PushStocksEventExecutionQueueProducer.class);
    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1), ZoneId.of("Europe/Moscow"));

    private final StockUpdatingStrategy multipleStocksUpdatingService =
            new DefaultStockUpdatingStrategy(
                    stockService,
                    skuService,
                    skuJpaLockService,
                    updatesChecker,
                    stockUpdateValidatorSpy,
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

    private static ItemStocks makeItemStock(String s, long l, String a) {
        UnitId unitId = new UnitId(s, l, a);

        List<Stock> stocks =
                ImmutableList.of(new Stock(StockType.FIT, 0, DateTime.fromLocalDateTime(LocalDateTime.now())));
        ResourceId warehouse = ResourceId.builder()
                .setPartnerId("1")
                .setYandexId("1")
                .build();
        return new ItemStocks(unitId, warehouse, stocks);
    }

    private ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId getBStockUnitId() {
        return new ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId("b", 1L, 1);
    }

    private ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId getUnitIdByItemStock(ItemStocks stock) {
        ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId unitId =
                new ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId();
        unitId.setSku(stock.getUnitId().getArticle());
        unitId.setVendorId(stock.getUnitId().getVendorId());
        unitId.setWarehouseId(Integer.valueOf(stock.getWarehouseId().getYandexId()));
        return unitId;
    }

    private Sku getSkuByItemStock(ItemStocks stock) {
        Sku sku = new Sku();
        ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId unitId = getUnitIdByItemStock(stock);
        sku.setUnitId(unitId);
        sku.setFfAvailable(0);
        return sku;
    }

    private Sku setSkuNotUpdatable(Sku sku) {
        sku.setUpdatable(false);
        return sku;
    }

    private static StocksState makeStocksState(String sku, long vendorId, int warehouseId) {
        ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId unitId
                = new ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId(
                sku, vendorId, warehouseId
        );

        Map<ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType, StockAmount> stockAmounts =
                ImmutableMap.of(
                        ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.FIT,
                        new StockAmount(0, LocalDateTime.now())
                );
        return new StocksState(unitId, stockAmounts, 0);
    }

    private Sku getSkuByStocksState(StocksState stock) {
        Sku sku = new Sku();
        ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId unitId = getUnitIdByStocksState(stock);
        sku.setUnitId(unitId);
        sku.setFfAvailable(0);
        return sku;
    }

    private ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId getUnitIdByStocksState(StocksState stock) {
        ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId unitId =
                new ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId();
        unitId.setSku(stock.getUnitId().getSku());
        unitId.setVendorId(stock.getUnitId().getVendorId());
        unitId.setWarehouseId(stock.getUnitId().getWarehouseId());
        return unitId;
    }

    @BeforeEach
    public void setUp() {
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.FULL_SYNC_THREAD_COUNT)).thenReturn(10);
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.FULL_SYNC_STOCK_SAVE_BATCH_SIZE))
                .thenReturn(1);
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.PUSH_STOCK_SAVE_BATCH_SIZE))
                .thenReturn(1);
    }

    @Test
    public void successfulPushing() {
        StocksState aStock = makeStocksState("", 1L, 1);
        StocksState bStock = makeStocksState("", 1L, 2);
        StocksState cStock = makeStocksState("", 1L, 3);
        List<StocksState> stocksStates = Arrays.asList(
                aStock,
                bStock,
                cStock
        );

        when(skuService.findSkusByUnitIds(any())).thenReturn(List.of());
        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());

        multipleStocksUpdatingService.pushStock(stocksStates);

        verify(updatesChecker, atLeastOnce()).getStockRefillDate(any(), any());
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStates.get(0).getUnitId())));
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStates.get(1).getUnitId())));
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStates.get(2).getUnitId())));
    }

    @Test
    public void pushStockAsyncIsSuccessful() {
        ItemStocks stock1 = makeItemStock("1", 1L, "a");
        ItemStocks stock2 = makeItemStock("2", 2L, "b");
        List<ItemStocks> itemStocks = List.of(stock1, stock2);

        multipleStocksUpdatingService.pushStock(DEFAULT_SOURCE, itemStocks);

        verify(stockUpdateValidatorSpy).validate(DEFAULT_SOURCE, itemStocks);
        verify(pushStocksEventExecutionQueueProducer).push(argThat(it -> it.size() == itemStocks.size()));
    }

    @Test
    public void pushOnlyUpdatable() {
        StocksState aStock = makeStocksState("", 1L, 1);
        StocksState bStock = makeStocksState("", 1L, 2);
        StocksState cStock = makeStocksState("", 1L, 3);
        List<StocksState> stocksStates = Arrays.asList(
                aStock,
                bStock,
                cStock
        );

        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByStocksState(aStock))))
                .thenReturn(List.of(getSkuByStocksState(aStock)));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByStocksState(bStock))))
                .thenReturn(List.of(setSkuNotUpdatable(getSkuByStocksState(bStock))));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByStocksState(cStock))))
                .thenReturn(List.of(getSkuByStocksState(cStock)));

        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());

        multipleStocksUpdatingService.pushStock(stocksStates);

        verify(skuService).saveAll(List.of(getSkuByStocksState(aStock)));
        verify(skuService).saveAll(List.of(getSkuByStocksState(cStock)));

        verify(skuService, never()).saveAll(List.of(getSkuByStocksState(bStock)));
    }

    @Test
    public void syncOnlyUpdatable() {

        ItemStocks aStock = makeItemStock("", 1L, "a");
        ItemStocks bStock = makeItemStock("", 1L, "b");
        ItemStocks cStock = makeItemStock("", 1L, "c");
        List<ItemStocks> itemStocks = Arrays.asList(
                aStock,
                bStock,
                cStock
        );

        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(aStock))))
                .thenReturn(List.of(getSkuByItemStock(aStock)));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(bStock))))
                .thenReturn(List.of(setSkuNotUpdatable(getSkuByItemStock(bStock))));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(cStock))))
                .thenReturn(List.of(getSkuByItemStock(cStock)));

        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());

        multipleStocksUpdatingService.syncStock(DEFAULT_SOURCE, itemStocks);
        verify(skuService).saveAll(List.of(getSkuByItemStock(aStock)));
        verify(skuService).saveAll(List.of(getSkuByItemStock(cStock)));

        verify(skuService, never()).saveAll(List.of(getSkuByItemStock(bStock)));
    }

    @Test
    public void successfulSyncing() {
        ItemStocks aStock = makeItemStock("", 1L, "a");
        ItemStocks bStock = makeItemStock("", 1L, "b");
        ItemStocks cStock = makeItemStock("", 1L, "c");
        List<ItemStocks> itemStocks = Arrays.asList(
                aStock,
                bStock,
                cStock
        );
        List<StocksState> stocksStateList = SSEntitiesConverter.convert(DEFAULT_SOURCE, itemStocks);

        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(aStock))))
                .thenReturn(List.of(getSkuByItemStock(aStock)));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(bStock))))
                .thenReturn(List.of(setSkuNotUpdatable(getSkuByItemStock(bStock))));
        when(skuService.findSkusByUnitIds(Collections.singleton(getUnitIdByItemStock(cStock))))
                .thenReturn(List.of(getSkuByItemStock(cStock)));

        when(updatesChecker.getStockRefillDate(any(), any())).thenReturn(Optional.empty());

        multipleStocksUpdatingService.syncStock(DEFAULT_SOURCE, itemStocks);
        Assert.assertEquals(stocksStateList.size(), 3);
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStateList.get(0).getUnitId())));
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStateList.get(1).getUnitId())));
        verify(skuService).findSkusByUnitIds(eq(Collections.singleton(stocksStateList.get(2).getUnitId())));
    }

    @Test
    public void failOnDuplicatesFoundWhilePushing() {
        List<ItemStocks> itemStocks = Arrays.asList(
                makeItemStock("", 1L, "b"),
                makeItemStock("", 1L, "abbcccdddd"),
                makeItemStock("", 1L, "abbcccdddd")
        );

        Assertions.assertThrows(DataValidationException.class,
                () -> multipleStocksUpdatingService.pushStock(DEFAULT_SOURCE, itemStocks));
    }

    @Test
    public void failOnDuplicatesFoundWhileSyncing() {
        List<ItemStocks> itemStocks = Arrays.asList(
                makeItemStock("", 1L, "abbcccdddd"),
                makeItemStock("", 1L, "b"),
                makeItemStock("", 1L, "a"),
                makeItemStock("", 1L, "abbcccdddd")
        );

        Assertions.assertThrows(DataValidationException.class,
                () -> multipleStocksUpdatingService.syncStock(DEFAULT_SOURCE, itemStocks));
    }

    @Test
    public void exceptionThrowingAfterPushingFailed() {
        List<ItemStocks> itemStocks = Arrays.asList(
                makeItemStock("", 1L, "b"),
                makeItemStock("", 1L, "c")
        );
        doThrow(VerySpecificException.class)
                .when(stockUpdatingStrategy).pushStock(anyList());
        multipleStocksUpdatingService.pushStock(DEFAULT_SOURCE, itemStocks);
    }

    public static class VerySpecificException extends RuntimeException {

    }

    private TransactionTemplate mockTransactionTemplate() {
        return new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                return action.doInTransaction(null);
            }
        };
    }
}
