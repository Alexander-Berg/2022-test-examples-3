package ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.core.domain.CommonDateRange;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.crm.campaign.services.segments.tasks.algorithms.sendings.PeriodUtils.mergePeriods;

class PeriodUtilsTest {

    /**
     * Два не пересекающихся периода не сливаются в один
     */
    @Test
    void testDoNotMergeTwoNotIntersectingPeriods() {
        var period1 = period(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 10));
        var period2 = period(LocalDate.of(2021, 3, 5), LocalDate.of(2021, 3, 9));

        var result = merge(period1, period2);

        assertThat(result, hasSize(2));
        assertPeriod(period1, result.get(0));
        assertPeriod(period2, result.get(1));
    }

    /**
     * Одинаковые периоды сливаются в один
     */
    @Test
    void testMergeEqualPeriods() {
        var startDate = LocalDate.now().minusDays(30);
        var endDate = LocalDate.now().minusDays(25);

        // Создаем два разных экземпляра для того чтобы избежать ссылочного равенства
        var period1 = period(startDate, endDate);
        var period2 = period(LocalDate.now().minusDays(6), LocalDate.now().minusDays(5));
        var period3 = period(startDate, endDate);

        var result = merge(period1, period2, period3);

        assertThat(result, hasSize(2));
        assertPeriod(period1, result.get(0));
        assertPeriod(period2, result.get(1));
    }

    /**
     * Пересекающиеся периоды сливаются в один
     */
    @Test
    void testIntersectingPeriodsIsMergedInOne() {
        var period1 = period(LocalDate.of(2021, 1, 6), LocalDate.of(2021, 1, 6));
        var period2 = period(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 8));
        var period3= period(LocalDate.of(2021, 1, 12), LocalDate.of(2021, 1, 14));
        var period4 = period(LocalDate.of(2021, 1, 17), LocalDate.of(2021, 1, 18));
        var period5 = period(LocalDate.of(2021, 1, 7), LocalDate.of(2021, 1, 10));
        var period6 = period(LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 18));

        var result = merge(period1, period2, period3, period3, period4, period5, period6);

        assertThat(result, hasSize(3));

        var expectedPeriod1 = period(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 10));
        assertPeriod(expectedPeriod1, result.get(0));

        var expectedPeriod2 = period(LocalDate.of(2021, 1, 12), LocalDate.of(2021, 1, 14));
        assertPeriod(expectedPeriod2, result.get(1));

        var expectedPeriod3 = period(LocalDate.of(2021, 1, 16), LocalDate.of(2021, 1, 18));
        assertPeriod(expectedPeriod3, result.get(2));
    }

    /**
     * Смежные периоды сливаются в один
     */
    @Test
    void testMergeAdjacentPeriods() {
        var period1 = period(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 6));
        var period2 = period(LocalDate.of(2021, 1, 7), LocalDate.of(2021, 1, 8));
        var period3= period(LocalDate.of(2021, 1, 9), LocalDate.of(2021, 1, 10));

        var result = merge(period1, period2, period3);
        assertThat(result, hasSize(1));

        var expected = period(LocalDate.of(2021, 1, 5), LocalDate.of(2021, 1, 10));
        assertPeriod(expected, result.get(0));
    }

    private static CommonDateRange period(LocalDate startDate, LocalDate endDate) {
        return new CommonDateRange(startDate, endDate);
    }

    private static List<CommonDateRange> merge(CommonDateRange... periods) {
        return mergePeriods(List.of(periods));
    }

    private static void assertPeriod(CommonDateRange expected, CommonDateRange actual) {
        assertEquals(expected.getStartDate(), actual.getStartDate(), "Invalid start date");
        assertEquals(expected.getEndDate(), actual.getEndDate(), "Invalid end date");
    }
}
