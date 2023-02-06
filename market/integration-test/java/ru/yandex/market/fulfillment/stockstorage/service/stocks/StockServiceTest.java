package ru.yandex.market.fulfillment.stockstorage.service.stocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;

import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.DEFECT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.EXPIRED;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.FIT;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.QUARANTINE;
import static ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType.SURPLUS;

@DatabaseSetup("classpath:database/states/stock_service/stocks_state.xml")
public class StockServiceTest extends AbstractContextualTest {

    @Autowired
    private StockService stockService;

    /**
     * Среди запрашиваемых UnitId есть SKU у которых есть только FIT стоки
     * Возвратится корректное число доступных FIT стоков
     */
    @Test
    public void getAvailableAmountByUnitIdWithOnlyFitStocks() {
        Map<UnitId, Long> unitIds = onlyFit();
        Map<UnitId, Long> result = stockService.getAvailableAmountByUnitIds(unitIds.keySet());
        Assert.assertEquals(unitIds, result);
    }

    /**
     * Среди запрашиваемых UnitId есть SKU у которого есть стоки FIT и PREODER
     * Для этого UnitId будут учитываться только PREODER стоки
     */
    @Test
    public void getAvailableAmountByUnitIdWithFitAndPreorderStocks() {
        Map<UnitId, Long> unitIds = onlyFit();
        unitIds.put(new UnitId("sku2", 35947L, 147), 190L);
        Map<UnitId, Long> result = stockService.getAvailableAmountByUnitIds(unitIds.keySet());
        Assert.assertEquals(unitIds, result);
    }

    /**
     * Среди запрашиваемых UnitId есть SKU у которого есть стоки FIT, DEFECT и EXPIRED
     * Для этого UnitId будут учитываться только FIT стоки
     */
    @Test
    public void getAvailableAmountByUnitIdWithFitAndNonValidTypes() {
        Map<UnitId, Long> unitIds = onlyFit();
        unitIds.put(new UnitId("sku3", 11111L, 147), 900L);
        Map<UnitId, Long> result = stockService.getAvailableAmountByUnitIds(unitIds.keySet());
        Assert.assertEquals(unitIds, result);
    }

    /**
     * Среди запрашиваемых UnitId есть SKU без FIT и PREORDER стоков.
     * У sku4 есть DEFECT, EXPIRED, QUARANTINE
     * У sku6 есть только SURPLUS
     * Для этих UnitId будет availableAmount = 0
     */
    @Test
    public void getAvailableAmountByUnitIdWithOnlyNonValidTypes() {
        Map<UnitId, Long> unitIds = onlyFit();
        unitIds.put(new UnitId("sku4", 11111L, 147), 0L);
        unitIds.put(new UnitId("sku6", 11111L, 147), 0L);
        Map<UnitId, Long> result = stockService.getAvailableAmountByUnitIds(unitIds.keySet());
        Assert.assertEquals(unitIds, result);
    }

    /**
     * Среди запрашиваемых UnitId есть SKU без FIT и PREORDER стоков.
     * У sku4 есть DEFECT, EXPIRED, QUARANTINE
     * У sku6 есть только SURPLUS
     */
    @Test
    public void getAllStocksByUnitIds() {
        Map<UnitId, List<Stock>> unitIds = getWithAllStocks();
        Map<UnitId, List<Stock>> result = stockService.findStocksByUnitIds(unitIds.keySet());

        List<Stock> expectedStock = unitIds.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        List<Stock> resultStocks = result.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        Assertions.assertThat(resultStocks)
                .usingElementComparatorOnFields("amount", "freezeAmount", "type", "id")
                .containsAll(expectedStock);
    }

    private static Map<UnitId, Long> onlyFit() {
        HashMap<UnitId, Long> unitIds = new HashMap<>();
        unitIds.put(new UnitId("sku1", 35947L, 145), 0L);
        unitIds.put(new UnitId("sku5", 11111L, 147), 900L);
        return unitIds;
    }

    private static Map<UnitId, List<Stock>> getWithAllStocks() {
        HashMap<UnitId, List<Stock>> unitIds = new HashMap<>();
        UnitId sku1 = new UnitId("sku1", 35947L, 145);
        unitIds.put(sku1, List.of(createStock(FIT, 100, 100, 1)));

        UnitId sku4 = new UnitId("sku4", 11111L, 147);
        unitIds.put(sku4, List.of(
                createStock(DEFECT, 1000, 0, 7),
                createStock(EXPIRED, 1000, 10, 8),
                createStock(QUARANTINE, 1000, 30, 9))
        );

        UnitId sku6 = new UnitId("sku6", 11111L, 147);
        unitIds.put(sku6, List.of(createStock(SURPLUS, 1000, 100, 11)));

        UnitId sku5 = new UnitId("sku5", 11111L, 147);
        unitIds.put(sku5, List.of(createStock(FIT, 1000, 100, 10)));

        return unitIds;
    }

    @SneakyThrows
    private static Stock createStock(StockType type, int amount, int freezeAmount, long id) {
        Stock stock = new Stock();
        stock.setType(type);
        stock.setAmount(amount);
        stock.setFreezeAmount(freezeAmount);
        FieldUtils.writeField(stock, "id", id, true);
        return stock;
    }
}
