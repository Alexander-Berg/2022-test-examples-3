package ru.yandex.direct.core.entity.adgroup.service.complex.internal;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupAddItem;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer.RequestSource;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.service.complex.internal.ComplexInternalAdGroupServiceTestHelper.internalAdGroupAddItemWithoutTargetings;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIENCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.BEHAVIORS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.GOAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERNAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.allGoalsMustBeEitherFromMetrikaOrCrypta;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.DEVICE_IDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.DEVICE_IDS_2;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.YANDEX_UIDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.YANDEX_UIDS_2;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allValidInternalAdAdditionalTargetings;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allValidInternalAdAdditionalTargetingsIncludingIrrelevant;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.filteringDeviceIdsTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.filteringYandexUidTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.invalidYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.targetingDeviceIdsTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.targetingYandexUidTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRules;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexInternalAdGroupServiceAddTest {
    @Autowired
    private Steps steps;

    @Autowired
    ComplexInternalAdGroupService service;

    @Autowired
    ComplexInternalAdGroupServiceTestHelper complexInternalAdGroupServiceTestHelper;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    private CampaignInfo campaign;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        checkNotNull(complexInternalAdGroupServiceTestHelper);
        campaign = steps.campaignSteps().createActiveInternalDistribCampaign();
        clientInfo = campaign.getClientInfo();
    }

    @Test
    public void emptyInput_Success() {
        MassResult<Long> result = add(emptyList());
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void withoutTargetings_OneValidAdGroup() {
        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
    }

    @Test
    public void withoutTargetings_TwoValidAdGroups() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(true, true));
        checkInternalAdGroupAndTargetingInDb(addItem1);
        checkInternalAdGroupAndTargetingInDb(addItem2);
    }

    @Test
    public void withoutTargetings_validAndInvalidAdGroups() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());
        addItem1.getAdGroup().setName(null); // invalid value
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, true));
        checkInternalAdGroupAndTargetingInDb(addItem2);
    }

    @Test
    public void withoutTargetings_TwoInvalidAdGroups() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());
        addItem1.getAdGroup().setName(null); // invalid value
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());
        addItem2.getAdGroup().setName(null); // invalid value

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, false));
    }

    @Test
    public void withTargeting_OneValidAdGroup() {
        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
    }

    @Test
    public void withTargeting_TwoValidAdGroup() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(validYandexUidTargeting(),
                        validInternalNetworkTargeting()));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(true, true));
        checkInternalAdGroupAndTargetingInDb(addItem1);
        checkInternalAdGroupAndTargetingInDb(addItem2);
    }

    @Test
    public void withTargeting_TwoValidAdGroupWithSameTargeting() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(true, true));
        checkInternalAdGroupAndTargetingInDb(addItem1);
        checkInternalAdGroupAndTargetingInDb(addItem2);
    }

    @Test
    public void withTargeting_TwoValidAdGroupButFirstWithoutAnyTargetings() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId());
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(true, true));
        checkInternalAdGroupAndTargetingInDb(addItem1);
        checkInternalAdGroupAndTargetingInDb(addItem2);
    }

    @Test
    public void withTargeting_InvalidAndValidAdGroups() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));
        addItem1.getAdGroup().setName(null); // invalid value
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, true));
        checkInternalAdGroupAndTargetingInDb(addItem2);
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("name")))));
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButFirstWithInvalidTargeting() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, true));
        checkInternalAdGroupAndTargetingInDb(addItem2);

        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButFirstHasOneInvalidTargeting() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(validInternalNetworkTargeting(), invalidYandexUidTargeting()));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, true));
        checkInternalAdGroupAndTargetingInDb(addItem2);

        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(1), field("value")))));
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButWithInvalidTargetings() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, false));

        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
    }

    @Test
    public void withTargeting_ValidAndInvalidAdGroupsButFirstWithInvalidTargetings() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));

        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));
        addItem2.getAdGroup().setName(null); // invalid value

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, false));

        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroup"), field("name")))));
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButFirstWithDuplicateTargetingsInListValue() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(targetingYandexUidTargetingWithValue(YANDEX_UIDS),
                        filteringYandexUidTargetingWithValue(YANDEX_UIDS)));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(targetingYandexUidTargetingWithValue(YANDEX_UIDS),
                        filteringYandexUidTargetingWithValue(YANDEX_UIDS_2)));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(false, true));
        checkInternalAdGroupAndTargetingInDb(addItem2);

        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value"), index(0)))));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value"), index(1)))));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(1), field("value"), index(2)))));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(1), field("value"), index(3)))));
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButSecondWithDuplicateTargetingsInSetValue() {
        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(targetingDeviceIdsTargetingWithValue(DEVICE_IDS),
                        filteringDeviceIdsTargetingWithValue(DEVICE_IDS_2)));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(asList(targetingDeviceIdsTargetingWithValue(DEVICE_IDS),
                        filteringDeviceIdsTargetingWithValue(DEVICE_IDS)));

        MassResult<Long> result = add(asList(addItem1, addItem2));

        assertThat(result, isSuccessful(true, false));
        checkInternalAdGroupAndTargetingInDb(addItem1);

        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(1), field("value")))));
    }

    @Test
    public void withRetargetingConditionMetrika_OneValidAdGroup() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(GOAL), defaultGoalByType(AUDIENCE))
        ));
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionMetrika_ValidAndInvalid() {
        RetargetingCondition retCondition1 = retargetingCondition(clientInfo.getClientId(), defaultRules(List.of(
                List.of(defaultGoalByType(GOAL), defaultGoalByType(AUDIENCE))
        )));

        Goal invalidGoal = (Goal) defaultGoalByType(GOAL)
                .withTime(null);
        RetargetingCondition retCondition2 = retargetingCondition(clientInfo.getClientId(), List.of(
                defaultRule(List.of(invalidGoal), RuleType.NOT)
        ));

        InternalAdGroupAddItem addItem1 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition1));
        InternalAdGroupAddItem addItem2 = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition2));

        MassResult<Long> result = add(List.of(addItem1, addItem2));

        assertThat(result, isSuccessful(true, false));
    }

    @Test
    public void withRetargetingConditionCrypta_OneValidAdGroup() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(INTERESTS)),
                List.of(defaultGoalByType(SOCIAL_DEMO))
        ));
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionMetrikaAndCrypta_OneValidAdGroup() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(GOAL), defaultGoalByType(AUDIENCE)),
                List.of(defaultGoalByType(INTERESTS), defaultGoalByType(INTERNAL)),
                List.of(defaultGoalByType(SOCIAL_DEMO))
        ));
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionNegativeCryptaAndMetrika_OneValidAdGroup() {
        List<Rule> rules = List.of(
                defaultRule(List.of(defaultGoalByType(GOAL), defaultGoalByType(AUDIENCE))),
                defaultRule(List.of(
                        defaultGoalByType(INTERESTS),
                        defaultGoalByType(INTERNAL),
                        defaultGoalByType(BEHAVIORS)
                ), RuleType.NOT),
                defaultRule(List.of(defaultGoalByType(SOCIAL_DEMO)))
        );
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionNegativeOnlyCrypta_OneValidAdGroup() {
        List<Rule> rules = List.of(
                defaultRule(List.of(defaultGoalByType(INTERESTS)), RuleType.NOT),
                defaultRule(List.of(defaultGoalByType(INTERNAL)), RuleType.NOT)
        );
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionNegativeOnlyMetrika_OneValidAdGroup() {
        List<Rule> rules = List.of(
                defaultRule(List.of(defaultGoalByType(GOAL)), RuleType.NOT),
                defaultRule(List.of(defaultGoalByType(AUDIENCE)), RuleType.NOT)
        );
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupAndTargetingInDb(addItem);
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingConditionMetrikaAndCrypta_InvalidMix() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(GOAL), defaultGoalByType(INTERESTS))
        ));
        RetargetingCondition retCondition = retargetingCondition(clientInfo.getClientId(), rules);

        InternalAdGroupAddItem addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = add(singletonList(addItem));

        assertThat(result, isSuccessful(false));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(validationError(
                        path(field("retargetingConditions"), index(0), field("rules"), index(0), field("goals")),
                        allGoalsMustBeEitherFromMetrikaOrCrypta())));
    }

    @Test
    public void withTargetingsThatShouldBeIgnored_OneValidAdGroup() {
        var addItem = internalAdGroupAddItemWithoutTargetings(campaign.getCampaignId())
                .withAdditionalTargetings(allValidInternalAdAdditionalTargetingsIncludingIrrelevant());

        var result = add(singletonList(addItem));

        assertThat(result, isSuccessful(true));
        checkInternalAdGroupAndTargetingInDbWithCustomTargetings(addItem, allValidInternalAdAdditionalTargetings());
    }

    private MassResult<Long> add(List<InternalAdGroupAddItem> addItems) {
        var operationContainer =
                new InternalAdGroupOperationContainer(
                        Applicability.PARTIAL,
                        clientInfo.getUid(),
                        UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                        true,
                        RequestSource.FRONT
                );
        return service.add(addItems, operationContainer);
    }

    private static RetargetingCondition retargetingCondition(ClientId clientId, List<Rule> rules) {
        return (RetargetingCondition) defaultRetCondition(clientId)
                .withType(ConditionType.interests)
                .withRules(rules);
    }

    private void checkInternalAdGroupAndTargetingInDb(InternalAdGroupAddItem addItem) {
        checkInternalAdGroupAndTargetingInDbWithCustomTargetings(addItem, addItem.getAdditionalTargetings());
    }

    private void checkInternalAdGroupAndTargetingInDbWithCustomTargetings(
            InternalAdGroupAddItem addItem, List<AdGroupAdditionalTargeting> expectedTargetings) {
        complexInternalAdGroupServiceTestHelper.checkInternalAdGroupAndTargetingInDb(
                clientInfo.getShard(),
                addItem.getAdGroup().withStatusBsSynced(StatusBsSynced.NO),
                expectedTargetings);
    }

    void checkInternalAdGroupRetargetingConditionInDb(Long adGroupId, RetargetingCondition expected) {
        var retargetingConditions = retargetingConditionRepository.getRetConditionsByAdGroupIds(
                clientInfo.getShard(), singletonList(adGroupId)).getOrDefault(adGroupId, List.of());

        assertThat(retargetingConditions, contains(
                beanDiffer(expected).useCompareStrategy(
                        allFieldsExcept(
                                newPath("id"),
                                newPath("lastChangeTime"),
                                newPath("available"),
                                newPath("rules/\\d+/goals/\\d+/allowToUse"),
                                newPath("rules/\\d+/goals/\\d+/keyword.*"),
                                newPath("rules/\\d+/goals/\\d+/name")
                        ))));
    }
}
