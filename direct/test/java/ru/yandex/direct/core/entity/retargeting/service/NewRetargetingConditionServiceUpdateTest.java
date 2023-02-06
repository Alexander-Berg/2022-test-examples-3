package ru.yandex.direct.core.entity.retargeting.service;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MobileGoalsService;
import ru.yandex.direct.core.entity.multipliers.repository.MultipliersRepository;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.retargeting.service.helper.RetargetingConditionBannerWithPixelsValidationHelper;
import ru.yandex.direct.core.entity.retargeting.service.helper.RetargetingConditionWithLalSegmentHelper;
import ru.yandex.direct.core.entity.retargeting.service.validation2.AddRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.DeleteRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.ReplaceGoalsInRetargetingValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingConditionCryptaSegmentsProvider;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingConditionsWithAdsValidator;
import ru.yandex.direct.core.entity.retargeting.service.validation2.UpdateRetargetingConditionValidationService2;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createCpmAdGroupWithForeignInventory;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.getPublicRetConditionInfo;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PRIVATE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PUBLIC_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.getFakeCryptaGoalsForTest;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetConditionWithLalSegments;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetConditions;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceUpdateTest extends BaseRetargetingConditionServiceTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFieldsExcept(
            newPath("deleted"), newPath("lastChangeTime"), newPath("available"), newPath(".*", "allowToUse"));

    @Autowired
    private Steps steps;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private RetargetingConditionRepository retConditionRepository;

    @Autowired
    private RetargetingGoalsRepository retGoalsRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private RetargetingConditionsCpmPriceValidationDataFactory cpmPriceValidationDataFactory;

    @Autowired
    private MultipliersRepository multipliersRepository;

    @Autowired
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private GoalUtilsService goalUtilsService;

    @Autowired
    private NetAcl netAcl;

    @Autowired
    private AddRetargetingConditionValidationService2 addValidationService;

    @Autowired
    private DeleteRetargetingConditionValidationService2 deleteValidationService;

    @Autowired
    private ReplaceGoalsInRetargetingValidationService2 replaceGoalsInRetargetingValidationService;

    @Autowired
    private RetargetingConditionBannerWithPixelsValidationHelper cpmBannerHelper;

    @Autowired
    private RetargetingConditionsWithAdsValidator retargetingConditionsWithAdsValidator;

    @Autowired
    private FindOrCreateRetargetingConditionService findOrCreateRetargetingConditionService;

    @Autowired
    private RetargetingConditionWithLalSegmentHelper retargetingConditionWithLalSegmentHelper;

    @Mock
    private RetargetingConditionCryptaSegmentsProvider retargetingConditionCryptaSegmentsProvider;

    private RetargetingCondition privateRetargetingCondition;
    private RetargetingCondition publicRetargetingCondition;
    private Long retargetingConditionId;
    private Long retargetingConditionId2;
    private AdGroupInfo cpmAdGroupInfo;
    private List<DealInfo> dealInfos;
    private ClientInfo cpmClientInfo;
    private ClientInfo agencyClientInfo;

    private List<RetargetingCondition> conditions;
    private List<Long> conditionIds;

    @Before
    public void before() {
        super.before();
        MockitoAnnotations.initMocks(this);
        retargetingConditionCryptaSegmentsProvider = mock(RetargetingConditionCryptaSegmentsProvider.class);
        FeatureService featureService = mock(FeatureService.class);

        var enabledFeatures = List.of(
                FeatureName.CPM_YNDX_FRONTPAGE_PROFILE,
                FeatureName.TGO_SOCIAL_DEMO_IN_USER_PROFILE,
                FeatureName.TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE,
                FeatureName.TGO_ALL_INTERESTS_IN_USER_PROFILE,
                FeatureName.TGO_METRIKA_AND_AUDIENCE_IN_USER_PROFILE
        );
        enabledFeatures.forEach(featureName ->
                when(featureService.isEnabledForClientId(any(ClientId.class), eq(featureName)))
                        .thenReturn(true)
        );
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                .thenReturn(listToSet(mapList(enabledFeatures,FeatureName::getName)));
        var targetingCategoriesCache = mock(TargetingCategoriesCache.class);
        when(targetingCategoriesCache.getTargetingCategories()).thenReturn(emptyList());

        when(retargetingConditionCryptaSegmentsProvider.getAllowedCryptaSegments(anyBoolean(), anyBoolean(), anyList()))
                .thenReturn(getFakeCryptaGoalsForTest());

        MobileGoalsService mobileGoalsService = mock(MobileGoalsService.class);
        when(mobileGoalsService.getAllAvailableInAppMobileGoals(any())).thenReturn(List.of());

        retargetingConditionOperationFactory = new RetargetingConditionOperationFactory(retConditionRepository,
                lalSegmentRepository,
                retGoalsRepository,
                adGroupRepository,
                multipliersRepository,
                bsResyncQueueRepository,
                shardHelper,
                goalUtilsService,
                addValidationService,
                new UpdateRetargetingConditionValidationService2(retConditionRepository,
                        cpmBannerHelper, retargetingConditionCryptaSegmentsProvider, lalSegmentRepository, goalUtilsService,
                        retargetingConditionsWithAdsValidator, featureService, adGroupRepository, campaignRepository,
                        targetingCategoriesCache,
                        cpmPriceValidationDataFactory, rbacService, mobileGoalsService, netAcl),
                deleteValidationService,
                replaceGoalsInRetargetingValidationService,
                featureService,
                findOrCreateRetargetingConditionService,
                retargetingConditionWithLalSegmentHelper);
        // создаем условия в базе
        conditions = bigRetConditions(clientId, 2);
        metrikaHelperStub.addGoalsFromConditions(uid, conditions);
        conditionIds = retConditionRepository.add(shard, conditions);

        agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        cpmClientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);

        privateRetargetingCondition = new RetargetingCondition();
        privateRetargetingCondition
                .withClientId(cpmClientInfo.getClientId().asLong())
                .withRules(singletonList(createRuleFromSocialDemoGoalIds(singletonList(PRIVATE_GOAL_ID))))
                .withType(ConditionType.interests);
        publicRetargetingCondition = new RetargetingCondition();
        publicRetargetingCondition
                .withClientId(cpmClientInfo.getClientId().asLong())
                .withRules(singletonList(createRuleFromSocialDemoGoalIds(singletonList(PUBLIC_GOAL_ID))))
                .withType(ConditionType.interests);

        metrikaHelperStub
                .addGoalIds(cpmClientInfo.getUid(), mapList(privateRetargetingCondition.collectGoals(), GoalBase::getId));
        metrikaHelperStub
                .addGoalIds(cpmClientInfo.getUid(), mapList(publicRetargetingCondition.collectGoals(), GoalBase::getId));

        dealInfos = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);

        cpmAdGroupInfo = createCpmAdGroupWithForeignInventory(dealInfos, steps, cpmClientInfo);
        retargetingConditionId = steps.retargetingSteps()
                .createRetargeting(null, cpmAdGroupInfo, getPublicRetConditionInfo(steps, cpmClientInfo))
                .getRetConditionId();
        retargetingConditionId2 = steps.retargetingSteps()
                .createRetargeting(null, cpmAdGroupInfo, getPublicRetConditionInfo(steps, cpmClientInfo))
                .getRetConditionId();
    }

    private Rule createRuleFromSocialDemoGoalIds(List<Long> socialDemoGoalIds) {
        List<Goal> goals = createGoalsFromGoalIds(socialDemoGoalIds);
        Rule rule = new Rule();
        rule.withGoals(goals).withType(RuleType.OR);
        return rule;
    }

    private List<Goal> createGoalsFromGoalIds(List<Long> socialDemoGoalIds) {
        return StreamEx.of(socialDemoGoalIds).map(t -> {
            Goal someGoal = new Goal();
            someGoal.withId(t).withType(GoalType.SOCIAL_DEMO);
            return someGoal;
        }).toList();
    }

    @After
    public void after() {
        steps.dealSteps().unlinkDeals(shard, mapList(dealInfos, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfos, DealInfo::getDeal), cpmAdGroupInfo.getClientInfo());
        placementsRepository.deletePlacementsBy(ImmutableList.of(BIG_PLACEMENT_PAGE_ID + 1));
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при обновлении условий ретаргетинга,
     * от которых права на пиксели данных баннеров  зависят
     */
    @Test
    public void updateRetargetingConditions_OneItemIsValid_ResultIsFullySuccessfull() {
        // готовим изменения
        List<ModelChanges<RetargetingCondition>> modelChangesList =
                prepareUpdate(conditions.get(0), conditionIds.get(0));

        // обновляем условия и проверяем результат
        MassResult<Long> result = retargetingConditionOperationFactory
                .updateRetargetingConditions(clientId, modelChangesList);
        assertThat(result, isSuccessful(true));
    }

    /**
     * Тест проверяет, что корректно валидируются баннеры с пикселями при обновлении условий ретаргетинга,
     * от которых права на пиксели данных баннеров  зависят
     */
    @Test
    public void updateRetargetingConditions_BothItemsIsValid_ResultIsFullySuccessfull() {
        // готовим изменения
        List<ModelChanges<RetargetingCondition>> modelChangesList = prepareUpdate(conditions, conditionIds);

        // обновляем условия и проверяем результат
        MassResult<Long> result = retargetingConditionOperationFactory
                .updateRetargetingConditions(clientId, modelChangesList);
        assertThat(result, isSuccessful(true, true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateRetargetingConditions_BothItemsIsValid_UpdatesBothItems() {
        // готовим изменения
        List<ModelChanges<RetargetingCondition>> modelChangesList = prepareUpdate(conditions, conditionIds);

        // обновляем условия
        MassResult<Long> result = retargetingConditionOperationFactory
                .updateRetargetingConditions(clientId, modelChangesList);
        assumeThat(result, isSuccessful(true, true));

        // получаем обновленные условия и проверяем
        List<RetargetingCondition> actualUpdatedConditions =
                retConditionRepository.getFromRetargetingConditionsTable(shard, clientId, conditionIds);

        assertThat("данные в бд после обновления соответствуют ожидаемым", actualUpdatedConditions, contains(
                beanDiffer(conditions.get(0)).useCompareStrategy(COMPARE_STRATEGY),
                beanDiffer(conditions.get(1)).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void updateRetargetingConditions_OneOfItemsIsValid_ItemResultIsInvalid() {
        // готовим изменения и делаем первый элемент с ошибкой
        ModelChanges<RetargetingCondition> modelChanges0 = retargetingModelChanges(conditionIds.get(0));
        modelChanges0.process(new ArrayList<>(), RetargetingCondition.RULES);

        ModelChanges<RetargetingCondition> modelChanges1 = changeCondition(
                conditions.get(1), conditionIds.get(1),
                "cond1 updated name " + RandomStringUtils.randomNumeric(4),
                "desc123", null);

        // обновляем условия и проверяем результат
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(clientId, asList(modelChanges0, modelChanges1));
        assertThat(result, isSuccessful(false, true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateRetargetingConditions_OneOfItemsIsValid_UpdatesOnlyValidItem() {
        // готовим изменения и делаем первый элемент с ошибкой.
        // готовим невалидные изменения, не изменяя условие, с которым будем сравнивать
        ModelChanges<RetargetingCondition> modelChanges0 = retargetingModelChanges(conditionIds.get(0));
        modelChanges0.process(new ArrayList<>(), RetargetingCondition.RULES);

        // а эти изменения готовим, изменяя само условие, с которым будем сравнивать результат в базе
        ModelChanges<RetargetingCondition> modelChanges1 = changeCondition(
                conditions.get(1), conditionIds.get(1),
                "cond1 updated name " + RandomStringUtils.randomNumeric(4),
                "desc123", null);

        // обновляем условия
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(clientId, asList(modelChanges0, modelChanges1));
        assumeThat(result, isSuccessful(false, true));

        // получаем обновленные условия и проверяем
        List<RetargetingCondition> actualUpdatedConditions =
                retConditionRepository.getFromRetargetingConditionsTable(shard, clientId, conditionIds);

        assertThat("в бд изменился элемент после обновления с валидными данными и не изменился с невалидными",
                actualUpdatedConditions, contains(
                        beanDiffer(conditions.get(0)).useCompareStrategy(COMPARE_STRATEGY),
                        beanDiffer(conditions.get(1)).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void updateRetargetingConditions_BothItemsIsInvalid_OperationResultIsBroken() {
        // готовим изменения и делаем оба элемента с ошибкой
        ModelChanges<RetargetingCondition> modelChanges0 = retargetingModelChanges(conditionIds.get(0));
        modelChanges0.process("", RetargetingCondition.NAME);

        ModelChanges<RetargetingCondition> modelChanges1 = retargetingModelChanges(conditionIds.get(1));
        modelChanges1.process(new ArrayList<>(), RetargetingCondition.RULES);

        // обновляем условия и проверяем результат
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(clientId, asList(modelChanges0, modelChanges1));
        assertThat(result, isSuccessful(false, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateRetargetingConditions_BothItemsIsInvalid_ConditionsIsNotChanged() {
        // готовим изменения и делаем оба элемента с ошибкой
        ModelChanges<RetargetingCondition> modelChanges0 = retargetingModelChanges(conditionIds.get(0));
        modelChanges0.process("", RetargetingCondition.NAME);

        ModelChanges<RetargetingCondition> modelChanges1 = retargetingModelChanges(conditionIds.get(1));
        modelChanges1.process(new ArrayList<>(), RetargetingCondition.RULES);

        // обновляем условия
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(clientId, asList(modelChanges0, modelChanges1));
        assumeThat(result, isSuccessful(false, false));

        // получаем обновленные условия и проверяем
        List<RetargetingCondition> actualUpdatedConditions =
                retConditionRepository.getFromRetargetingConditionsTable(shard, clientId, conditionIds);

        assertThat("данные в бд не изменились после попытки обновления с ошибочными данными",
                actualUpdatedConditions, contains(
                        beanDiffer(conditions.get(0)).useCompareStrategy(COMPARE_STRATEGY),
                        beanDiffer(conditions.get(1)).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_NoErrors() {
        // готовим изменения
        List<ModelChanges<RetargetingCondition>> modelChangesList = singletonList(
                changeCondition(publicRetargetingCondition, retargetingConditionId, null,
                        null, null));

        // обновляем условия и проверяем результат
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(cpmClientInfo.getClientId(), modelChangesList);
        Assert.assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Retargetings_BelongingToAdgroupWithBannerPixels_Errors() {
        // готовим изменения
        List<ModelChanges<RetargetingCondition>> modelChangesList = singletonList(
                changeCondition(privateRetargetingCondition, retargetingConditionId2,
                        privateRetargetingCondition.getName(),
                        privateRetargetingCondition.getDescription(), privateRetargetingCondition.getRules()));

        // обновляем условия и проверяем результат
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .updateRetargetingConditions(cpmClientInfo.getClientId(), modelChangesList);

        Assert.assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(path(index(0)),
                        noRightsToPixel(dcmPixelUrl(), emptyList(), CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void updateRetargetingConditions_EmptyList_ResultIsSuccessful() {
        MassResult<Long> result =
                retargetingConditionOperationFactory.updateRetargetingConditions(clientId, emptyList());
        assumeThat("результат операции положителен", result.isSuccessful(), is(true));
    }

    @Test
    public void updateRetargetingConditions_WithLalSegments() {
        RetargetingCondition retargetingCondition = bigRetConditionWithLalSegments(null);

        steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);

        retargetingCondition.getRules().get(0).getGoals().get(1).setUnionWithId(null);
        retargetingCondition.getRules().get(2).setGoals(List.of(
                retargetingCondition.getRules().get(2).getGoals().get(1)));

        List<ModelChanges<RetargetingCondition>> modelChangesList = singletonList(
                changeCondition(retargetingCondition, retargetingCondition.getId(), null, null,
                        retargetingCondition.getRules()));

        MassResult<Long> result = retargetingConditionOperationFactory.updateRetargetingConditions(
                clientId, modelChangesList);
        assumeThat(result, isSuccessful(true));

        RetargetingCondition actualUpdatedCondition = retConditionRepository.getFromRetargetingConditionsTable(
                shard, clientId, List.of(retargetingCondition.getId())).get(0);

        assertEquals("в первом правиле union_with_id должен быть выставлен",
                actualUpdatedCondition.getRules().get(0).getGoals().get(0).getId(),
                actualUpdatedCondition.getRules().get(0).getGoals().get(1).getUnionWithId());
        assertNull("во втором правиле union_with_id не должен быть выставлен",
                actualUpdatedCondition.getRules().get(1).getGoals().get(1).getUnionWithId());
        assertNull("в третьем правиле union_with_id не должен быть выставлен",
                actualUpdatedCondition.getRules().get(2).getGoals().get(0).getUnionWithId());
    }

    private ModelChanges<RetargetingCondition> changeCondition(
            RetargetingCondition condition, long id, String name,
            String description, List<Rule> rules) {
        ModelChanges<RetargetingCondition> modelChanges = retargetingModelChanges(id);

        condition.setId(id);
        if (name != null) {
            condition.setName(name);
            modelChanges.process(condition.getName(), RetargetingCondition.NAME);
        }
        if (description != null) {
            condition.setDescription(description);
            modelChanges.process(condition.getDescription(), RetargetingCondition.DESCRIPTION);
        }
        if (rules != null) {
            condition.setRules(rules);
            modelChanges.process(condition.getRules(), RetargetingCondition.RULES);
        }
        return modelChanges;
    }

    private List<ModelChanges<RetargetingCondition>> prepareUpdate(
            RetargetingCondition condition, Long conditionId) {
        List<Rule> newRules = defaultRetCondition(clientId).getRules();
        ModelChanges<RetargetingCondition> modelChanges = changeCondition(
                condition, conditionId,
                "cond0 updated name " + RandomStringUtils.randomNumeric(4),
                null, newRules);

        metrikaHelperStub.addGoalsFromRules(uid, newRules);

        return singletonList(modelChanges);
    }

    private List<ModelChanges<RetargetingCondition>> prepareUpdate(
            List<RetargetingCondition> conditions, List<Long> conditionIds) {
        ModelChanges<RetargetingCondition> modelChanges0 = changeCondition(
                conditions.get(0), conditionIds.get(0),
                "cond0 updated name " + RandomStringUtils.randomNumeric(4),
                null, null);

        List<Rule> newRules = defaultRetCondition(clientId).getRules();
        ModelChanges<RetargetingCondition> modelChanges1 = changeCondition(
                conditions.get(1), conditionIds.get(1),
                "cond1 updated name " + RandomStringUtils.randomNumeric(4),
                "desc123", newRules);

        metrikaHelperStub.addGoalsFromRules(uid, newRules);

        return asList(modelChanges0, modelChanges1);
    }

    private static ModelChanges<RetargetingCondition> retargetingModelChanges(long id) {
        return new ModelChanges<>(id, RetargetingCondition.class);
    }
}
