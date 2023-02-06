package ru.yandex.market.fulfillment.stockstorage.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezeData;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.BackorderNotAllowedException;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.NotEnoughAvailableStockException;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuRepository;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.StockService;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.backorder.BackorderService;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StocksAvailabilityCheckingServiceTest {

    private static final StockType STOCK_TYPE = StockType.FIT;

    private final StockService stockService = mock(StockService.class);
    private final BackorderService backorderService = mock(BackorderService.class);
    private final SkuRepository skuRepository = mock(SkuRepository.class);
    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);

    private final StocksAvailabilityCheckingService checker = new StocksAvailabilityCheckingService(
            skuRepository, stockService,
            backorderService, systemPropertyService
    );

    @BeforeEach
    public void setUp() throws Exception {
        when(backorderService.isBackorderAllowed(anyInt())).thenReturn(false);
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE))
                .thenReturn(false);
        when(skuRepository.findAllByUnitIdIn(any())).thenReturn(List.of());
    }

    @Test
    public void takingLastStocks() throws NotEnoughAvailableStockException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, false),
                s2, FreezeData.of(10, false));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(5)), s2, List.of(createStock(10))));

        Set<UnitId> unitIdsToCreate = checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE);
        assertEquals(unitIdsToCreate, Set.of());
    }

    @Test
    public void noStocksFound() throws NotEnoughAvailableStockException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, false),
                s2, FreezeData.of(10, false));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Collections.emptyMap());

        Assertions.assertThrows(NotEnoughAvailableStockException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE),
                "Failed to freeze stocks. Not enough available items" + s1.getSku() + s2.getSku());

    }

    @Test
    public void oneStockNotFound() throws NotEnoughAvailableStockException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, false),
                s2, FreezeData.of(10, false));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(5))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE),
                "Failed to freeze stocks. Not enough available items" + s2.getSku());
    }

    @Test
    public void oneStockHasNotEnoughItems() throws NotEnoughAvailableStockException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, false),
                s2, FreezeData.of(requiredToMuch, false));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(5)), s2, List.of(createStock(foundNotEnough))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    @Test
    public void combineErrors() throws NotEnoughAvailableStockException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(requiredToMuch, false),
                s2, FreezeData.of(requiredToMuch, false));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(foundNotEnough)), s2, List.of(createStock(foundNotEnough))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    @Test
    public void tryTakingLastStocksWithBackorder() throws BackorderNotAllowedException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, true),
                s2, FreezeData.of(10, true));

        when(backorderService.isBackorderAllowed(anyInt())).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(5)), s2, List.of(createStock(10))));

        checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE);

        when(backorderService.isBackorderAllowed(anyInt())).thenReturn(false);

        Assertions.assertThrows(BackorderNotAllowedException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    @Test
    public void backorderAllowedTest() {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(requiredToMuch, true),
                s2, FreezeData.of(requiredToMuch, true));

        when(backorderService.isBackorderAllowed(anyInt())).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(foundNotEnough)), s2, List.of(createStock(foundNotEnough))));

        Set<UnitId> unitIdsToCreate = checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE);
        assertEquals(unitIdsToCreate, Set.of());
    }

    @Test
    public void backorderNotAllowedTest() throws BackorderNotAllowedException {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(5, true),
                s2, FreezeData.of(10, true));

        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(5)), s2, List.of(createStock(10))));

        Assertions.assertThrows(BackorderNotAllowedException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    @Test
    public void onlyOneWarehouseAllowed() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(requiredToMuch, true),
                s2, FreezeData.of(foundNotEnough, false));

        when(backorderService.isBackorderAllowed(warehouse1)).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(foundNotEnough)), s2, List.of(createStock(foundNotEnough))));

        Set<UnitId> unitIdsToCreate = checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE);
        assertEquals(unitIdsToCreate, Set.of());
    }

    @Test
    public void onlyOneWarehouseAllowedExceptional() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(requiredToMuch, true),
                s2, FreezeData.of(foundNotEnough, false));

        when(backorderService.isBackorderAllowed(warehouse2)).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(foundNotEnough)), s2, List.of(createStock(foundNotEnough))));

        Assertions.assertThrows(BackorderNotAllowedException.class,
                () -> checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE),
                "Backorder not allowed to warehouse: " + warehouse1);
    }

    @Test
    public void stocksHasNotEnoughItemsButOnlyOneWithBackorder() {
        UnitId s1 = unit(1L, "sku1");
        UnitId s2 = unit(2L, "sku2");

        int requiredToMuch = 1000;
        int foundNotEnough = 10;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(foundNotEnough, true),
                s2, FreezeData.of(requiredToMuch, false));

        when(backorderService.isBackorderAllowed(anyInt())).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(requiredToMuch)), s2, List.of(createStock(foundNotEnough))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class, () ->
                checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    /**
     * Вход:
     * - {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE} включена
     * - sku1/wh1 - есть в БД
     * - sku2/wh2 - нет в БД
     * - backorder разрешен на wh2
     * - фриз на sku1(backorder=false) и sku2(backorder=true)
     * <p>
     * Выход:
     * список unitId, содержащий sku2/wh2
     */
    @Test
    public void returnUnitIdsToCreateForOnlyOneSkuWithEnabledProperty() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int amountToFreeze = 1000;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(amountToFreeze, false),
                s2, FreezeData.of(amountToFreeze, true));

        enableCreatingSkuOnBackorderedFreeze();
        when(backorderService.isBackorderAllowed(warehouse2)).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(amountToFreeze))));

        Set<UnitId> unitIdsToCreate = checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE);
        assertEquals(unitIdsToCreate, Set.of(s2));
    }

    /**
     * Вход:
     * - {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE} включена
     * - sku1/wh1 - есть в БД
     * - sku2/wh2 - нет в БД
     * - backorder не разрешен ни на один склад
     * - фриз на sku1(backorder=false) и sku2(backorder=true)
     * <p>
     * Выход:
     * exception, т.к. мы запрещаем фризить без разрешения backorder'а на склад
     */
    @Test
    public void failedCheckWithCreatingSkuBecauseOfNotAllowedBackorder() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int amountToFreeze = 1000;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(amountToFreeze, false),
                s2, FreezeData.of(amountToFreeze, true));

        enableCreatingSkuOnBackorderedFreeze();
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(amountToFreeze))));

        Assertions.assertThrows(BackorderNotAllowedException.class, () ->
                checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    /**
     * Вход:
     * - {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE} включена
     * - sku1/wh1 - есть в БД со стоком
     * - sku2/wh2 - есть в БД, но без стоков
     * - backorder разрешен на wh2
     * - фриз на sku1(backorder=false) и sku2(backorder=true)
     * <p>
     * Выход:
     * exception, т.к. мы разрешаем только создавать отсутсвующие в БД sku, но не дополнять стоки
     */
    @Test
    public void failedCheckWithExistSkuWithoutStocks() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int amountToFreeze = 1000;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(amountToFreeze, false),
                s2, FreezeData.of(amountToFreeze, true));

        enableCreatingSkuOnBackorderedFreeze();
        when(backorderService.isBackorderAllowed(warehouse2)).thenReturn(true);
        when(skuRepository.findAllByUnitIdIn(any())).thenReturn(List.of(Sku.fromUnitId(s2)));
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(amountToFreeze))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class, () ->
                checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    /**
     * Вход:
     * - {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE} включена
     * - sku1/wh1 - есть в БД со стоком
     * - sku2/wh2 - нет в БД
     * - backorder разрешен на wh2
     * - фриз на sku1(backorder=false) и sku2(backorder=false)
     * <p>
     * Выход:
     * exception, т.к. мы запрещаем созадвать sku для не backorder'ных заказов
     */
    @Test
    public void failedCheckWithExistSkuWithStocks() throws BackorderNotAllowedException {

        int warehouse1 = 1;
        int warehouse2 = 2;

        UnitId s1 = unit(1L, "sku1", warehouse1);
        UnitId s2 = unit(2L, "sku2", warehouse2);

        int amountToFreeze = 1000;

        Map<UnitId, FreezeData> stocksToFreeze = of(
                s1, FreezeData.of(amountToFreeze, false),
                s2, FreezeData.of(amountToFreeze, false));

        enableCreatingSkuOnBackorderedFreeze();
        when(backorderService.isBackorderAllowed(warehouse2)).thenReturn(true);
        when(stockService.findEnabledStocksByUnitIds(ImmutableSet.of(s1, s2), STOCK_TYPE))
                .thenReturn(Map.of(s1, List.of(createStock(amountToFreeze))));

        Assertions.assertThrows(NotEnoughAvailableStockException.class, () ->
                checker.checkFreezeStocksAvailable(stocksToFreeze, STOCK_TYPE));
    }

    private void enableCreatingSkuOnBackorderedFreeze() {
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE))
                .thenReturn(true);
    }

    private UnitId unit(long vendor, String sku) {
        return unit(vendor, sku, 1);
    }

    private UnitId unit(long vendor, String sku, int warehouseId) {
        return new UnitId(sku, vendor, warehouseId);
    }

    private Stock createStock(int quantity) {
        Stock stock = new Stock();
        stock.setAmount(quantity);
        return stock;
    }
}
