package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.performancefilter.container.PerformanceFiltersQueryFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbschema.ppc.enums.BidsPerformanceStatusbssynced;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.result.DefectIds;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.feature.FeatureName.ADVANCED_GEOTARGETING;
import static ru.yandex.direct.feature.FeatureName.TURBO_SMARTS;
import static ru.yandex.direct.grid.processing.data.TestGdUpdateCampaigns.defaultSmartCampaign;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGNS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Тесты на обновление смарт кампании
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationUpdateGraphqlServiceSmartCampaignTest {

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final TemplateMutation<GdUpdateCampaigns, GdUpdateCampaignPayload> UPDATE_CAMPAIGN_MUTATION =
            new TemplateMutation<>(UPDATE_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdUpdateCampaigns.class, GdUpdateCampaignPayload.class);
    private static final int COUNTER_ID = 5;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;
    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;
    private int shard;
    private long campaignId;
    private CampaignInfo campaignInfo;

    private static CampaignAttributionModel defaultAttributionModel;


    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = userInfo.getShard();
        operator = userInfo.getUser();

        TestAuthHelper.setDirectAuthentication(operator);

        metrikaClientStub.addUserCounter(operator.getUid(), COUNTER_ID);

        campaignInfo = steps.campaignSteps().createActiveSmartCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    public static Object[] parametersForCheckTurboSmarts() {
        return new Object[][]{
                {false, null, null},
                {false, true, true},
                {false, false, false},
                {true, true, true},
                {true, false, false},
        };
    }

    @Test
    @Parameters(method = "parametersForCheckTurboSmarts")
    @TestCaseName("feature enabled: {0}, has turbo smarts: {1}, expect: {2}")
    public void updateSmartCampaign_CheckTurboSmarts(boolean featureEnabled,
                                                     Boolean hasTurboSmarts,
                                                     Boolean expectHasTurboSmarts) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), TURBO_SMARTS, featureEnabled);

        GdUpdateSmartCampaign campaign = defaultSmartCampaign(campaignId, hasTurboSmarts, List.of(COUNTER_ID),
                defaultAttributionModel);

        GdUpdateCampaignPayload response = sendRequest(campaign);
        checkState(response.getValidationResult() == null && response.getUpdatedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard,
                mapList(response.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        SmartCampaign expect = TestCampaigns.defaultSmartCampaign()
                .withStrategy(defaultAutobudgetRoiStrategy(0, false))
                .withHasTurboSmarts(expectHasTurboSmarts)
                .withContextLimit(0)
                .withHasExtendedGeoTargeting(true)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withContextPriceCoef(100)
                .withMetrikaCounters(List.of((long) COUNTER_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaigns).as("list of campaign data")
                    .hasSize(1);
            soft.assertThat(campaigns.get(0)).as("smart campaign")
                    .is(matchedBy(beanDiffer(expect).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateSmartCampaign_WithFeatureWhenTurboSmartsIsNull_CannotBeNull() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), TURBO_SMARTS, true);

        GdUpdateSmartCampaign campaign = defaultSmartCampaign(campaignId, null, List.of(COUNTER_ID),
                defaultAttributionModel);

        GdUpdateCampaignPayload response = sendRequest(campaign);
        GdValidationResult vr = response.getValidationResult();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).as("list of campaign data")
                    .isEqualTo(singletonList(null));
            soft.assertThat(vr).as("validation results")
                    .isNotNull();
            soft.assertThat(vr.getErrors().get(0)).as("validation error")
                    .isEqualTo(new GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.getCode())
                            .withPath("campaignUpdateItems[0].hasTurboSmarts"));
        });
    }

    /**
     * Проверяем изменения в фильтрах, при изменении стратегии кампании на autobudgetRoi
     */
    @Test
    public void updateStartegyToAutobudgetRoi() {
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedId);

        BidsPerformanceRecord bidPerformance = PerformanceFiltersSteps.getDefaultBidsPerformanceRecord();
        bidPerformance.setAutobudgetpriority(null);
        bidPerformance.setStatusbssynced(BidsPerformanceStatusbssynced.Yes);
        bidPerformance = steps.performanceFilterSteps()
                .addBidsPerformance(shard, adGroupInfo.getAdGroupId(), bidPerformance);

        GdUpdateSmartCampaign gdUpdateSmartCampaign = defaultSmartCampaign(campaignId, List.of(COUNTER_ID),
                defaultAttributionModel)
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_ROI)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withGoalId(0L)
                                .withRoiCoef(BigDecimal.ONE)
                                .withReserveReturn(100L)));

        GdUpdateCampaignPayload response = sendRequest(gdUpdateSmartCampaign);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(bidPerformance.getPerfFilterId()))
                .build();
        List<PerformanceFilter> actualPerformanceFilters = performanceFilterRepository.getFilters(shard, queryFilter);

        PerformanceFilter expectedPerformanceFilter = new PerformanceFilter()
                .withAutobudgetPriority(3)
                .withStatusBsSynced(StatusBsSynced.NO);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualPerformanceFilters).as("количество фильтров")
                    .hasSize(1);
            soft.assertThat(actualPerformanceFilters.get(0)).is(matchedBy(beanDiffer(expectedPerformanceFilter)
                    .useCompareStrategy(onlyExpectedFields())));
        });
    }

    /**
     * Проверяем изменения в фильтрах, при изменении стратегии кампании на autobudgetRoi
     */
    @Test
    public void updateSmartCampaign_ChangeBidsPerformanceBsStatus() {
        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedId);

        BidsPerformanceRecord bidPerformance = PerformanceFiltersSteps.getDefaultBidsPerformanceRecord();
        bidPerformance.setAutobudgetpriority(1L);
        bidPerformance.setStatusbssynced(BidsPerformanceStatusbssynced.Yes);
        bidPerformance = steps.performanceFilterSteps()
                .addBidsPerformance(shard, adGroupInfo.getAdGroupId(), bidPerformance);

        GdUpdateSmartCampaign gdUpdateSmartCampaign = defaultSmartCampaign(campaignId, List.of(COUNTER_ID),
                defaultAttributionModel)
                .withBiddingStrategy(new GdCampaignBiddingStrategy()
                        .withPlatform(GdCampaignPlatform.BOTH)
                        .withStrategyName(GdCampaignStrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                        .withStrategyData(new GdCampaignStrategyData()
                                .withAvgBid(BigDecimal.TEN)));

        GdUpdateCampaignPayload response = sendRequest(gdUpdateSmartCampaign);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        PerformanceFiltersQueryFilter queryFilter = PerformanceFiltersQueryFilter.newBuilder()
                .withPerfFilterIds(Collections.singletonList(bidPerformance.getPerfFilterId()))
                .build();
        List<PerformanceFilter> actualPerformanceFilters = performanceFilterRepository.getFilters(shard, queryFilter);

        PerformanceFilter expectedPerformanceFilter = new PerformanceFilter()
                .withAutobudgetPriority(1)
                .withStatusBsSynced(StatusBsSynced.NO);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualPerformanceFilters).as("количество фильтров")
                    .hasSize(1);
            soft.assertThat(actualPerformanceFilters.get(0)).is(matchedBy(beanDiffer(expectedPerformanceFilter)
                    .useCompareStrategy(onlyExpectedFields())));
        });
    }

    public static Object[] parametersForCheckAdvancedGeoTargeting() {
        return new Object[][]{
                {false, null, null, null},
                {true, true, true, false},
                {true, true, false, true},
                {true, true, true, true},
                {true, false, true, false},
                {true, true, null, true},
                {true, null, true, true},
        };
    }

    @Test
    @Parameters(method = "parametersForCheckAdvancedGeoTargeting")
    @TestCaseName("feature enabled: {0}, currentRegion: {1}, regularRegion: {2}, hasExtendedGeoTargeting: {3}")
    public void updateSmartCampaign_CheckAdvancedGeoTargeting(
            boolean featureEnabled,
            Boolean useCurrentRegion,
            Boolean useRegularRegion,
            Boolean hasExtendedGeoTargeting) {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), ADVANCED_GEOTARGETING, featureEnabled);

        GdUpdateSmartCampaign campaign = defaultSmartCampaign(campaignId, List.of(COUNTER_ID), defaultAttributionModel)
                .withUseCurrentRegion(useCurrentRegion)
                .withUseRegularRegion(useRegularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting);

        GdUpdateCampaignPayload response = sendRequest(campaign);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getUpdatedCampaigns()).hasSize(1);
            soft.assertThat(response.getValidationResult()).isNull();
        });

        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard,
                mapList(response.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId));

        SmartCampaign expect = new SmartCampaign()
                .withUseCurrentRegion(useCurrentRegion != null && useCurrentRegion)
                .withUseRegularRegion(useRegularRegion != null && useRegularRegion)
                .withHasExtendedGeoTargeting(!featureEnabled
                        || (hasExtendedGeoTargeting != null && hasExtendedGeoTargeting));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaigns).as("list of campaign data")
                    .hasSize(1);
            soft.assertThat(campaigns.get(0)).as("smart campaign")
                    .is(matchedBy(beanDiffer(expect).useCompareStrategy(onlyExpectedFields())));
        });
    }

    private GdUpdateCampaignPayload sendRequest(GdUpdateSmartCampaign campaign) {
        GdUpdateCampaignUnion gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withSmartCampaign(campaign);
        GdUpdateCampaigns input = new GdUpdateCampaigns().withCampaignUpdateItems(List.of(gdUpdateCampaignUnion));
        return processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator);
    }
}
