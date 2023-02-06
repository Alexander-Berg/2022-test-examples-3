package ru.yandex.market.api.partner.controllers.stats.goodsreport;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.StockInfoDTO;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.StockType;
import ru.yandex.market.api.partner.controllers.stats.goodsreport.model.WarehouseStockInfoDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRow;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsRowWarehouse;
import ru.yandex.market.core.partner.fulfillment.yt.SalesDynamicsYtStorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@DbUnitDataSet(before = "GoodsReportStocksServiceTest.before.csv")
public class GoodsReportStocksServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 101L;

    @Autowired
    private GoodsReportStocksService stocksService;

    @Autowired
    private SalesDynamicsYtStorage salesDynamicsYtStorage;


    @BeforeEach
    void setUp() {
        mockYtStorage();
    }

    @Test
    @DisplayName("Проверка получения информации о стоках на складах поставщика")
    void getStocks() {
        Map<String, List<WarehouseStockInfoDTO>> stocks =
                stocksService.getStocks(SUPPLIER_ID, Set.of("someSku", "otherSku", "dynamicSku"));

        assertEquals(stocks.size(), 3);
        List<WarehouseStockInfoDTO> someSku = stocks.get("someSku");
        List<WarehouseStockInfoDTO> otherSku = stocks.get("otherSku");
        List<WarehouseStockInfoDTO> dynamicSku = stocks.get("dynamicSku");

        assertEquals(someSku.size(), 2);
        assertEquals(otherSku.size(), 4);
        assertEquals(dynamicSku.size(), 1);

        checkContainsWarehouses(someSku, List.of(147L, 171L));
        checkContainsWarehouses(otherSku, List.of(147L, 171L, 172L, 555L));
        checkContainsWarehouses(dynamicSku, List.of(172L));

        someSku.forEach(ware -> assertTrue(ware.isNotEmpty()));
        otherSku.forEach(ware -> assertTrue(ware.isNotEmpty()));
        dynamicSku.forEach(ware -> assertTrue(ware.isNotEmpty()));

        checkStocks(
                555L,
                otherSku,
                Map.of(
                        StockType.AVAILABLE, 7L,
                        StockType.FREEZE, 3L,
                        StockType.QUARANTINE, 2L,
                        StockType.EXPIRED, 1L,
                        StockType.DEFECT, 5L,
                        StockType.FIT, 10L
                )
        );

        checkStocks(
                172,
                dynamicSku,
                Map.of(
                        StockType.TRANSIT, 5L,
                        StockType.SUGGEST, 10L
                )
        );
    }

    @Test
    @DisplayName("Проверка получения информации о стоках, когда данных отчета Динамика продаж нет")
    void getStocksWithoutSalesDynamics() {
        doNothing().when(salesDynamicsYtStorage).getSalesDynamicsReport(any(), any(), any());

        Map<String, List<WarehouseStockInfoDTO>> stocks =
                stocksService.getStocks(SUPPLIER_ID, Set.of("someSku", "otherSku", "dynamicSku"));

        assertEquals(stocks.size(), 2);
        List<WarehouseStockInfoDTO> someSku = stocks.get("someSku");
        List<WarehouseStockInfoDTO> otherSku = stocks.get("otherSku");

        checkContainsWarehouses(someSku, List.of(147L, 171L));
        checkContainsWarehouses(otherSku, List.of(172L, 555L));

        checkStocks(
                555L,
                otherSku,
                Map.of(
                        StockType.AVAILABLE, 7L,
                        StockType.FREEZE, 3L,
                        StockType.QUARANTINE, 2L,
                        StockType.EXPIRED, 1L,
                        StockType.DEFECT, 5L,
                        StockType.FIT, 10L
                )
        );
    }

    private void checkContainsWarehouses(List<WarehouseStockInfoDTO> warehouses, List<Long> warehouseIds) {
        assertEquals(warehouseIds.size(), warehouses.size());
        assertNull(
                warehouses.stream()
                        .map(WarehouseStockInfoDTO::getWarehouseId)
                        .filter(id -> !warehouseIds.contains(id))
                        .findAny()
                        .orElse(null));
    }

    private void checkStocks(
            long warehouseId,
            List<WarehouseStockInfoDTO> warehouses,
            Map<StockType, Long> expectedStocks
    ) {
        WarehouseStockInfoDTO warehouse =
                warehouses.stream().filter(ware -> ware.getWarehouseId() == warehouseId).findFirst().orElseThrow();

        Map<StockType, Long> actualStocks =
                warehouse.getStocks()
                        .stream()
                        .collect(Collectors.toMap(StockInfoDTO::getStockType, StockInfoDTO::getCount));

        assertEquals(expectedStocks.size(), actualStocks.size());
        for (Map.Entry<StockType, Long> stockEntry : expectedStocks.entrySet()) {
            assertEquals(actualStocks.get(stockEntry.getKey()), stockEntry.getValue());
        }
        for (Map.Entry<StockType, Long> stockEntry : actualStocks.entrySet()) {
            assertEquals(expectedStocks.get(stockEntry.getKey()), stockEntry.getValue());
        }
    }

    private void mockYtStorage() {
        List<SalesDynamicsRow> rows = List.of(
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("someSku")
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(2)
                                .withDemandDynamicRecWeek2(5)
                                .build())
                        .addWarehouse(171, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(3)
                                .withDemandDynamicRecWeek2(1)
                                .build())
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("otherSku")
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withDemandDynamicRecWeek2(5)
                                .build())
                        .addWarehouse(171, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(3)
                                .withDemandDynamicRecWeek2(1)
                                .build())
                        .addWarehouse(172, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(6)
                                .withDemandDynamicRecWeek2(8)
                                .build())
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("dynamicSku")
                        .addWarehouse(172, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(5)
                                .withDemandDynamicRecWeek2(10)
                                .build())
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("someSku")
                        .build(),
                SalesDynamicsRow.SalesDynamicsReportRowBuilder.aSalesDynamicsReportRow()
                        .withShopSku("excessSku")
                        .addWarehouse(147, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(500)
                                .withDemandDynamicRecWeek2(1000)
                                .build())
                        .addWarehouse(171, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(500)
                                .withDemandDynamicRecWeek2(1000)
                                .build())
                        .addWarehouse(172, SalesDynamicsRowWarehouse.builder()
                                .withTransitSsku(500)
                                .withDemandDynamicRecWeek2(1000)
                                .build())
                        .build()
        );
        doAnswer(invocation -> {
            Consumer<Iterator<SalesDynamicsRow>> consumer = invocation.getArgument(2);
            consumer.accept(rows.iterator());
            return null;
        }).when(salesDynamicsYtStorage).getSalesDynamicsReport(any(), any(), any());
    }
}
