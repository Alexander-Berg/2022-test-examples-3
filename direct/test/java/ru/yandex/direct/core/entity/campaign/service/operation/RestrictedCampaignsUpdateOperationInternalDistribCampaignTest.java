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
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithForbiddenStrategyAddOperationSupport;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.campaign.InternalDistribCampaignInfo;
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
import ru.yandex.direct.validation.defect.NumberDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
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
public class RestrictedCampaignsUpdateOperationInternalDistribCampaignTest {

    private static final BeanFieldPath[] EXCLUDED_FIELDS = {
            newPath(InternalDistribCampaign.AUTOBUDGET_FORECAST_DATE.name()),
            newPath(InternalDistribCampaign.METRIKA_COUNTERS.name()),
            newPath(InternalDistribCampaign.MEANINGFUL_GOALS.name()),
            newPath(InternalDistribCampaign.CREATE_TIME.name()),
            newPath(InternalDistribCampaign.SOURCE.name()),
            newPath(InternalDistribCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalDistribCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalDistribCampaign.METATYPE.name())
    };

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(EXCLUDED_FIELDS)
            .forFields(newPath(InternalDistribCampaign.LAST_CHANGE.name()))
            .useMatcher(approximatelyNow())
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
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public MetrikaClientStub metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    @Autowired
    private Steps steps;

    private InternalDistribCampaignInfo campaignInfo;
    private ModelChanges<InternalDistribCampaign> modelChanges;

    @Before
    public void setUp() {
        campaignInfo = steps.internalDistribCampaignSteps().createDefaultCampaign();
        modelChanges = new ModelChanges<>(campaignInfo.getId(), InternalDistribCampaign.class);
    }


    @Test
    public void update_HasValidationError() {
        long invalidRotationGoalId = MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID - 1;
        modelChanges.process(invalidRotationGoalId, InternalDistribCampaign.ROTATION_GOAL_ID);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalDistribCampaign.ROTATION_GOAL_ID)),
                        NumberDefects.greaterThanOrEqualTo(MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID)))));
    }

    @Test
    public void update_ForbiddenToChangeStrategy() {
        DbStrategy strategy = CampaignWithForbiddenStrategyAddOperationSupport.defaultStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        modelChanges.process(strategy, InternalDistribCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalDistribCampaign.STRATEGY)),
                        forbiddenToChange()))));
    }

    @Test
    public void update_Success() {
        var expectedCampaign = campaignInfo.getTypedCampaign()
                .withRotationGoalId(RandomNumberUtils.nextPositiveLong())
                .withPageId(List.of(RandomNumberUtils.nextPositiveLong()))
                .withName("Update name " + RandomStringUtils.randomAlphanumeric(7))
                .withStatusBsSynced(CampaignStatusBsSynced.NO);

        modelChanges.process(expectedCampaign.getRotationGoalId(), InternalDistribCampaign.ROTATION_GOAL_ID);
        modelChanges.process(expectedCampaign.getPageId(), InternalDistribCampaign.PAGE_ID);
        modelChanges.process(expectedCampaign.getName(), InternalDistribCampaign.NAME);

        RestrictedCampaignsUpdateOperation updateOperation = getUpdateOperation(modelChanges);
        MassResult<Long> result = updateOperation.apply();

        var campaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult())
                .is(matchedBy(hasNoErrors()));
        assertThat(campaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }


    private RestrictedCampaignsUpdateOperation getUpdateOperation(ModelChanges<InternalDistribCampaign> modelChanges) {
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
