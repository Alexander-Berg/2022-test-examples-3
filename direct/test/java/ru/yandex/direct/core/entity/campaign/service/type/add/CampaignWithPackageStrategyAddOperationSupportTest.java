package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa;
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCampaignIds;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator.ORDER_ID_OFFSET;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds.Gen.CAMPAIGNS_WITH_DIFFERENT_TYPES_IN_ONE_PACKAGE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.feature.FeatureName.GET_STRATEGY_ID_FROM_SHARD_INC_STRATEGY_ID;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
import static ru.yandex.direct.feature.FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithPackageStrategyAddOperationSupportTest {
    @Autowired
    public Steps steps;

    @Autowired
    public CampaignOperationService campaignOperationService;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public StrategyTypedRepository strategyTypedRepository;

    @Autowired
    public StrategyOperationFactory strategyOperationFactory;

    @Autowired
    public ShardSupport shardSupport;

    @Autowired
    public WalletService walletService;

    @Autowired
    public MetrikaClientStub metrikaClientStub;

    private ClientInfo client;

    private final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private final DbStrategy defaultStrategy = TestCampaignsStrategy.defaultAutobudgetAvgCpa(now);
    private final int counterId = RandomNumberUtils.nextPositiveInteger();
    private final Long goalId = defaultStrategy.getStrategyData().getGoalId();

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(client.getClientId(), client.getUid());
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_AddStrategyAndSetStrategyId() {
        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign());
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        long strategyId = campaignId + ORDER_ID_OFFSET;
        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_NewStrategyIdGenerationEnabled_AddStrategyAndSetStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(),
                GET_STRATEGY_ID_FROM_SHARD_INC_STRATEGY_ID, true);
        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign());
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        long actualStrategyId = actualCampaign.getStrategyId();
        long oldExpectedStrategyId = campaignId + ORDER_ID_OFFSET;

        assertThat(actualStrategyId).isNotZero();
        assertThat(actualStrategyId).isNotEqualTo(oldExpectedStrategyId);
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_SecondStageFeatureEnabled_ValidationOkWithNullStrategy_SetStrategyId() {
        var strategyId = createAutobudgetAvgCpaStrategyAndGetId();
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        List<? extends BaseCampaign> campaigns =
                List.of(defaultTextCampaign().withStrategy((DbStrategy) new DbStrategy().withPlatform(CampaignsPlatform.BOTH)).withStrategyId(strategyId));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> campaignAddResult = restrictedCampaignAddOperation.prepareAndApply();
        Long campaignId = campaignAddResult.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getSafely(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_PackageStrategiesStageTwoEnabled_StrategyIdAdded_CopyStrategyToCampaign() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        List<? extends BaseCampaign> campaigns =
                List.of(defaultTextCampaign()
                        .withStrategyId(strategyId)
                        .withStrategy((DbStrategy) new DbStrategy()
                                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                                .withPlatform(CampaignsPlatform.BOTH)));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);

        var lastBidderRestartTimeForExpectedStrategy = actualCampaign
                .getStrategy()
                .getStrategyData()
                .getLastBidderRestartTime();
        var expectedStrategy =
                (DbStrategy) TestCampaignsStrategy.defaultAutobudgetAvgCpa(lastBidderRestartTimeForExpectedStrategy)
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withPlatform(CampaignsPlatform.BOTH);
        assertThat(actualCampaign.getStrategy())
                .usingRecursiveComparison()
                .isEqualTo(expectedStrategy);
    }

    @Test
    public void addCampaign_FeaturesEnabled_LinkingToPrivateStrategyWithoutCampaignsChangingToPublic() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign().withStrategyId(strategyId));

        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
        var strategy = strategyTypedRepository.getTyped(client.getShard(), List.of(strategyId)).get(0);
        assertThat(((CommonStrategy) strategy).getIsPublic()).isTrue();
        assertThat(((CommonStrategy) strategy).getName()).isNotNull();
    }

    @Test
    public void addCampaign_FeaturesEnabled_LinkingPrivateStrategyWithOneCampBecomePublic() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign().withStrategyId(strategyId));
        List<? extends BaseCampaign> campaigns2 = List.of(defaultTextCampaign().withStrategyId(strategyId));

        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        addOperation.prepareAndApply();

        RestrictedCampaignsAddOperation addOperation2 =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns2, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());

        MassResult<Long> result = addOperation2.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
        var strategy = strategyTypedRepository.getTyped(client.getShard(), List.of(strategyId)).get(0);
        assertThat(((CommonStrategy) strategy).getIsPublic()).isTrue();
        assertThat(((CommonStrategy) strategy).getName())
                .isEqualTo(String.format("Strategy %d dated %s", strategyId,
                        ((CommonStrategy) strategy).getLastChange().toLocalDate()));
    }

    @Test
    public void addTwoCampaignsWithOneStrategyId_FeaturesEnabled_LinkingToPrivatePackageStrategy_StrategyBecomePublic() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign().withStrategyId(strategyId),
                defaultTextCampaign().withStrategyId(strategyId));

        RestrictedCampaignsAddOperation addOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        var strategy = strategyTypedRepository.getTyped(client.getShard(), List.of(strategyId)).get(0);
        assertThat(((StrategyWithCampaignIds) strategy).getCids().size()).isEqualTo(2);
        assertThat(((CommonStrategy) strategy).getIsPublic()).isTrue();
        assertThat(((CommonStrategy) strategy).getName())
                .isEqualTo(String.format("Strategy %d dated %s", strategyId,
                        ((CommonStrategy) strategy).getLastChange().toLocalDate()));
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_PackageStrategiesStageTwoEnabled_CreateWithoutStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        List<? extends BaseCampaign> campaigns = List.of(defaultTextCampaign());
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        assertThat(actualCampaign.getStrategyId()).isPositive();
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_PackageStrategiesStageTwoDisabled_StrategyIdNotAdded_NewStrategyCreated() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();
        testAddCampaignWithStrategyId_PackageStrategiesStageTwoDisabled(strategyId);
    }

    @Test
    public void addCampaign_StrategyAddingEnabled_PackageStrategiesStageTwoDisabled_StrategyIdNull_NewStrategyCreated() {
        testAddCampaignWithStrategyId_PackageStrategiesStageTwoDisabled(null);
    }

    @Test
    public void addCampaignWithStrategyId_MetrikaCountersNotSetInCampaign_StrategyWithMeaningfulGoals_NewCampaignCreated() {
        CommonStrategy strategyToAdd = autobudgetAvgCpa()
                .withMeaningfulGoals(List.of(
                        new MeaningfulGoal()
                                .withGoalId(goalId)
                                .withConversionValue(BigDecimal.TEN)))
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
        var strategyAddResult = operation.prepareAndApply();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);
        steps.featureSteps().addClientFeature(client.getClientId(), UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true);

        List<? extends BaseCampaign> campaigns =
                List.of(defaultTextCampaign()
                        .withStrategyId(strategyAddResult.get(0).getResult())
                        .withStrategy((DbStrategy) new DbStrategy()
                                .withPlatform(CampaignsPlatform.BOTH))
                );
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noWarningsOnErrors() {
        Long strategyId = createAutobudgetAvgCpaStrategyAndGetId();

        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, true);

        var textCampaign = defaultTextCampaign().withStrategyId(strategyId);
        var dynamicCampaign = defaultDynamicCampaign().withStrategyId(strategyId);

        RestrictedCampaignsAddOperation textCampaignAddOperation = defaultAddOperation(textCampaign);
        MassResult<Long> textCampaignAddResult = textCampaignAddOperation.prepareAndApply();

        RestrictedCampaignsAddOperation dynamicCampaignAddOperation = defaultAddOperation(dynamicCampaign);
        MassResult<Long> dynamicCampaignAddResult = dynamicCampaignAddOperation.prepareAndApply();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(textCampaignAddResult.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(dynamicCampaignAddResult.getValidationResult())
                    .is(matchedBy(hasDefectWithDefinition(validationError(CAMPAIGNS_WITH_DIFFERENT_TYPES_IN_ONE_PACKAGE))));
            softly.assertThat(dynamicCampaignAddResult.getValidationResult())
                    .is(matchedBy(hasNoWarnings()));
        });
    }

    private RestrictedCampaignsAddOperation defaultAddOperation(BaseCampaign... campaign) {
        return campaignOperationService.createRestrictedCampaignAddOperation(
                List.of(campaign),
                client.getUid(),
                UidAndClientId.of(client.getUid(), client.getClientId()),
                new CampaignOptions()
        );
    }

    private void testAddCampaignWithStrategyId_PackageStrategiesStageTwoDisabled(Long strategyIdForOperation) {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO, false);

        var strategyToAdd = TestCampaignsStrategy.defaultAutobudgetAvgCpa(now);
        List<? extends BaseCampaign> campaigns =
                List.of(defaultTextCampaign()
                        .withStrategyId(strategyIdForOperation)
                        .withStrategy(strategyToAdd));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaigns, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        MassResult<Long> result = restrictedCampaignAddOperation.prepareAndApply();
        Long campaignId = result.get(0).getResult();

        var actualCampaign = campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(),
                List.of(campaignId),
                CampaignWithPackageStrategy.class).get(0);

        long strategyId = campaignId + ORDER_ID_OFFSET;
        assertThat(actualCampaign.getStrategyId()).isEqualTo(strategyId);
        assertThat(actualCampaign.getStrategy())
                .usingRecursiveComparison()
                .isEqualTo(strategyToAdd);
    }

    private Long createAutobudgetAvgCpaStrategyAndGetId() {
        CommonStrategy strategyToAdd = autobudgetAvgCpa().withMetrikaCounters(List.of((long) counterId));
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
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));

        return result.get(0).getResult();
    }

    private AutobudgetAvgCpa autobudgetAvgCpa() {
        stubGoal();

        var strategyData = defaultStrategy.getStrategyData();
        return new AutobudgetAvgCpa()
                .withType(StrategyName.AUTOBUDGET_AVG_CPA)
                .withIsPublic(false)
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
