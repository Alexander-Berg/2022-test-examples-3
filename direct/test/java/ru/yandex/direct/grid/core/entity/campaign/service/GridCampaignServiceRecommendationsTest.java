package ru.yandex.direct.grid.core.entity.campaign.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.goal.service.CampaignConversionPriceForGoalsWithCategoryCpaSource;
import ru.yandex.direct.core.entity.goal.service.ConversionPriceForecastService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridCampaignYtRepository;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.GdiGoalConversion;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignGoalsCostPerAction;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCpaSource;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiGoalCostPerAction;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.feature.FeatureName.CATEGORY_CPA_SOURCE_FOR_CONVERSION_PRICE_RECOMMENDATION;
import static ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService.toGdiCampaignGoalCostPerActionForCategory;
import static ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService.toGdiCampaignGoalCostPerActionForLogin;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getDefaultCampaignStatsWithEmptyGoalStat;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getDefaultEntityStats;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getEntityStats;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class GridCampaignServiceRecommendationsTest {
    private static final long CLIENT_ID_L = 123L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(CLIENT_ID_L);

    public static final LocalDate END_DATE = LocalDate.now();
    public static final LocalDate START_DATE = END_DATE.minusDays(7L);

    //кампании по которым запрашивается статистика на кампанию
    public static final Long CAMPAIGN_ID1 = 1L;
    public static final Long CAMPAIGN_ID2 = 2L;
    public static final Long CAMPAIGN_ID3 = 3L;
    public static final Long CAMPAIGN_ID4 = 4L;
    public static final Long CAMPAIGN_ID5 = 5L;
    public static final List<Long> SINGLE_CAMPAIGN_ID = List.of(CAMPAIGN_ID1);

    //кампании клиента по которым запрашивается статистика для вычисления среднего costPerAction
    public static final Set<Long> ALL_CLIENT_CAMPAIGN_IDS = Set.of(CAMPAIGN_ID1, CAMPAIGN_ID2, CAMPAIGN_ID3,
            CAMPAIGN_ID4, CAMPAIGN_ID5);

    public static final Long COUNTER_ID = 1001L;
    public static final Long GOAL_ID1 = 50001L;
    public static final Long GOAL_ID2 = 50002L;
    public static final Long GOAL_ID3 = 50003L;
    public static final Set<Long> GOAL_IDS = Set.of(GOAL_ID1, GOAL_ID2, GOAL_ID3);

    //default_cost_per_action = default_cost / default_goal_action_count
    public static final Long DEFAULT_COST = 10000L;
    public static final Map<Long, Long> DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS = Map.of(
            GOAL_ID1, 10L,
            GOAL_ID2, 20L,
            GOAL_ID3, 40L);
    public static final Map<Long, BigDecimal> DEFAULT_GOALS_COST_PER_ACTION_BY_GOAL_IDS = Map.of(
            GOAL_ID1, BigDecimal.valueOf(1000L),
            GOAL_ID2, BigDecimal.valueOf(500L),
            GOAL_ID3, BigDecimal.valueOf(250L));

    public static final String URL1 = "https://www.velostrana.ru/";

    public static final BigDecimal DEFAULT_CONVERSION_RATE = BigDecimal.valueOf(0.15);

    @Mock
    private CampaignService campaignService;

    @Mock
    private FeatureService featureService;

    @Mock
    private CampMetrikaCountersService campMetrikaCountersService;

    @Mock
    private MetrikaClient metrikaClient;

    @InjectMocks
    private GridCampaignService gridCampaignService;

    @Mock
    private GridCampaignYtRepository gridCampaignYtRepository;

    @Mock
    private ConversionPriceForecastService conversionPriceForecastService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(Map.of(CAMPAIGN_ID1, getDefaultEntityStats()))
                .when(gridCampaignYtRepository)
                .getCampaignEntityStats(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE));

        doReturn(getDefaultEntityStatsForAllCampaigns())
                .when(gridCampaignYtRepository)
                .getCampaignEntityStats(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE));

        doReturn(true).when(featureService).isEnabledForClientId(CLIENT_ID,
                CATEGORY_CPA_SOURCE_FOR_CONVERSION_PRICE_RECOMMENDATION);
    }

    public static Map<Long, GdiEntityStats> getDefaultEntityStatsForAllCampaigns() {
        return listToMap(ALL_CLIENT_CAMPAIGN_IDS, id -> id, id -> getEntityStats(DEFAULT_COST));
    }

    public static Map<Long, List<GdiGoalConversion>> getGoalConversionForCampaigns(Collection<Long> campaignIds,
                                                                                   List<GdiGoalConversion> goalsConversions) {
        return listToMap(campaignIds, id -> id, id -> goalsConversions);
    }

    public static List<GdiGoalConversion> getGoalsConversions(Map<Long, Long> goalsActionsCountByGoalIds) {
        return mapList(goalsActionsCountByGoalIds.keySet(),
                goalId -> new GdiGoalConversion()
                        .withGoalId(goalId)
                        .withGoals(goalsActionsCountByGoalIds.get(goalId)));
    }

    public static List<CounterGoal> getTestCounterGoals(Collection<Long> goalIds) {
        return mapList(goalIds, goal -> new CounterGoal()
                .withId(goal.intValue())
                .withName(RandomStringUtils.random(6))
                .withParentId(0L)
                .withType(CounterGoal.Type.ACTION));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_singleGoals_goalWithCampaignStat() {
        //запрос рекомендаций при наличии статистики на кампании
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.of(GOAL_ID1));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);
        Long goalActionCount = 10L;

        doReturn(getGoalConversionForCampaigns(Set.of(CAMPAIGN_ID1), getGoalsConversions(Map.of(GOAL_ID1,
                goalActionCount))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));


        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID1)
                                        .withCostPerAction(BigDecimal.valueOf(DEFAULT_COST)
                                                .divide(BigDecimal.valueOf(goalActionCount), RoundingMode.HALF_UP))
                                        .withSource(GdiCpaSource.CAMPAIGN)
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_allGoalsWithCampaignStat() {
        //запрос рекомендаций при наличии статистики на кампании
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        doReturn(getGoalConversionForCampaigns(Set.of(CAMPAIGN_ID1),
                getGoalsConversions(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS)))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));


        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID1)
                                        .withCostPerAction(DEFAULT_GOALS_COST_PER_ACTION_BY_GOAL_IDS.get(GOAL_ID1))
                                        .withSource(GdiCpaSource.CAMPAIGN),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID2)
                                        .withCostPerAction(DEFAULT_GOALS_COST_PER_ACTION_BY_GOAL_IDS.get(GOAL_ID2))
                                        .withSource(GdiCpaSource.CAMPAIGN),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID3)
                                        .withCostPerAction(DEFAULT_GOALS_COST_PER_ACTION_BY_GOAL_IDS.get(GOAL_ID3))
                                        .withSource(GdiCpaSource.CAMPAIGN)
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_oneGoalHasNoStatPerCamp() {
        Map<Long, Long> goalConversionCountByGoalId = Map.of(GOAL_ID1, 2000L, GOAL_ID2, 400L);
        Long goalActionCount = 250L;

        //запрос в статистику по конкретной кампании
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));

        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        doReturn(getGoalConversionForCampaigns(Set.of(CAMPAIGN_ID1), getGoalsConversions(goalConversionCountByGoalId)))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(Map.of(GOAL_ID3,
                goalActionCount))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID3)));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID1)
                                        .withCostPerAction(BigDecimal.valueOf(DEFAULT_COST)
                                                .divide(BigDecimal.valueOf(goalConversionCountByGoalId.get(GOAL_ID1)),
                                                        RoundingMode.HALF_EVEN))
                                        .withSource(GdiCpaSource.CAMPAIGN),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID2)
                                        .withCostPerAction(BigDecimal.valueOf(DEFAULT_COST)
                                                .divide(BigDecimal.valueOf(goalConversionCountByGoalId.get(GOAL_ID2)),
                                                        RoundingMode.HALF_EVEN))
                                        .withSource(GdiCpaSource.CAMPAIGN),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID3)
                                        .withCostPerAction(BigDecimal.valueOf(40L))
                                        .withSource(GdiCpaSource.LOGIN)
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_allCpaSources() {
        Map<Long, Long> goalConversionCountByGoalId = Map.of(GOAL_ID1, 2000L);
        Long goalActionCount = 250L;

        //запрос в статистику по конкретной кампании
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));

        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        doReturn(getGoalConversionForCampaigns(Set.of(CAMPAIGN_ID1), getGoalsConversions(goalConversionCountByGoalId)))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(Map.of(GOAL_ID3,
                goalActionCount))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID3, GOAL_ID2)));

        doReturn(
                Map.of(
                        CAMPAIGN_ID1,
                        new CampaignConversionPriceForGoalsWithCategoryCpaSource(Map.of(GOAL_ID2,
                                BigDecimal.valueOf(567L)))))
                .when(conversionPriceForecastService)
                .getRecommendedConversionPriceByGoalIds(eq(CLIENT_ID), eq(Set.of(GOAL_ID2)), eq(urlsByCampaignId));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID1)
                                        .withCostPerAction(BigDecimal.valueOf(DEFAULT_COST)
                                                .divide(BigDecimal.valueOf(goalConversionCountByGoalId.get(GOAL_ID1)),
                                                        RoundingMode.HALF_EVEN))
                                        .withSource(GdiCpaSource.CAMPAIGN),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID2)
                                        .withCostPerAction(BigDecimal.valueOf(567L))
                                        .withSource(GdiCpaSource.CATEGORY),
                                new GdiGoalCostPerAction()
                                        .withGoalId(GOAL_ID3)
                                        .withCostPerAction(BigDecimal.valueOf(40L))
                                        .withSource(GdiCpaSource.LOGIN)
                        ))));
    }


    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_singleGoal_requestStatPerLogin() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.of(GOAL_ID1));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));


        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS,
                getGoalsConversions(Map.of(GOAL_ID1, 50L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(
                                List.of(toGdiCampaignGoalCostPerActionForLogin(GOAL_ID1, BigDecimal.valueOf(200L)))
                        )));
    }


    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_requestStatPerLogin() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE), eq(GOAL_IDS));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);


        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS,
                getGoalsConversions(Map.of(GOAL_ID1, 10L, GOAL_ID2, 20L, GOAL_ID3, 40L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                toGdiCampaignGoalCostPerActionForLogin(GOAL_ID1, BigDecimal.valueOf(1000L)),
                                toGdiCampaignGoalCostPerActionForLogin(GOAL_ID2, BigDecimal.valueOf(500L)),
                                toGdiCampaignGoalCostPerActionForLogin(GOAL_ID3, BigDecimal.valueOf(250L))
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_singleGoal_emptyStat_noMeanPriceByCategory() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.of(GOAL_ID1));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        Map<Long, GdiCampaignStats> campaignsStatsByCampaignId = listToMap(ALL_CLIENT_CAMPAIGN_IDS, id -> id,
                id -> getDefaultCampaignStatsWithEmptyGoalStat());

        doReturn(campaignsStatsByCampaignId)
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE), eq(Set.of(GOAL_ID1)));

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        doReturn(Map.of(CAMPAIGN_ID1, new CampaignConversionPriceForGoalsWithCategoryCpaSource(emptyMap())))
                .when(conversionPriceForecastService)
                .getRecommendedConversionPriceByGoalIds(eq(CLIENT_ID), eq(Set.of(GOAL_ID1)), eq(urlsByCampaignId));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(emptyList())));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_singleGoal_emptyStat_oneCampHasMeanPriceByCategory() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.of(GOAL_ID1));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        Map<Long, GdiCampaignStats> campaignsStatsByCampaignId = listToMap(ALL_CLIENT_CAMPAIGN_IDS, id -> id,
                id -> getDefaultCampaignStatsWithEmptyGoalStat());

        doReturn(campaignsStatsByCampaignId)
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE), eq(Set.of(GOAL_ID1)));

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        doReturn(
                Map.of(
                        CAMPAIGN_ID1,
                        new CampaignConversionPriceForGoalsWithCategoryCpaSource(Map.of(GOAL_ID1,
                                BigDecimal.valueOf(100L)))))
                .when(conversionPriceForecastService)
                .getRecommendedConversionPriceByGoalIds(eq(CLIENT_ID), eq(Set.of(GOAL_ID1)), eq(urlsByCampaignId));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                toGdiCampaignGoalCostPerActionForCategory(GOAL_ID1, BigDecimal.valueOf(100L))
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_emptyStat_oneCampHasMeanPriceByCategory() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        Map<Long, GdiCampaignStats> campaignsStatsByCampaignId = listToMap(ALL_CLIENT_CAMPAIGN_IDS, id -> id,
                id -> getDefaultCampaignStatsWithEmptyGoalStat());

        doReturn(campaignsStatsByCampaignId)
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE), eq(GOAL_IDS));

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        doReturn(Map.of(
                CAMPAIGN_ID1,
                new CampaignConversionPriceForGoalsWithCategoryCpaSource(Map.of(
                        GOAL_ID1, BigDecimal.valueOf(100L),
                        GOAL_ID2, BigDecimal.valueOf(200L),
                        GOAL_ID3, BigDecimal.valueOf(300L)))))
                .when(conversionPriceForecastService)
                .getRecommendedConversionPriceByGoalIds(eq(CLIENT_ID), eq(GOAL_IDS), eq(urlsByCampaignId));

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(List.of(
                                toGdiCampaignGoalCostPerActionForCategory(GOAL_ID1, BigDecimal.valueOf(100L)),
                                toGdiCampaignGoalCostPerActionForCategory(GOAL_ID2, BigDecimal.valueOf(200L)),
                                toGdiCampaignGoalCostPerActionForCategory(GOAL_ID3, BigDecimal.valueOf(300L))
                        ))));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInSpecifiedCampaigns_severalGoals_emptyStat_noMeanPriceByCategory() {
        Map<Long, List<Long>> goalIdsByCampaignId = Map.of(CAMPAIGN_ID1, List.copyOf(GOAL_IDS));
        Map<Long, String> urlsByCampaignId = Map.of(CAMPAIGN_ID1, URL1);

        //запрос в статистику по конкретной кампании
        doReturn(Map.of(CAMPAIGN_ID1, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(Set.of(CAMPAIGN_ID1)), eq(START_DATE), eq(END_DATE), eq(GOAL_IDS));

        //запрос в статистику по всем кампаниям
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        Map<Long, GdiCampaignStats> campaignsStatsByCampaignId = listToMap(ALL_CLIENT_CAMPAIGN_IDS, id -> id,
                id -> getDefaultCampaignStatsWithEmptyGoalStat());

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        doReturn(Map.of(CAMPAIGN_ID1, new CampaignConversionPriceForGoalsWithCategoryCpaSource(emptyMap())))
                .when(conversionPriceForecastService)
                .getRecommendedConversionPriceByGoalIds(CLIENT_ID, GOAL_IDS, urlsByCampaignId);

        var result = gridCampaignService.getAverageCostPerActionForGoalsInSpecifiedCampaigns(
                CLIENT_ID, goalIdsByCampaignId, urlsByCampaignId, START_DATE, END_DATE);

        assertThat(result)
                .contains(entry(CAMPAIGN_ID1, new GdiCampaignGoalsCostPerAction()
                        .withGoalsCostPerAction(emptyList())));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_goalWithStat() {
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(Map.of(GOAL_ID1, 10L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        var result = gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(
                CLIENT_ID, Set.of(GOAL_ID1), START_DATE, END_DATE);

        //средняя цена конверсии = сумма всех cost's в кампаниях / сумма всех достижений цели в этих компаниях
        //для 5ти кампаний с одинаковыми значениями: DEFAULT_COST * 5 / (goalActionCount * 5) = 50000 / 50 = 1000;
        assertThat(result).contains(entry(GOAL_ID1, BigDecimal.valueOf(1000L)));
    }

    @Test
    //если включена фича USE_CAMPAIGN_WITH_GOAL_COUNTERS_FOR_RECOMMENDED_COST_PER_ACTION_ESTIMATION_ENABLED
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_goalWithStat_allCampaignsHasGoalCounter() {

        doReturn(Map.of(CAMPAIGN_ID1, List.of(COUNTER_ID),
                CAMPAIGN_ID2, List.of(COUNTER_ID),
                CAMPAIGN_ID3, List.of(COUNTER_ID),
                CAMPAIGN_ID4, List.of(COUNTER_ID),
                CAMPAIGN_ID5, List.of(COUNTER_ID)))
                .when(campMetrikaCountersService).getCounterByCampaignIds(CLIENT_ID,
                ALL_CLIENT_CAMPAIGN_IDS);

        doReturn(Map.of(COUNTER_ID.intValue(), getTestCounterGoals(GOAL_IDS)))
                .when(metrikaClient).getMassCountersGoalsFromMetrika(Set.of(COUNTER_ID.intValue()));

        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(Map.of(CAMPAIGN_ID1, getGoalsConversions(Map.of(GOAL_ID1, 10L)),
                CAMPAIGN_ID2, getGoalsConversions(Map.of(GOAL_ID1, 0L)),
                CAMPAIGN_ID3, getGoalsConversions(Map.of(GOAL_ID1, 0L)),
                CAMPAIGN_ID4, getGoalsConversions(Map.of(GOAL_ID1, 0L)),
                CAMPAIGN_ID5, getGoalsConversions(Map.of(GOAL_ID1, 0L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        var result = gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(
                CLIENT_ID, Set.of(GOAL_ID1), START_DATE, END_DATE);

        //конверсии по цели есть только в одной кампании, но счетчик от этой цели используется во всех кампаниях.
        //При включенной фиче для расчета рекомендации включаем в расчет cost со всех кампаний,
        // где есть счетчик этой цели.
        //для 5ти кампаний с одинаковыми значениями: DEFAULT_COST * 5 / (goalActionCount) = 50000 / 10 = 5000;
        assertThat(result).contains(entry(GOAL_ID1, BigDecimal.valueOf(5000L)));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_severalGoalWithStat() {

        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(
                Map.of(GOAL_ID1, 10L, GOAL_ID2, 20L, GOAL_ID3, 40L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        var result = gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(
                CLIENT_ID, GOAL_IDS, START_DATE, END_DATE);

        //средняя цена конверсии = сумма всех cost's в кампаниях / сумма всех достижений цели в этих компаниях
        //достижения целей в 5 кампаниях: GOAL_ID1: 10 * 5, GOAL_ID2: 20 * 5, GOAL_ID3: 40 * 5
        assertThat(result).containsOnly(
                entry(GOAL_ID1, BigDecimal.valueOf(1000L)),
                entry(GOAL_ID2, BigDecimal.valueOf(500L)),
                entry(GOAL_ID3, BigDecimal.valueOf(250L)));
    }

    @Test
    //если включена фича USE_CAMPAIGN_WITH_GOAL_COUNTERS_FOR_RECOMMENDED_COST_PER_ACTION_ESTIMATION_ENABLED
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_severalGoalWithStat_allCampaignsHasGoalCounter() {

        doReturn(Map.of(CAMPAIGN_ID1, List.of(COUNTER_ID),
                CAMPAIGN_ID2, List.of(COUNTER_ID),
                CAMPAIGN_ID3, List.of(COUNTER_ID),
                CAMPAIGN_ID4, List.of(COUNTER_ID),
                CAMPAIGN_ID5, List.of(COUNTER_ID)))
                .when(campMetrikaCountersService).getCounterByCampaignIds(CLIENT_ID,
                ALL_CLIENT_CAMPAIGN_IDS);

        doReturn(Map.of(COUNTER_ID.intValue(), getTestCounterGoals(GOAL_IDS)))
                .when(metrikaClient).getMassCountersGoalsFromMetrika(Set.of(COUNTER_ID.intValue()));

        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(
                Map.of(GOAL_ID1, 10L, GOAL_ID2, 20L, GOAL_ID3, 40L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        doReturn(Map.of(CAMPAIGN_ID1, getGoalsConversions(Map.of(GOAL_ID1, 10L, GOAL_ID2, 0L, GOAL_ID3, 0L)),
                CAMPAIGN_ID2, getGoalsConversions(Map.of(GOAL_ID1, 0L, GOAL_ID2, 20L, GOAL_ID3, 0L)),
                CAMPAIGN_ID3, getGoalsConversions(Map.of(GOAL_ID1, 0L, GOAL_ID2, 0L, GOAL_ID3, 40L)),
                CAMPAIGN_ID4, getGoalsConversions(Map.of(GOAL_ID1, 0L, GOAL_ID2, 0L, GOAL_ID3, 0L)),
                CAMPAIGN_ID5, getGoalsConversions(Map.of(GOAL_ID1, 0L, GOAL_ID2, 0L, GOAL_ID3, 0L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(GOAL_IDS));

        var result = gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(
                CLIENT_ID, GOAL_IDS, START_DATE, END_DATE);

        //конверсии по цели есть только в одной кампании, но счетчик от этой цели используется во всех кампаниях.
        //При включенной фиче для расчета рекомендации включаем в расчет cost со всех кампаний,
        // где есть счетчик этой цели.

        //totalCost в 5 кампаниях DEFAULT_COST * 5 = 50000
        //достижения целей в 5 кампаниях: GOAL_ID1: 10 , GOAL_ID2: 20 , GOAL_ID3: 40
        assertThat(result).containsOnly(
                entry(GOAL_ID1, BigDecimal.valueOf(5000L)),
                entry(GOAL_ID2, BigDecimal.valueOf(2500L)),
                entry(GOAL_ID3, BigDecimal.valueOf(1250L)));
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_goalWithZeroStat() {
        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, getGoalsConversions(Map.of(GOAL_ID1, 0L))))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));
        var result =
                gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(CLIENT_ID,
                        Set.of(GOAL_ID1), START_DATE, END_DATE);

        assertThat(result).isEmpty();
    }

    @Test
    public void testGetAverageCostPerActionForGoalsInAllClientCampaigns_goalWithEmptyStat() {

        doReturn(ALL_CLIENT_CAMPAIGN_IDS)
                .when(campaignService).getClientCampaignIds(CLIENT_ID);

        doReturn(getGoalConversionForCampaigns(ALL_CLIENT_CAMPAIGN_IDS, emptyList()))
                .when(gridCampaignYtRepository)
                .getCampaignGoalsConversionsCount(eq(ALL_CLIENT_CAMPAIGN_IDS), eq(START_DATE), eq(END_DATE),
                        eq(Set.of(GOAL_ID1)));

        var result =
                gridCampaignService.getAverageCostPerActionForGoalsByLoginStats(CLIENT_ID,
                        Set.of(GOAL_ID1), START_DATE, END_DATE);

        assertThat(result).isEmpty();
    }
}
