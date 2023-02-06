package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.entity.strategy.service.converter.StrategyToCampaignConverterFacade;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithPackageStrategyUpdateOperationSupportOnModelChangesValidatedTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignOperationService campaignOperationService;

    @Autowired
    public WalletService walletService;

    @Autowired
    public StrategyTypedRepository strategyTypedRepository;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public StrategyOperationFactory strategyOperationFactory;

    @Autowired
    public MetrikaClientStub metrikaClientStub;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public StrategyToCampaignConverterFacade strategyToCampaignConverterFacade;

    @Autowired
    public CampaignWithPackageStrategyUpdateOperationSupport support;

    private ClientInfo client;
    private RestrictedCampaignsUpdateOperationContainer container;
    private BaseStrategy newStrategy;

    private static final Long strategyIdStub = 3242L;
    private static final LocalDateTime now = LocalDateTime.now();

    private static CampaignWithPackageStrategy oldCampaign;

    @Parameterized.Parameter(0)
    public boolean isPackageStrategiesStageTwoEnabled;

    @Parameterized.Parameter(1)
    public Long mcStrategyId;

    @Parameterized.Parameter(2)
    public DbStrategy mcDbStrategy;

    @Parameterized.Parameters(name = "secondStageFeature: {0}, mcStrategyId: {1}, mcDbStrategy: {2}")
    public static List<Object[]> params() {
        List<Object> packageStrategiesStageTwoEnabledValues = List.of(true, false);

        var mcStrategyIdValues = new ArrayList<>();
        mcStrategyIdValues.add(null);
        mcStrategyIdValues.add(1L); // заглушка для обозначения, что в modelChanges будет передан старый strategyId
        mcStrategyIdValues.add(strategyIdStub); // заглушка для обозначения, что в modelChanges будет передан новый
        // валидный strategyId

        List<Object> mcDbStrategyValues = new ArrayList<>();
        mcDbStrategyValues.add(null); // заглушка для обозначения, что в modelChanges будет передан старый strategy
        // с не значимыми для пакетной стратегии изменениями
        mcDbStrategyValues.add(TestCampaignsStrategy.defaultAutobudgetMaxImpressions());

        return StreamEx.cartesianProduct(List.of(packageStrategiesStageTwoEnabledValues, mcStrategyIdValues,
                        mcDbStrategyValues))
                .map(List::toArray)
                .collect(Collectors.toList());
    }

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(client.getClientId(), client.getUid());

        container = RestrictedCampaignsUpdateOperationContainer.create(
                client.getShard(), client.getUid(), client.getClientId(), client.getUid(), client.getUid());
    }

    @Test
    public void onModelChangesValidated() {
        oldCampaign = createCampaignWithStrategyId();
        var validMcStrategyId = getActualMcStrategyId();
        var modelChanges = getCampaignStrategyBaseModelChanges(validMcStrategyId);

        support.onModelChangesValidated(container, List.of(modelChanges));

        ModelChanges<CampaignWithPackageStrategy> expectedModelChanges;
        if (Objects.equals(oldCampaign.getStrategyId(), validMcStrategyId)) {
            // столбцы 3 и 4 в таблице кейсов
            // ModelChanges для стратегии оставляем теми же, а создание/обновление пакетной стратегии
            // будет на уровне updateRelatedEntitiesInTransaction
            expectedModelChanges = getCampaignStrategyBaseModelChanges(validMcStrategyId);
        } else if (!isPackageStrategiesStageTwoEnabled || validMcStrategyId == null) {
            // столбец 2 строка 2 в таблице кейсов, а также случай с выключенной фичей второго этапа
            // в первом случае "сбрасываем" strategyId до 0, во втором -- до того, что указан в кампании
            // в первом случае создадим новую стратегию на этапе updateRelatedEntitiesInTransaction,
            // во втором -- обновим исходную стратегию также на этапе updateRelatedEntitiesInTransaction
            // ModelChanges для стратегии оставляем теми же, тк новую стратегию создадим из кампании (случай 1),
            // либо обновим старую также из изменений кампании
            var strategyIdToResetOn = !isPackageStrategiesStageTwoEnabled ? oldCampaign.getStrategyId() : 0;
            expectedModelChanges = getModelChangesWithResetStrategyIdAndSameStrategy(strategyIdToResetOn);
        } else {
            // столбцы 5 и 6 в таблице кейсов
            // обновляем ModelChanges до значений стратегии с переданным strategyId
            expectedModelChanges = getModelChangesWithChangedStrategy();
        }

        if (modelChanges.isPropChanged(CampaignWithPackageStrategy.LAST_CHANGE)) {
            expectedModelChanges.process(modelChanges.getChangedProp(CampaignWithPackageStrategy.LAST_CHANGE),
                    CampaignWithPackageStrategy.LAST_CHANGE);
        }
        assertThat(modelChanges).usingRecursiveComparison().isEqualTo(expectedModelChanges);
    }

    private ModelChanges<CampaignWithPackageStrategy> getModelChangesWithResetStrategyIdAndSameStrategy(Long strategyIdToResetOn) {
        return getCampaignStrategyBaseModelChanges(strategyIdToResetOn);
    }

    private ModelChanges<CampaignWithPackageStrategy> getModelChangesWithChangedStrategy() {
        var expectedModelChanges = new ModelChanges<>(oldCampaign.getId(), CampaignWithPackageStrategy.class);
        strategyToCampaignConverterFacade.copyStrategyToCampaignModelChanges(now, newStrategy, expectedModelChanges,
                oldCampaign.getClass());
        expectedModelChanges.getChangedProp(CampaignWithPackageStrategy.STRATEGY)
                .setPlatform(oldCampaign.getStrategy().getPlatform());
        expectedModelChanges.getChangedProp(CampaignWithPackageStrategy.STRATEGY)
                .setStrategy(oldCampaign.getStrategy().getStrategy());
        expectedModelChanges.process(
                expectedModelChanges.getChangedProp(CampaignWithPackageStrategy.LAST_CHANGE),
                CampaignWithPackageStrategy.LAST_CHANGE);

        return expectedModelChanges;
    }

    private Long getActualMcStrategyId() {
        if (mcStrategyId == null) {
            return mcStrategyId;
        } else if (mcStrategyId == 1) {
            return oldCampaign.getStrategyId();
        } else {
            newStrategy = createStrategy(autobudgetAvgCpa()
                    .withAttributionModel(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE));
            return newStrategy.getId();
        }
    }

    private CampaignWithPackageStrategy createCampaignWithStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO,
                isPackageStrategiesStageTwoEnabled);

        Long oldStrategyId = createStrategy(autobudgetAvgCpa()).getId();

        var campaignToAdd = defaultTextCampaign();
        List<? extends BaseCampaign> campaignsToAdd = List.of(campaignToAdd.withStrategyId(oldStrategyId));
        RestrictedCampaignsAddOperation restrictedCampaignAddOperation =
                campaignOperationService.createRestrictedCampaignAddOperation(campaignsToAdd, client.getUid(),
                        UidAndClientId.of(client.getUid(), client.getClientId()), new CampaignOptions());
        var result = restrictedCampaignAddOperation.prepareAndApply();
        Assert.assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());

        return campaignTypedRepository.getStrictlyFullyFilled(
                client.getShard(), List.of(result.get(0).getResult()), CampaignWithPackageStrategy.class).get(0);
    }

    private BaseStrategy createStrategy(BaseStrategy strategy) {
        StrategyAddOperation operation =
                strategyOperationFactory.createStrategyAddOperation(
                        client.getShard(),
                        client.getUid(),
                        client.getClientId(),
                        client.getUid(),
                        List.of(strategy),
                        new StrategyOperationOptions()
                );
        var result = operation.prepareAndApply();
        Assert.assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());

        var createdStrategyId = result.get(0).getResult();
        return strategyTypedRepository.getIdToModelTyped(client.getShard(), List.of(createdStrategyId)).get(createdStrategyId);
    }

    private ModelChanges<CampaignWithPackageStrategy> getCampaignStrategyBaseModelChanges(Long validMcStrategyId) {
        return new ModelChanges<>(oldCampaign.getId(), CampaignWithPackageStrategy.class)
                .process(validMcStrategyId, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(getActualDbStrategy(), CampaignWithPackageStrategy.STRATEGY);
    }

    private DbStrategy getActualDbStrategy() {
        return mcDbStrategy == null
                ? oldCampaign.getStrategy()
                : (DbStrategy) mcDbStrategy
                .withPlatform(oldCampaign.getStrategy().getPlatform())
                .withStrategy(oldCampaign.getStrategy().getStrategy());
    }

}
