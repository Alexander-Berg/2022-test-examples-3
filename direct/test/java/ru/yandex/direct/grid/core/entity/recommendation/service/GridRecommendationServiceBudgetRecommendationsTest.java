package ru.yandex.direct.grid.core.entity.recommendation.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.direct.communication.container.web.CommunicationMessage;
import ru.yandex.direct.communication.container.web.CommunicationTargetObject;
import ru.yandex.direct.communication.model.Slot;
import ru.yandex.direct.communication.service.CommunicationChannelService;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.container.CampaignsSelectionCriteria;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.campdaybudgethistory.repository.CampDayBudgetStopHistoryRepository;
import ru.yandex.direct.core.entity.client.model.ClientsOptions;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.payment.service.AutopayService;
import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.walletparams.repository.WalletPaymentTransactionsRepository;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.core.entity.recommendation.repository.GridRecommendationYtRepository;
import ru.yandex.direct.grid.core.entity.recommendation.service.cpmprice.GridBannerFormatsForPriceSalesRecommendationService;
import ru.yandex.direct.grid.core.entity.recommendation.service.outdoor.GridOutdoorVideoRecommendationForAdGroupService;
import ru.yandex.direct.grid.core.entity.recommendation.service.outdoor.GridOutdoorVideoRecommendationForBannersService;
import ru.yandex.direct.grid.core.entity.recommendation.service.outdoor.GridOutdoorVideoRecommendationForPlacementsService;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendation;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiDailyBudget;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationKpiWeeklyBudget;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationSummary;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationSummaryWithKpi;
import ru.yandex.direct.grid.processing.model.recommendation.GdRecommendationWithKpi;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.ALL_ALLOWED_CAMPAIGN_RECOMMENDATION_TYPES;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.ALL_ALLOWED_SUMMARY_RECOMMENDATION_TYPES;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.DAILY_BUDGET_RECOMMENDATION_MESSAGE_NAME;
import static ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService.WEEKLY_BUDGET_RECOMMENDATION_MESSAGE_NAME;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.dailyBudget;
import static ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType.increaseStrategyWeeklyBudget;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class GridRecommendationServiceBudgetRecommendationsTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private RbacService rbacService;

    @Mock
    private GridOutdoorVideoRecommendationForAdGroupService gridOutdoorVideoRecommendationForAdGroupService;

    @Mock
    private GridOutdoorVideoRecommendationForBannersService gridOutdoorVideoRecommendationForBannersService;

    @Mock
    private GridOutdoorVideoRecommendationForPlacementsService gridOutdoorVideoRecommendationForPlacementsService;

    @Mock
    private GridBannerFormatsForPriceSalesRecommendationService gridBannerFormatsForPriceSalesRecommendationService;

    @Mock
    private WalletPaymentTransactionsRepository walletPaymentTransactionsRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private CampDayBudgetStopHistoryRepository campDayBudgetStopHistoryRepository;

    @Mock
    private AutopayService autopayService;

    @Mock
    private CommunicationChannelService communicationChannelService;

    @Mock
    private GridRecommendationYtRepository recommendationYtRepository;

    @Mock
    private FeatureService featureService;

    private GridRecommendationService gridRecommendationService;

    private static final Long CLIENT_ID = 111L;

    private static final Long CID = 555L;

    private static final User OPERATOR = new User().withRole(RbacRole.CLIENT).withUid(1L);

    private final List<GdiRecommendation> oldRecommendations = List.of(
            new GdiRecommendation().withCid(CID).withType(GdiRecommendationType.increaseStrategyTargetCPA),
            new GdiRecommendation().withCid(CID).withType(GdiRecommendationType.decreaseStrategyTargetROI),
            new GdiRecommendation().withCid(CID).withType(dailyBudget),
            new GdiRecommendation().withCid(CID).withType(increaseStrategyWeeklyBudget)
    );

    @Before
    public void setUp() {
        openMocks(this);

        when(clientService.getWorkCurrency(any(ClientId.class))).thenReturn(Currencies.getCurrency(CurrencyCode.RUB));
        when(clientService.getClientOptions(any(ClientId.class)))
                .thenReturn(new ClientsOptions());
        when(campaignRepository.getCampaigns(anyInt(), any(CampaignsSelectionCriteria.class)))
                .thenReturn(getTestCampaigns());
        when(campaignRepository.getArchivedCampaigns(anyInt(), any()))
                .thenReturn(Set.of());
        when(campaignRepository.getUniversalCampaigns(anyInt(), any()))
                .thenReturn(Set.of());
        when(shardHelper.getShardByClientId(any(ClientId.class)))
                .thenReturn(1);
        var testCampaignIds = getTestCampaigns().stream().map(Campaign::getId).collect(Collectors.toSet());
        when(rbacService.getWritableCampaigns(anyLong(), any()))
                .thenReturn(testCampaignIds);
        when(rbacService.getVisibleCampaigns(anyLong(), any()))
                .thenReturn(testCampaignIds);
        mockOldRecommendations(singleton(GdiRecommendationType.removePagesFromBlackListOfACampaign), List.of());
        //"включаем" фичи которые влияют на запрос полного списка типов рекомендаций
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                        .thenReturn(Set.of(RecommendationType.autopayStopped.getFeature().getName(),
                                RecommendationType.overdraftDebt.getFeature().getName(),
                                RecommendationType.mainInvoice.getFeature().getName()));
        //теперь в запросе рекомендаций будет полный перечень типов (ALL_ALLOWED_SUMMARY_RECOMMENDATION_TYPES)
        mockOldRecommendations(ALL_ALLOWED_SUMMARY_RECOMMENDATION_TYPES, oldRecommendations);
        mockOldRecommendations(ALL_ALLOWED_CAMPAIGN_RECOMMENDATION_TYPES, oldRecommendations);

        gridRecommendationService = new GridRecommendationService(campaignRepository, adGroupRepository, shardHelper,
                recommendationYtRepository, featureService, rbacService, gridOutdoorVideoRecommendationForAdGroupService,
                gridOutdoorVideoRecommendationForBannersService, gridOutdoorVideoRecommendationForPlacementsService,
                gridBannerFormatsForPriceSalesRecommendationService, walletPaymentTransactionsRepository,
                walletRepository, clientService, campDayBudgetStopHistoryRepository, autopayService,
                communicationChannelService);
    }

    @Test
    public void getRecommendationSummary_oldRecommendationsOnly(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        var expectedRecTypes = listToSet(mapList(oldRecommendations, GdiRecommendation::getType));
        var actualRecTypes = listToSet(mapList(result, GdRecommendationSummary::getType));

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getRecommendationSummary_disableOldDailyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != dailyBudget);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getRecommendationSummary_disableOldWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != increaseStrategyWeeklyBudget);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getRecommendationSummary_disableOldDailyAndWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != increaseStrategyWeeklyBudget &&
                        t != dailyBudget);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getRecommendationSummary_newDailyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var dailyBudgetMessage = getMessageWithRecommendations(dailyBudget, 100L, null,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(dailyBudgetMessage);

        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        //Должны вернуться все типы которые были изначально. Для дневных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //проверим корректность тела новой дневной рекомендации
        var actualNewRecommendation = (GdRecommendationSummaryWithKpi) filterList(result,
                t -> t.getType() == dailyBudget).get(0);
        var actualKpi = (GdRecommendationKpiDailyBudget) actualNewRecommendation.getKpi();
        assertGdRecommendationKpiDailyBudget(actualKpi);
    }

    @Test
    public void getRecommendationSummary_newWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, true);
        var message = getMessageWithRecommendations(increaseStrategyWeeklyBudget, 0L, 15L,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(message);

        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        //должны вернуться все типы которые были изначально. для недельных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //проверим корректность тела новой недельной рекомендации
        var actualNewRecommendation = (GdRecommendationSummaryWithKpi) filterList(result,
                t -> t.getType() == increaseStrategyWeeklyBudget).get(0);
        var actualKpi = (GdRecommendationKpiWeeklyBudget) actualNewRecommendation.getKpi();
        assertGdRecommendationKpiWeeklyBudget(actualKpi);
    }

    @Test
    public void getRecommendationSummary_newDailyAndWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, true);
        var weeklyBudgetMessage = getMessageWithRecommendations(increaseStrategyWeeklyBudget, 0L, 15L,
                new BigDecimal("3000"), new BigDecimal("250"));
        var dailyBudgetMessage = getMessageWithRecommendations(dailyBudget, 100L, null,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(dailyBudgetMessage, weeklyBudgetMessage);

        var result = gridRecommendationService.getRecommendationSummary(CLIENT_ID, OPERATOR, null);
        //должны вернуться все типы которые были изначально. для недельных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = mapList(result, GdRecommendationSummary::getType);
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //проверим корректность тела новой дневной рекомендации
        var actualNewDailyRecommendation = (GdRecommendationSummaryWithKpi) filterList(result,
                t -> t.getType() == dailyBudget).get(0);
        var actualDailyKpi = (GdRecommendationKpiDailyBudget) actualNewDailyRecommendation.getKpi();
        assertGdRecommendationKpiDailyBudget(actualDailyKpi);

        //проверим корректность тела новой недельной рекомендации
        var actualNewWeeilyRecommendation = (GdRecommendationSummaryWithKpi) filterList(result,
                t -> t.getType() == increaseStrategyWeeklyBudget).get(0);
        var actualWeeklyKpi = (GdRecommendationKpiWeeklyBudget) actualNewWeeilyRecommendation.getKpi();
        assertGdRecommendationKpiWeeklyBudget(actualWeeklyKpi);
    }

    @Test
    public void getCampaignRecommendations_oldRecommendationsOnly(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        var expectedRecTypes = listToSet(mapList(oldRecommendations,
                GdiRecommendation::getType));
        var actualRecTypes = listToSet(mapList(actualRecommendations,
                GdRecommendation::getType));

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getCampaignRecommendations_disableOldDailyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != dailyBudget);
        var actualRecTypes = listToSet(mapList(actualRecommendations,
                GdRecommendation::getType));

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getCampaignRecommendations_disableOldWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != increaseStrategyWeeklyBudget);
        var actualRecTypes = listToSet(mapList(actualRecommendations,
                GdRecommendation::getType));

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getCampaignRecommendations_disableOldDailyAndWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        var expectedRecTypes = filterList(
                mapList(oldRecommendations, GdiRecommendation::getType), t -> t != increaseStrategyWeeklyBudget &&
                        t != dailyBudget);
        var actualRecTypes = listToSet(mapList(actualRecommendations,
                GdRecommendation::getType));

        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);
    }

    @Test
    public void getCampaignRecommendations_newDailyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, false);
        var dailyBudgetMessage = getMessageWithRecommendations(dailyBudget, 100L, null,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(dailyBudgetMessage);

        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        //Должны вернуться все типы которые были изначально. Для дневных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = listToSet(mapList(actualRecommendations, GdRecommendation::getType));
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //Проверим содержимое дневной рекомендации, убедимся что оно совпадает с телом сообщения
        var actualNewRecommendation = (GdRecommendationWithKpi) filterList(actualRecommendations,
                t -> t.getType() == dailyBudget).get(0);
        var actualKpi = (GdRecommendationKpiDailyBudget) actualNewRecommendation.getKpi();
        assertGdRecommendationKpiDailyBudget(actualKpi);
    }

    @Test
    public void getCampaignRecommendations_newWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, false);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, true);
        var message = getMessageWithRecommendations(increaseStrategyWeeklyBudget, 0L, 15L,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(message);

        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        //Должны вернуться все типы которые были изначально. для недельных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = mapList(actualRecommendations, GdRecommendation::getType);
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //проверим корректность тела новой недельной рекомендации
        var actualNewWeeilyRecommendation = (GdRecommendationWithKpi) filterList(actualRecommendations,
                t -> t.getType() == increaseStrategyWeeklyBudget).get(0);
        var actualWeeklyKpi = (GdRecommendationKpiWeeklyBudget) actualNewWeeilyRecommendation.getKpi();
        assertGdRecommendationKpiWeeklyBudget(actualWeeklyKpi);
    }

    @Test
    public void getCampaignRecommendations_newDailyAndWeeklyRecommendations(){
        enableFeature(FeatureName.DISABLE_OLD_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.DISABLE_OLD_WEEKLY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_DAILY_BUDGET_RECOMMENDATION, true);
        enableFeature(FeatureName.ENABLE_NEW_WEEKLY_BUDGET_RECOMMENDATION, true);
        var weeklyBudgetMessage = getMessageWithRecommendations(increaseStrategyWeeklyBudget, 0L, 15L,
                new BigDecimal("3000"), new BigDecimal("250"));
        var dailyBudgetMessage = getMessageWithRecommendations(dailyBudget, 100L, null,
                new BigDecimal("3000"), new BigDecimal("250"));
        mockNewRecommendations(dailyBudgetMessage, weeklyBudgetMessage);

        var result = gridRecommendationService.getCampaignRecommendations(CLIENT_ID,
                OPERATOR, emptySet(), Set.of(CID));
        assertThat(result).isNotEmpty();
        var actualRecommendations = result.get(CID);
        assertThat(actualRecommendations).isNotNull();
        assertThat(actualRecommendations).isNotEmpty();

        //Должны вернуться все типы которые были изначально. Для недельных это будет новая рекомендация
        var expectedRecTypes = mapList(oldRecommendations, GdiRecommendation::getType);
        var actualRecTypes = mapList(actualRecommendations, GdRecommendation::getType);
        assertThat(actualRecTypes).containsExactlyInAnyOrderElementsOf(expectedRecTypes);

        //проверим корректность тела новой дневной рекомендации
        var actualNewDailyRecommendation = (GdRecommendationWithKpi) filterList(actualRecommendations,
                t -> t.getType() == dailyBudget).get(0);
        var actualDailyKpi = (GdRecommendationKpiDailyBudget) actualNewDailyRecommendation.getKpi();
        assertGdRecommendationKpiDailyBudget(actualDailyKpi);

        //проверим корректность тела новой недельной рекомендации
        var actualNewWeeilyRecommendation = (GdRecommendationWithKpi) filterList(actualRecommendations,
                t -> t.getType() == increaseStrategyWeeklyBudget).get(0);
        var actualWeeklyKpi = (GdRecommendationKpiWeeklyBudget) actualNewWeeilyRecommendation.getKpi();
        assertGdRecommendationKpiWeeklyBudget(actualWeeklyKpi);
    }

    private void assertGdRecommendationKpiDailyBudget(GdRecommendationKpiDailyBudget actualKpi){
        assertThat(actualKpi).isNotNull();
        assertThat(actualKpi.getRecommendedDailyBudget()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(actualKpi.getPeriod()).isEqualByComparingTo(7);
        assertThat(actualKpi.getCost()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(actualKpi.getClicks()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(actualKpi.getGoals()).isEqualByComparingTo(new BigDecimal("0"));
        assertThat(actualKpi.getSign().name()).isEqualTo("CPC");
    }

    private void assertGdRecommendationKpiWeeklyBudget(GdRecommendationKpiWeeklyBudget actualKpi){
        assertThat(actualKpi).isNotNull();
        assertThat(actualKpi.getRecommendedWeeklyBudget()).isEqualByComparingTo(new BigDecimal("250"));
        assertThat(actualKpi.getPeriod()).isEqualByComparingTo(7);
        assertThat(actualKpi.getCost()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(actualKpi.getClicks()).isEqualByComparingTo(new BigDecimal("0"));
        assertThat(actualKpi.getGoals()).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(actualKpi.getSign().name()).isEqualTo("CPC");
    }

    private void enableFeature(FeatureName feature, boolean isEnabled){
        when(featureService.isEnabledForClientId(any(ClientId.class), eq(feature)))
                .thenReturn(isEnabled);
    }

    private void mockOldRecommendations(Collection<GdiRecommendationType> types,
                                        List<GdiRecommendation> outputRecommendations){
        when(recommendationYtRepository.getRecommendations(anyLong(),
                eq(types), any(), any(), any(), any(), any(), any()))
                .thenReturn(outputRecommendations);
    }

    private void mockNewRecommendations(CommunicationMessage... messages){
        when(communicationChannelService.getCommunicationMessage(any(ClientId.class), anyLong(), any(), any(),
                anyString()))
                .thenReturn(List.of(messages));
    }

    private CommunicationMessage getMessageWithRecommendations(GdiRecommendationType type, Long clicksIncrease, Long conversionsIncrease,
                                        BigDecimal budgetIncrease, BigDecimal newBudget){
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("client.currency", CurrencyRub.getInstance());
        if (budgetIncrease != null){
            messageData.put("budget_spendings_increase", budgetIncrease);
        }
        if (clicksIncrease != null){
            messageData.put("clicks_increase", clicksIncrease);
        }
        if (conversionsIncrease != null){
            messageData.put("conversions_increase", conversionsIncrease);
        }
        if (newBudget != null){
            messageData.put(type == dailyBudget ? "new_daily_budget" : "new_week_budget", newBudget);
        }

        return new CommunicationMessage()
                .withName(type == dailyBudget ? DAILY_BUDGET_RECOMMENDATION_MESSAGE_NAME :
                        WEEKLY_BUDGET_RECOMMENDATION_MESSAGE_NAME)
                .withEventId(1L)
                .withEventVersionId(3L)
                .withMajorVersion(17L)
                .withMinorVersion(37L)
                .withRequestId(777L)
                .withSlot(Slot.getDefaultInstance())
                .withTargetObject(new CommunicationTargetObject().withId(CID))
                .withData(messageData);
    }

    private List<Campaign> getTestCampaigns(){
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyData(
                        new StrategyData()
                                .withAvgCpa(new BigDecimal("450"))
                                .withSum(new BigDecimal("1000"))
                                .withGoalId(123456L));

        var textCampaign = new Campaign()
                .withId(CID)
                .withAutobudget(true)
                .withType(CampaignType.TEXT)
                .withStrategy(strategy);

        return List.of(textCampaign);
    }
}
