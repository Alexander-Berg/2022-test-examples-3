package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.metrika.service.MobileGoalsService;
import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingConditionValidationData;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.TargetingCategoriesCache;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition.DEFAULT_TYPE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddRetargetingConditionValidationServiceOperationalErrorTest {

    private static final int SHARD = 3;
    private static final ClientId CLIENT_ID = ClientId.fromLong(123L);
    private static final long UID = 345L;
    private static final long GOAL_ID = 778L;
    private static final long NOT_EXISTING_FOR_CLIENT_GOAL_ID = 2098720L;

    private AddRetargetingConditionValidationService2 addValidationService;
    private RetargetingConditionRepository retConditionRepository;
    private RetargetingConditionCryptaSegmentsProvider retargetingConditionCryptaSegmentsProvider;
    private ShardHelper shardHelper;
    private GoalUtilsService goalUtilsService;
    private RbacService rbacService;
    private FeatureService featureService;
    private MobileGoalsService mobileGoalsService;

    private static RetargetingCondition conditionWithGoalId(Long goalId) {
        Goal goal = new Goal();
        goal.withId(goalId)
                .withTime(2);
        Rule rule = new Rule();
        rule.withType(RuleType.ALL)
                .withGoals(singletonList(goal));
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withName("a")
                .withType(DEFAULT_TYPE)
                .withClientId(CLIENT_ID.asLong())
                .withRules(singletonList(rule));
        return retargetingCondition;
    }

    private static RetargetingCondition condition1() {
        return conditionWithGoalId(GOAL_ID);
    }

    @Before
    public void before() {
        retConditionRepository = mock(RetargetingConditionRepository.class);
        when(retConditionRepository.getValidationData(any(), any())).thenReturn(emptyList());

        retargetingConditionCryptaSegmentsProvider = mock(RetargetingConditionCryptaSegmentsProvider.class);
        when(retargetingConditionCryptaSegmentsProvider.getAllowedCryptaSegments(anyBoolean(), anyBoolean(), anyList()))
                .thenReturn(emptyMap());

        var lalSegmentRepository = mock(LalSegmentRepository.class);
        when(lalSegmentRepository.getLalSegmentsByParentIds(anyList())).thenReturn(List.of());

        var targetingCategoriesCache = mock(TargetingCategoriesCache.class);
        when(targetingCategoriesCache.getTargetingCategories()).thenReturn(emptyList());

        shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(eq(CLIENT_ID))).thenReturn(SHARD);


        rbacService = mock(RbacService.class);
        when(rbacService.getClientRepresentativesUids(CLIENT_ID)).thenReturn(singletonList(UID));
        when(rbacService.isInternalAdProduct(any())).thenReturn(false);

        goalUtilsService = spy(new GoalUtilsService(mock(MetrikaHelper.class), rbacService));
        Mockito.doReturn(singleton(GOAL_ID)).when(goalUtilsService).getAvailableMetrikaGoalIds(any(), anyCollection());

        featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientId(CLIENT_ID, FeatureName.SKIP_GOAL_EXISTENCE_FOR_AGENCY)).thenReturn(false);

        mobileGoalsService = mock(MobileGoalsService.class);
        when(mobileGoalsService.getAllAvailableInAppMobileGoals(any())).thenReturn(List.of());

        var netAcl = mock(NetAcl.class);
        addValidationService = new AddRetargetingConditionValidationService2(
                retConditionRepository, retargetingConditionCryptaSegmentsProvider, lalSegmentRepository,
                shardHelper,
                goalUtilsService,
                targetingCategoriesCache, featureService, rbacService, mobileGoalsService, netAcl);
    }

    @Test
    public void getValidationDataWhenValidateCalled() {
        ValidationResult<List<RetargetingCondition>, Defect>
                actual = addValidationService.validate(emptyList(), CLIENT_ID);
        verify(shardHelper).getShardByClientIdStrictly(eq(CLIENT_ID));
        verify(retConditionRepository).getValidationData(eq(SHARD), eq(CLIENT_ID));
        verify(goalUtilsService).getAvailableMetrikaGoalIds(any(), anyCollection());
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<RetargetingCondition>, Defect> actual =
                addValidationService.validate(singletonList(condition1()), CLIENT_ID);
        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void operationalErrorDefectTypeMaxElementsExceededWhenClientHasTooManyRetConds() {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(CLIENT_ID.asLong());
        List<RetargetingCondition> retList = singletonList(retargetingCondition);
        when(retConditionRepository.getValidationData(any(), any())).thenReturn(
                Collections.nCopies(Constants.MAX_RET_CONDITIONS_PER_CLIENT + 1,
                        new RetargetingConditionValidationData(1, "", "[]")));

        ValidationResult<List<RetargetingCondition>, Defect> actual =
                addValidationService.validate(retList, CLIENT_ID);
        assertThat(actual.flattenErrors(),
                contains(validationError(path(),
                        CollectionDefects.maxElementsExceeded(Constants.MAX_RET_CONDITIONS_PER_CLIENT))));
    }


    @Test
    public void goalExistenceCheckSkipTest() {

        var condition = conditionWithGoalId(NOT_EXISTING_FOR_CLIENT_GOAL_ID);

        ValidationResult<List<RetargetingCondition>, Defect> actual =
                addValidationService.validate(singletonList(condition), CLIENT_ID);
        assertThat(
                actual.flattenErrors(),
                hasItem(validationError(DefectIds.OBJECT_NOT_FOUND))
        );

        // enabling feature
        when(featureService.isEnabledForClientId(CLIENT_ID, FeatureName.SKIP_GOAL_EXISTENCE_FOR_AGENCY)).thenReturn(true);

        ValidationResult<List<RetargetingCondition>, Defect> noError =
                addValidationService.validate(singletonList(condition), CLIENT_ID);
        assertThat(
                noError.flattenErrors(),
                not(hasItem(validationError(DefectIds.OBJECT_NOT_FOUND)))
        );

    }

}
