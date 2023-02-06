package ru.yandex.market.core.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.matchers.ShopSkuInfoMatchers;
import ru.yandex.market.core.fulfillment.matchers.StockMatchers;
import ru.yandex.market.core.fulfillment.model.FulfillmentStockFilter;
import ru.yandex.market.core.order.SupplierWarehouseShopSkuKey;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class StockYtDaoTest extends FunctionalTest {

    private static final List<YTreeMapNode> TABLE_SOURCE = List.of(
            YTree.mapBuilder()
                    .key("warehouse_id").value(1)
                    .key("supplier_id").value(1)
                    .key("shop_sku").value("teabag1")
                    .key("actual_date").value("2017-01-01")
                    .key("available").value(10)
                    .key("quarantine").value(20)
                    .key("defect").value(30)
                    .key("freeze").value(4)
                    .key("expired").value(1)
                    .key("utilization").value(0)
                    .key("lifetime").value(-1)
                    .key("warehouse_name").value("warehouse1")
                    .key("sm_length").value(1)
                    .key("sm_width").value(2)
                    .key("sm_height").value(3)
                    .key("mg_weight").value(999000)
                    .key("almost_deadstock_since").value("2017-01-01")
                    .key("deadstock_since").value("2017-01-16")
                    .buildMap(),
            YTree.mapBuilder()
                    .key("warehouse_id").value(2)
                    .key("supplier_id").value(1)
                    .key("shop_sku").value("teabag1")
                    .key("actual_date").value("2017-01-01")
                    .key("available").value(10)
                    .key("quarantine").value(20)
                    .key("defect").value(30)
                    .key("freeze").value(4)
                    .key("expired").value(1)
                    .key("utilization").value(0)
                    .key("lifetime").value(-1)
                    .key("warehouse_name").value("warehouse2")
                    .key("sm_length").value(1)
                    .key("sm_width").value(2)
                    .key("sm_height").value(3)
                    .key("mg_weight").value(999000)
                    .key("almost_deadstock_since").value("2017-01-01")
                    .key("deadstock_since").value("2017-01-16")
                    .buildMap(),
            YTree.mapBuilder()
                    .key("warehouse_id").value(1)
                    .key("supplier_id").value(1)
                    .key("shop_sku").value("pingpong")
                    .key("actual_date").value("2017-01-01")
                    .key("available").value(11)
                    .key("quarantine").value(22)
                    .key("defect").value(33)
                    .key("freeze").value(0)
                    .key("expired").value(4)
                    .key("utilization").value(0)
                    .key("lifetime").value(-1)
                    .key("warehouse_name").value("warehouse1")
                    .key("sm_length").value(1)
                    .key("sm_width").value(2)
                    .key("sm_height").value(3)
                    .key("mg_weight").value(999000)
                    .key("almost_deadstock_since").value("2017-01-01")
                    .key("deadstock_since").value("2017-01-16")
                    .buildMap()
    );

    @Autowired
    private EnvironmentService env;
    private StockYtDao stockYtDao;

    @BeforeEach
    void setUpTableRead() {
        Yt yt = Mockito.mock(Yt.class);
        YtTables tables = Mockito.mock(YtTables.class);

        stockYtDao = new StockYtDao(() -> yt, "//home/non_existent_table_path", env);

        Mockito.when(yt.tables()).thenReturn(tables);

        Mockito.doAnswer(invocation -> {
            Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
            TABLE_SOURCE.forEach(consumer);
            return null;
        }).when(tables)
                .read(Mockito.any(YPath.class), Mockito.any(), Mockito.any(Consumer.class));
    }

    @Test
    void testGetCurrentStocks() {
        final List<SkuStockInfo<SupplierWarehouseShopSkuKey>> currentStocks =
                stockYtDao.getCurrentStocks(new FulfillmentStockFilter(Arrays.asList(2L), 1L));

        assertThat(
                currentStocks,
                contains(ImmutableList.of(
                        MbiMatchers.<SkuStockInfo<SupplierWarehouseShopSkuKey>>newAllOfBuilder()
                                .add(SkuStockInfo::getStock, Matchers.allOf(
                                        StockMatchers.hasWarehouseId(2L),
                                        StockMatchers.hasSupplierId(1L),
                                        StockMatchers.hasShopSku("teabag1"),
                                        StockMatchers.hasDateTime(LocalDateTime.of(2017, 1, 1, 0, 0)),
                                        StockMatchers.hasAvailable(10),
                                        StockMatchers.hasQuarantine(20),
                                        StockMatchers.hasDefect(30),
                                        StockMatchers.hasFreeze(4),
                                        StockMatchers.hasExpired(1),
                                        StockMatchers.hasLifetime(-1)
                                ))
                                .add(SkuStockInfo::getShopSkuInfo, allOf(
                                        ShopSkuInfoMatchers.hasLength(1),
                                        ShopSkuInfoMatchers.hasWidth(2),
                                        ShopSkuInfoMatchers.hasHeight(3),
                                        ShopSkuInfoMatchers.hasWeight(new BigDecimal("0.999"))
                                ))
                                .build()
                ))
        );

    }

    @Test
    void testGetCurrentStocksSummary() {
        final List<SkuStockInfo<SupplierWarehouseShopSkuKey>> currentStocks =
                stockYtDao.getCurrentStocks(new FulfillmentStockFilter(null, 1L));

        assertThat(
                currentStocks,
                contains(ImmutableList.of(
                        MbiMatchers.<SkuStockInfo<SupplierWarehouseShopSkuKey>>newAllOfBuilder()
                                .add(SkuStockInfo::getStock, Matchers.allOf(
                                        StockMatchers.hasWarehouseName("warehouse1"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasWarehouseId(1L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasSupplierId(1L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasShopSku("teabag1"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDateTime(LocalDateTime.of(2017, 1, 1, 0, 0)),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasAvailable(10),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasQuarantine(20),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDefect(30),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasFreeze(4),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasExpired(1)
                                ))
                                .build(),
                        MbiMatchers.<SkuStockInfo<SupplierWarehouseShopSkuKey>>newAllOfBuilder()
                                .add(SkuStockInfo::getStock, Matchers.allOf(
                                        StockMatchers.hasWarehouseName("warehouse2"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasWarehouseId(2L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasSupplierId(1L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasShopSku("teabag1"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDateTime(LocalDateTime.of(2017, 1, 1, 0, 0)),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasAvailable(10),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasQuarantine(20),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDefect(30),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasFreeze(4),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasExpired(1)
                                ))
                                .build(),
                        MbiMatchers.<SkuStockInfo<SupplierWarehouseShopSkuKey>>newAllOfBuilder()
                                .add(SkuStockInfo::getStock, Matchers.allOf(
                                        StockMatchers.hasWarehouseName("warehouse1"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasWarehouseId(1L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasSupplierId(1L),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasShopSku("pingpong"),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDateTime(LocalDateTime.of(2017, 1, 1, 0, 0)),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasAvailable(11),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasQuarantine(22),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasDefect(33),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasFreeze(0),
                                        StockMatchers.<SupplierWarehouseShopSkuKey>hasExpired(4)
                                ))
                                .build()
                ))
        );
    }
}
