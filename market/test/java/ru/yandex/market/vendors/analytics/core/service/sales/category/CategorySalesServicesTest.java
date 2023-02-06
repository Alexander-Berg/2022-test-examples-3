package ru.yandex.market.vendors.analytics.core.service.sales.category;

import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.vendors.analytics.core.model.sales.common.RawPeriodicSales;
import ru.yandex.market.vendors.analytics.core.service.strategies.TopSelectionStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.vendors.analytics.core.service.SalesTestUtils.getRawSales;

/**
 * Тесты для сервисов про продажи по категориям.
 */
public class CategorySalesServicesTest {

    /**
     * {@link CategorySalesServiceUtils#cutAndSortSales(Map, int, TopSelectionStrategy, Set)}.
     */
    @Test
    @DisplayName("Проверка, что категории обрезаются и сортируются по деньгам корректно")
    void cutAndSortMoneyCategoriesTest() {
        var siblingsSalesByDates = Map.of(
                "2017-01-01", getSiblingsSalesList("2017-01-01", 3),
                "2017-01-02", getSiblingsSalesList("2017-01-02", 4)
        );
        var importantCategoryHids = Set.of(1L);
        var expected = Map.of(
                "2017-01-01", List.of(
                        getRawSales("2017-01-01", 3, 300, 70),
                        getRawSales("2017-01-01", 2, 200, 80),
                        getRawSales("2017-01-01", 1, 100, 90)
                ),
                "2017-01-02", List.of(
                        getRawSales("2017-01-02", 3, 300, 70),
                        getRawSales("2017-01-02", 2, 200, 80),
                        getRawSales("2017-01-02", 1, 100, 90)
                )
        );

        Map<String, List<RawPeriodicSales>> actual = CategorySalesServiceUtils.cutAndSortSales(
                siblingsSalesByDates,
                2,
                TopSelectionStrategy.MONEY,
                importantCategoryHids
        );
        assertEquals(expected, actual);
    }

    /**
     * {@link CategorySalesServiceUtils#cutAndSortSales(Map, int, TopSelectionStrategy, Set)}.
     */
    @Test
    @DisplayName("Проверка, что категории обрезаются и сортируются по штукам корректно")
    void cutAndSortCountCategoriesTest() {
        var siblingsSalesByDates = Map.of(
                "2017-01-01", getSiblingsSalesList("2017-01-01", 3),
                "2017-01-02", getSiblingsSalesList("2017-01-02", 4)
        );
        var importantCategoryHids = Set.of(4L);
        var expected = Map.of(
                "2017-01-01", List.of(
                        getRawSales("2017-01-01", 1, 100, 90),
                        getRawSales("2017-01-01", 2, 200, 80)
                ),
                "2017-01-02", List.of(
                        getRawSales("2017-01-02", 1, 100, 90),
                        getRawSales("2017-01-02", 2, 200, 80),
                        getRawSales("2017-01-02", 4, 400, 60)
                )
        );

        Map<String, List<RawPeriodicSales>> actual = CategorySalesServiceUtils.cutAndSortSales(
                siblingsSalesByDates,
                2,
                TopSelectionStrategy.COUNT,
                importantCategoryHids
        );
        assertEquals(expected, actual);
    }

    private static List<RawPeriodicSales> getSiblingsSalesList(String date, int count) {
        return IntStreamEx.rangeClosed(1, count)
                .reverseSorted()
                .mapToObj(i -> getRawSales(date, i, 100 * i, 100 - (10 * i)))
                .toList();
    }
}
