package ru.yandex.direct.core.entity.campaign.service.operation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.RfCloseByClickType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.info.campaign.InternalAutobudgetCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.defect.CommonDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.InternalCampaignWithRotationGoalIdValidator.MOBILE_ROTATION_GOAL_ID;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.rbac.RbacService.INTERNAL_AD_UID_PRODUCTS_FOR_GET_METRIKA_COUNTERS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class RestrictedCampaignsUpdateOperationInternalAutobudgetCampaignTest {

    private static final Integer COUNTER_ID = 5321;
    private static final long GOAL_ID = 333;

    private static final BeanFieldPath[] EXCLUDED_FIELDS = {
            newPath(InternalAutobudgetCampaign.AUTOBUDGET_FORECAST_DATE.name()),
            newPath(InternalAutobudgetCampaign.MEANINGFUL_GOALS.name()),
            newPath(InternalAutobudgetCampaign.CREATE_TIME.name()),
            newPath(InternalAutobudgetCampaign.SOURCE.name()),
            newPath(InternalAutobudgetCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalAutobudgetCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalAutobudgetCampaign.METATYPE.name())
    };

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(EXCLUDED_FIELDS)
            .forFields(newPath(InternalAutobudgetCampaign.LAST_CHANGE.name()))
            .useMatcher(approximatelyNow())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());

    private static final DefaultCompareStrategy RETARGETING_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFields()
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    public RetargetingService retargetingService;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private MetrikaClientStub metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    @Autowired
    private Steps steps;

    private InternalAutobudgetCampaignInfo campaignInfo;
    private ModelChanges<InternalAutobudgetCampaign> modelChanges;

    @Before
    public void setUp() {
        var clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        campaignInfo = steps.internalAutobudgetCampaignSteps().createDefaultCampaign(clientInfo);
        modelChanges = new ModelChanges<>(campaignInfo.getId(), InternalAutobudgetCampaign.class);
    }


    @Test
    public void update_HasValidationError() {
        modelChanges.process(null, InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalAutobudgetCampaign.STRATEGY)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void update_HasValidationError_ForbiddenUpdateRotationGoalId() {
        modelChanges.process(12412L, InternalAutobudgetCampaign.ROTATION_GOAL_ID);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalAutobudgetCampaign.ROTATION_GOAL_ID)),
                        forbiddenToChange()))));
    }

    @Test
    public void update_HasValidationError_ForbiddenMobileGoalIdForNotMobileCampaign() {
        DbStrategy strategy = campaignInfo.getTypedCampaign().getStrategy();
        strategy.getStrategyData().setGoalId(MOBILE_ROTATION_GOAL_ID);
        modelChanges.process(strategy, InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalAutobudgetCampaign.ROTATION_GOAL_ID)),
                        CommonDefects.inconsistentState()))));
    }

    @Test
    public void update_Success() {
        var expectedCampaign = campaignInfo.getTypedCampaign()
                .withPageId(List.of(RandomNumberUtils.nextPositiveLong()))
                .withName("Update name " + RandomStringUtils.randomAlphanumeric(7))
                .withRfCloseByClick(RfCloseByClickType.ADGROUP)
                .withMetrikaCounters(null)
                .withStatusBsSynced(CampaignStatusBsSynced.NO);
        BigDecimal newSum = expectedCampaign.getStrategy().getStrategyData()
                .getSum().add(BigDecimal.TEN);
        expectedCampaign.getStrategy().getStrategyData().setSum(newSum);

        modelChanges.process(expectedCampaign.getStrategy(), InternalAutobudgetCampaign.STRATEGY);
        modelChanges.process(expectedCampaign.getPageId(), InternalAutobudgetCampaign.PAGE_ID);
        modelChanges.process(expectedCampaign.getName(), InternalAutobudgetCampaign.NAME);
        modelChanges.process(expectedCampaign.getRfCloseByClick(), InternalAutobudgetCampaign.RF_CLOSE_BY_CLICK);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        var campaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));
        assertThat(campaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void update_GoalIdInStrategy() {
        metrikaClient.addUserCounter(INTERNAL_AD_UID_PRODUCTS_FOR_GET_METRIKA_COUNTERS, COUNTER_ID);
        metrikaClient.addCounterGoal(COUNTER_ID, (int) GOAL_ID);

        var expectedCampaign = campaignInfo.getTypedCampaign()
                .withMetrikaCounters(List.of(COUNTER_ID.longValue()))
                .withRotationGoalId(GOAL_ID)
                .withStatusBsSynced(CampaignStatusBsSynced.NO);
        expectedCampaign.getStrategy().getStrategyData().setGoalId(GOAL_ID);

        modelChanges.process(expectedCampaign.getMetrikaCounters(), InternalAutobudgetCampaign.METRIKA_COUNTERS);
        modelChanges.process(expectedCampaign.getStrategy(), InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        var campaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));
        assertThat(campaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));

        // проверим, что если сбросили цель в стратегии, то rotationGoalId тоже должны сбросить
        expectedCampaign.getStrategy().getStrategyData().setGoalId(null);
        modelChanges.process(expectedCampaign.getStrategy(), InternalAutobudgetCampaign.STRATEGY);
        expectedCampaign.setRotationGoalId(null);

        result = getUpdateOperation(modelChanges).apply();
        campaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(), List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));
        assertThat(campaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void updateStrategy_WithGoalId3_Success() {
        var clientInfo = steps.internalAdProductSteps().createMobileInternalAdProduct();

        campaignInfo = new InternalAutobudgetCampaignInfo();
        var expectedCampaign = campaignInfo.getTypedCampaign()
                .withIsMobile(true)
                .withRotationGoalId(3L)
                .withMetrikaCounters(null)
                .withStatusBsSynced(CampaignStatusBsSynced.NO);
        expectedCampaign.getStrategy().getStrategyData().setGoalId(3L);

        campaignInfo = steps.internalAutobudgetCampaignSteps().createCampaign(clientInfo, campaignInfo);
        modelChanges = new ModelChanges<>(campaignInfo.getId(), InternalAutobudgetCampaign.class);

        modelChanges.process(expectedCampaign.getStrategy(), InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        var campaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));
        assertThat(campaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void updateStrategy_CheckNotResetStatusBsSynced_ForRetargeting() {
        AdGroupInfo activeInternalAdGroup = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(activeInternalAdGroup);

        DbStrategy strategy = campaignInfo.getTypedCampaign().getStrategy();
        BigDecimal newSum = strategy.getStrategyData().getSum().add(BigDecimal.TEN);
        strategy.getStrategyData().setSum(newSum);

        modelChanges.process(strategy, InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();
        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));

        Retargeting expectedRetargeting = defaultRetargeting.getRetargeting()
                .withCampaignId(campaignInfo.getCampaignId())
                .withStatusBsSynced(StatusBsSynced.YES);
        List<Retargeting> retargetings = retargetingService.get(campaignInfo.getClientId(), campaignInfo.getUid(),
                List.of(expectedRetargeting.getId()));
        assertThat(retargetings)
                .hasSize(1);
        assertThat(retargetings.get(0))
                .is(matchedBy(beanDiffer(expectedRetargeting).useCompareStrategy(RETARGETING_COMPARE_STRATEGY)));
    }

    @Test
    public void updateStrategy_WithGoalId3AndIsMobileFalse() {
        InternalAutobudgetCampaign campaign = campaignInfo.getTypedCampaign()
                .withMetrikaCounters(null)
                .withRotationGoalId(3L);
        campaign.getStrategy().getStrategyData().setGoalId(3L);

        modelChanges.process(campaign.getStrategy(), InternalAutobudgetCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalAutobudgetCampaign.ROTATION_GOAL_ID)),
                        CommonDefects.inconsistentState()))));
    }


    private RestrictedCampaignsUpdateOperation getUpdateOperation(ModelChanges<InternalAutobudgetCampaign> modelChanges) {
        var options = new CampaignOptions();
        return new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                dslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);
    }

}
