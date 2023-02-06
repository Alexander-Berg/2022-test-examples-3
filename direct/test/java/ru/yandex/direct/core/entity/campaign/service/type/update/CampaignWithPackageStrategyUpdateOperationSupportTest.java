package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa;
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy;
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.DynamicCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static java.time.LocalDateTime.now;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.CAMPAIGNS_WITH_DIFFERENT_TYPES_IN_ONE_PACKAGE;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.CAMPAIGN_STRATEGY_INFO_IS_IGNORED_ON_CAMPAIGN_UPDATE;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.NEW_STRATEGY_HAS_BEEN_CREATED;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpa;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignWithPackageStrategyUpdateOperationSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignOperationService campaignOperationService;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public StrategyTypedRepository strategyTypedRepository;

    @Autowired
    public BannerService bannerService;

    @Autowired
    public StrategyOperationFactory strategyOperationFactory;

    @Autowired
    public WalletService walletService;

    @Autowired
    public MetrikaClientStub metrikaClientStub;

    /**
     * ModelProperties that affects strategy. Are taken from
     * {@link ru.yandex.direct.core.entity.campaign.service.validation.type.bean.CampaignWithPackageStrategyUpdatePreValidatorHelper}
     */
    public static Object[][] testModelProperties() {
        return new Object[][]{
                {CampaignWithPackageStrategy.STRATEGY.name(), CampaignWithPackageStrategy.STRATEGY, new DbStrategy()},
                {TextCampaign.METRIKA_COUNTERS.name(), TextCampaign.METRIKA_COUNTERS, List.of(1L)},
                {TextCampaign.MEANINGFUL_GOALS.name(), TextCampaign.MEANINGFUL_GOALS,
                        List.of(new MeaningfulGoal().withGoalId(1L))},
                {TextCampaign.DAY_BUDGET.name(), TextCampaign.DAY_BUDGET, BigDecimal.valueOf(10000L)},
                {TextCampaign.DAY_BUDGET_SHOW_MODE.name(), TextCampaign.DAY_BUDGET, DayBudgetShowMode.DEFAULT_},
                {TextCampaign.ATTRIBUTION_MODEL.name(), TextCampaign.ATTRIBUTION_MODEL,
                        CampaignAttributionModel.FIRST_CLICK},
                {TextCampaign.ENABLE_CPC_HOLD.name(), TextCampaign.ENABLE_CPC_HOLD, true},
        };
    }

    private ClientInfo client;
    private final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private final DbStrategy defaultStrategy = TestCampaignsStrategy.defaultAutobudgetAvgCpa(now);
    private final int counterId = RandomNumberUtils.nextPositiveInteger();
    private final Long goalId = defaultStrategy.getStrategyData().getGoalId();

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(client.getClientId(), client.getUid());
        steps.featureSteps().setCurrentClient(client.getClientId());
    }

    @Test
    public void updateCampaignWithStrategyId_StrategyIdOfCampaignNotChanged() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);
        List<Long> metrikaCounters = List.of(122L);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.METRIKA_COUNTERS, metrikaCounters);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isEqualTo(campaign.getTypedCampaign().getStrategyId());
        });
    }

    @Test
    public void updateCampaignWithStrategyId_StrategyUpdated() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        List<Long> metrikaCounters = List.of(122L);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.METRIKA_COUNTERS, metrikaCounters);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);

        var strategy = (DefaultManualStrategy) strategyTypedRepository.getTyped(
                client.getShard(), List.of(updatedCampaign.getStrategyId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(strategy.getMetrikaCounters()).isEqualTo(metrikaCounters);
        });
    }

    @Test
    public void updateCampaignWithStrategyId_ChangeStrategyType_StrategyUpdated() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        List<Long> metrikaCounters = List.of(122L);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.METRIKA_COUNTERS, metrikaCounters);

        DbStrategy dbStrategy = defaultAutobudgetAvgCpa(now());
        dbStrategy.getStrategyData()
                .withAvgCpa(BigDecimal.valueOf(500))
                .withSum(BigDecimal.valueOf(1500))
                .withBid(null);
        mc.process(dbStrategy, TextCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);

        var strategy = (AutobudgetAvgCpa) strategyTypedRepository.getTyped(
                client.getShard(), List.of(updatedCampaign.getStrategyId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(strategy.getMetrikaCounters()).isEqualTo(metrikaCounters);
        });
    }

    @Test
    public void updateCampaignWithPackageStrategy_PackageStrategyUpdated() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        var campaign = createTextCampaignWithStrategyId(null);

        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getId(),
                TextCampaign.class, CampaignWithAttributionModel.ATTRIBUTION_MODEL,
                CampaignAttributionModel.FIRST_CLICK);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaign.getId()),
                CampaignWithPackageStrategy.class).get(0);

        var updatedStrategy = (CommonStrategy) strategyTypedRepository.getTyped(
                client.getShard(), List.of(campaign.getStrategyId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isEqualTo(campaign.getStrategyId());
            softly.assertThat(((CampaignWithAttributionModel) updatedCampaign).getAttributionModel())
                    .isEqualTo(CampaignAttributionModel.FIRST_CLICK);
            softly.assertThat(updatedStrategy.getAttributionModel()).isEqualTo(StrategyAttributionModel.FIRST_CLICK);
            softly.assertThat(updatedStrategy.getIsPublic()).isFalse();
        });
    }

    @Test
    public void updateCampaignWithStrategyId_PackageStrategiesStageTwoDisabled_ShouldNotUpdateStrategyId() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        Long newStrategyId = createAutoBudgetAvgCpaStrategyAndGetId(false);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.STRATEGY_ID, newStrategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isNotEqualTo(newStrategyId);
            softly.assertThat(updatedCampaign.getStrategy())
                    .usingRecursiveComparison()
                    .isEqualTo(campaign.getTypedCampaign().getStrategy());
        });
    }

    @Test
    public void updateCampaignWithStrategyId_PackageStrategiesStageTwoDisabled_ShouldUpdateStrategyWithoutStrategyId() {
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        Long newStrategyId = createAutoBudgetAvgCpaStrategyAndGetId(false);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.STRATEGY_ID, newStrategyId);

        DbStrategy dbStrategy = campaign.getTypedCampaign().getStrategy().copy();
        dbStrategy.getStrategyData()
                .withAvgCpa(BigDecimal.valueOf(500))
                .withSum(BigDecimal.valueOf(1500))
                .withBid(null);
        mc.process(dbStrategy, TextCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isNotEqualTo(newStrategyId);
            softly.assertThat(updatedCampaign.getStrategy())
                    .usingRecursiveComparison()
                    .isEqualTo(dbStrategy);
        });
    }

    @Test
    public void updateCampaignWithStrategyId_PackageStrategiesStageTwoEnabled_ShouldUpdateStrategyAndStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        Long newStrategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(),
                TextCampaign.STRATEGY_ID, newStrategyId);

        DbStrategy dbStrategy = campaign.getTypedCampaign().getStrategy().copy();
        dbStrategy.getStrategyData()
                .withAvgCpa(BigDecimal.valueOf(500))
                .withSum(BigDecimal.valueOf(1500))
                .withBid(null);
        mc.process(dbStrategy, TextCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(campaign.getShard(),
                List.of(campaign.getCampaignId())).get(0);

        var updatedCampaignStrategy = updatedCampaign.getStrategy();
        var oldCampaignStrategy = campaign.getTypedCampaign().getStrategy();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrors()));
            softly.assertThat(result.getValidationResult()).is(matchedBy(
                    hasWarningWithDefinition(validationError(CAMPAIGN_STRATEGY_INFO_IS_IGNORED_ON_CAMPAIGN_UPDATE)))
            );
            softly.assertThat(updatedCampaign.getStrategyId()).isEqualTo(newStrategyId);
            softly.assertThat(updatedCampaignStrategy.getStrategyName())
                    .isEqualTo(ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA);
            softly.assertThat(updatedCampaignStrategy.getStrategy())
                    .isEqualTo(oldCampaignStrategy.getStrategy());
            softly.assertThat(updatedCampaignStrategy.getPlatform())
                    .isEqualTo(oldCampaignStrategy.getPlatform());
        });
    }

    @Test
    public void notWarningOnErrors() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        Long publicStrategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        TextCampaign textCampaign = createTextCampaignWithStrategyId(publicStrategyId);
        DynamicCampaignInfo dynamicCampaign = steps.dynamicCampaignSteps().createDefaultCampaign(client);
        DbStrategy dbStrategy = textCampaign.getStrategy().copy();
        dbStrategy.getStrategyData()
                .withAvgCpa(BigDecimal.valueOf(500))
                .withSum(BigDecimal.valueOf(1500))
                .withBid(null);
        ModelChanges<DynamicCampaign> mc = new ModelChanges<>(dynamicCampaign.getCampaignId(), DynamicCampaign.class)
                .process(publicStrategyId, DynamicCampaign.STRATEGY_ID)
                .process(dbStrategy, DynamicCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var actualTextCampaign = campaignTypedRepository.getSafely(client.getShard(), List.of(textCampaign.getId()),
                TextCampaign.class)
                .get(0);
        var actualDynamicCampaign = campaignTypedRepository.getSafely(client.getShard(),
                List.of(dynamicCampaign.getCampaignId()), DynamicCampaign.class)
                .get(0);

        var result = updateOperation.apply();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .is(matchedBy(hasDefectWithDefinition(validationError(CAMPAIGNS_WITH_DIFFERENT_TYPES_IN_ONE_PACKAGE))));
            softly.assertThat(result.getValidationResult())
                    .is(matchedBy(hasNoWarnings()));
            softly.assertThat(actualDynamicCampaign.getStrategy()).isEqualTo(dynamicCampaign.getTypedCampaign().getStrategy());
            softly.assertThat(actualTextCampaign.getStrategyId()).isEqualTo(publicStrategyId);
            softly.assertThat(actualDynamicCampaign.getStrategyId()).isNotEqualTo(publicStrategyId);
        });
    }

    @Test
    public void updateCampaignWithStrategyId_PackageStrategiesStageTwoEnabled_ResetStrategy_ShouldCreateNewNonPublicPackageStrategy() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        var campaign = createTextCampaignWithStrategyId(strategyId);

        ModelChanges<CampaignWithPackageStrategy> mc = ModelChanges.build(campaign.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, null);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(client.getShard(),
                List.of(campaign.getId())).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isNotEqualTo(strategyId);
            softly.assertThat(updatedCampaign.getStrategy().getStrategyName())
                    .isEqualTo(ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA);
        });
    }

    @Test
    public void updateCampaignWithStrategyId_ShouldResetBannersBsSyncStatus() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        Long bannerId = steps.bannerSteps().createActiveTextBanner(campaign).getBannerId();

        ModelChanges<TextCampaign> mc = ModelChanges.build(campaign.getTypedCampaign(), TextCampaign.STRATEGY_ID,
                strategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var actualBanner = bannerService.getBannersByIds(List.of(bannerId)).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(actualBanner.getStatusBsSynced())
                    .isEqualTo(StatusBsSynced.NO);
        });
    }

    @TestCaseName("изменения поля {0}")
    @Parameters(method = "testModelProperties")
    @Test
    public void campaignUpdateValidationWarningOnUpdateFieldsAffectesStrategy(
            String propsName,
            ModelProperty<? extends CampaignWithPackageStrategy, Object> property,
            Object value
    ) {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);

        ModelChanges<TextCampaign> mc = new ModelChanges(campaign.getCampaignId(), TextCampaign.class)
                .process(strategyId, TextCampaign.STRATEGY_ID)
                .process(value, property);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasWarningWithDefinition(
                    validationError(CAMPAIGN_STRATEGY_INFO_IS_IGNORED_ON_CAMPAIGN_UPDATE)
            )));
        });
    }

    @Test
    public void notTriggerWarningOnCampaignNameUpdate() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);

        ModelChanges<TextCampaign> mc = new ModelChanges(campaign.getCampaignId(), TextCampaign.class)
                .process(strategyId, TextCampaign.STRATEGY_ID)
                .process("some name", TextCampaign.NAME);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
        });
    }

    @Test
    public void notTriggerWarningOnCampaignWithPrivatePackageUpdate() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        var dbStrategy = campaign.getTypedCampaign().getStrategy()
                .withStrategyData(
                        campaign.getTypedCampaign().getStrategy().getStrategyData()
                                .withAvgCpa(BigDecimal.TEN)
                );

        ModelChanges<TextCampaign> mc = new ModelChanges(campaign.getCampaignId(), TextCampaign.class)
                .process(campaign.getTypedCampaign().getStrategyId(), TextCampaign.STRATEGY_ID)
                .process(dbStrategy, CampaignWithPackageStrategy.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaign.getId()),
                TextCampaign.class
        ).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(actualCampaign.getStrategy()).isEqualTo(dbStrategy);
        });
    }

    @Test
    public void campaignRemovalFromPublicPackageWarning() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        CampaignWithPackageStrategy campaign = createTextCampaignWithStrategyId(strategyId);
        var dbStrategy = campaign.getStrategy()
                .withStrategyData(
                        campaign.getStrategy().getStrategyData()
                                .withAvgCpa(BigDecimal.TEN)
                );
        ModelChanges<CampaignWithPackageStrategy> mc = new ModelChanges(campaign.getId(),
                CampaignWithPackageStrategy.class)
                .process(strategyId, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(dbStrategy, CampaignWithPackageStrategy.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaign.getId()),
                TextCampaign.class
        ).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrors()));
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasWarningWithDefinition(validationError(NEW_STRATEGY_HAS_BEEN_CREATED))));
            softly.assertThat(actualCampaign.getStrategy()).isEqualTo(dbStrategy);
            softly.assertThat(actualCampaign.getStrategyId()).isNotEqualTo(strategyId);
        });

    }

    @Test
    public void noWarningsIfCampaignStrategyUpdateDoesNotCreateNewPackage() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        CampaignWithPackageStrategy campaign = createTextCampaignWithStrategyId(strategyId);
        var dbStrategy = campaign.getStrategy();
        ModelChanges<CampaignWithPackageStrategy> mc = new ModelChanges(campaign.getId(),
                CampaignWithPackageStrategy.class)
                .process(strategyId, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(dbStrategy, CampaignWithPackageStrategy.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaign.getId()),
                TextCampaign.class
        ).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(actualCampaign.getStrategy()).isEqualTo(dbStrategy);
            softly.assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
        });
    }

    @Test
    public void errorOnZeroStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        TextCampaignInfo campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        ModelChanges<TextCampaign> mc = new ModelChanges(campaign.getCampaignId(), TextCampaign.class)
                .process(0L, TextCampaign.STRATEGY_ID)
                .process(new DbStrategy(), TextCampaign.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .is(matchedBy(hasDefectWithDefinition(validationError(MUST_BE_VALID_ID))));
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoWarnings()));
        });
    }

    @Test
    public void unbindCampaignNoWarning() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(true);
        CampaignWithPackageStrategy campaign = createTextCampaignWithStrategyId(strategyId);
        var dbStrategy = campaign.getStrategy()
                .withStrategyData(
                        campaign.getStrategy().getStrategyData()
                                .withAvgCpa(BigDecimal.TEN)
                );
        ModelChanges<CampaignWithPackageStrategy> mc = new ModelChanges(campaign.getId(),
                CampaignWithPackageStrategy.class)
                .process(null, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(dbStrategy, CampaignWithPackageStrategy.STRATEGY);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();
        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaign.getId()),
                TextCampaign.class
        ).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(actualCampaign.getStrategy()).isEqualTo(dbStrategy);
            softly.assertThat(actualCampaign.getStrategyId()).isNotEqualTo(strategyId);
        });
    }

    @Test
    public void privatePackageStrategyShouldBecome_WhenLinkNewCampaign() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(false);
        Long strategyId2 = createAutoBudgetAvgCpaStrategyAndGetId(false);
        var campaign = createTextCampaignWithStrategyId(strategyId);
        var campaign2 = createTextCampaignWithStrategyId(null);

        ModelChanges<CampaignWithPackageStrategy> mc = ModelChanges.build(campaign.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId2);

        ModelChanges<CampaignWithPackageStrategy> mc2 = ModelChanges.build(campaign2.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc, mc2), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaignsById = campaignTypedRepository.getIdToModelTyped(
                client.getShard(), List.of(campaign.getId(), campaign2.getId()));
        var updatedCampaign1 = (CampaignWithPackageStrategy) updatedCampaignsById.get(campaign.getId());
        var updatedCampaign2 = (CampaignWithPackageStrategy) updatedCampaignsById.get(campaign2.getId());

        var strategyAfterUpdate = (CommonStrategy) strategyTypedRepository.getTyped(client.getShard(),
                List.of(strategyId)).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign1.getStrategyId()).isEqualTo(strategyId2);
            softly.assertThat(updatedCampaign2.getStrategyId()).isEqualTo(strategyId);
            softly.assertThat(strategyAfterUpdate.getIsPublic()).isTrue();
            softly.assertThat(strategyAfterUpdate.getName()).isNotNull();
        });
    }

    @Test
    public void privatePackageStrategyShouldBecomePublic_WhenLinkedCidsSizeMoreThanOne() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(false);
        createTextCampaignWithStrategyId(strategyId);
        var campaign2 = createTextCampaignWithStrategyId(null);

        ModelChanges<CampaignWithPackageStrategy> mc = ModelChanges.build(campaign2.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign2 = (CampaignWithPackageStrategy) campaignTypedRepository.getTypedCampaigns(
                client.getShard(), List.of(campaign2.getId())).get(0);

        var strategyAfterUpdate = (CommonStrategy) strategyTypedRepository.getTyped(client.getShard(),
                List.of(strategyId)).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign2.getStrategyId()).isEqualTo(strategyId);
            softly.assertThat(strategyAfterUpdate.getIsPublic()).isTrue();
            softly.assertThat(strategyAfterUpdate.getName())
                    .isEqualTo(String.format("Strategy %d dated %s", strategyId, now.toLocalDate()));
        });
    }

    @Test
    public void privatePackageStrategyShouldBecomePublic_WhenLinkedCidsSizeMoreThanOne_AddTwoNewCampaignsInOneOperation() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        Long strategyId = createAutoBudgetAvgCpaStrategyAndGetId(false);
        createTextCampaignWithStrategyId(strategyId);
        var campaign2 = createTextCampaignWithStrategyId(null);
        var campaign3 = createTextCampaignWithStrategyId(null);

        ModelChanges<CampaignWithPackageStrategy> mc = ModelChanges.build(campaign2.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId);

        ModelChanges<CampaignWithPackageStrategy> mc2 = ModelChanges.build(campaign3.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc, mc2), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var strategyAfterUpdate = (CommonStrategy) strategyTypedRepository.getTyped(client.getShard(),
                List.of(strategyId)).get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(strategyAfterUpdate.getCids().size()).isEqualTo(3);
            softly.assertThat(strategyAfterUpdate.getIsPublic()).isTrue();
            softly.assertThat(strategyAfterUpdate.getName())
                    .isEqualTo(String.format("Strategy %d dated %s", strategyId, now.toLocalDate()));
        });
    }

    @Test
    public void linkCampaignToPublicPackageStrategy_StrategyIsPublicAndNameNotChanged() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        String oldStrategyName = "strategyName";
        CommonStrategy strategyToAdd =
                autobudgetAvgCpa(true)
                        .withName(oldStrategyName)
                        .withMetrikaCounters(List.of((long) counterId));
        StrategyAddOperation strategyOperation =
                strategyOperationFactory.createStrategyAddOperation(
                        client.getShard(),
                        client.getUid(),
                        client.getClientId(),
                        client.getUid(),
                        List.of(strategyToAdd),
                        new StrategyOperationOptions()
                );
        Long strategyId = strategyOperation.prepareAndApply().get(0).getResult();

        var campaign = steps.textCampaignSteps().createDefaultCampaign(client);

        ModelChanges<CampaignWithPackageStrategy> mc = ModelChanges.build(campaign.getId(),
                CampaignWithPackageStrategy.class, CampaignWithPackageStrategy.STRATEGY_ID, strategyId);

        RestrictedCampaignsUpdateOperation updateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(List.of(mc), client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), Applicability.PARTIAL,
                        new CampaignOptions());

        var result = updateOperation.apply();

        var updatedCampaign = (CampaignWithPackageStrategy) campaignTypedRepository.getTypedCampaigns(
                client.getShard(), List.of(campaign.getId(), campaign.getId())).get(0);

        var strategyAfterUpdate = (CommonStrategy) strategyTypedRepository.getTyped(client.getShard(),
                List.of(strategyId)).get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoErrorsAndWarnings()));
            softly.assertThat(updatedCampaign.getStrategyId()).isEqualTo(strategyId);
            softly.assertThat(strategyAfterUpdate.getName()).isEqualTo(oldStrategyName);
            softly.assertThat(strategyAfterUpdate.getIsPublic()).isTrue();
        });
    }

    private TextCampaign createTextCampaignWithStrategyId(Long strategyId) {
        List<? extends BaseCampaign> campaigns =
                List.of(defaultTextCampaign().withStrategyId(strategyId));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        Assert.assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());
        Long campaignId = result.get(0).getResult();

        var campaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaignId),
                TextCampaign.class).get(0);

        return campaign;
    }

    private Long createAutoBudgetAvgCpaStrategyAndGetId(boolean isPublicStrategy) {
        CommonStrategy strategyToAdd =
                autobudgetAvgCpa(isPublicStrategy)
                        .withMetrikaCounters(List.of((long) counterId));
        StrategyAddOperation operation =
                strategyOperationFactory.createStrategyAddOperation(
                        client.getShard(),
                        client.getUid(),
                        client.getClientId(),
                        client.getUid(),
                        List.of(strategyToAdd),
                        new StrategyOperationOptions()
                );
        var result = operation.prepareAndApply();
        Assert.assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());

        return result.get(0).getResult();
    }

    private AutobudgetAvgCpa autobudgetAvgCpa(boolean isPublicStrategy) {
        stubGoal();

        var strategyData = defaultStrategy.getStrategyData();
        return new AutobudgetAvgCpa()
                .withType(StrategyName.AUTOBUDGET_AVG_CPA)
                .withIsPublic(isPublicStrategy)
                .withAttributionModel(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStatusArchived(false)
                .withSum(strategyData.getSum())
                .withGoalId(strategyData.getGoalId())
                .withAvgCpa(strategyData.getAvgCpa())
                .withIsPayForConversionEnabled(strategyData.getPayForConversion());
    }

    private void stubGoal() {
        metrikaClientStub.addUserCounter(client.getUid(), counterId);
        metrikaClientStub.addCounterGoal(counterId, goalId.intValue());
    }
}
