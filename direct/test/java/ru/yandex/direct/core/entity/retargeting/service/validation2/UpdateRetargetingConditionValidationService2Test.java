package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.metrika.service.MobileGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.retargeting.service.helper.RetargetingConditionBannerWithPixelsValidationHelper;
import ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice.RetargetingConditionsCpmPriceValidationDataFactory;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition.DEFAULT_TYPE;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.MockServices.emptyRetargetingConditionsCpmPriceValidationDataFactory;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class UpdateRetargetingConditionValidationService2Test {
    private static final ClientId CLIENT_ID = ClientId.fromLong(11L);
    private static final int SHARD = 4;
    private static final long GOAL_ID = 778L;
    private static final long VALID_ID = 1L;

    private UpdateRetargetingConditionValidationService2 validationUnderTest;
    private RetargetingCondition validRetCond;

    @Before
    public void before() {
        RetargetingConditionRepository retConditionRepository = mock(RetargetingConditionRepository.class);
        when(retConditionRepository.getExistingIds(eq(SHARD), eq(CLIENT_ID), any()))
                .thenReturn(singletonList(VALID_ID));

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(eq(CLIENT_ID))).thenReturn(SHARD);

        GoalUtilsService goalUtilsService = mock(GoalUtilsService.class);
        when(goalUtilsService.getAvailableMetrikaGoalIds(any(), anyCollection())).thenReturn(
                singleton(GOAL_ID));

        RetargetingConditionCryptaSegmentsProvider retargetingConditionCryptaSegmentsProvider =
                mock(RetargetingConditionCryptaSegmentsProvider.class);
        when(retargetingConditionCryptaSegmentsProvider.getAllowedCryptaSegments(anyBoolean(), anyBoolean(), anyList()))
                .thenReturn(emptyMap());

        LalSegmentRepository lalSegmentRepository = mock(LalSegmentRepository.class);
        when(lalSegmentRepository.getLalSegmentsByParentIds(anyList())).thenReturn(List.of());

        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);

        CampaignRepository campaignRepository = mock(CampaignRepository.class);

        RetargetingConditionsWithAdsValidator
                retargetingConditionsWithAdsValidator = mock(RetargetingConditionsWithAdsValidator.class);
        when(retargetingConditionsWithAdsValidator.validateInterconnectionsWithAds(
                anyInt(), any(ClientId.class), any(List.class), any(Map.class), anyBoolean()))
                .thenAnswer((InvocationOnMock invocation) -> ListValidationBuilder.of(invocation.getArgument(2))
                        .getResult());

        FeatureService featureService = mock(FeatureService.class);

        RetargetingConditionBannerWithPixelsValidationHelper retargetingConditionBannerWithPixelsValidationHelper =
                mock(RetargetingConditionBannerWithPixelsValidationHelper.class);

        RetargetingConditionsCpmPriceValidationDataFactory cpmPriceValidationDataFactory =
                emptyRetargetingConditionsCpmPriceValidationDataFactory();

        RbacService rbacService = mock(RbacService.class);
        when(rbacService.isInternalAdProduct(any())).thenReturn(false);

        MobileGoalsService mobileGoalsService = mock(MobileGoalsService.class);
        when(mobileGoalsService.getAllAvailableInAppMobileGoals(any())).thenReturn(List.of());

        TargetingCategoriesCache targetingCategoriesCache = mock(TargetingCategoriesCache.class);

        var netAcl = mock(NetAcl.class);
        validationUnderTest = new UpdateRetargetingConditionValidationService2(retConditionRepository,
                retargetingConditionBannerWithPixelsValidationHelper,
                retargetingConditionCryptaSegmentsProvider,
                lalSegmentRepository,
                goalUtilsService,
                retargetingConditionsWithAdsValidator, featureService, adGroupRepository, campaignRepository,
                targetingCategoriesCache,
                cpmPriceValidationDataFactory, rbacService, mobileGoalsService, netAcl);

        Goal goal = new Goal();
        goal.withId(GOAL_ID)
                .withTime(1);

        Rule rule = new Rule();
        rule.withType(RuleType.ALL)
                .withGoals(singletonList(goal));

        validRetCond = new RetargetingCondition();
        validRetCond
                .withId(VALID_ID)
                .withType(DEFAULT_TYPE)
                .withClientId(10L)
                .withName("xxx")
                .withRules(singletonList(rule));
    }

    @Test
    public void preValidateNoAnyErrorsWhenPreValidateValid() {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actual =
                validationUnderTest
                        .preValidate(singletonList(retargetingConditionModelChanges(VALID_ID)), CLIENT_ID, SHARD);
        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void preValidateInvalidValueWhenIdIsNull() {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actual =
                validationUnderTest
                        .preValidate(singletonList(retargetingConditionModelChanges(null)), CLIENT_ID, SHARD);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field("id")), CommonDefects.notNull())));
    }

    @Test
    public void preValidateInvalidValueWhenIdIsZero() {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actual =
                validationUnderTest.preValidate(singletonList(retargetingConditionModelChanges(0L)), CLIENT_ID, SHARD);
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field("id")), CommonDefects.validId())));
    }

    @Test
    public void preValidateNotFoundWhenIdIsUnknown() {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actual =
                validationUnderTest.preValidate(
                        singletonList(retargetingConditionModelChanges(VALID_ID + 1)), CLIENT_ID, SHARD);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(field("id")), CommonDefects.objectNotFound())));
    }

    @Test
    public void preValidateDuplicatedRetargetingConditionIdWhenIdIsRepeated() {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actual =
                validationUnderTest.preValidate(
                        Arrays.asList(
                                retargetingConditionModelChanges(VALID_ID),
                                retargetingConditionModelChanges(VALID_ID)),
                        CLIENT_ID, SHARD);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(
                validationError(path(), RetargetingDefects.duplicatedRetargetingConditionId())));
        assertThat(actual.getSubResults().get(index(1)).flattenErrors(), contains(
                validationError(path(), RetargetingDefects.duplicatedRetargetingConditionId())));
    }

    @Test
    public void validateValid() {
        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(VALID_ID);
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(modelChanges));
        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(validRetCond);

        ValidationResult<List<RetargetingCondition>, Defect> actual = validationUnderTest.validate(
                preMassValidation, singletonList(validRetCond),
                singletonList(appliedChanges), CLIENT_ID, SHARD);
        assertThat(ValidationResult.getValidItems(actual), hasSize(1));
        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void validateScopeIsNotChanged() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID)
                .withTime(1);
        Rule changedRule = new Rule();
        changedRule
                .withType(RuleType.NOT)
                .withGoals(singletonList(
                        goal
                ));
        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(VALID_ID);
        modelChanges.process(singletonList(changedRule), RetargetingCondition.RULES);
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(modelChanges));
        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(validRetCond);

        ValidationResult<List<RetargetingCondition>, Defect> actual = validationUnderTest.validate(
                preMassValidation, singletonList(validRetCond),
                singletonList(appliedChanges), CLIENT_ID, SHARD);

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(), RetargetingDefects.cannotChangeRetargetingScope())));
    }

    @Test
    public void validateMergePreValidation() {
        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(VALID_ID);
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(modelChanges));

        ValidationResult<ModelChanges<RetargetingCondition>, Defect> childResult =
                preMassValidation.getOrCreateSubValidationResult(index(0), modelChanges);

        ValidationResult<Long, Defect> grandchildResult =
                childResult.getOrCreateSubValidationResult(field("id"), null);

        grandchildResult.addError(CommonDefects.invalidValue());

        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(validRetCond);

        ValidationResult<List<RetargetingCondition>, Defect> actual = validationUnderTest.validate(
                preMassValidation, emptyList(),
                singletonList(appliedChanges), CLIENT_ID, SHARD);
        assertThat(ValidationResult.getValidItems(actual), hasSize(0));
        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("id")), CommonDefects.invalidValue())));
    }

    @Test
    public void validInterestsConditionWithNoRules() {
        validRetCond.setRules(emptyList());

        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(VALID_ID);
        modelChanges.process(emptyList(), RetargetingCondition.RULES);
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(modelChanges));
        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(validRetCond);

        ValidationResult<List<RetargetingCondition>, Defect> actual = validationUnderTest.validate(
                preMassValidation, singletonList(validRetCond),
                singletonList(appliedChanges), CLIENT_ID, SHARD);
        assertThat(actual.getErrors(), hasSize(0));
    }

    private static ModelChanges<RetargetingCondition> retargetingConditionModelChanges(Long id) {
        return new ModelChanges<>(id, RetargetingCondition.class);
    }
}
