package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange;
import ru.yandex.direct.core.entity.performancefilter.model.NowOptimizingBy;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterTab;
import ru.yandex.direct.core.entity.performancefilter.model.TargetFunnel;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.performancefilter.utils.PerformanceFilterUtils.PERFORMANCE_FILTER_CONDITION_COMPARATOR;
import static ru.yandex.direct.core.testing.data.TestFeeds.DEFAULT_FEED_BUSINESS_TYPE;
import static ru.yandex.direct.core.testing.data.TestFeeds.DEFAULT_FEED_SOURCE;
import static ru.yandex.direct.core.testing.data.TestFeeds.DEFAULT_FEED_TYPE;

public class TestPerformanceFilters {

    private static final LocalDateTime LOCAL_TIME = LocalDate.now().minusDays(1).atTime(0, 0);

    private TestPerformanceFilters() {
    }

    public static PerformanceFilter hotelPerformanceFilter(@Nullable Long adGroupId, @Nullable Long feedId) {
        return defaultPerformanceFilter(adGroupId, feedId)
                .withBusinessType(BusinessType.HOTELS)
                .withFeedType(FeedType.GOOGLE_HOTELS)
                .withConditions(googleHotelFilterConditions());
    }

    public static PerformanceFilter defaultPerformanceFilter() {
        return defaultPerformanceFilter(null, null);
    }

    public static PerformanceFilter defaultPerformanceFilter(@Nullable Long adGroupId, @Nullable Long feedId) {
        return new PerformanceFilter()
                .withBusinessType(DEFAULT_FEED_BUSINESS_TYPE)
                .withFeedType(DEFAULT_FEED_TYPE)
                .withSource(DEFAULT_FEED_SOURCE)
                .withPid(adGroupId)
                .withFeedId(feedId)
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withName("test feed")
                .withNowOptimizingBy(NowOptimizingBy.CPC)
                .withPriceCpa(BigDecimal.valueOf(10L))
                .withPriceCpc(BigDecimal.valueOf(5L))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withTargetFunnel(TargetFunnel.NEW_AUDITORY)
                .withLastChange(LOCAL_TIME)
                .withAutobudgetPriority(null)
                .withConditions(defaultFilterConditions())
                .withTab(PerformanceFilterTab.TREE);
    }

    public static PerformanceFilterCondition defaultSiteFilterCondition() {
        return new PerformanceFilterCondition<Boolean>("available", Operator.EQUALS, "true");
    }

    public static PerformanceFilterCondition defaultUacFilterCondition() {
        return new PerformanceFilterCondition<Boolean>("available", Operator.NOT_EQUALS, "false");
    }

    public static List<PerformanceFilterCondition> defaultFilterConditions() {
        // Схема: YANDEX_MARKET
        PerformanceFilterCondition<Boolean> condition1 =
                new PerformanceFilterCondition<>("available", Operator.EQUALS, "true");
        condition1.setParsedValue(Boolean.TRUE);

        PerformanceFilterCondition<List<DecimalRange>> condition2 =
                new PerformanceFilterCondition<>("price", Operator.RANGE, "[\"3000.00-100000.00\",\"111.00-222.00\"]");
        condition2.setParsedValue(Stream.of(
                new DecimalRange("3000.00-100000.00"),
                new DecimalRange("111.00-222.00"))
                .collect(toList()));

        return Stream.of(condition1, condition2).collect(toList());
    }

    /**
     * Другой набор условий фильтра. Используется в юнит-тестах (ядровых и гридовых) для проверки функциональности
     * сохранения изменений фильтров. Схема: YANDEX_MARKET.
     */
    public static List<PerformanceFilterCondition> otherFilterConditions() {
        PerformanceFilterCondition<List<String>> condition1 =
                new PerformanceFilterCondition<>("model", Operator.CONTAINS, "[\"ggg\",\"www\"]");
        condition1.setParsedValue(asList("ggg", "www"));

        PerformanceFilterCondition<List<DecimalRange>> condition2 =
                new PerformanceFilterCondition<>("price", Operator.RANGE, "[\"1000.00-30000.00\",\"333.00-444.00\"]");
        condition2.setParsedValue(Stream.of(
                new DecimalRange("1000.00-30000.00"),
                new DecimalRange("333.00-444.00"))
                .collect(toList()));

        return Stream.of(condition1, condition2).collect(toList());
    }

    public static List<PerformanceFilterCondition> googleHotelFilterConditions() {
        PerformanceFilterCondition<List<String>> condition1 =
                new PerformanceFilterCondition<>("name", Operator.CONTAINS, "[\"Marriott\",\"Hilton\"]");
        condition1.setParsedValue(asList("Marriott", "Hilton"));

        PerformanceFilterCondition<List<DecimalRange>> condition2 =
                new PerformanceFilterCondition<>("Price", Operator.RANGE, "[\"1000.00-30000.00\",\"333.00-444.00\"]");
        condition2.setParsedValue(Stream.of(
                new DecimalRange("1000.00-30000.00"),
                new DecimalRange("333.00-444.00"))
                .collect(toList()));

        PerformanceFilterCondition<List<String>> condition3 =
                new PerformanceFilterCondition<>("location", Operator.CONTAINS, "[\"Moscow\",\"Kiev\"]");
        condition3.setParsedValue(asList("Moscow", "Kiev"));

        PerformanceFilterCondition<List<String>> condition4 =
                new PerformanceFilterCondition<>("class", Operator.EQUALS, "[\"3\"]");
        condition4.setParsedValue(List.of("3"));

        return Stream.of(condition1, condition2, condition3, condition4).collect(toList());
    }

    public static void sortConditions(List<PerformanceFilterCondition> conditions) {
        conditions.sort(PERFORMANCE_FILTER_CONDITION_COMPARATOR);
    }

    /**
     * Сортирует условия и проверяет что фильтры одинаковые
     * (переданные фильтры меняются)
     */
    public static void compareFilters(PerformanceFilter actualFilter, PerformanceFilter expectedFilter,
                                      CompareStrategy compareStrategy) {
        if (expectedFilter.getConditions() != null) {
            sortConditions(expectedFilter.getConditions());
        }
        if (actualFilter.getConditions() != null) {
            sortConditions(actualFilter.getConditions());
        }
        assertThat(actualFilter, beanDiffer(expectedFilter).useCompareStrategy(compareStrategy));
    }

    /**
     * Сортирует условия и проверяет что фильтры одинаковые. Переданные фильтры меняются.
     * BigDecimal поля с ценами сравниваются через compareTo.
     */
    public static void compareFilters(PerformanceFilter actualFilter, PerformanceFilter expectedFilter) {
        CompareStrategy compareStrategy = onlyExpectedFields()
                .forFields(newPath("priceCpa"), newPath("priceCpc")).useDiffer(new BigDecimalDiffer());
        compareFilters(actualFilter, expectedFilter, compareStrategy);
    }
}
