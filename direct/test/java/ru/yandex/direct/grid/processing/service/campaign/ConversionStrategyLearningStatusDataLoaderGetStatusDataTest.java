package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dataloader.BatchLoaderEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdCampaignAggregatedStatusInfo;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdDynamicCampaign;
import ru.yandex.direct.grid.model.campaign.strategy.GdCampaignStrategyWeekBudget;
import ru.yandex.direct.grid.model.campaign.strategy.GdStrategyType;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyExtractorFacade;
import ru.yandex.direct.grid.model.entity.campaign.strategy.GdStrategyWeekBudgetExtractor;


@RunWith(Parameterized.class)
public class ConversionStrategyLearningStatusDataLoaderGetStatusDataTest {

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public Set<GdCampaign> campaigns;

    @Parameterized.Parameter(2)
    public boolean isEnabledCrrStrategy;

    @Parameterized.Parameter(3)
    public int expectedGridCampaignServiceCalls;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] params() {
        return new Object[][]{
                {
                    "есть кампании для которых необходимо вычислить статус обучения",
                        Set.of(new GdDynamicCampaign()
                                .withAggregatedStatusInfo(new GdCampaignAggregatedStatusInfo().withStatus(GdSelfStatusEnum.RUN_OK))
                                .withStrategy(new GdCampaignStrategyWeekBudget()
                                        .withStrategyType(GdStrategyType.WEEK_BUDGET)
                                        .withGoalId(0L))
                                .withFlatStrategy(new GdCampaignStrategyWeekBudget()
                                        .withStrategyType(GdStrategyType.WEEK_BUDGET)
                                        .withLastBidderRestartTime(LocalDate.now()))),
                        true,
                        2
                },
                {
                    "нет кампаний для которых необходимо вычислить статус обучения",
                        Set.of(new GdDynamicCampaign()
                                .withAggregatedStatusInfo(new GdCampaignAggregatedStatusInfo().withStatus(GdSelfStatusEnum.ON_MODERATION))),
                        true,
                        0
                }
        };
    }

    private BatchLoaderEnvironment environment;
    private GridCampaignService gridCampaignService;
    private GdStrategyExtractorFacade gdStrategyExtractorFacade;

    @Before
    public void beforeEach() {
        this.environment = BatchLoaderEnvironment.newBatchLoaderEnvironment().build();
        this.gdStrategyExtractorFacade = new GdStrategyExtractorFacade(Map.of(GdStrategyType.WEEK_BUDGET, new GdStrategyWeekBudgetExtractor()));
        this.gridCampaignService = Mockito.mock(GridCampaignService.class);
    }

    @Test
    public void test() {
        Mockito.when(gridCampaignService.getCampaignGoalStatsWithOptimizationForDifferentDateRanges(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new HashMap<>());
        ConversionStrategyLearningStatusDataLoader.getStatusDataByCampaign(
                campaigns,
                environment,
                gridCampaignService,
                gdStrategyExtractorFacade,
                isEnabledCrrStrategy
        );
        Mockito.verify(gridCampaignService, Mockito.times(expectedGridCampaignServiceCalls)).getCampaignGoalStatsWithOptimizationForDifferentDateRanges(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
