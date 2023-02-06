package ru.yandex.direct.grid.core.util;

import java.math.BigDecimal;
import java.util.List;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.direct.grid.core.entity.model.GdiEntityStatsFilter;
import ru.yandex.direct.grid.core.entity.model.GdiGoalStatsFilter;
import ru.yandex.direct.grid.core.entity.model.GdiOfferStatsFilter;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class RepositoryUtil {

    public static GdiEntityStatsFilter buildStatsFilter() {
        return new GdiEntityStatsFilter()
                .withMinCost(BigDecimal.valueOf(1L))
                .withMaxCost(BigDecimal.valueOf(2L))
                .withMinCostWithTax(BigDecimal.valueOf(1L))
                .withMaxCostWithTax(BigDecimal.valueOf(2L))
                .withMinShows(3L)
                .withMaxShows(4L)
                .withMinClicks(5L)
                .withMaxClicks(6L)
                .withMinCtr(BigDecimal.valueOf(7L))
                .withMaxCtr(BigDecimal.valueOf(8L))
                .withMinAvgClickCost(BigDecimal.valueOf(9L))
                .withMaxAvgClickCost(BigDecimal.valueOf(10L))
                .withMinAvgShowPosition(BigDecimal.valueOf(11L))
                .withMaxAvgShowPosition(BigDecimal.valueOf(12L))
                .withMinAvgClickPosition(BigDecimal.valueOf(13L))
                .withMaxAvgClickPosition(BigDecimal.valueOf(14L))
                .withMinBounceRate(BigDecimal.valueOf(15L))
                .withMaxBounceRate(BigDecimal.valueOf(16L))
                .withMinConversionRate(BigDecimal.valueOf(17L))
                .withMaxConversionRate(BigDecimal.valueOf(18L))
                .withMinAvgGoalCost(BigDecimal.valueOf(19L))
                .withMaxAvgGoalCost(BigDecimal.valueOf(20L))
                .withMinGoals(21L)
                .withMaxGoals(22L)
                .withMinAvgDepth(BigDecimal.valueOf(23L))
                .withMaxAvgDepth(BigDecimal.valueOf(24L))
                .withMinProfitability(BigDecimal.valueOf(25L))
                .withMaxProfitability(BigDecimal.valueOf(26L))
                .withMinRevenue(BigDecimal.valueOf(27L))
                .withMaxRevenue(BigDecimal.valueOf(28L));
    }

    public static GdiGoalStatsFilter buildGoalStatFilter() {
        return new GdiGoalStatsFilter()
                .withGoalId(1L)
                .withMinGoals(2L)
                .withMaxGoals(3L)
                .withMinConversionRate(BigDecimal.valueOf(4L))
                .withMaxConversionRate(BigDecimal.valueOf(5L))
                .withMinCostPerAction(BigDecimal.valueOf(6L))
                .withMaxCostPerAction(BigDecimal.valueOf(7L));
    }

    public static GdiOfferStatsFilter buildOfferStatsFilter() {
        return new GdiOfferStatsFilter()
                .withMinShows(BigDecimal.valueOf(1L))
                .withMaxShows(BigDecimal.valueOf(2L))
                .withMinClicks(BigDecimal.valueOf(3L))
                .withMaxClicks(BigDecimal.valueOf(4L))
                .withMinCtr(BigDecimal.valueOf(5L))
                .withMaxCtr(BigDecimal.valueOf(6L))
                .withMinCost(BigDecimal.valueOf(7L))
                .withMaxCost(BigDecimal.valueOf(8L))
                .withMinCostWithTax(BigDecimal.valueOf(9L))
                .withMaxCostWithTax(BigDecimal.valueOf(10L))
                .withMinRevenue(BigDecimal.valueOf(25L))
                .withMaxRevenue(BigDecimal.valueOf(26L))
                .withMinCrr(BigDecimal.valueOf(27L))
                .withMaxCrr(BigDecimal.valueOf(28L))
                .withMinCarts(BigDecimal.valueOf(11L))
                .withMaxCarts(BigDecimal.valueOf(12L))
                .withMinPurchases(BigDecimal.valueOf(13L))
                .withMaxPurchases(BigDecimal.valueOf(14L))
                .withMinAvgClickCost(BigDecimal.valueOf(15L))
                .withMaxAvgClickCost(BigDecimal.valueOf(16L))
                .withMinAvgProductPrice(BigDecimal.valueOf(17L))
                .withMaxAvgProductPrice(BigDecimal.valueOf(18L))
                .withMinAvgPurchaseRevenue(BigDecimal.valueOf(19L))
                .withMaxAvgPurchaseRevenue(BigDecimal.valueOf(20L))
                .withMinAutobudgetGoals(BigDecimal.valueOf(21L))
                .withMaxAutobudgetGoals(BigDecimal.valueOf(22L))
                .withMinMeaningfulGoals(BigDecimal.valueOf(23L))
                .withMaxMeaningfulGoals(BigDecimal.valueOf(24L));
    }

    // Select#toString() в jooq возращает выбираемые поля в произвольном порядке,
    // и просто сравнить два запроса нельзя.
    // Поэтому сортируем строки из запроса и проверяем совпадение отсортированных списков
    // При изменении просто нужно вставить требуемый запрос в файл, не редактируя
    public static void compareQueries(String query1, String query2) {

//        Раскомментировать при расхождении для удобства поиска ошибки
//        assertEquals(query1, query2);

        List<String> list1 = StreamEx.split(query1, "\n")
                .map(String::trim)
                .map(s -> StringUtils.stripEnd(s, ","))
                .sorted()
                .collect(toList());

        List<String> list2 = StreamEx.split(query2, "\n")
                .map(String::trim)
                .map(s -> StringUtils.stripEnd(s, ","))
                .sorted()
                .collect(toList());

        assertEquals(list1, list2);
    }
}
