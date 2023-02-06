package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCampaignIds;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.entity.strategy.service.converter.CampaignToStrategyConverterService;
import ru.yandex.direct.core.entity.strategy.service.converter.StrategyToCampaignConverterFacade;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithPackageStrategyUpdateOperationSupportUpdateRelatedEntitiesInTransactionTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignToStrategyConverterService campaignToStrategyConverterService;

    @Autowired
    public CampaignOperationService campaignOperationService;

    @Autowired
    public WalletService walletService;

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;

    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public StrategyTypedRepository strategyTypedRepository;

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
    private CommonStrategy oldStrategy;
    private Long oldStrategyId;
    private RestrictedCampaignsUpdateOperationContainer container;

    private static final Long strategyIdStub = 3242L;
    private static final LocalDateTime now = LocalDateTime.now();

    private CampaignWithPackageStrategy oldCampaign;

    @Parameterized.Parameter(0)
    public boolean isPackageStrategiesStageTwoEnabled;

    @Parameterized.Parameter(1)
    public Boolean isOldStrategyPublic;

    @Parameterized.Parameter(2)
    public Long mcStrategyId;

    @Parameterized.Parameter(3)
    public DbStrategy mcDbStrategy;

    @Parameterized.Parameter(4)
    public CampaignAttributionModel mcAttributionModel;

    @Parameterized.Parameter(5)
    public CampaignsPlatform mcPlatform;

    @Parameterized.Parameters(name = "secondStageFeature: {0}, isOldStrategyPublic: {1}, mcStrategyId: {2}, " +
            "mcDbStrategy: {3}, mcAttributionModel: {4}, mcPlatform: {5}")
    public static List<Object[]> params() {
        List<Object> packageStrategiesStageTwoEnabledValues = List.of(true, false);

        List<Object> mcStrategyIdValues = new ArrayList<>();
        mcStrategyIdValues.add(null);
        mcStrategyIdValues.add(1L);
        mcStrategyIdValues.add(strategyIdStub); // заглушки 1L и strategyIdStub аналогичны
        // тем, что в тестовом файле для метода onModelChangesValidated

        List<Object> mcDbStrategyValues = new ArrayList<>();
        mcDbStrategyValues.add(null);
        mcDbStrategyValues.add(TestCampaignsStrategy.defaultAutobudgetMaxImpressions());
        List<Object> mcAttributionModelValues = List.of(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK,
                CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE);

        List<Object> isOldStrategyPublicValues = new ArrayList<>();
        isOldStrategyPublicValues.add(null);
        isOldStrategyPublicValues.add(false);
        isOldStrategyPublicValues.add(true);

        List<Object> mcPlatformValues = List.of(CampaignsPlatform.BOTH, CampaignsPlatform.SEARCH);

        var testsData = StreamEx.cartesianProduct(List.of(packageStrategiesStageTwoEnabledValues,
                        isOldStrategyPublicValues, mcStrategyIdValues,
                        mcDbStrategyValues, mcAttributionModelValues, mcPlatformValues))
                .map(List::toArray)
                .collect(Collectors.toList());

        // невозможно отлинковать кампанию от непубличного пакета, не прилинковав ее к другому пакету
        testsData.removeIf(testData -> (boolean) testData[0]
                && (testData[1] == null || !((boolean) testData[1]))
                && (testData[2] == null));

        return testsData;
    }

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();
        walletService.createWalletForNewClient(client.getClientId(), client.getUid());

        container = RestrictedCampaignsUpdateOperationContainer.create(
                client.getShard(), client.getUid(), client.getClientId(), client.getUid(), client.getUid());
    }

    @Test
    public void test() {
        oldCampaign = createCampaignWithStrategyId();
        oldStrategyId = oldCampaign.getStrategyId();
        oldStrategy =
                (CommonStrategy) strategyTypedRepository.getTyped(client.getShard(), List.of(oldStrategyId)).get(0);
        var validMcStrategyId = getActualMcStrategyId();
        var strategyWithMcStrategyId = validMcStrategyId != null && !validMcStrategyId.equals(oldStrategyId)
                ? strategyTypedRepository.getTyped(client.getShard(), List.of(validMcStrategyId)).get(0)
                : null;
        var validMcDbStrategy = getActualMcDbStrategy();

        var modelChanges = constructCampaignStrategyModelChanges(validMcStrategyId, validMcDbStrategy);
        applyModelChangesToCampaign(modelChanges);

        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(client.getShard(),
                List.of(oldCampaign.getId())).get(0);
        var actualStrategy = strategyTypedRepository.getTyped(client.getShard(),
                List.of(actualCampaign.getStrategyId())).get(0);

        if (shouldUpdateCampaignAndPrivatePackageIfNeeded(modelChanges)) {
            // столбец 3, столбец 4 строка 1 в таблице кейсов
            // проверяем, что кампания обновилась за счет исходных изменений стратегии в ModelChanges
            checkCampaignStrategyUpdate(actualCampaign, validMcDbStrategy);
            if (!isAttributionModelChanged(modelChanges) && mcDbStrategy == null) {
                // столбец 3 и столец 1 строка 1 в таблице кейсов
                // в случае изменений в стратегии кампании,
                // которые не влияют на пакетную стратегию (например, изменение platform),
                // проверяем, что пакетная стратегия кампании не обновилась
                checkPackageStrategyNotUpdated(actualStrategy);
            } else {
                // столбец 4 строка 1 в таблице кейсов
                // проверяем, что пакетная стратегия кампании обновилась
                checkPackageStrategyUpdated(actualCampaign, actualStrategy);
            }
        } else if (shouldMoveStrategyToPackageWithMcStrategyId(modelChanges, validMcStrategyId)) {
            // столбцы 5 и 6 в таблице кейсов
            // проверяем, что strategyId кампании аналогичен strategyId из modelChanges,
            // что старая стратегия и переданная стратегии не изменились
            checkCampaignMovedToPackageWithMcStrategyId(validMcStrategyId, actualStrategy, strategyWithMcStrategyId);
        } else {
            // столбец 2 строка 2 и столбец 4 строка 2 из таблицы кейсов
            // проверяем, что создается новая стратегия для кампании, в старой нет никаких изменений,
            // новая стратегия аналогична стратегии в кампании
            checkNewStrategyIsCreatedForCampaign(validMcStrategyId, actualStrategy);
        }
    }

    private ModelChanges<CampaignWithPackageStrategy> constructCampaignStrategyModelChanges(Long validMcStrategyId,
                                                                                            DbStrategy validMcDbStrategy) {
        var modelChanges = new ModelChanges<>(oldCampaign.getId(), CampaignWithPackageStrategy.class);
        modelChanges
                .process(validMcStrategyId, CampaignWithPackageStrategy.STRATEGY_ID)
                .process(validMcDbStrategy, CampaignWithPackageStrategy.STRATEGY);

        if (oldCampaign instanceof CampaignWithAttributionModel) {
            modelChanges.castModel(CampaignWithAttributionModel.class)
                    .process(mcAttributionModel, CampaignWithAttributionModel.ATTRIBUTION_MODEL);
        }

        support.onModelChangesValidated(container, List.of(modelChanges));
        return modelChanges;
    }

    private AppliedChanges<CampaignWithPackageStrategy> applyModelChangesToCampaign(ModelChanges<CampaignWithPackageStrategy> modelChanges) {
        var appliedChanges = modelChanges.applyTo(oldCampaign);
        campaignModifyRepository.updateCampaigns(dslContextProvider.ppc(client.getShard()), container,
                List.of(appliedChanges));

        support.updateRelatedEntitiesInTransaction(
                dslContextProvider.ppc(client.getShard()),
                container,
                List.of(appliedChanges));

        return appliedChanges;
    }

    private boolean shouldUpdateCampaignAndPrivatePackageIfNeeded(ModelChanges<CampaignWithPackageStrategy> modelChanges) {
        return !(isPackageStrategiesStageTwoEnabled
                && (isStrategyIdChanged(modelChanges)
                || (isAttributionModelChanged(modelChanges) || mcDbStrategy != null) && isOldStrategyPublic()));
    }

    private boolean shouldMoveStrategyToPackageWithMcStrategyId(ModelChanges<CampaignWithPackageStrategy> modelChanges,
                                                                Long validMcStrategyId) {
        return isPackageStrategiesStageTwoEnabled
                && modelChanges.isPropChanged(CampaignWithPackageStrategy.STRATEGY_ID)
                && validMcStrategyId != null && !validMcStrategyId.equals(oldStrategyId);
    }

    private void checkCampaignStrategyUpdate(CampaignWithPackageStrategy actualCampaign, DbStrategy validMcDbStrategy) {
        assertThat(actualCampaign.getStrategyId())
                .isEqualTo(oldStrategyId);
        validMcDbStrategy.getStrategyData()
                .setLastBidderRestartTime(actualCampaign.getStrategy().getStrategyData().getLastBidderRestartTime());

        assertThat(actualCampaign.getStrategy())
                .usingRecursiveComparison()
                .isEqualTo(validMcDbStrategy);
        if (actualCampaign instanceof CampaignWithAttributionModel) {
            assertThat(((CampaignWithAttributionModel) actualCampaign).getAttributionModel())
                    .isEqualTo(mcAttributionModel);
        }
    }

    private void checkPackageStrategyUpdated(CampaignWithPackageStrategy actualCampaign, BaseStrategy actualStrategy) {
        var expectedStrategy =
                (CommonStrategy) campaignToStrategyConverterService.toStrategyWithIdEqualToCampaignStrategyId(client.getClientId(),
                        now, actualCampaign);
        var actualStrategyLastChange = ((CommonStrategy) actualStrategy).getLastChange();
        assertThat(actualStrategyLastChange).isNotNull();
        expectedStrategy.withLastChange(actualStrategyLastChange);

        fillExpectedStrategyFieldsNotDependingOnToCompany(expectedStrategy);
        assertThat(actualStrategy).isEqualTo(expectedStrategy);
    }

    private void checkPackageStrategyNotUpdated(BaseStrategy actualStrategy) {
        assertThat(actualStrategy).isEqualTo(oldStrategy);
    }

    private void fillExpectedStrategyFieldsNotDependingOnToCompany(CommonStrategy strategy) {
        strategy.withName(oldStrategy.getName());
        strategy.withWalletId(oldStrategy.getWalletId());
        strategy.withStatusArchived(oldStrategy.getStatusArchived());
        strategy.withCids(oldStrategy.getCids());
    }

    private void checkCampaignMovedToPackageWithMcStrategyId(Long validMcStrategyId, BaseStrategy actualStrategy,
                                                             BaseStrategy strategyWithMcStrategyId) {
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(client.getShard(),
                List.of(oldCampaign.getId())).get(0);
        assertThat(actualCampaign.getStrategyId()).isEqualTo(validMcStrategyId);

        checkOldStrategyNotChanged();

        ((StrategyWithCampaignIds) strategyWithMcStrategyId).withCids(List.of(actualCampaign.getId()));
        var beanDiffer = beanDiffer(strategyWithMcStrategyId)
                .useCompareStrategy(allFieldsExcept(newPath("lastChange")));
        assertThat(actualStrategy)
                .is(matchedBy(beanDiffer));
    }

    private void checkNewStrategyIsCreatedForCampaign(Long validMcStrategyId, BaseStrategy actualStrategy) {
        var actualCampaign = (TextCampaign) campaignTypedRepository.getTypedCampaigns(client.getShard(),
                List.of(oldCampaign.getId())).get(0);
        assertThat(actualCampaign.getStrategyId()).isNotEqualTo(oldStrategyId);
        assertThat(actualCampaign.getStrategyId()).isNotEqualTo(validMcStrategyId);

        checkOldStrategyNotChanged();

        var expectedStrategy =
                campaignToStrategyConverterService.toStrategyWithIdEqualToCampaignStrategyId(client.getClientId(),
                        now, actualCampaign);
        var actualStrategyLastChange = ((CommonStrategy) actualStrategy).getLastChange();
        assertThat(actualStrategyLastChange).isNotNull();
        ((CommonStrategy) expectedStrategy).withLastChange(actualStrategyLastChange);

        assertThat(actualStrategy).isEqualTo(expectedStrategy);
    }

    private void checkOldStrategyNotChanged() {
        var currentOldStrategy = strategyTypedRepository.getTyped(client.getShard(),
                List.of(oldStrategyId)).get(0);
        assertThat(((StrategyWithCampaignIds) currentOldStrategy).getCids()).isNull();
        ((StrategyWithCampaignIds) oldStrategy).setCids(null);
        var beanDiffer = beanDiffer(oldStrategy)
                .useCompareStrategy(allFieldsExcept(newPath("lastChange")));
        assertThat(currentOldStrategy)
                .is(matchedBy(beanDiffer));
    }

    private Long getActualMcStrategyId() {
        if (mcStrategyId == null) {
            return mcStrategyId;
        } else if (mcStrategyId == 1) {
            return oldCampaign.getStrategyId();
        } else {
            BaseStrategy newStrategy = createStrategy(autobudgetAvgCpa()
                    .withAttributionModel(StrategyAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE));
            return newStrategy.getId();
        }
    }

    private CampaignWithPackageStrategy createCampaignWithStrategyId() {
        steps.featureSteps().addClientFeature(client.getClientId(), PACKAGE_STRATEGIES_STAGE_TWO,
                isPackageStrategiesStageTwoEnabled);

        var strategyId = createStrategy(autobudgetAvgCpa().withIsPublic(isOldStrategyPublic)).getId();

        var isPublic = isOldStrategyPublic != null && isOldStrategyPublic;
        var campaignToAdd = defaultTextCampaign().withStrategyId(isPublic ? strategyId : null);
        List<? extends BaseCampaign> campaignsToAdd = List.of(campaignToAdd);
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

        var strategyId = result.get(0).getResult();
        return strategyTypedRepository.getIdToModelTyped(client.getShard(), List.of(strategyId)).get(strategyId);
    }

    private DbStrategy getActualMcDbStrategy() {
        return mcDbStrategy == null
                ? (DbStrategy) oldCampaign.getStrategy().copy().withPlatform(null)
                : mcDbStrategy;
    }

    private boolean isOldStrategyPublic() {
        return isOldStrategyPublic != null && isOldStrategyPublic;
    }

    private boolean isStrategyIdChanged(ModelChanges<CampaignWithPackageStrategy> modelChanges) {
        return modelChanges.isPropChanged(CampaignWithPackageStrategy.STRATEGY_ID)
                && modelChanges.getChangedProp(CampaignWithPackageStrategy.STRATEGY_ID) != oldStrategyId;
    }

    private boolean isAttributionModelChanged(ModelChanges<CampaignWithPackageStrategy> modelChanges) {
        return oldCampaign instanceof CampaignWithAttributionModel
                && modelChanges.isPropChanged(CampaignWithAttributionModel.ATTRIBUTION_MODEL)
                && modelChanges.castModel(CampaignWithAttributionModel.class).getChangedProp(CampaignWithAttributionModel.ATTRIBUTION_MODEL) != CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK;
    }

}
