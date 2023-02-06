package ru.yandex.direct.grid.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsPromotionsRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandSafetyService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.campcalltrackingsettings.repository.CampCalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.service.VcardHelper;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridCampaignRepository;
import ru.yandex.direct.grid.core.entity.campaign.repository.GridCampaignYtRepository;
import ru.yandex.direct.grid.core.entity.model.GdiEntityConversion;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.model.GdiGoalConversion;
import ru.yandex.direct.grid.core.entity.model.GdiGoalStats;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.core.entity.model.client.GdiClientInfo;
import ru.yandex.direct.grid.core.util.stats.GridStatUtils;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.grid.model.campaign.GdiCampaignActionsHolder;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.strategyDataToDb;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.OLD_CAMPAIGNS_PROMOTIONS_TRESHOLD;
import static ru.yandex.direct.grid.core.entity.campaign.service.GridCampaignService.sumAggregatedGoalConversions;
import static ru.yandex.direct.grid.core.util.ClientTestDataUtils.getTestClientNds;
import static ru.yandex.direct.grid.core.util.ClientTestDataUtils.getTestGdiClientInfo;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.TEST_DATE;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getDefaultCampaignStats;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getDefaultCampaignStatsWithEmptyGoalStat;
import static ru.yandex.direct.grid.core.util.GridCoreCampaignStatsTestDataUtils.getGoalStat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class GridCampaignServiceTest {
    private static final long CLIENT_ID = 123L;
    private static final int SHARD = 10;
    private static final ClientNds NDS = getTestClientNds(CLIENT_ID);

    private static final GdiClientInfo CLIENT_INFO = getTestGdiClientInfo(CLIENT_ID, CLIENT_ID, SHARD);
    private static final UidClientIdShard UID_CLIENT_ID_SHARD = UidClientIdShard.of(CLIENT_ID, CLIENT_ID, SHARD);

    private static final Long GOAL_1 = 11L, GOAL_2 = 22L, GOAL_3 = 33L;

    private static CampaignAttributionModel defaultAttributionModel;

    private static final CampaignsPromotion CAMPAIGNS_PROMOTION1 = new CampaignsPromotion()
            .withPromotionId(25L)
            .withStart(LocalDate.now())
            .withFinish(LocalDate.now().plusDays(2))
            .withPercent(100L);
    private static final CampaignsPromotion CAMPAIGNS_PROMOTION2 = new CampaignsPromotion()
            .withPromotionId(25L)
            .withStart(LocalDate.now().minusDays(OLD_CAMPAIGNS_PROMOTIONS_TRESHOLD + 2))
            .withFinish(LocalDate.now().minusDays(OLD_CAMPAIGNS_PROMOTIONS_TRESHOLD + 1))
            .withPercent(100L);

    @Mock
    private GridCampaignAccessService gridCampaignAccessService;

    @Mock
    private CampaignService campaignService;

    @Mock
    private ClientNdsService clientNdsService;

    @InjectMocks
    private GridCampaignService gridCampaignService;

    @Mock
    private GridCampaignRepository gridCampaignRepository;

    @Mock
    private GridCampaignYtRepository gridCampaignYtRepository;

    @Mock
    private VcardRepository vcardRepository;

    @Mock
    private VcardHelper vcardHelper;

    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @Mock
    private CampaignWithBrandSafetyService campaignWithBrandSafetyService;

    @Mock
    private AdGroupService adGroupService;

    @Mock
    private FeatureService featureService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private CampCalltrackingSettingsRepository campCalltrackingSettingsRepository;

    @Mock
    private CampaignsPromotionsRepository campaignsPromotionsRepository;

    @Mock
    private CampaignConstantsService campaignConstantsService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        defaultAttributionModel = CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE;
        doReturn(defaultAttributionModel).when(campaignConstantsService).getDefaultAttributionModel();
    }

    @Test
    public void testGetAllCampaigns() {
        long campaignId1 = 1L;
        long campaignId2 = 2L;
        GdiCampaignActionsHolder actionsHolder = new GdiCampaignActionsHolder()
                .withCanEdit(true)
                .withHasAgency(false)
                .withHasManager(false)
                .withActions(Collections.singleton(GdiCampaignAction.EDIT_CAMP));

        doReturn(Arrays.asList(
                defaultCampaign(campaignId1)
                        .withSumSpent(BigDecimal.valueOf(118.56))
                        .withSum(BigDecimal.valueOf(1180.54))
                        .withAllowedPageIds(null)
                        .withSumToPay(BigDecimal.valueOf(118)),
                defaultCampaign(campaignId2)))
                .when(gridCampaignRepository).getAllCampaigns(eq(UID_CLIENT_ID_SHARD));
        doReturn(ImmutableMap.of(campaignId1, true, campaignId2, false))
                .when(campaignService).moneyOnCampaignsIsBlocked(any(), eq(false), eq(false), eq(true));
        doReturn(NDS)
                .when(clientNdsService).getEffectiveClientNds(eq(ClientId.fromLong(CLIENT_ID)),
                eq(ClientId.fromNullableLong(CLIENT_INFO.getAgencyClientId())),
                eq(CLIENT_INFO.getNonResident()));
        doReturn(Collections.singletonMap(campaignId1, actionsHolder))
                .when(gridCampaignAccessService).getCampaignsActions(any(), any(), anyCollection(), any());
        doReturn(Map.of(campaignId1, emptyList(),
                campaignId2, List.of(4_294_967_296L, 4_294_967_297L)))
                .when(campaignWithBrandSafetyService).getCategories(any());
        doReturn(Map.of(campaignId1, singletonList(CAMPAIGNS_PROMOTION1.withCid(campaignId1)),
                campaignId2, singletonList(CAMPAIGNS_PROMOTION2.withCid(campaignId1))))
                .when(campaignsPromotionsRepository).getCampaignsPromotionsByCid(anyInt(), anyCollection());

        User user = new User()
                .withUid(UID_CLIENT_ID_SHARD.getUid())
                .withClientId(UID_CLIENT_ID_SHARD.getClientId());
        List<GdiCampaign> campaigns =
                gridCampaignService.getAllCampaigns(SHARD, user, CLIENT_INFO, new User());

        assertThat(campaigns)
                .size().isEqualTo(2);

        // Сервис вычитает NDS и добавляет флаг заблокированности, если надо
        assertThat(campaigns)
                .is(matchedBy(beanDiffer(
                        Arrays.asList(
                                defaultCampaign(campaignId1)
                                        .withActions(actionsHolder)
                                        .withMoneyBlocked(true)
                                        .withSum(new BigDecimal("1000.4576"))
                                        .withSumSpent(new BigDecimal("100.4745"))
                                        .withSumRest(new BigDecimal("899.9830"))
                                        .withAllowedPageIds(null)
                                        .withSumLast(new BigDecimal("1000.0000"))
                                        .withAttributionModel(defaultAttributionModel)
                                        .withSumToPay(new BigDecimal("100.0000"))
                                        .withBrandSafetyCategories(emptyList())
                                        .withCampaignsPromotions(singletonList(CAMPAIGNS_PROMOTION1)),
                                defaultCampaign(campaignId2)
                                        .withActions(null)
                                        .withMoneyBlocked(false)
                                        .withSum(new BigDecimal("1000.0000"))
                                        .withSumSpent(new BigDecimal("0.0000"))
                                        .withSumRest(new BigDecimal("1000.0000"))
                                        .withAllowedPageIds(null)
                                        .withSumLast(new BigDecimal("1000.0000"))
                                        .withAttributionModel(defaultAttributionModel)
                                        .withSumToPay(new BigDecimal("0.0000"))
                                        .withBrandSafetyCategories(List.of(4_294_967_296L, 4_294_967_297L))
                                        .withCampaignsPromotions(emptyList())
                        )
                )));
    }

    @Test
    public void testGetStats() {
        doReturn(Collections.singletonMap(1L, getDefaultCampaignStatsWithEmptyGoalStat()))
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(Set.of(1L, 2L)), eq(TEST_DATE), eq(TEST_DATE.plusDays(1)),
                        eq(emptySet()), isNull());

        GdiCampaign campaign1 = createPayForConversionCampaignModel(1L, GOAL_1);
        GdiCampaign campaign2 = createPayForClicksCampaignModel(2L, GOAL_2);
        Map<Long, GdiCampaignStats> result =
                gridCampaignService.getCampaignStats(List.of(campaign1, campaign2), TEST_DATE, TEST_DATE.plusDays(1),
                        emptySet(), emptySet(), Set.of());

        assertThat(result)
                .containsOnly(
                        entry(1L, getDefaultCampaignStatsWithEmptyGoalStat()),
                        entry(2L, gridCampaignService.createZeroStats(emptySet()))
                );
    }

    @Test
    public void testGetStatsByStrategy_WithEmptyGoals() {
        Set<String> enabledFeatures = Set.of(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName(),
                FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT.getName());
        doReturn(Map.of(1L, getDefaultCampaignStatsWithEmptyGoalStat()))
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(Set.of(1L, 2L)), eq(TEST_DATE), eq(TEST_DATE.plusDays(1)), eq(emptySet()));

        GdiCampaign campaign1 = createPayForConversionCampaignModel(1L, GOAL_1);
        GdiCampaign campaign2 = createPayForClicksCampaignModel(2L, GOAL_2);
        Map<Long, GdiCampaignStats> result =
                gridCampaignService.getCampaignStats(List.of(campaign1, campaign2),
                        TEST_DATE, TEST_DATE.plusDays(1), emptySet(), Set.of(GOAL_1, GOAL_2), enabledFeatures);

        assertThat(result)
                .containsOnly(
                        entry(1L, getDefaultCampaignStatsWithEmptyGoalStat()),
                        entry(2L, gridCampaignService.createZeroStats(emptySet()))
                );
    }

    @Test
    public void testGetStatsByStrategy_WithAvailableGoals() {
        Set<String> enabledFeatures = Set.of(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName(),
                FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT.getName());
        List<GdiGoalStats> goalStats = List.of(getGoalStat(GOAL_1, BigDecimal.ONE, 5L));
        doReturn(Map.of(1L, getDefaultCampaignStats(goalStats)))
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(Set.of(1L, 2L)), eq(TEST_DATE), eq(TEST_DATE.plusDays(1)), eq(Set.of(GOAL_1)));

        GdiCampaign campaign1 = createPayForConversionCampaignModel(1L, GOAL_1);
        GdiCampaign campaign2 = createPayForClicksCampaignModel(2L, GOAL_2);
        Map<Long, GdiCampaignStats> result =
                gridCampaignService.getCampaignStats(List.of(campaign1, campaign2),
                        TEST_DATE, TEST_DATE.plusDays(1), Set.of(GOAL_1), Set.of(GOAL_1, GOAL_2), enabledFeatures);

        assertThat(result)
                .containsOnly(
                        entry(1L, getDefaultCampaignStats(goalStats)),
                        entry(2L, gridCampaignService.createZeroStats(Set.of(GOAL_1)))
                );
    }

    @Test
    public void testGetStatsByStrategy_WithUnavailableGoals() {
        Set<String> enabledFeatures = Set.of(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName(),
                FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT.getName());
        List<GdiGoalStats> goalStats1 = List.of(getGoalStat(GOAL_2, BigDecimal.ONE, 5L));
        doReturn(Map.of(1L, getDefaultCampaignStats(goalStats1)))
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(Set.of(1L)), eq(TEST_DATE), eq(TEST_DATE.plusDays(1)),
                        eq(Set.of(GOAL_2, GOAL_3)));

        List<GdiGoalStats> goalStats2 = List.of(getGoalStat(GOAL_2, BigDecimal.ONE, 10L));
        doReturn(Map.of(2L, getDefaultCampaignStats(goalStats2)))
                .when(gridCampaignYtRepository)
                .getCampaignStats(eq(Set.of(2L)), eq(TEST_DATE), eq(TEST_DATE.plusDays(1)), eq(Set.of(GOAL_2)));

        GdiCampaign campaign1 = createPayForConversionCampaignModel(1L, GOAL_3);
        GdiCampaign campaign2 = createPayForClicksCampaignModel(2L, GOAL_2);
        Map<Long, GdiCampaignStats> result =
                gridCampaignService.getCampaignStats(List.of(campaign1, campaign2),
                        TEST_DATE, TEST_DATE.plusDays(1), Set.of(GOAL_2, GOAL_3),
                        Set.of(GOAL_1, GOAL_2), enabledFeatures);

        GdiCampaignStats expectingCampaignStats1 = getDefaultCampaignStats(goalStats1)
                .withIsRestrictedByUnavailableGoals(true);
        GridStatUtils.resetStatWithClicks(expectingCampaignStats1.getStat());
        assertThat(result).containsOnly(
                entry(1L, expectingCampaignStats1),
                entry(2L, getDefaultCampaignStats(goalStats2))
        );
    }

    @Test
    public void testGetPaidDaysLeft_NoCosts() {
        Integer result = gridCampaignService.getPaidDaysLeft(BigDecimal.valueOf(20), Map.of());
        assertThat(result).isNull();
    }

    @Test
    public void testGetPaidDaysLeft_ZeroCosts() {
        Map<LocalDate, BigDecimal> costsByDay = Map.of(
                TEST_DATE, BigDecimal.ZERO,
                TEST_DATE.plusDays(1), BigDecimal.ZERO,
                TEST_DATE.plusDays(2), BigDecimal.ZERO);

        Integer result = gridCampaignService.getPaidDaysLeft(BigDecimal.valueOf(20), costsByDay);
        assertThat(result).isNull();
    }

    @Test
    public void testGetPaidDaysLeft_NegativeWalletSum() {
        Map<LocalDate, BigDecimal> costsByDay = Map.of(
                TEST_DATE, BigDecimal.ZERO,
                TEST_DATE.plusDays(1), BigDecimal.valueOf(10),
                TEST_DATE.plusDays(2), BigDecimal.ZERO);

        Integer result = gridCampaignService.getPaidDaysLeft(BigDecimal.valueOf(-20), costsByDay);
        assertThat(result).isEqualTo(0);
    }

    @Test
    public void testGetPaidDaysLeft_WithoutRounding() {
        // Средняя скорость трат = (10+20)/2 = 15
        Map<LocalDate, BigDecimal> costsByDay = Map.of(
                TEST_DATE, BigDecimal.ZERO,
                TEST_DATE.plusDays(1), BigDecimal.valueOf(10),
                TEST_DATE.plusDays(2), BigDecimal.valueOf(20));

        // На кошельке осталось 30, значит по прогнозу хватит на два дня
        Integer result = gridCampaignService.getPaidDaysLeft(BigDecimal.valueOf(30), costsByDay);
        assertThat(result).isEqualTo(2);
    }

    @Test
    public void testGetPaidDaysLeft_WithRounding() {
        // Средняя скорость трат = (10+20)/2 = 15
        Map<LocalDate, BigDecimal> costsByDay = Map.of(
                TEST_DATE, BigDecimal.ZERO,
                TEST_DATE.plusDays(1), BigDecimal.valueOf(10),
                TEST_DATE.plusDays(2), BigDecimal.valueOf(20));

        // На кошельке осталось 29, значит по прогнозу хватит только на один день
        Integer result = gridCampaignService.getPaidDaysLeft(BigDecimal.valueOf(29), costsByDay);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGdiGoalConversionAdder_OneGoalConversion() {
        var goalId = 1L;
        var one = new GdiGoalConversion()
                .withGoalId(goalId)
                .withGoals(50L)
                .withRevenue(1230L);

        assertThat(sumAggregatedGoalConversions(goalId, List.of(one)))
                .isEqualTo(one);
    }

    @Test
    public void testGdiGoalConversionAdder_SumOfGoalConversions() {
        var goalId = 1L;
        var one = new GdiGoalConversion()
                .withGoalId(1L)
                .withGoals(50L)
                .withRevenue(1230L);
        var two = new GdiGoalConversion()
                .withGoalId(2L)
                .withGoals(11L)
                .withRevenue(1000L);
        var three = new GdiGoalConversion()
                .withGoalId(3L)
                .withGoals(16L)
                .withRevenue(2050L);

        var sumOfGoalConversions = new GdiGoalConversion()
                .withGoalId(goalId)
                .withGoals(one.getGoals() + two.getGoals() + three.getGoals())
                .withRevenue(one.getRevenue() + two.getRevenue() + three.getRevenue());

        assertThat(sumAggregatedGoalConversions(goalId, List.of(one, two, three)))
                .isEqualTo(sumOfGoalConversions);

    }

    @Test
    public void shouldRecalculateCrrForCampaignStatsUsingCampaignGoalType() {
        GdiCampaign campaign = createPayForConversionCampaignModel(1L, GOAL_1);
        var startDay = LocalDate.now().minusDays(1);
        var endDay = LocalDate.now();
        var campaignStat = new GdiCampaignStats()
                .withStat(new GdiEntityStats()
                        .withCost(BigDecimal.TEN)
                        .withClicks(BigDecimal.TEN)
                ).withGoalStats(List.of());
        var conversions = new GdiEntityConversion()
                .withGoals(100L)
                .withRevenue(10L);

        doReturn(Map.of(1L, campaignStat))
                .when(gridCampaignYtRepository).getCampaignStats(
                Set.of(campaign.getId()), startDay, endDay, Set.of());
        doReturn(Map.of(1L, conversions))
                .when(gridCampaignYtRepository).getCampaignsConversionCount(
                Set.of(campaign.getId()), startDay, endDay, null, false, false);

        var result = gridCampaignService.getCampaignStatsForCampaignGoals(
                List.of(campaign), startDay, endDay, null, Set.of());

        assertThat(result).isNotNull()
                .containsOnlyKeys(1L);
        assertThat(result.get(1L).getStat())
                .isNotNull()
                .hasFieldOrPropertyWithValue("revenue", BigDecimal.TEN)
                .hasFieldOrPropertyWithValue("crr", BigDecimal.valueOf(100));
    }

    @Test
    public void testGetCampaignStatsByStrategyForCampaignGoals() {
        Set<String> enabledFeatures = Set.of(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName(),
                FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT.getName());
        GdiCampaign campaign1 = createPayForConversionCampaignModel(1L, GOAL_1);
        GdiCampaign campaign2 = createPayForClicksCampaignModel(2L, GOAL_2);

        var conversions1 = new GdiEntityConversion().withGoals(100L).withRevenue(10L);
        var conversions2 = new GdiEntityConversion().withGoals(200L).withRevenue(20L);

        GdiCampaignStats campaignStats1 = getDefaultCampaignStatsWithEmptyGoalStat();
        GdiCampaignStats campaignStats2 = getDefaultCampaignStatsWithEmptyGoalStat();
        doReturn(Map.of(
                1L, campaignStats1,
                2L, campaignStats2)
        ).when(gridCampaignYtRepository).getCampaignStats(Set.of(1L, 2L), TEST_DATE, TEST_DATE.plusDays(1), Set.of());
        doReturn(Map.of(1L, conversions1))
                .when(gridCampaignYtRepository).getCampaignsConversionCount(
                Set.of(1L), TEST_DATE, TEST_DATE.plusDays(1));
        doReturn(Map.of(2L, conversions2))
                .when(gridCampaignYtRepository).getCampaignsConversionCountWithStatsFiltration(
                Set.of(2L), TEST_DATE, TEST_DATE.plusDays(1), Set.of(GOAL_2, GOAL_3));

        Map<Long, GdiCampaignStats> result =
                gridCampaignService.getCampaignStatsForCampaignGoals(
                        List.of(campaign1, campaign2), TEST_DATE, TEST_DATE.plusDays(1),
                        Set.of(GOAL_2, GOAL_3), enabledFeatures);

        assertThat(result).containsOnlyKeys(1L, 2L);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.get(1L).getIsRestrictedByUnavailableGoals()).isEqualTo(true);
            softly.assertThat(result.get(1L).getStat())
                    .hasFieldOrPropertyWithValue("goals", BigDecimal.valueOf(conversions1.getGoals()))
                    .hasFieldOrPropertyWithValue("revenue", BigDecimal.valueOf(conversions1.getRevenue()))
                    .hasFieldOrPropertyWithValue("clicks", BigDecimal.ZERO);
            softly.assertThat(result.get(2L).getIsRestrictedByUnavailableGoals()).isNull();
            softly.assertThat(result.get(2L).getStat())
                    .hasFieldOrPropertyWithValue("goals", BigDecimal.valueOf(conversions2.getGoals()))
                    .hasFieldOrPropertyWithValue("revenue", BigDecimal.valueOf(conversions2.getRevenue()))
                    .hasFieldOrPropertyWithValue("clicks", campaignStats2.getStat().getClicks());
        });
    }

    private GdiCampaign createPayForConversionCampaignModel(Long id, Long goalId) {
        return new GdiCampaign()
                .withId(id)
                .withStrategyData(strategyDataToDb(
                        new StrategyData()
                                .withPayForConversion(true)
                                .withGoalId(goalId))
                );
    }

    private GdiCampaign createPayForClicksCampaignModel(Long id, Long goalId) {
        return new GdiCampaign()
                .withId(id)
                .withStrategyData(strategyDataToDb(new StrategyData().withPayForConversion(false)))
                .withMeaningfulGoals(List.of(new MeaningfulGoal().withGoalId(goalId)));
    }

}
