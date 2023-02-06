package ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockAmount;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
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
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.validator.StockUpdateValidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class CheckUpdateConditionTest {

    private static final int MARSCHROUTE_WAREHOUSE_ID = 145;

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

    private AbstractStockUpdatingStrategy service;

    @BeforeEach
    public void setUp() {
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.FULL_SYNC_THREAD_COUNT)).thenReturn(10);
        when(transactionTemplate.execute(any()))
                .then(e ->
                        ((TransactionCallback) e.getArgument(0)).doInTransaction(null)
                );
    }

    @Test
    public void shouldReturnTrueIfDefaultStrategyAndAmountAndDateHaveNotChanged() {
        service = getDefaultStrategy();
        Stock stock = createStock(MARSCHROUTE_WAREHOUSE_ID, LocalDateTime.MIN, 1);
        StockAmount stockAmount = new StockAmount(1, LocalDateTime.MIN);

        assertTrue(service.stockHasNoChanges(stock, stockAmount));
    }

    @Test
    public void shouldReturnFalseIfDefaultStrategyAndAmountHasChanged() {
        service = getDefaultStrategy();
        Stock stock = createStock(MARSCHROUTE_WAREHOUSE_ID, LocalDateTime.MIN, 2);
        StockAmount stockAmount = new StockAmount(1, LocalDateTime.MIN);

        assertFalse(service.stockHasNoChanges(stock, stockAmount));
    }

    @Test
    public void shouldReturnFalseIfDefaultStrategyAndDateHasChanged() {
        service = getDefaultStrategy();
        Stock stock = createStock(MARSCHROUTE_WAREHOUSE_ID, LocalDateTime.MAX, 1);
        StockAmount stockAmount = new StockAmount(1, LocalDateTime.MIN);

        assertFalse(service.stockHasNoChanges(stock, stockAmount));
    }

    @Test
    public void shouldReturnTrueIfCheckOnlyAmountStrategyAndAmountHasNotChanged() {
        service = getCheckOnlyAmountStrategy();
        Stock stock = createStock(MARSCHROUTE_WAREHOUSE_ID + 1, LocalDateTime.MIN, 1);
        StockAmount stockAmount = new StockAmount(1, LocalDateTime.MAX);

        assertTrue(service.stockHasNoChanges(stock, stockAmount));
    }

    @Test
    public void shouldReturnFalseIfCheckOnlyAmountStrategyAndAmountHasChanged() {
        service = getCheckOnlyAmountStrategy();
        Stock stock = createStock(MARSCHROUTE_WAREHOUSE_ID + 1, LocalDateTime.MIN, 2);
        StockAmount stockAmount = new StockAmount(1, LocalDateTime.MAX);

        assertFalse(service.stockHasNoChanges(stock, stockAmount));
    }

    private DefaultStockUpdatingStrategy getDefaultStrategy() {
        return new DefaultStockUpdatingStrategy(
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
    }

    private CheckOnlyStockAmountStrategy getCheckOnlyAmountStrategy() {
        return new CheckOnlyStockAmountStrategy(
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
    }

    private Stock createStock(int warehouseId, LocalDateTime dateTimeOfUpdated, int amount) {
        UnitId unitId = new UnitId();
        unitId.setWarehouseId(warehouseId);
        Sku sku = new Sku();
        sku.setUnitId(unitId);
        Stock stock = new Stock();
        stock.setSku(sku);
        stock.setFfUpdated(dateTimeOfUpdated);
        stock.setAmount(amount);
        return stock;
    }
}
