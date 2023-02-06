package ru.yandex.market.billing.fulfillment.supplies.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher;
import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupply;
import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupplyItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.fulfillment.supplies.FulfillmentSupplyStatus;
import ru.yandex.market.core.billing.model.WithdrawItem;
import ru.yandex.market.core.fulfillment.FulfillmentSupplyType;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.core.fulfillment.model.Warehouse;
import ru.yandex.market.core.util.DateTimes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasCategoryId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasFactCount;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasMappingUpdateTime;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasShopSku;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasSupplierId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasSupplyId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyItemMatcher.hasSurplusCount;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasOperationType;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasPalletCount;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasServiceId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasStatus;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasType;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.hasXDocServiceId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.FulfillmentSupplyMatcher.haxBoxCount;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.betweenFinishedTrantime;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasCount;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasMarketName;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasName;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasServiceRequestId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasStockType;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasSupplyPrice;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasWarehouse;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasWithdrawId;
import static ru.yandex.market.billing.fulfillment.supplies.matchers.WithdrawItemMatcher.hasWithdrawOperationType;
import static ru.yandex.market.core.fulfillment.model.StockType.DEFECT;
import static ru.yandex.market.core.fulfillment.model.StockType.EXPIRED;
import static ru.yandex.market.core.fulfillment.model.StockType.FIT;
import static ru.yandex.market.core.fulfillment.model.StockType.SURPLUS;

/**
 * Тесты для {@link FulfillmentSupplyYtDao}.
 *
 * @author vbudnev
 */
class FulfillmentSupplyYtDaoTest extends FunctionalTest {

    @Autowired
    private FulfillmentSupplyYtDao fulfillmentSupplyYtDao;

    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.ignore_1p.before.csv")
    @DisplayName("Импорт поставок из yt не подтягивает данные для 1p")
    @Test
    void test_getSupplyItems_no1p() {
        final List<FulfillmentSupplyItem> supplies = getSupplyItems();
        assertThat(supplies, hasSize(1));

        FulfillmentSupplyItem non1pItem = supplies.get(0);
        assertThat(non1pItem,
                allOf(
                        hasSupplierId(22L),
                        hasCategoryId(2000L),
                        hasSupplyId(1L),
                        hasShopSku("sku_general"),
                        hasFactCount(5),
                        hasMappingUpdateTime(DateTimes.toInstantAtDefaultTz(2019, 5, 20, 14, 12, 21))
                )
        );
    }

    @DisplayName("Проверка импорта полей, необходимых для логики обиливания магистрального xdoc")
    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.xdoc.before.csv")
    @Test
    void test_getSupplies_xdoc() {
        final List<FulfillmentSupply> supplies = getSupplies();
        //сортировка для удобства проверки в тесте
        supplies.sort(Comparator.comparing(FulfillmentSupply::getId));

        assertThat(supplies,
                contains(
                        allOf(
                                hasId(11L),
                                hasType(FulfillmentSupplyType.SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.IN_PROGRESS),
                                hasPalletCount(0L),
                                haxBoxCount(0L),
                                hasXDocServiceId(0L),
                                hasServiceId(0L),
                                hasOperationType(FulfillmentOperationType.XDOC)
                        ),
                        allOf(
                                hasId(12L),
                                hasType(FulfillmentSupplyType.SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.IN_PROGRESS),
                                hasPalletCount(5L),
                                haxBoxCount(1L),
                                hasXDocServiceId(22L),
                                hasServiceId(1L),
                                hasOperationType(FulfillmentOperationType.XDOC)
                        )
                )
        );
    }

    @DisplayName("Проверка импорта полей, необходимых для логики обиливания излишков")
    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.surplus.before.csv")
    @Test
    void test_getSupplies_surplus() {
        final List<FulfillmentSupplyItem> supplyItems = getSupplyItems();

        assertThat(
                supplyItems,
                contains(
                        allOf(
                                hasSupplierId(22),
                                hasShopSku("some_sku"),
                                hasSurplusCount(1)
                        ),
                        allOf(
                                hasSupplierId(23),
                                hasShopSku("some_sku2"),
                                hasSurplusCount(5)
                        )
                )
        );
    }

