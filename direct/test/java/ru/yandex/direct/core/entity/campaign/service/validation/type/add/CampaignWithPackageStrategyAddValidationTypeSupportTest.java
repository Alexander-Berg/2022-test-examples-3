package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

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
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
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
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCampaignIds;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.MAX_NUMBER_OF_CIDS_ABLE_TO_LINK_TO_PACKAGE_STRATEGY;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignStrategyInfoIsIgnoredOnCampaignAdd;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignsWithDifferentTypesInOnePackage;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.publicStrategyNotSupportedForClientsWithoutWallet;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.strategyInfoMissed;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.tooMuchCampaignsLinkedToStrategy;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unavailableStrategyTypeForPublication;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.core.testing.data.strategy.TestCpmDefaultStrategy.clientCpmDefaultStrategy;
import static ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
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
public class CampaignWithPackageStrategyAddValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private WalletService walletService;

    @Autowired
    private StrategyOperationFactory strategyOperationFactory;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private CampaignWithPackageStrategyAddValidationTypeSupport typeSupport;

    private ClientInfo clientInfo;

    private static final int MAX_VALUE_OF_CAMPAIGNS_TO_LINK = 3;
    private static final long TEST_STRATEGY_ID = RandomUtils.nextLong();

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignWithPackageStrategyParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC},
                {CampaignType.INTERNAL_AUTOBUDGET},
                {CampaignType.CPM_YNDX_FRONTPAGE},
                {CampaignType.CPM_PRICE},
                {CampaignType.CPM_BANNER},
                {CampaignType.CONTENT_PROMOTION},
        });
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(clientInfo.getClientId(), clientInfo.getUid());
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        ppcPropertiesSupport.set(MAX_NUMBER_OF_CIDS_ABLE_TO_LINK_TO_PACKAGE_STRATEGY,
                String.valueOf(MAX_VALUE_OF_CAMPAIGNS_TO_LINK));
    }

    @Test
    public void preValidateSuccess_WhenInvalidCampaignStrategyAndPackageStrategyState_FeatureDisabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, false);
        var vr = preValidateAndGetResult(null, null, emptyMap());

        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void preValidateSuccess_WhenOnlyStrategyFilled() {
        var vr = preValidateAndGetResult(null, new DbStrategy(), emptyMap());

        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void preValidateFail_WhenStrategyIdIsInvalid() {
        var vr = preValidateAndGetResult(-1L, new DbStrategy(), emptyMap());

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                CommonDefects.validId())));
    }

    @Test
    public void preValidateFail_ExceptCpmPriceCampaign_WhenStrategyAndStrategyIdNull() {
        var vr = preValidateAndGetResult(null, null, emptyMap());

        if (campaignType.equals(CampaignType.CPM_PRICE)) {
            assertThat(vr, hasNoDefectsDefinitions());
        } else {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                    strategyInfoMissed())));
        }
    }

    @Test
    public void preValidateSuccess_WhenStrategyAndStrategyIdNotNull_CampaignStrategyNotFilled() {
        var vr = preValidateAndGetResult(TEST_STRATEGY_ID,
                new DbStrategy(), Map.of(TEST_STRATEGY_ID, clientDefaultManualStrategy()));

        if (campaignType == CampaignType.CPM_YNDX_FRONTPAGE || campaignType == CampaignType.CPM_BANNER
                || campaignType == CampaignType.PERFORMANCE || campaignType == CampaignType.INTERNAL_AUTOBUDGET) {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                    inconsistentStrategyToCampaignType())));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    @Test
    public void preValidateSuccessWithWarning_WhenStrategyAndStrategyIdFilled_CampaignStrategyIsDifferentFromPackageStrategy() {
        var vr = preValidateAndGetResult(TEST_STRATEGY_ID,
                (DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withPlatform(CampaignsPlatform.CONTEXT)
                        .withStrategyData(new StrategyData().withBudget(BigDecimal.ONE)),
                Map.of(TEST_STRATEGY_ID, clientDefaultManualStrategy()));

        if (campaignType == CampaignType.CPM_YNDX_FRONTPAGE || campaignType == CampaignType.CPM_BANNER
                || campaignType == CampaignType.PERFORMANCE || campaignType == CampaignType.INTERNAL_AUTOBUDGET) {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                    inconsistentStrategyToCampaignType())));
        } else {
            assertThat(vr, hasWarningWithDefinition(validationError(
                    path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                    campaignStrategyInfoIsIgnoredOnCampaignAdd())));
        }
    }

    @Test
    public void preValidateFail_WhenInconsistentStrategyTypeToCampaignType() {
        var vr = preValidateAndGetResult(TEST_STRATEGY_ID, null,
                Map.of(TEST_STRATEGY_ID, clientCpmDefaultStrategy()));

        if (campaignType == CampaignType.CPM_YNDX_FRONTPAGE || campaignType == CampaignType.CPM_BANNER) {
            assertThat(vr, hasNoErrorsAndWarnings());
        } else {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                    inconsistentStrategyToCampaignType())));
        }
    }

    @Test
    public void preValidateFail_WhenStrategyNotFound() {
        var vr = preValidateAndGetResult(TEST_STRATEGY_ID, null, emptyMap());

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                StrategyDefects.strategyNotFound())));
    }

    @Test
    public void validateSuccess() {
        var strategy = createStrategy(clientInfo, autobudgetAvgCpa());
        var vr = validateAndGetResult(strategy.getId(), 414L,
                Map.of(strategy.getId(), strategy));

        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void validateFail_WhenTryToLinkToPrivatePackageWithOneCampaignLinked_CampaignWithoutWallet() {
        var vr = validateAndGetResult(TEST_STRATEGY_ID, 0L,
                Map.of(TEST_STRATEGY_ID, autobudgetAvgCpa()
                        .withId(TEST_STRATEGY_ID)
                        .withIsPublic(false)
                        .withCids(List.of(12L))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0)),
                publicStrategyNotSupportedForClientsWithoutWallet())));
    }

    @Test
    public void validateFail_WhenTryToLinkToPrivatePackageWithOneCampaignLinked_StrategyWithTypeUnavailableForPublication() {
        var vr = validateAndGetResult(TEST_STRATEGY_ID, 4523L,
                Map.of(TEST_STRATEGY_ID, clientDefaultManualStrategy()
                        .withId(TEST_STRATEGY_ID)
                        .withIsPublic(false)
                        .withCids(List.of(12L))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0)),
                unavailableStrategyTypeForPublication())));
    }

    @Test
    public void validateFail_WhenTryToLinkToStrategy_SumOfStrategyCidsAndNewCidsIsMoreThanLimit() {
        var vr = validateAndGetResult(TEST_STRATEGY_ID, 414L,
                Map.of(TEST_STRATEGY_ID, clientDefaultManualStrategy()
                        .withId(TEST_STRATEGY_ID)
                        .withCids(List.of(12L, 23L, 34L))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithPackageStrategy.STRATEGY_ID)),
                tooMuchCampaignsLinkedToStrategy(MAX_VALUE_OF_CAMPAIGNS_TO_LINK))));
    }

    @Test
    public void validateSuccess_WhenLinkCampaign_CampaignTypeSameAsStrategyLinkedCids() {
        if (campaignType == CampaignType.TEXT || campaignType == CampaignType.DYNAMIC || campaignType == CampaignType.CPM_BANNER) {
            var strategy = createStrategy(clientInfo, autobudgetAvgCpa());
            var linkedCampaign = createCampaignWithStrategyId(campaignType, strategy.getId());
            var strategyId = strategy.getId();
            var vr = validateAndGetResult(strategyId, 414L,
                    Map.of(strategyId, ((StrategyWithCampaignIds) strategy)
                            .withCids(List.of(linkedCampaign.getId()))));

            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    @Test
    public void validateFail_WhenTryToLinkCampaign_CampaignTypeDifferentFromPrivateStrategyLinkedCids() {
        if (campaignType == CampaignType.CPM_PRICE || campaignType == CampaignType.CPM_YNDX_FRONTPAGE
                || campaignType == CampaignType.INTERNAL_AUTOBUDGET) {
            return;
        }

        var strategy = createStrategy(clientInfo, autobudgetAvgCpa());
        var strategyId = strategy.getId();
        createCampaignWithStrategyId(CampaignType.TEXT, strategyId);
        var updatedStrategy = strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(strategyId)).get(0);

        var vr = validateAndGetResult(strategyId, 414L,
                Map.of(strategyId, updatedStrategy));

        if (campaignType == CampaignType.TEXT) {
            assertThat(vr, hasNoDefectsDefinitions());
        } else {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0)),
                    campaignsWithDifferentTypesInOnePackage())));
        }
    }

    @Test
    public void validateFail_WhenTryToLinkCampaign_CampaignTypeDifferentFromPublicStrategyLinkedCids() {
        var strategy = createStrategy(clientInfo, autobudgetAvgCpa());
        var strategyId = strategy.getId();

        createCampaignWithStrategyId(CampaignType.TEXT, strategyId);
        createCampaignWithStrategyId(CampaignType.TEXT, strategyId);
        var updatedStrategy = strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(strategyId)).get(0);

        var vr = validateAndGetResult(strategyId, 414L,
                Map.of(strategyId, updatedStrategy));

        if (campaignType == CampaignType.TEXT) {
            assertThat(vr, hasNoDefectsDefinitions());
        } else {
            assertThat(vr, hasDefectWithDefinition(validationError(
                    path(index(0)),
                    campaignsWithDifferentTypesInOnePackage())));
        }
    }

    private CampaignWithPackageStrategy createCampaignWithStrategyId(CampaignType campaignType,
                                                                     @Nullable Long strategyId) {
        List<? extends BaseCampaign> campaigns =
                List.of(((CampaignWithPackageStrategy) defaultCampaignByCampaignType(campaignType)).withStrategyId(strategyId));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, clientInfo.getUid(),
                        UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        return campaignTypedRepository.getStrictlyFullyFilled(clientInfo.getShard(),
                List.of(result.get(0).getResult()), CampaignWithPackageStrategy.class).get(0);
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

    private ValidationResult<List<CampaignWithPackageStrategy>, Defect> preValidateAndGetResult(
            @Nullable Long strategyId, @Nullable DbStrategy strategy, Map<Long, BaseStrategy> clientStrategyById) {
        return typeSupport.preValidate(
                new CampaignValidationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), null, new CampaignOptions(), null,
                        clientStrategyById),
                new ValidationResult<>(List.of(
                        ((CampaignWithPackageStrategy) TestCampaigns.newCampaignByCampaignType(campaignType))
                                .withStrategyId(strategyId)
                                .withStrategy(strategy))));
    }

    private ValidationResult<List<CampaignWithPackageStrategy>, Defect> validateAndGetResult(
            Long strategyId, Long walletId, Map<Long, BaseStrategy> clientStrategyById) {
        return typeSupport.validate(
                new CampaignValidationContainerImpl(clientInfo.getShard(), clientInfo.getUid(),
                        clientInfo.getClientId(), null, new CampaignOptions(), null,
                        clientStrategyById),
                new ValidationResult<>(List.of(
                        ((CampaignWithPackageStrategy) TestCampaigns.newCampaignByCampaignType(campaignType)
                                .withWalletId(walletId))
                                .withStrategyId(strategyId))));
    }
}
