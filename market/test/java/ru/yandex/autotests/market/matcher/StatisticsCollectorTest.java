package ru.yandex.autotests.market.matcher;

import org.junit.Test;
import ru.yandex.autotests.market.Category;
import ru.yandex.autotests.market.comparison.ComparisonItem;
import ru.yandex.autotests.market.ResponseData;
import ru.yandex.autotests.market.services.matcher.MatcherResultComparator;
import ru.yandex.autotests.market.services.matcher.result.StatisticsCollector;
import ru.yandex.market.ir.http.Matcher;

import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.market.services.matcher.MatcherResultComparator.DIFFERENT;
import static ru.yandex.autotests.market.services.matcher.MatcherResultComparator.SAME;
import static ru.yandex.autotests.market.services.matcher.MatcherResultComparator.SLIGHTLY_DIFFERENT;

public class StatisticsCollectorTest {
    private static final double AVERAGE_RESPONSE_TIME_DELTA = 0.001;

    private final StatisticsCollector collector = new StatisticsCollector();


    @Test
    public void offerCountByStatus() {
        collector.add(unchanged());
        collector.add(slightlyChanged());
        collector.add(changed());
        assertEquals(3, collector.getOffersCount());
        assertEquals(1, collector.getUnchangedOffersCount());
        assertEquals(1, collector.getSlightlyChangedOffersCount());
        assertEquals(1, collector.getChangedOffersCount());
    }

    @Test
    public void averageResponseTime() {
        collector.add(withTimes(1000000, 1000000));
        collector.add(withTimes(2000000, 3000000));
        assertEquals(1.5, collector.getAverageStableResponseTimeMs(), AVERAGE_RESPONSE_TIME_DELTA);
        assertEquals(2, collector.getAverageTestingResponseTimeMs(), AVERAGE_RESPONSE_TIME_DELTA);
    }

    @Test
    public void mostChangedCategories() {
        collector.add(unchangedWithCategory(1));
        collector.add(slightlyChangedWithCategory(1));
        collector.add(changedWithCategory(1));

        collector.add(unchangedWithCategory(2));
        collector.add(slightlyChangedWithCategory(2));

        collector.add(unchangedWithCategory(3));
        collector.add(changedWithCategory(3));

        collector.add(changedWithCategory(4));
        collector.add(changedWithCategory(4));

        collector.add(unchangedWithCategory(5));  // не должно попасть в mostChangedCategories

        Category[] mostChangedCategories = collector.getNMostChangedCategories(100);
        assertEquals(4, mostChangedCategories.length);
        checkMostChangedCategory(mostChangedCategories[0], 4, 2, 0, 0);
        checkMostChangedCategory(mostChangedCategories[1], 1, 1, 1, 1);
        checkMostChangedCategory(mostChangedCategories[2], 3, 1, 0, 1);
        checkMostChangedCategory(mostChangedCategories[3], 2, 0, 1, 1);
    }

    private static void checkMostChangedCategory(Category category, int id, int changedCount, int slightlyChangedCount, int unchangedCount) {
        assertEquals(id, category.getId());
        assertEquals(changedCount, category.getChangedOffersCount());
        assertEquals(slightlyChangedCount, category.getSlightlyChangedOffersCount());
        assertEquals(unchangedCount, category.getUnchangedOffersCount());
    }


    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> withTimes(long stableResponseTime, long testingResponseTime) {
        return matchingComparison(SAME, 0, stableResponseTime, testingResponseTime);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> unchangedWithCategory(long categoryId) {
        return matchingComparison(SAME, categoryId, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> slightlyChangedWithCategory(long categoryId) {
        return matchingComparison(SLIGHTLY_DIFFERENT, categoryId, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> changedWithCategory(long categoryId) {
        return matchingComparison(DIFFERENT, categoryId, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> unchanged() {
        return matchingComparison(SAME, 0, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> slightlyChanged() {
        return matchingComparison(SLIGHTLY_DIFFERENT, 0, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> changed() {
        return matchingComparison(DIFFERENT, 0, 0, 0);
    }

    private static ComparisonItem<Matcher.Offer, Matcher.MatchResult, MatcherResultComparator> matchingComparison(MatcherResultComparator status, long categoryId, long stableResponseTime, long testingResponseTime) {
        return new ComparisonItem<>(
            Matcher.Offer.newBuilder().setHid(categoryId).build(),
                "1",
                status,
                new ResponseData<>(stableResponseTime, null),
                new ResponseData<>(testingResponseTime, null)
        );
    }
}