    @DisplayName("Проверка импорта поставок с типом Межскладское перемещение и Аномалии")
    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.anomaly_and_transfer.before.csv")
    @Test
    void test_getSupplies_transfer_and_anomaly() {
        final List<FulfillmentSupply> supplies = getSupplies();

        assertThat(supplies,
                contains(
                        // поставки
                        allOf(
                                hasId(11L),
                                hasType(FulfillmentSupplyType.CROSS_WAREHOUSE_TRANSFER_SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.FINISHED)
                        ),
                        allOf(
                                hasId(12L),
                                hasType(FulfillmentSupplyType.ADDITIONAL_ANOMALY_SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.FINISHED)
                        )
                )
        );
    }

    @DisplayName("Проверка фильтра статусов для поставок(supplies)")
    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.supplies_filters.before.csv")
    @Test
    void test_getSupplies() {
        final List<FulfillmentSupply> supplies = getSupplies();
        //сортировка для удобства проверки в тесте
        supplies.sort(Comparator.comparing(FulfillmentSupply::getId));

        assertThat(supplies,
                contains(
                        // поставки
                        allOf(
                                hasId(11L),
                                hasType(FulfillmentSupplyType.SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.IN_PROGRESS)
                        ),
                        allOf(
                                hasId(12L),
                                hasType(FulfillmentSupplyType.SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.PROCESSED)
                        ),
                        allOf(
                                hasId(14L),
                                hasType(FulfillmentSupplyType.SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.FINISHED)
                        ),
                        // возвраты
                        allOf(
                                hasId(21L),
                                hasType(FulfillmentSupplyType.CUSTOMER_RETURN_SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.IN_PROGRESS)
                        ),
                        allOf(
                                hasId(22L),
                                hasType(FulfillmentSupplyType.CUSTOMER_RETURN_SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.PROCESSED)
                        ),
                        allOf(
                                hasId(24L),
                                hasType(FulfillmentSupplyType.CUSTOMER_RETURN_SUPPLY),
                                hasStatus(FulfillmentSupplyStatus.FINISHED)
                        )
                )
        );
    }

    @DisplayName("Проверка фильтра статусов для поставок(supply_items)")
    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.supplies_filters.before.csv")
    @Test
    void test_getSupplyItems() {
        List<Long> ids = getSupplyItems()
                .stream()
                .map(FulfillmentSupplyItem::getSupplyId)
                .sorted()
                .collect(Collectors.toList());

        assertThat(ids, is(ImmutableList.of(11L, 12L, 14L, 21L, 22L, 24L)));
    }

    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.same_shop_sku_on_join.before.csv")
    @DisplayName("Связывание информации из маппинга идет по shop_sku+supplier_id")
    @Test
    void test_getSupplyItems_joinOnComplexKey() {
        final List<FulfillmentSupplyItem> supplies = getSupplyItems();
        supplies.sort(
                (o1, o2) -> new CompareToBuilder()
                        .append(o1.getSupplierId(), o2.getSupplierId())
                        .append(o1.getShopSku(), o2.getShopSku())
                        .build()
        );

        assertThat(
                supplies,
                contains(
                        allOf(hasSupplierId(22), hasShopSku("sku_1"), hasCategoryId(2000L)),
                        allOf(hasSupplierId(33), hasShopSku("sku_1"), hasCategoryId(3000L)),
                        allOf(hasSupplierId(44), hasShopSku("sku_2"), hasCategoryId(4000L)),
                        allOf(hasSupplierId(55), hasShopSku("sku_2"), hasCategoryId(5000L))
                )
        );
    }

