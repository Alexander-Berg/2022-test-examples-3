package ru.yandex.market.vendors.analytics.core.service.sales.shop;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.vendors.analytics.core.model.region.RegionInfo;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.price.RawShopRegionSales;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.price.RegionShopsPrices;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.price.ShopPrice;
import ru.yandex.market.vendors.analytics.core.service.strategies.TopSelectionStrategy;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.vendors.analytics.core.service.sales.shop.ShopAssortmentTestUtils.createRawSales;

/**
 * Tests for static methods of {@link ModelShopsPricesService}.
 *
 * @author antipov93.
 */
public class ModelShopsPricesServiceStaticTest {

    private static final List<RawShopRegionSales> SHOPS_SALES_IN_REGION = List.of(
            createRawSales("a.ru", 1, "2019-01-01", 300, 10),
            createRawSales("b.ru", 1, "2019-01-01", 200, 20),
            createRawSales("c.ru", 1, "2019-01-01", 100, 50)
    );

    @Test
    @DisplayName("Выбор топовых магазинов по количеству продаж")
    void sortAndLimitShopByCount() {
        List<RawShopRegionSales> result = ModelShopsPricesService.sortAndLimitShops(
                SHOPS_SALES_IN_REGION,
                TopSelectionStrategy.COUNT,
                2
        );
        assertEquals(
                List.of("c.ru", "b.ru"),
                result.stream().map(RawShopRegionSales::getShopDomain).collect(toList())
        );
    }

    @Test
    @DisplayName("Выбор топовых магазинов по суммарной стоимости продаж")
    void sortAndLimitShopByMoney() {
        List<RawShopRegionSales> result = ModelShopsPricesService.sortAndLimitShops(
                SHOPS_SALES_IN_REGION,
                TopSelectionStrategy.MONEY,
                2
        );
        assertEquals(
                List.of("a.ru", "b.ru"),
                result.stream().map(RawShopRegionSales::getShopDomain).collect(toList())
        );
    }

    @Test
    @DisplayName("Цена в топовых магазинах")
    void processShopsInRegion() {
        RegionShopsPrices expected = new RegionShopsPrices(
                new RegionInfo(1L, "Россия", "Russia", 3, 0L),
                List.of(
                        new ShopPrice("c.ru", 2),
                        new ShopPrice("b.ru", 10)
                )
        );
        RegionShopsPrices result = ModelShopsPricesService.processShopsInRegion(
                new RegionInfo(1L, "Россия", "Russia", 3, 0L),
                SHOPS_SALES_IN_REGION,
                TopSelectionStrategy.COUNT,
                2
        );
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Выбор топовых регионов")
    void sortAndLimitRegions() {
        List<Long> expected = List.of(2L, 1L, 4L, 3L);

        List<Long> result = ModelShopsPricesService.sortAndLimitRegions(
                Map.of(
                        1L, List.of(
                                // money=80,count=30
                                createRawSales("a.ru", 1, "2019-01-01", 15, 10),
                                createRawSales("b.ru", 1, "2019-01-01", 25, 10),
                                createRawSales("c.ru", 1, "2019-01-01", 40, 10)
                        ),
                        2L, List.of(
                                // money=100,count=10
                                createRawSales("d.ru", 2, "2019-01-01", 100, 10)
                        ),
                        3L, List.of(
                                // money=5,count=3
                                createRawSales("a.ru", 3, "2019-01-01", 3, 1),
                                createRawSales("b.ru", 3, "2019-01-01", 1, 1),
                                createRawSales("c.ru", 3, "2019-01-01", 1, 1)
                        ),
                        4L, List.of(
                                // money=15,count=6
                                createRawSales("a.ru", 4, "2019-01-01", 5, 2),
                                createRawSales("b.ru", 4, "2019-01-01", 5, 2),
                                createRawSales("c.ru", 4, "2019-01-01", 5, 2)
                        ),
                        5L, List.of(
                                // money=79,count=1
                                createRawSales("a.ru", 5, "2019-01-01", 79, 1)
                        )
                ),
                TopSelectionStrategy.MONEY,
                2,
                Set.of(1L, 3L, 4L, 6L)
        );
        assertEquals(expected, result);
    }
}
