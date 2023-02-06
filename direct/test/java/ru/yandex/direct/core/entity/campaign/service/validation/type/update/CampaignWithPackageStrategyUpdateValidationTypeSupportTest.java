package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.MAX_NUMBER_OF_CIDS_ABLE_TO_LINK_TO_PACKAGE_STRATEGY;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignNotInPackage;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignStrategyInfoIsIgnoredOnCampaignUpdate;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignsWithDifferentTypesInOnePackage;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.newStrategyHasBeenCreated;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.publicStrategyNotSupportedForClientsWithoutWallet;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.strategyInfoMissed;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.tooMuchCampaignsLinkedToStrategy;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unavailableStrategyTypeForPublication;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy.clientCpmDefaultStrategy;
import static ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CampaignWithPackageStrategyUpdateValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private StrategyOperationFactory strategyOperationFactory;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private CampaignWithPackageStrategyUpdateValidationTypeSupport typeSupport;

    private static final int MAX_VALUE_OF_CAMPAIGNS_TO_LINK = 3;

    private ClientInfo clientInfo;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignWithPackageStrategyParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC},
                {CampaignType.CONTENT_PROMOTION},
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(clientInfo.getClientId(), clientInfo.getUid());
        steps.featureSteps().setCurrentClient(clientInfo.getClientId());
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        ppcPropertiesSupport.set(MAX_NUMBER_OF_CIDS_ABLE_TO_LINK_TO_PACKAGE_STRATEGY,
                String.valueOf(MAX_VALUE_OF_CAMPAIGNS_TO_LINK));
    }

    @Test
    public void preValidateSuccess_WhenInvalidCampaignStrategyAndPackageStrategyState_FeatureDisabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, false);
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var campaignStrategyId = campaign.getStrategyId();
        var campaignStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaignStrategyId)).get(0);

        var vr = preValidateAndGetResult(-1L, null, Map.of(campaignStrategyId, campaignStrategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void preValidateFail_WhenStrategyIdIsInvalid() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var campaignStrategyId = campaign.getStrategyId();
        var campaignStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaignStrategyId)).get(0);

        var vr = preValidateAndGetResult(-1L, null, Map.of(campaignStrategyId, campaignStrategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                CommonDefects.validId()))));
    }

    @Test
    public void preValidateFail_WhenStrategyAndStrategyIdChangeToNull() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);
        var vr = preValidateAndGetResult(null, null,
                Map.of(strategy.getId(), strategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                strategyInfoMissed()))));
    }

    @Test
    public void preValidateFail_WhenTryToResetNonPublicStrategy() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);
        var vr = preValidateAndGetResult(null, new DbStrategy(),
                Map.of(strategy.getId(), strategy),
                campaign.getId());

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0)),
                campaignNotInPackage()))));
    }

    @Test
    public void preValidateWarning_WhenStrategyAndStrategyIdChanging_CampaignFinalStrategyEqualToPackageStrategy() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var strategyToUpdate = createStrategy(clientInfo, clientDefaultManualStrategy());
        var vr = preValidateAndGetResult(strategyToUpdate.getId(),
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()),
                Map.of(strategy.getId(), strategyToUpdate,
                        strategyToUpdate.getId(), strategyToUpdate), campaign.getId());

        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(
                path(index(0)), campaignStrategyInfoIsIgnoredOnCampaignUpdate()))));
    }

    @Test
    public void preValidateSuccessWithWarning_WhenStrategyAndStrategyIdChanging_CampaignStrategyChangesDifferentFromNewPackageStrategy() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var strategyToUpdate = createStrategy(clientInfo, clientDefaultManualStrategy());
        var vr = preValidateAndGetResult(strategyToUpdate.getId(),
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData().withBudget(BigDecimal.ONE)),
                Map.of(strategy.getId(), strategy,
                        strategyToUpdate.getId(), strategyToUpdate), campaign.getId());

        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(
                path(index(0)), campaignStrategyInfoIsIgnoredOnCampaignUpdate()))));
    }

    @Test
    public void preValidateFail_WhenInconsistentStrategyTypeToCampaignType() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var strategyToUpdate = createStrategy(clientInfo, clientCpmDefaultStrategy());
        var vr = preValidateAndGetResult(strategyToUpdate.getId(), null,
                Map.of(strategy.getId(), strategy,
                        strategyToUpdate.getId(), strategyToUpdate),
                campaign.getId());

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                inconsistentStrategyToCampaignType()))));
    }

    @Test
    public void preValidateOk_WhenInvalidStrategy() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        DbStrategy newStrategy = null;
        var vr = preValidateAndGetResult(strategy.getId(), newStrategy,
                Map.of(strategy.getId(), strategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidateOk_WhenInvalidStrategyData() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var newStrategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.DEFAULT_)
                .withStrategyData(null);
        var vr = preValidateAndGetResult(strategy.getId(), newStrategy,
                Map.of(strategy.getId(), strategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void preValidateFail_WhenStrategyNotFound() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var anotherClientInfo = createNewClientWithEnabledFeatures();
        var anotherClientStrategy = createStrategy(anotherClientInfo, clientDefaultManualStrategy());
        var vr = preValidateAndGetResult(anotherClientStrategy.getId(),
                null, Map.of(strategy.getId(), strategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                StrategyDefects.strategyNotFound()))));
    }

    @Test
    public void validateSuccess_WhenLinkCampaignToPrivatePackageWithOneCamp_AllConditionsSatisfied() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa());
        createCampaignWithStrategyId(campaignType, clientStrategy.getId());
        var filledStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(clientStrategy.getId())).get(0);

        var vr = validateAndGetResult(filledStrategy.getId(), 414L,
                Map.of(filledStrategy.getId(), filledStrategy));

        assertThat(vr).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateFail_WhenTryToLinkToPrivatePackageWithOneCampaignLinked_CampaignWithoutWallet() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var vr = validateAndGetResult(strategy.getId(), 0L,
                Map.of(strategy.getId(), strategy));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0)),
                publicStrategyNotSupportedForClientsWithoutWallet()))));
    }

    @Test
    public void validateFail_WhenTryToLinkToPrivatePackageWithOneCampaignLinked_StrategyWithTypeUnavailableForPublication() {
        var campaign = createCampaignWithStrategyId(campaignType, null);
        var strategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(campaign.getStrategyId())).get(0);

        var vr = validateAndGetResult(strategy.getId(), 4523L,
                Map.of(strategy.getId(), strategy));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0)),
                unavailableStrategyTypeForPublication()))));
    }

    @Test
    public void validateFail_WhenTryToLinkToStrategy_SumOfStrategyCidsAndNewCidsIsMoreThanLimit() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa());
        createCampaignWithStrategyId(campaignType, clientStrategy.getId());
        createCampaignWithStrategyId(campaignType, clientStrategy.getId());
        createCampaignWithStrategyId(campaignType, clientStrategy.getId());
        var filledStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(clientStrategy.getId())).get(0);

        var vr = validateAndGetResult(filledStrategy.getId(), 4523L,
                Map.of(filledStrategy.getId(), filledStrategy));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                tooMuchCampaignsLinkedToStrategy(MAX_VALUE_OF_CAMPAIGNS_TO_LINK)))));
    }

    @Test
    public void validateFail_WhenTryToLinkCampaign_CampaignTypeDifferentFromPrivateStrategyLinkedCids() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa());
        createCampaignWithStrategyId(CampaignType.CPM_BANNER, clientStrategy.getId());
        var filledStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(clientStrategy.getId())).get(0);

        var vr = validateAndGetResult(filledStrategy.getId(), 4523L,
                Map.of(filledStrategy.getId(), filledStrategy));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                campaignsWithDifferentTypesInOnePackage()))));
    }

    @Test
    public void validateFail_WhenTryToLinkCampaign_CampaignTypeDifferentFromPublicStrategyLinkedCids() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa());
        createCampaignWithStrategyId(CampaignType.CPM_BANNER, clientStrategy.getId());
        createCampaignWithStrategyId(CampaignType.CPM_BANNER, clientStrategy.getId());
        var filledStrategy =
                strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(clientStrategy.getId())).get(0);

        var vr = validateAndGetResult(filledStrategy.getId(), 4523L,
                Map.of(filledStrategy.getId(), filledStrategy));

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                campaignsWithDifferentTypesInOnePackage()))));
    }

    @Test
    public void preValidateSuccessWithWarning_WhenStrategyAndStrategyIdChanging_CampaignStrategyChangesDifferentFromOldPackageStrategy() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa().withIsPublic(true));
        var campaign = createCampaignWithStrategyId(campaignType, clientStrategy.getId());

        var vr = preValidateAndGetResult(clientStrategy.getId(),
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData().withBudget(BigDecimal.ONE)),
                Map.of(clientStrategy.getId(), clientStrategy), campaign.getId());

        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(0)),
                newStrategyHasBeenCreated()))));
    }

    @Test
    public void preValidateSuccessWithWarning_WhenStrategyChanging_CampaignStrategyChangesDifferentFromOldPublicPackageStrategy() {
        if (campaignType == CampaignType.CONTENT_PROMOTION) {
            return;
        }

        var clientStrategy = createStrategy(clientInfo, autobudgetAvgCpa().withIsPublic(true));
        var campaign = createCampaignWithStrategyId(campaignType, clientStrategy.getId());

        var newStrategy = (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.PERIOD_FIX_BID)
                .withStrategyData(new StrategyData().withBudget(BigDecimal.ONE));
        var modelChanges = ModelChanges.build(campaign.getId(), CampaignWithPackageStrategy.class,
                CampaignWithPackageStrategy.STRATEGY, newStrategy);
        var clientStrategyById = Map.of(clientStrategy.getId(), clientStrategy);

        var vr = typeSupport.preValidate(
                new CampaignValidationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), null,
                        new CampaignOptions(), null, clientStrategyById),
                new ValidationResult<>(List.of(modelChanges)));

        assertThat(vr).is(matchedBy(hasWarningWithDefinition(validationError(path(index(0)),
                newStrategyHasBeenCreated()))));
    }

    private BaseStrategy createStrategy(ClientInfo clientInfo, BaseStrategy strategy) {
        var addOperation = strategyOperationFactory.createStrategyAddOperation(
                clientInfo.getShard(),
                clientInfo.getUid(),
                clientInfo.getClientId(),
                clientInfo.getUid(),
                List.of(strategy),
                new StrategyOperationOptions()
        );
        var result = addOperation.prepareAndApply();
        return strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(result.get(0).getResult())).get(0);
    }

    private CampaignWithPackageStrategy createCampaignWithStrategyId(CampaignType campaignType,
                                                                     @Nullable Long strategyId) {
        List<? extends BaseCampaign> campaigns =
                List.of(((CampaignWithPackageStrategy) defaultCampaignByCampaignType(campaignType)).withStrategyId(strategyId));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, clientInfo.getUid(),
                        UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        return campaignTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                List.of(result.get(0).getResult()), CampaignWithPackageStrategy.class).get(0);
    }

    private ClientInfo createNewClientWithEnabledFeatures() {
        var anotherClientInfo = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(anotherClientInfo.getClientId(), anotherClientInfo.getUid());
        steps.featureSteps().addClientFeature(anotherClientInfo.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        return anotherClientInfo;
    }

    private ValidationResult<List<ModelChanges<CampaignWithPackageStrategy>>, Defect> preValidateAndGetResult(
            @Nullable Long strategyId,
            @Nullable DbStrategy strategy,
            Map<Long, BaseStrategy> clientStrategyById,
            Long campaignId) {
        var modelChanges = new ModelChanges<>(campaignId, CampaignWithPackageStrategy.class)
                .process(strategyId, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(strategy, CampaignWithPackageStrategy.STRATEGY);

        return typeSupport.preValidate(
                new CampaignValidationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), null,
                        new CampaignOptions(), null, clientStrategyById),
                new ValidationResult<>(List.of(modelChanges)));

    }

    private ValidationResult<List<CampaignWithPackageStrategy>, Defect> validateAndGetResult(
            @Nullable Long strategyId, @Nullable Long walletId, Map<Long, BaseStrategy> clientStrategyById) {
        return typeSupport.validate(
                new CampaignValidationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), null,
                        new CampaignOptions(), null, clientStrategyById),
                new ValidationResult<>(new ValidationResult<>(List.of(
                        ((CampaignWithPackageStrategy) TestCampaigns.newCampaignByCampaignType(campaignType)
                                .withId(RandomUtils.nextLong())
                                .withWalletId(walletId))
                                .withStrategyId(strategyId)))));
    }

}
