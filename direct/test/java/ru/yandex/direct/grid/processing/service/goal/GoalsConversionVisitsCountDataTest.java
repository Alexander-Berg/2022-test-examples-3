package ru.yandex.direct.grid.processing.service.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsConversionService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterWithAdditionalInformation;
import ru.yandex.direct.core.entity.mobilegoals.repository.MobileGoalsStatisticRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService;
import ru.yandex.direct.grid.core.entity.goal.model.GoalConversionVisit;
import ru.yandex.direct.grid.processing.model.goal.GdGoalConversionVisitsCount;
import ru.yandex.direct.grid.processing.model.goal.GdGoalsConversionVisitsCount;
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.setUnion;


@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class GoalsConversionVisitsCountDataTest {

    private static final Long UID = RandomNumberUtils.nextPositiveLong();
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final int DAYS_FOR_STAT_REQUEST = 7;

    private static final Long COUNTER_ID_1 = 125L;
    private static final MetrikaCounterWithAdditionalInformation COUNTER_1 =
            new MetrikaCounterWithAdditionalInformation().withId(COUNTER_ID_1);
    private static final Long COUNTER_ID_2 = 126L;
    private static final MetrikaCounterWithAdditionalInformation COUNTER_2 =
            new MetrikaCounterWithAdditionalInformation().withId(COUNTER_ID_2);

    private static final Long CLIENT_CAMPAIGN_ID1 = 1001L;
    private static final Long CLIENT_CAMPAIGN_ID2 = 1002L;
    private static final Long CLIENT_CAMPAIGN_ID3 = 1003L;
    private static final Set<Long> CLIENT_CAMPAIGN_IDS = Set.of(CLIENT_CAMPAIGN_ID1, CLIENT_CAMPAIGN_ID2,
            CLIENT_CAMPAIGN_ID3);

    private GoalDataService goalDataService;

    private MetrikaGoalsConversionService metrikaGoalsConversionService;

    private MobileGoalsStatisticRepository mobileGoalsStatisticRepository;

    private CampMetrikaCountersService campMetrikaCountersService;

    private GridCampaignService gridCampaignService;

    private CampaignService campaignService;

    private MetrikaGoalsService metrikaGoalsService;

    private CampaignGoalsService campaignGoalsService;

    Pair<LocalDate, LocalDate> bsStatisticsStartDateEndDate = getBoundaryDatesForTodayMinusDays();

    @Mock
    private GoalConversionsCacheService goalConversionsCacheService;

    public static final Long GOAL_ID1 = 50001L;
    public static final Long GOAL_ID2 = 50002L;
    //составные цели
    public static final Long GOAL_ID3 = 50003L;
    public static final Long GOAL_ID4 = 50004L;
    public static final Long GOAL_ID5 = 50005L;
    public static final Long GOAL_ID6 = 50006L;
    //ecom-цели
    public static final Long GOAL_ID7 = 50007L;

    public static final Map<Long, GoalConversionInfo> DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS = Map.of(
            GOAL_ID1, new GoalConversionInfo(GOAL_ID1, 25L, true),
            GOAL_ID2, new GoalConversionInfo(GOAL_ID2, 0L, false));

    public static final Map<Long, GoalConversionInfo> DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS = Map.of(
            GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 5L, true),
            GOAL_ID4, new GoalConversionInfo(GOAL_ID4, 40L, false),
            GOAL_ID5, new GoalConversionInfo(GOAL_ID5, 20L, true),
            GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 5L, false));

    public static final Map<Long, GoalConversionInfo> DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS = Map.of(
            GOAL_ID7, new GoalConversionInfo(GOAL_ID7, 42L, null));

    public static final Map<Long, BigDecimal> DEFAULT_GOALS_COST_PER_ACTION_BY_GOAL_IDS = Map.of(
            GOAL_ID1, BigDecimal.valueOf(1000L),
            GOAL_ID2, BigDecimal.valueOf(500L),
            GOAL_ID3, BigDecimal.valueOf(250L));

    private static Pair<LocalDate, LocalDate> getBoundaryDatesForTodayMinusDays() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(DAYS_FOR_STAT_REQUEST);
        return Pair.of(startDate, endDate);
    }

    private static Goal getTestGoal(Long goalId, GoalType type, Integer counterId,
                                    @Nullable MetrikaCounterGoalType metrikaCounterGoalType,
                                    @Nullable Long parentGoalId) {
        return (Goal) new Goal()
                .withId(goalId)
                .withType(type)
                .withMetrikaCounterGoalType(metrikaCounterGoalType)
                .withParentId(parentGoalId)
                .withCounterId(counterId);
    }

    private static Set<Goal> getTestOriginalGoals(Long counterId) {
        return Set.of(
                getTestGoal(GOAL_ID1, GoalType.GOAL, counterId.intValue(), MetrikaCounterGoalType.URL, null),
                getTestGoal(GOAL_ID2, GoalType.AUDIENCE, counterId.intValue(), MetrikaCounterGoalType.URL, null));
    }

    private static Set<Goal> getTestCombinedGoals(Long counterId) {
        return Set.of(
                getTestGoal(GOAL_ID3, GoalType.GOAL, counterId.intValue(), MetrikaCounterGoalType.STEP, 0L),
                getTestGoal(GOAL_ID4, GoalType.GOAL, counterId.intValue(), MetrikaCounterGoalType.ACTION, GOAL_ID3),
                getTestGoal(GOAL_ID5, GoalType.GOAL, counterId.intValue(), MetrikaCounterGoalType.URL, GOAL_ID3),
                getTestGoal(GOAL_ID6, GoalType.GOAL, counterId.intValue(), MetrikaCounterGoalType.CALL, GOAL_ID3)
        );
    }

    private static Set<Goal> getTestEcomGoals(Long counterId) {
        return Set.of(
                getTestGoal(GOAL_ID7, GoalType.ECOMMERCE, counterId.intValue(), MetrikaCounterGoalType.ECOMMERCE, null)
        );
    }

    @Before
    public void prepare() {
        metrikaGoalsService = mock(MetrikaGoalsService.class);
        metrikaGoalsConversionService = mock(MetrikaGoalsConversionService.class);
        mobileGoalsStatisticRepository = mock(MobileGoalsStatisticRepository.class);
        campMetrikaCountersService = mock(CampMetrikaCountersService.class);
        gridCampaignService = mock(GridCampaignService.class);
        campaignService = mock(CampaignService.class);
        campaignGoalsService = mock(CampaignGoalsService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);

        GoalConversionsCacheService goalConversionsCacheService = mock(GoalConversionsCacheService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(campMetrikaCountersService.getAvailableAndFilterInputCountersInMetrikaForGoals(eq(CLIENT_ID),
                eq(Set.of(COUNTER_ID_1))))
                .thenReturn(Set.of(COUNTER_1));

        when(campMetrikaCountersService.getAvailableAndFilterInputCountersInMetrikaForGoals(eq(CLIENT_ID),
                eq(Set.of(COUNTER_ID_2))))
                .thenReturn(Set.of(COUNTER_2));

        when(campaignService.getClientCampaignIds(CLIENT_ID))
                .thenReturn(CLIENT_CAMPAIGN_IDS);

        when(featureService.isEnabledForClientId(any(ClientId.class),
                eq(FeatureName.GOALS_ONLY_WITH_CAMPAIGN_COUNTERS_USED))).thenReturn(false);
        when(featureService.isEnabledForClientId(any(ClientId.class),
                eq(FeatureName.ENABLE_GOAL_CONVERSION_STATISTICS_FOR_7_DAYS))).thenReturn(true);
        goalDataService = new GoalDataService(metrikaGoalsService, null, campaignGoalsService, null,
                campMetrikaCountersService,
                metrikaGoalsConversionService, mobileGoalsStatisticRepository, gridCampaignService, featureService,
                goalConversionsCacheService, campaignRepository, shardHelper);
    }

    @Test
    public void testGoalConversionVisitsCount_withoutEcomAndCombinedGoals() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_1))))
                .thenReturn(getTestOriginalGoals(COUNTER_ID_1));

        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_1)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2))
                );

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_1));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(Set.of(GOAL_ID1, GOAL_ID2),
                emptySet(),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withoutEcomAndCombinedGoals_goalWithEmptyStat() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_1))))
                .thenReturn(getTestOriginalGoals(COUNTER_ID_1));

        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_1)), eq(false)))
                .thenReturn(Map.of(GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_1));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(Set.of(GOAL_ID1, GOAL_ID2),
                emptySet(),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoals() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика не вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(getGoalsWithDefaultTestConversionValues(Set.of(GOAL_ID1, GOAL_ID2), emptySet(),
                        emptySet()));

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(extractVisitCount(DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(
                Set.of(GOAL_ID1, GOAL_ID2),
                Set.of(GOAL_ID3, GOAL_ID4, GOAL_ID5, GOAL_ID6),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoalsFromMetikaAndBs() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика (вдруг) вернула статистику по составной цели и ее последенму шагу
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, null)
                )); //значение из метрики будет игнорироваться в пользу статистики из БК

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(extractVisitCount(DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(
                Set.of(GOAL_ID1, GOAL_ID2),
                Set.of(GOAL_ID3, GOAL_ID4, GOAL_ID5, GOAL_ID6),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoalsFromMetikaAndZeroBsStat() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика (вдруг) вернула статистику по составной цели и ее последенму шагу
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, null)
                )); //значение из метрики будет игнорироваться в пользу статистики из БК

        //из БК получаем нулевую статистику по всем шагам составных целей (включая родительскую)
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>(Map.of(
                        GOAL_ID3, 0L,
                        GOAL_ID4, 0L,
                        GOAL_ID5, 0L,
                        GOAL_ID6, 0L)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResult(
                DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS,
                Map.of(
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 0L, null),
                        GOAL_ID4, new GoalConversionInfo(GOAL_ID4, 0L, null),
                        GOAL_ID5, new GoalConversionInfo(GOAL_ID5, 0L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 0L, null)
                ),
                emptyMap());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoalsFromMetikaAndEmptyBsStat() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика (вдруг) вернула статистику по составной цели и ее последенму шагу
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, null)
                )); //значение из метрики будет игнорироваться в пользу статистики из БК

        //из БК получаем пустую статистику
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>());

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        //Если по составным целям пустая статистика в БК, такие цели вернуться с нулевыми конверсиями (красим в красный)
        // независимо от того что вернула метрика
        GdGoalsConversionVisitsCount expectedResult = getExpectedResult(
                DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS,
                Map.of(
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 0L, null),
                        GOAL_ID4, new GoalConversionInfo(GOAL_ID4, 0L, null),
                        GOAL_ID5, new GoalConversionInfo(GOAL_ID5, 0L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 0L, null)
                ),
                emptyMap());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoalsAndLastStepZeroBsStat() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика не вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)));

        //из БК получаем статистику только по промежуточному шагу составной цели
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>(Map.of( //нет статистики по родительской цели и по последнему шагу:
                        // GOAL_ID3, GOAL_ID6 соотв.
                        GOAL_ID3, 0L,
                        GOAL_ID4, 40L,
                        GOAL_ID5, 20L,
                        GOAL_ID6, 0L)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        //Если по последнему шагу 0 конверсий, родительская цель и полседний шаг возвращаются как есть (с нулями)
        GdGoalsConversionVisitsCount expectedResult = getExpectedResult(
                DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS,
                Map.of(
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 0L, null),
                        GOAL_ID4, new GoalConversionInfo(GOAL_ID4, 40L, null),
                        GOAL_ID5, new GoalConversionInfo(GOAL_ID5, 20L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 0L, null)
                ),
                emptyMap());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withEcomGoals() {
        //у клиента есть обычные и ecom-цели
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_1))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_1), getTestEcomGoals(COUNTER_ID_1)));

        //метрика возвращает статистику только по обычным (по ecom не возвращает никогда)
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_1)), eq(false)))
                .thenReturn(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS);

        //из БК получаем статистику по ecom
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(extractVisitCount(DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_1));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResult(
                DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS,
                emptyMap(),
                DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS);

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withEcomGoalsZeroStat() {
        //у клиента есть обычные и ecom-цели
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_1))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_1), getTestEcomGoals(COUNTER_ID_1)));

        //метрика возвращает статистику только по обычным (по ecom не возвращает никогда)
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_1)), eq(false)))
                .thenReturn(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS);

        //из БК получаем нулевую статистику по ecom
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>(Map.of(GOAL_ID7, 0L)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_1));

        GdGoalsConversionVisitsCount expectedResult = getExpectedResult(
                DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS,
                emptyMap(),
                Map.of(GOAL_ID7, new GoalConversionInfo(GOAL_ID7, 0L, null)));

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoals_inconsistentConversionCount() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика не вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)));

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        //НО количество конверсий в последнем шаге не совпадает с родительской целью
        //в таких случаях конверсии по составным целям не возвращаем
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>(Map.of(
                        GOAL_ID3, 25L,  //не соотв. последнему шагу
                        GOAL_ID4, 40L,
                        GOAL_ID5, 20L,
                        GOAL_ID6, 5L)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        //Если по составным целям некорректная статистика в БК, такие цели ручка возвращать не будет (красим в серый)
        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(
                Set.of(GOAL_ID1, GOAL_ID2),
                emptySet(),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    public void testGoalConversionVisitsCount_withCombinedGoalsFromMetrikaAndBsInconsistentConversionCount() {
        when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));

        //метрика вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(false)))
                .thenReturn(Map.of(
                        GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                        GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                        GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, null),
                        GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, null)));

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        //НО количество конверсий в последнем шаге не совпадает с родительской целью
        //в таких случаях конверсии по составным целям не возвращаем
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>(Map.of(
                        GOAL_ID3, 25L,  //не соотв. последнему шагу
                        GOAL_ID4, 40L,
                        GOAL_ID5, 20L,
                        GOAL_ID6, 5L)));

        GdGoalsConversionVisitsCount result = goalDataService
                .getGdGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));

        //Если по составным целям некорректная статистика в БК, такие цели ручка возвращать не будет (красим в серый)
        //Значения конверсий из метрики игнорируются
        GdGoalsConversionVisitsCount expectedResult = getExpectedResultWithDefaultValues(
                Set.of(GOAL_ID1, GOAL_ID2),
                emptySet(),
                emptySet());

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    public static Object[][] parametersTestData() {
        return new Object[][]{
                new Object[]{"Without available goals", false},
                new Object[]{"With available goals", true}
        };
    }

    @Test
    @Parameters(method = "parametersTestData")
    @TestCaseName("{0}")
    public void shouldReturnHasPriceFromMetrika_withCombinedGoalsAndEmptyBsStat(String description,
                                                                                boolean withUnavailableGoals) {
        if (!withUnavailableGoals) {
            when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                    .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));
        }

        //метрика вернула статистику по составной цели
        when(metrikaGoalsConversionService
                .getGoalsConversion(eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(withUnavailableGoals)))
                .thenReturn(Map.of(
                                GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                                GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                                GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, true),
                                GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, false)
                        )
                );

        //из БК получаем пустую статистику
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>());

        var expectedResult = Map.of(
                GOAL_ID1, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1)),
                GOAL_ID2, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)),
                GOAL_ID3, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID3, 0L, true)),
                GOAL_ID4, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 0L, null)),
                GOAL_ID5, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 0L, null)),
                GOAL_ID6, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID6, 0L, false))
        );

        Map<Long, GoalConversionVisit> result;
        if (withUnavailableGoals) {
            Set<Goal> goals = setUnion(getTestCombinedGoals(COUNTER_ID_2), getTestOriginalGoals(COUNTER_ID_2));
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(CLIENT_ID, goals);
        } else {
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));
        }

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    @Parameters(method = "parametersTestData")
    @TestCaseName("{0}")
    public void shouldReturnHasPriceFromMetrika_withCombinedGoalsAndBsStat(String description,
                                                                           boolean withUnavailableGoals) {
        if (!withUnavailableGoals) {
            when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                    .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));
        }

        //метрика вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(
                eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(withUnavailableGoals)))
                .thenReturn(Map.of(
                                GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                                GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2),
                                GOAL_ID3, new GoalConversionInfo(GOAL_ID3, 300L, true),
                                GOAL_ID6, new GoalConversionInfo(GOAL_ID6, 300L, false)
                        )
                );

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(extractVisitCount(DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));

        var expectedResult = Map.of(
                GOAL_ID1, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1)),
                GOAL_ID2, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)),
                GOAL_ID3, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID3, 5L, true)),
                GOAL_ID4, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 40L, null)),
                GOAL_ID5, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 20L, null)),
                GOAL_ID6, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID6, 5L, false))
        );

        Map<Long, GoalConversionVisit> result;
        if (withUnavailableGoals) {
            Set<Goal> goals = setUnion(getTestCombinedGoals(COUNTER_ID_2), getTestOriginalGoals(COUNTER_ID_2));
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(CLIENT_ID, goals);
        } else {
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));
        }

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    @Parameters(method = "parametersTestData")
    @TestCaseName("{0}")
    public void shouldReturnHasPriceAsNullForCombinedGoals_withoutCombinedGoalsAndEmptyBsStat(String description,
                                                                                              boolean withUnavailableGoals) {
        if (!withUnavailableGoals) {
            when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                    .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));
        }

        //метрика не вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(
                eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(withUnavailableGoals)))
                .thenReturn(Map.of(
                                GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                                GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)
                        )
                );

        //из БК получаем пустую статистику
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(new HashMap<>());

        var expectedResult = Map.of(
                GOAL_ID1, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1)),
                GOAL_ID2, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)),
                GOAL_ID3, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID3, 0L, null)),
                GOAL_ID4, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 0L, null)),
                GOAL_ID5, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 0L, null)),
                GOAL_ID6, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID6, 0L, null))
        );

        Map<Long, GoalConversionVisit> result;
        if (withUnavailableGoals) {
            Set<Goal> goals = setUnion(getTestCombinedGoals(COUNTER_ID_2), getTestOriginalGoals(COUNTER_ID_2));
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(CLIENT_ID, goals);
        } else {
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));
        }

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    @Test
    @Parameters(method = "parametersTestData")
    @TestCaseName("{0}")
    public void shouldReturnHasPriceAsNullForCombinedGoals_withoutCombinedGoalsAndBsStat(String description,
                                                                                         boolean withUnavailableGoals) {
        if (!withUnavailableGoals) {
            when(metrikaGoalsService.getAvailableMetrikaGoalsForClient(eq(UID), eq(CLIENT_ID), eq(Set.of(COUNTER_2))))
                    .thenReturn(setUnion(getTestOriginalGoals(COUNTER_ID_2), getTestCombinedGoals(COUNTER_ID_2)));
        }

        //метрика не вернула статистику по составной цели
        when(metrikaGoalsConversionService.getGoalsConversion(
                eq(CLIENT_ID), eq(Set.of(COUNTER_ID_2)), eq(withUnavailableGoals)))
                .thenReturn(Map.of(
                                GOAL_ID1, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1),
                                GOAL_ID2, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)
                        )
                );

        //из БК получаем статистику по всем шагам составных целей (включая родительскую)
        when(gridCampaignService.getTotalActionsCountForGoalsInAllClientCampaigns(CLIENT_ID,
                DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.keySet(),
                bsStatisticsStartDateEndDate.getLeft(),
                bsStatisticsStartDateEndDate.getRight()))
                .thenReturn(extractVisitCount(DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));

        var expectedResult = Map.of(
                GOAL_ID1, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID1)),
                GOAL_ID2, convertConversionInfoToConversionVisit(DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(GOAL_ID2)),
                GOAL_ID4, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 40L, null)),
                GOAL_ID5, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID4, 20L, null)),
                GOAL_ID3, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID3, 5L, null)),
                GOAL_ID6, convertConversionInfoToConversionVisit(new GoalConversionInfo(GOAL_ID6, 5L, null))
        );

        Map<Long, GoalConversionVisit> result;
        if (withUnavailableGoals) {
            Set<Goal> goals = setUnion(getTestCombinedGoals(COUNTER_ID_2), getTestOriginalGoals(COUNTER_ID_2));
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(CLIENT_ID, goals);
        } else {
            result = goalDataService.getGoalsConversionVisitsCountForAllGoals(UID, CLIENT_ID, Set.of(COUNTER_ID_2));
        }

        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }

    private static GdGoalsConversionVisitsCount getExpectedResultWithDefaultValues(Set<Long> goalIds,
                                                                                   Set<Long> combinedGoalIds,
                                                                                   Set<Long> ecomGoalIds) {
        return getExpectedResult(getSpecificGoalIdsFromMap(goalIds, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS),
                getSpecificGoalIdsFromMap(combinedGoalIds, DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS),
                getSpecificGoalIdsFromMap(ecomGoalIds, DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS));
    }

    private static Map<Long, GoalConversionInfo> getSpecificGoalIdsFromMap(Set<Long> goalIds, Map<Long, GoalConversionInfo> goalActionsCount) {
        return EntryStream.of(goalActionsCount)
                .filterKeys(goalIds::contains)
                .toMap();
    }

    private static GdGoalsConversionVisitsCount getExpectedResult(Map<Long, GoalConversionInfo> goalActionsCountByGoalId,
                                                                  Map<Long, GoalConversionInfo> combinedGoalActionCountByGoalId,
                                                                  Map<Long, GoalConversionInfo> ecomGoalsActionsCountByGoalId) {

        Set<GdGoalConversionVisitsCount> conversionVisitsCounts = new HashSet<>();
        goalActionsCountByGoalId.forEach((goalId, actionsCount) -> conversionVisitsCounts.add(getModel(goalId,
                actionsCount)));
        combinedGoalActionCountByGoalId.forEach((goalId, actionsCount) -> conversionVisitsCounts.add(getModel(goalId,
                actionsCount)));
        ecomGoalsActionsCountByGoalId.forEach((goalId, actionsCount) -> conversionVisitsCounts.add(getModel(goalId,
                actionsCount)));

        return new GdGoalsConversionVisitsCount()
                .withGoalsConversionVisitsCount(conversionVisitsCounts)
                .withIsMetrikaAvailable(true);
    }

    private static GdGoalConversionVisitsCount getModel(Long goalId, GoalConversionInfo conversionVisitsCount) {
        return new GdGoalConversionVisitsCount()
                .withId(goalId)
                .withConversionVisitsCount(conversionVisitsCount.getCount());
    }

    /**
     * Из наборов целей собирает Map c тестовыми целями и конверсиями которые хотим проверять
     */
    private static Map<Long, GoalConversionInfo> getGoalsWithDefaultTestConversionValues(Set<Long> goalIds,
                                                                                         Set<Long> combinedGoalIds,
                                                                                         Set<Long> ecomGoalIds) {
        Map<Long, GoalConversionInfo> result = new HashMap<>();
        goalIds.forEach(goalId -> result.put(goalId, DEFAULT_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(goalId)));
        combinedGoalIds.forEach(goalId -> result.put(goalId, DEFAULT_COMBINED_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(goalId)));
        ecomGoalIds.forEach(goalId -> result.put(goalId, DEFAULT_ECOM_GOALS_ACTIONS_COUNT_BY_GOAL_IDS.get(goalId)));
        return result;
    }

    private Map<Long, Long> extractVisitCount(Map<Long, GoalConversionInfo> conversionCountMap) {
        return EntryStream.of(conversionCountMap)
                .mapValues(GoalConversionInfo::getCount)
                .toMap();
    }

    private GoalConversionVisit convertConversionInfoToConversionVisit(GoalConversionInfo conversionInfo) {
        return new GoalConversionVisit()
                .withCount(conversionInfo.getCount())
                .withHasPrice(conversionInfo.isHasPrice());
    }
}