    @DbUnitDataSet(before = "FulfillmentWithdrawItemYtDaoTest.before.csv")
    @DisplayName("Сохранение изъятий из YT таблиц")
    @Test
    void shouldRetrieveWithdrawItemsWhenYTHasData() {
        LocalDate TEST_DATE_2019_11_20 = LocalDate.of(2019, 11, 20);
        List<WithdrawItem> withdrawItems = fulfillmentSupplyYtDao
                .getWithdrawItems(TEST_DATE_2019_11_20, TEST_DATE_2019_11_20.plusDays(1L));

        assertThat(
                withdrawItems,
                contains(
                        allOf(hasWithdrawId(7),
                                hasServiceRequestId("40"),
                                hasWarehouse(Warehouse.SOFINO),
                                hasStockType(FIT),
                                WithdrawItemMatcher.hasSupplierId(22L),
                                betweenFinishedTrantime(TEST_DATE_2019_11_20, TEST_DATE_2019_11_20.plusDays(1L)),
                                WithdrawItemMatcher.hasShopSku("some_sku"),
                                hasCount(1),
                                hasName("name3"),
                                hasMarketName("market_name3"),
                                hasSupplyPrice(new BigDecimal("1.12"))
                        ),
                        allOf(hasWithdrawId(8),
                                hasWarehouse(Warehouse.ROSTOV_ON_DON),
                                hasStockType(EXPIRED),
                                WithdrawItemMatcher.hasSupplierId(23L),
                                WithdrawItemMatcher.hasShopSku("1"),
                                hasCount(0),
                                hasName("name4"),
                                hasMarketName("market_name4"),
                                hasSupplyPrice(new BigDecimal("1"))
                        ),
                        allOf(hasWithdrawId(9),
                                hasWarehouse(Warehouse.ROSTOV_ON_DON),
                                hasStockType(DEFECT),
                                WithdrawItemMatcher.hasSupplierId(24L),
                                hasCount(10),
                                hasName("name8"),
                                hasMarketName("market_name8"),
                                hasSupplyPrice(new BigDecimal("1"))
                        ),
                        allOf(hasWithdrawId(10),
                                hasWarehouse(Warehouse.ROSTOV_ON_DON),
                                hasStockType(SURPLUS),
                                WithdrawItemMatcher.hasSupplierId(25L),
                                hasCount(4),
                                hasName("name9"),
                                hasMarketName("market_name9"),
                                hasSupplyPrice(new BigDecimal("1"))
                        ),
                        allOf(hasWithdrawId(11),
                                hasServiceRequestId("40"),
                                hasWarehouse(Warehouse.SOFINO),
                                hasStockType(FIT),
                                WithdrawItemMatcher.hasSupplierId(22L),
                                betweenFinishedTrantime(TEST_DATE_2019_11_20, TEST_DATE_2019_11_20.plusDays(1L)),
                                WithdrawItemMatcher.hasShopSku("some_sku"),
                                hasCount(1),
                                hasName("name3"),
                                hasMarketName("market_name3"),
                                hasSupplyPrice(new BigDecimal("1")),
                                hasWithdrawOperationType(FulfillmentOperationType.DISPOSAL)
                        )
                )
        );
    }

    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.disposal.before.csv")
    @DisplayName("Сохранение данных о добровольной утилизации из YT таблиц")
    @Test
    void test_getSupplies_importVoluntaryDisposal() {
        List<FulfillmentSupply> actualSupplies = getSupplies();

        assertThat(actualSupplies,
                contains(
                        // поставки
                        allOf(
                                hasId(1L),
                                hasType(FulfillmentSupplyType.SELF_REQUESTED_DISPOSAL),
                                hasStatus(FulfillmentSupplyStatus.PROCESSED)
                        ),
                        allOf(
                                hasId(2L),
                                hasType(FulfillmentSupplyType.SELF_REQUESTED_DISPOSAL),
                                hasStatus(FulfillmentSupplyStatus.FINISHED)
                        )
                )
        );
    }

    @DbUnitDataSet(before = "FulfillmentSupplyYtDaoTest.disposal.before.csv")
    @DisplayName("Сохранение данных о добровольной утилизации по товарам из YT таблиц")
    @Test
    void test_getSupplyItems_importVoluntaryDisposal() {
        List<FulfillmentSupplyItem> actualSupplyItems = getSupplyItems();

        assertThat(
                actualSupplyItems,
                contains(
                        allOf(
                                hasSupplierId(22),
                                hasShopSku("sku_general"),
                                hasCategoryId(2000L)
                        ),
                        allOf(
                                hasSupplierId(22),
                                hasShopSku("sku_1"),
                                hasCategoryId(3000L)
                        )
                )
        );
    }

    private List<FulfillmentSupplyItem> getSupplyItems() {
        final List<FulfillmentSupplyItem> supplyItems = new ArrayList<>();
        fulfillmentSupplyYtDao.supplyItemsApplyWithConsumer(supplyItems::add);
        return supplyItems;
    }

    private List<FulfillmentSupply> getSupplies() {
        final List<FulfillmentSupply> supplies = new ArrayList<>();
        fulfillmentSupplyYtDao.suppliesApplyWithConsumer(supplies::add);
        return supplies;
    }
}
