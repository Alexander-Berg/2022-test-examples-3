package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRelevanceMatchCategory;
import ru.yandex.direct.grid.processing.model.showcondition.GdAutotargetingStatInput;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionCategoryStat;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionStatItem;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionTotals;
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod;
import ru.yandex.direct.grid.processing.service.showcondition.converter.TargetingCategory;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.intapi.client.IntApiClient;
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem;
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ShowConditionGraphQlServiceAutotargetingStatTest {

    @Autowired
    private IntApiClient mockedIntApiClient;

    @Autowired
    private ShowConditionGraphQlService service;

    @Before
    public void before() {
        doReturn(new CampaignStatisticsResponse()
                .withData(List.of(
                        new CampaignStatisticsItem()
                                .withTargetingCategory(TargetingCategory.EXACT.getStringValue())
                                .withSearchQuery("test")
                                .withContextCond("phrase1")
                                .withClicks(1L)
                                .withConversions(1L)
                                .withCost(1D)
                                .withCostPerConversion(1D)
                                .withShows(1L),
                        new CampaignStatisticsItem()
                                .withTargetingCategory(TargetingCategory.COMPETITOR.getStringValue())
                                .withContextCond("---autotargeting")
                                .withSearchQuery("test2")
                                .withClicks(2L)
                                .withConversions(2L)
                                .withCost(2D)
                                .withCostPerConversion(2D)
                                .withShows(2L),
                        new CampaignStatisticsItem()
                                .withTargetingCategory(TargetingCategory.COMPETITOR.getStringValue())
                                .withContextCondAsPhrase("---autotargeting")
                                .withSearchQuery("test3")
                                .withClicks(3L)
                                .withConversions(3L)
                                .withCost(3D)
                                .withCostPerConversion(3D)
                                .withShows(3L),
                        new CampaignStatisticsItem()
                                .withTargetingCategory(TargetingCategory.ACCESSORY.getStringValue())
                                .withContextCondAsPhrase("---autotargeting")
                                .withSearchQuery("test4")
                                .withClicks(4L)
                                .withConversions(null)
                                .withCost(4D)
                                .withCostPerConversion(null)
                                .withShows(4L),
                        new CampaignStatisticsItem()
                                .withTargetingCategory(TargetingCategory.UNDEFINED.getStringValue())
                                .withContextCondAsPhrase("---autotargeting")
                                .withSearchQuery("test5")
                                .withClicks(5L)
                                .withConversions(null)
                                .withCost(3D)
                                .withCostPerConversion(null)
                                .withShows(5L)
                )))
                .when(mockedIntApiClient).getCampaignStatistics(any());
    }

    @Test
    public void getAutotargetingStat() {
        Long campaignId = 12345L;
        final var result = service.getAutotargetingStat(
                ContextHelper.buildDefaultContext(),
                null,
                new GdAutotargetingStatInput()
                        .withCampaignId(campaignId)
                        .withPeriod(new GdCampaignStatisticsPeriod()));

        assertThat(result)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                // Порядок важен: первой идёт категория с наибольшим cost
                .isEqualTo(List.of(
                        new GdShowConditionCategoryStat()
                                .withTotals(new GdShowConditionTotals()
                                        .withClicks(BigDecimal.valueOf(5))
                                        .withCost(BigDecimal.valueOf(5.0))
                                        .withShows(BigDecimal.valueOf(5))
                                        .withConversions(BigDecimal.valueOf(5))
                                        .withCostPerConversion(BigDecimal.valueOf(1.0)))
                                .withCategory(GdRelevanceMatchCategory.COMPETITOR_MARK)
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(new GdShowConditionTotals()
                                                        .withClicks(BigDecimal.valueOf(3))
                                                        .withCost(BigDecimal.valueOf(3.0))
                                                        .withShows(BigDecimal.valueOf(3))
                                                        .withConversions(BigDecimal.valueOf(3))
                                                        .withCostPerConversion(BigDecimal.valueOf(3.0)))
                                                .withSearchQuery("test3"),
                                        new GdShowConditionStatItem()
                                                .withTotals(new GdShowConditionTotals()
                                                        .withClicks(BigDecimal.valueOf(2))
                                                        .withCost(BigDecimal.valueOf(2.0))
                                                        .withShows(BigDecimal.valueOf(2))
                                                        .withConversions(BigDecimal.valueOf(2))
                                                        .withCostPerConversion(BigDecimal.valueOf(2.0)))
                                                .withSearchQuery("test2"))),
                        new GdShowConditionCategoryStat()
                                .withTotals(new GdShowConditionTotals()
                                        .withClicks(BigDecimal.valueOf(4))
                                        .withCost(BigDecimal.valueOf(4.0))
                                        .withShows(BigDecimal.valueOf(4))
                                        .withConversions(BigDecimal.valueOf(0))
                                        .withCostPerConversion(BigDecimal.valueOf(0.0)))
                                .withCategory(GdRelevanceMatchCategory.ACCESSORY_MARK)
                                .withSearchQueries(List.of(
                                        new GdShowConditionStatItem()
                                                .withTotals(new GdShowConditionTotals()
                                                        .withClicks(BigDecimal.valueOf(4))
                                                        .withCost(BigDecimal.valueOf(4.0))
                                                        .withShows(BigDecimal.valueOf(4))
                                                        .withConversions(BigDecimal.valueOf(0))
                                                        .withCostPerConversion(BigDecimal.valueOf(0.0)))
                                                .withSearchQuery("test4"))),
                        new GdShowConditionCategoryStat()
                                .withTotals(new GdShowConditionTotals()
                                        .withClicks(BigDecimal.valueOf(5))
                                        .withCost(BigDecimal.valueOf(3.0))
                                        .withShows(BigDecimal.valueOf(5))
                                        .withConversions(BigDecimal.valueOf(0))
                                        .withCostPerConversion(BigDecimal.valueOf(0.0)))
                                .withCategory(null)
                                .withSearchQueries(List.of()))
                );
    }
}
