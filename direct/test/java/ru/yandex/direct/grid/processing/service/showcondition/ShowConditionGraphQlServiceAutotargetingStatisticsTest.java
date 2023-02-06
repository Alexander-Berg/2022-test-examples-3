package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRelevanceMatchCategory;
import ru.yandex.direct.grid.processing.model.showcondition.GdAutotargetingStatInput;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionCategoryStat;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionStatItem;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionTotals;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrder;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrderBy;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrderByField;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod;
import ru.yandex.direct.grid.processing.service.showcondition.converter.TargetingCategory;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.intapi.client.IntApiClient;
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem;
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ShowConditionGraphQlServiceAutotargetingStatisticsTest {
    @Autowired
    private IntApiClient mockedIntApi;

    @Autowired
    private ShowConditionGraphQlService service;

    private GridGraphQLContext gridGraphQLContext;
    private GdClient gdClient;

    private final Long campaignId = 12354L;

    @Before
    public void before() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        gdClient = new GdClient()
                .withInfo(gridGraphQLContext.getQueriedClient());
    }

    private CampaignStatisticsResponse defaultStatisticsResponse() {
        return new CampaignStatisticsResponse()
                .withData(List.of(
                        createStatisticsItem(TargetingCategory.COMPETITOR, "test2", 0L, 0L, null, null, 3L),
                        createStatisticsItem(TargetingCategory.BROADER, "test3", 1L, 1L, 10.0, 10.0, 200L),
                        createStatisticsItem(TargetingCategory.BROADER, "test1", 1L, null, 2.54, 0.0, 803L),
                        createStatisticsItem(TargetingCategory.BROADER, "test5", null, 1L, null, 0.0, null),
                        createStatisticsItem(TargetingCategory.EXACT, "test4", 20L, 20L, 116.56, 5.83, 562L)
                ))
                .withTotals(createTotals(22L, 22L, 129.1, 5.87, 1568L));
    }

    private CampaignStatisticsItem createStatisticsItem(TargetingCategory category, String searchQuery,
                                                        Long clicks, Long conversions, Double cost,
                                                        Double costPerConversion, Long shows) {
        return new CampaignStatisticsItem()
                .withTargetingCategory(category.getStringValue())
                .withContextCond("---autotargeting")
                .withSearchQuery(searchQuery)
                .withClicks(clicks)
                .withConversions(conversions)
                .withCost(cost)
                .withCostPerConversion(costPerConversion)
                .withShows(shows);
    }

    private CampaignStatisticsItem createTotals(Long clicks, Long conversions, Double cost,
                                                Double costPerConversion, Long shows) {
        return new CampaignStatisticsItem()
                .withClicks(clicks)
                .withConversions(conversions)
                .withCost(cost)
                .withCostPerConversion(costPerConversion)
                .withShows(shows);
    }


    /**
     * Получаем статистику по всем категориям и по всем элементам уже отсортированной (дефолтная сортировка);
     * с нашей стороны считаем totals на уровне категорий и глобально, а также группируем по категориям и сортируем их.
     */
    @Test
    public void getAutotargetingStatistics_NoLimitOffset_NoOrderBy_NoCategory() {
        doReturn(defaultStatisticsResponse())
                .when(mockedIntApi).getCampaignStatistics(any());

        final var result = service.getAutotargetingStatistics(
                gridGraphQLContext,
                gdClient,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod()));


        var campaignTotals = result.getTotals();
        var categories = result.getRowset();
        final var softAssertions = new SoftAssertions();

        softAssertions
                .assertThat(campaignTotals)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getGdShowConditionTotals(22L, 22L, 129.1, 5.87, 1568L));

        softAssertions
                .assertThat(categories)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.EXACT_MARK)
                                .withTotals(getGdShowConditionTotals(20L, 20L, 116.56, 5.83, 562L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(20L, 20L, 116.56, 5.83, 562L))
                                                .withSearchQuery("test4"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.BROADER_MARK)
                                .withTotals(getGdShowConditionTotals(2L, 2L, 12.54, 6.27, 1003L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 1L, 10.0, 10.0, 200L))
                                                .withSearchQuery("test3"),
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                                .withSearchQuery("test1"),
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(0L, 1L, 0.0, 0.0, 0L))
                                                .withSearchQuery("test5"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.COMPETITOR_MARK)
                                .withTotals(getGdShowConditionTotals(0L, 0L, 0.0, 0.0, 3L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(0L, 0L, 0.0, 0.0, 3L))
                                                .withSearchQuery("test2")))
                ));

        softAssertions.assertAll();
    }

    /**
     * Получаем статистику по всем категориям и по всем элементам уже отсортированной (дефолтная сортировка);
     * с нашей стороны считаем totals на уровне категорий и глобально, группируем по категориям и сортируем их,
     * а также применяем limit и offset на число элементов каждой категории.
     */
    @Test
    public void getAutotargetingStatistics_WithLimitOffset_NoOrderBy_NoCategory() {
        doReturn(defaultStatisticsResponse())
                .when(mockedIntApi).getCampaignStatistics(any());

        final var result = service.getAutotargetingStatistics(
                gridGraphQLContext,
                gdClient,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod())
                        .withLimitOffset(new GdLimitOffset()
                                .withOffset(1)
                                .withLimit(1)));

        var campaignTotals = result.getTotals();
        var categories = result.getRowset();
        final var softAssertions = new SoftAssertions();

        softAssertions
                .assertThat(campaignTotals)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getGdShowConditionTotals(22L, 22L, 129.1, 5.87, 1568L));

        softAssertions
                .assertThat(categories)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.EXACT_MARK)
                                .withTotals(getGdShowConditionTotals(20L, 20L, 116.56, 5.83, 562L))
                                .withSearchQueries(List.of()),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.BROADER_MARK)
                                .withTotals(getGdShowConditionTotals(2L, 2L, 12.54, 6.27, 1003L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                                .withSearchQuery("test1"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.COMPETITOR_MARK)
                                .withTotals(getGdShowConditionTotals(0L, 0L, 0.0, 0.0, 3L))
                                .withSearchQueries(List.of())
                ));

        softAssertions.assertAll();
    }

    /**
     * Получаем статистику по всем категориям и по всем элементам уже отсортированной (кастомная сортировка);
     * с нашей стороны считаем totals на уровне категорий и глобально, группируем по категориям и сортируем их,
     * а также применяем limit и offset на число элементов каждой категории.
     */
    @Test
    public void getAutotargetingStatistics_WithLimitOffset_WithOrderBy_NoCategory() {
        doReturn(new CampaignStatisticsResponse()
                        .withData(List.of(
                                createStatisticsItem(TargetingCategory.COMPETITOR, "test2", 0L, 0L, null, null, 3L),
                                createStatisticsItem(TargetingCategory.BROADER, "test1", 1L, null, 2.54, 0.0, 803L),
                                createStatisticsItem(TargetingCategory.BROADER, "test3", 1L, 1L, 10.0, 10.0, 200L),
                                createStatisticsItem(TargetingCategory.BROADER, "test5", null, 1L, null, 0.0, null),
                                createStatisticsItem(TargetingCategory.EXACT, "test4", 20L, 20L, 116.56, 5.83, 562L)
                        ))
                        .withTotals(createTotals(22L, 22L, 129.1, 5.87, 1568L)))
                .when(mockedIntApi).getCampaignStatistics(any());

        final var result = service.getAutotargetingStatistics(
                gridGraphQLContext,
                gdClient,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod())
                        .withLimitOffset(new GdLimitOffset()
                                .withOffset(0)
                                .withLimit(2))
                        .withOrderBy(new GdCampaignStatisticsOrderBy()
                                .withField(GdCampaignStatisticsOrderByField.SHOWS)
                                .withOrder(GdCampaignStatisticsOrder.DESC)));


        var campaignTotals = result.getTotals();
        var categories = result.getRowset();
        final var softAssertions = new SoftAssertions();

        softAssertions
                .assertThat(campaignTotals)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getGdShowConditionTotals(22L, 22L, 129.1, 5.87, 1568L));

        softAssertions
                .assertThat(categories)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.BROADER_MARK)
                                .withTotals(getGdShowConditionTotals(2L, 2L, 12.54, 6.27, 1003L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                                .withSearchQuery("test1"),
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 1L, 10.0, 10.0, 200L))
                                                .withSearchQuery("test3"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.EXACT_MARK)
                                .withTotals(getGdShowConditionTotals(20L, 20L, 116.56, 5.83, 562L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(20L, 20L, 116.56, 5.83, 562L))
                                                .withSearchQuery("test4"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.COMPETITOR_MARK)
                                .withTotals(getGdShowConditionTotals(0L, 0L, 0.0, 0.0, 3L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(0L, 0L, 0.0, 0.0, 3L))
                                                .withSearchQuery("test2")))
                ));

        softAssertions.assertAll();
    }

    /**
     * Получаем статистику по 1 определенной категории уже отсортированной (кастомная сортировка) сразу с offset-limit
     * элементов; totals берем как есть, группируем результаты в категорию.
     */
    @Test
    public void getAutotargetingStatistics_WithLimitOffset_WithOrderBy_WithCategory() {
        doReturn(new CampaignStatisticsResponse()
                        .withData(List.of(
                                createStatisticsItem(TargetingCategory.BROADER, "test5", null, 1L, null, 0.0, null),
                                createStatisticsItem(TargetingCategory.BROADER, "test3", 1L, 1L, 10.0, 10.0, 200L),
                                createStatisticsItem(TargetingCategory.BROADER, "test1", 1L, null, 2.54, 0.0, 803L)
                        ))
                        .withTotals(createTotals(2L, 2L, 12.54, 6.27, 1003L)))
                .when(mockedIntApi).getCampaignStatistics(any());

        final var result = service.getAutotargetingStatistics(
                ContextHelper.buildDefaultContext(),
                gdClient,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod())
                        .withLimitOffset(new GdLimitOffset()
                                .withOffset(0)
                                .withLimit(3))
                        .withOrderBy(new GdCampaignStatisticsOrderBy()
                                .withField(GdCampaignStatisticsOrderByField.SHOWS)
                                .withOrder(GdCampaignStatisticsOrder.ASC))
                        .withRelevanceMatchCategory(List.of(GdRelevanceMatchCategory.BROADER_MARK)));


        var campaignTotals = result.getTotals();
        var categories = result.getRowset();
        final var softAssertions = new SoftAssertions();

        softAssertions
                .assertThat(campaignTotals)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(getGdShowConditionTotals(2L, 2L, 12.54, 6.27, 1003L));

        softAssertions
                .assertThat(categories)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(
                        new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.BROADER_MARK)
                                .withTotals(getGdShowConditionTotals(2L, 2L, 12.54, 6.27, 1003L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(0L, 1L, 0.0, 0.0, 0L))
                                                .withSearchQuery("test5"),
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 1L, 10.0, 10.0, 200L))
                                                .withSearchQuery("test3"),
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                                .withSearchQuery("test1")))
                ));

        softAssertions.assertAll();
    }

    /**
     * Случай, когда есть статистика по автотаргетингу без категории.
     */
    @Test
    public void getAutotargetingStatistics_NoLimitOffset_NoOrderBy_NoCategory_WithUndefinedCategory() {
        doReturn(new CampaignStatisticsResponse()
                .withData(List.of(
                        createStatisticsItem(TargetingCategory.BROADER, "test1", 1L, null, 2.54, 0.0, 803L),
                        createStatisticsItem(TargetingCategory.UNDEFINED, "query 1", 10L, 1L, 1.01, 1.01, 200L),
                        createStatisticsItem(TargetingCategory.UNDEFINED, "query 2", 11L, null, 1.02, 0.0, 201L)
                ))
                .withTotals(createTotals(22L, 1L, 4.57, 4.57, 1204L)))
                .when(mockedIntApi).getCampaignStatistics(any());

        final var result = service.getAutotargetingStatistics(
                gridGraphQLContext,
                gdClient,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod()));


        var campaignTotals = result.getTotals();
        var categories = result.getRowset();
        final var softAssertions = new SoftAssertions();

        softAssertions
                .assertThat(campaignTotals)
                .isEqualTo(getGdShowConditionTotals(22L, 1L, 4.57, 4.57, 1204L));

        softAssertions
                .assertThat(categories)
                .hasSize(2);

        softAssertions
                .assertThat(categories)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(new GdShowConditionCategoryStat()
                                .withCategory(GdRelevanceMatchCategory.BROADER_MARK)
                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(getGdShowConditionTotals(1L, 0L, 2.54, 0.0, 803L))
                                                .withSearchQuery("test1"))),
                        new GdShowConditionCategoryStat()
                                .withCategory(null)
                                .withTotals(getGdShowConditionTotals(21L, 1L, 2.03, 2.03, 401L))
                                .withSearchQueries(List.of())
                        )
                );

        softAssertions.assertAll();
    }

    private GdShowConditionTotals getGdShowConditionTotals(long clicks, long conversions, double cost,
                                                           double costPerConversion, long shows) {
        return new GdShowConditionTotals()
                .withClicks(BigDecimal.valueOf(clicks))
                .withConversions(BigDecimal.valueOf(conversions))
                .withCost(BigDecimal.valueOf(cost))
                .withCostPerConversion(BigDecimal.valueOf(costPerConversion))
                .withShows(BigDecimal.valueOf(shows));
    }
}
