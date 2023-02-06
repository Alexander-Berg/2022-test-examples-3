package ru.yandex.direct.core.entity.adgroup.service.complex.internal;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer.RequestSource;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupUpdateItem;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.service.complex.internal.ComplexInternalAdGroupServiceTestHelper.internalAdGroupUpdateItemWithoutTargetings;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.DEVICE_IDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.DEVICE_IDS_2;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.YANDEX_UIDS;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.YANDEX_UIDS_2;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.allIrrelevantValidInternalAdAdditionalTargetings;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.filteringDeviceIdsTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.filteringYandexUidTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.invalidYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.irrelevantValidTimeTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.targetingDeviceIdsTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.targetingYandexUidTargetingWithValue;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validTimeTargeting;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validYandexUidTargeting;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRules;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.concat;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexInternalAdGroupServiceUpdateTest {
    @Autowired
    private Steps steps;

    @Autowired
    ComplexInternalAdGroupService serviceUnderTest;

    @Autowired
    AdGroupAdditionalTargetingRepository additionalTargetingRepository;

    @Autowired
    ComplexInternalAdGroupServiceTestHelper complexInternalAdGroupServiceTestHelper;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    private CampaignInfo campaign;
    private ClientInfo clientInfo;
    private AdGroup existentAdGroup1;

    private AdGroup existentAdGroup2;
    private List<AdGroupAdditionalTargeting> existentTargetingsForAdGroup2;

    private AdGroup existentAdGroupWithTimeTargeting;
    private List<AdGroupAdditionalTargeting> existentTargetingsForAdGroupWithTimeTargeting;

    @Before
    public void before() {
        checkNotNull(complexInternalAdGroupServiceTestHelper);

        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        campaign = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        existentAdGroup1 = adGroupInfo1.getAdGroup();

        existentAdGroup2 = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo).getAdGroup();
        existentAdGroupWithTimeTargeting = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo).getAdGroup();
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(),
                List.of(existentAdGroup1.getId(), existentAdGroup2.getId(), existentAdGroupWithTimeTargeting.getId()),
                StatusBsSynced.YES);

        existentTargetingsForAdGroup2 = asList(
                validInternalNetworkTargeting().withAdGroupId(existentAdGroup2.getId()),
                validYandexUidTargeting().withAdGroupId(existentAdGroup2.getId()));
        additionalTargetingRepository.add(campaignInfo.getShard(), campaignInfo.getClientId(),
                existentTargetingsForAdGroup2);

        existentTargetingsForAdGroupWithTimeTargeting = List.of(
                validTimeTargeting().withAdGroupId(existentAdGroupWithTimeTargeting.getId()));
        additionalTargetingRepository.add(campaignInfo.getShard(), campaignInfo.getClientId(),
                existentTargetingsForAdGroupWithTimeTargeting);
    }

    @Test
    public void emptyInput_Success() {
        MassResult<Long> result = update(emptyList());
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void withTargetings_OneValidAdGroups() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        MassResult<Long> result = update(singletonList(updateItem));

        assertThat(result, isFullySuccessful());
        checkComplexInternalAdGroupUpdate(updateItem);
    }

    @Test
    public void withTargetings_TwoValidAdGroups() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));
        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isFullySuccessful());
        checkComplexInternalAdGroupUpdate(updateItem1);
        checkComplexInternalAdGroupUpdate(updateItem2);
    }

    @Test
    public void withTargetings_InvalidAndValidAdGroups() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));
        updateItem1.getAdGroupChanges().process(null, InternalAdGroup.NAME); // invalid value

        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(false, true));
        assertThat(result.get(0).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroupChanges"), field("name")))));

        checkExistentAdGroup(existentAdGroup1, emptyList());
        checkComplexInternalAdGroupUpdate(updateItem2);
    }

    @Test
    public void withTargetings_validAndInvalidAdGroups() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(singletonList(validYandexUidTargeting()));
        updateItem2.getAdGroupChanges().process(null, InternalAdGroup.NAME); // invalid value

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(true, false));
        assertThat(
                result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("adGroupChanges"), field("name")))));

        checkComplexInternalAdGroupUpdate(updateItem1);
        checkExistentAdGroup(existentAdGroup2, existentTargetingsForAdGroup2);
    }

    @Test
    public void withTargetings_TwoValidAdGroupsButSecondWithInvalidTargeting() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()));

        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(true, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));

        checkComplexInternalAdGroupUpdate(updateItem1);
        checkExistentAdGroup(existentAdGroup2, existentTargetingsForAdGroup2);
    }

    @Test
    public void withTargetings_ValidWithoutTargetingAndInvalidWithTargeting() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId());
        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(singletonList(invalidYandexUidTargeting()));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(true, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(validationError(
                        path(field("additionalTargetings"), index(0), field("value")), invalidValue())));

        checkComplexInternalAdGroupUpdate(updateItem1);
        checkExistentAdGroup(existentAdGroup2, existentTargetingsForAdGroup2);
    }

    @Test
    public void withoutTargetings_OneValidAdGroups() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId());

        MassResult<Long> result = update(singletonList(updateItem));

        assertThat(result, isFullySuccessful());
        checkComplexInternalAdGroupUpdate(updateItem);
    }


    @Test
    public void withTargeting_TwoValidAdGroupsButFirstWithDuplicateTargetingsInListValue() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(asList(targetingYandexUidTargetingWithValue(YANDEX_UIDS),
                        filteringYandexUidTargetingWithValue(YANDEX_UIDS)));
        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(asList(targetingYandexUidTargetingWithValue(YANDEX_UIDS),
                        filteringYandexUidTargetingWithValue(YANDEX_UIDS_2)));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(false, true));
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

        checkComplexInternalAdGroupUpdate(updateItem2);
    }

    @Test
    public void withTargeting_TwoValidAdGroupsButSecondWithDuplicateTargetingsInSetValue() {
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withAdditionalTargetings(asList(targetingDeviceIdsTargetingWithValue(DEVICE_IDS),
                        filteringDeviceIdsTargetingWithValue(DEVICE_IDS_2)));
        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(asList(targetingDeviceIdsTargetingWithValue(DEVICE_IDS),
                        filteringDeviceIdsTargetingWithValue(DEVICE_IDS)));

        MassResult<Long> result = update(asList(updateItem1, updateItem2));

        assertThat(result, isSuccessful(true, false));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(0), field("value")))));
        assertThat(result.get(1).getValidationResult(),
                hasDefectWithDefinition(anyValidationErrorOnPath(
                        path(field("additionalTargetings"), index(1), field("value")))));

        checkComplexInternalAdGroupUpdate(updateItem1);
    }

    @Test
    public void withRetargetingCondition_OneValidAdGroup() {
        RetargetingCondition retCondition = createMetrikaGoalTargeting();
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = update(updateItem);

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withRetargetingCondition_ValidAndInvalid() {
        RetargetingCondition retCondition = createMetrikaGoalTargeting();
        RetargetingCondition invalidRetCondition = createInvalidMetrikaGoalTargeting();
        InternalAdGroupUpdateItem updateItem1 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));
        InternalAdGroupUpdateItem updateItem2 = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withRetargetingConditions(singletonList(invalidRetCondition));

        MassResult<Long> result = update(List.of(updateItem1, updateItem2));

        assertThat(result, isSuccessful(true, false));
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withCryptaRetargetingCondition_OneValidAdGroup() {
        RetargetingCondition retCondition = createCryptaTargeting();
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = update(updateItem);

        assertThat(result, isFullySuccessful());
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void withNegativeCryptaRetargetingCondition_OneValidAdGroup() {
        RetargetingCondition retCondition = createNegativeCryptaTargeting();
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = update(updateItem);

        assertThat(result.getValidationResult().flattenErrors(), empty());
        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void removeRetargetingCondition_OneValidAdGroup() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(createMetrikaGoalTargeting()));
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // без условий ретаргетинга
        updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId());
        result = update(updateItem);
        assertThat(result, isFullySuccessful());

        checkInternalAdGroupDoesNotHaveRetargetingConditionInDb(result.get(0).getResult());
    }

    @Test
    public void doNotRemoveRetargetingCondition_OneValidAdGroup() {
        RetargetingCondition retCondition = createMetrikaGoalTargeting();
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // с null вместо условий ретаргетинга
        updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(null);
        result = update(updateItem);
        assertThat(result, isFullySuccessful());

        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void replaceRetargetingCondition_OneValidAdGroup() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(createMetrikaGoalTargeting()));

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        RetargetingCondition retCondition = createCryptaTargeting();

        updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));

        result = update(updateItem);
        assertThat(result, isFullySuccessful());

        checkInternalAdGroupRetargetingConditionInDb(result.get(0).getResult(), retCondition);
    }

    @Test
    public void checkStatusBsSynced_WithoutTargetings_WithoutChanges() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void checkStatusBsSynced_WithoutTargetings_ChangeGroupFields() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithoutTargetings_AddAdditionalTargeting() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()))
                .withRetargetingConditions(emptyList());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithoutTargetings_AddRetargeting() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(singletonList(createMetrikaGoalTargeting()));

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithAdditionalTargeting_WithoutChanges() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup2.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(existentTargetingsForAdGroup2)
                .withRetargetingConditions(emptyList());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup2.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void checkStatusBsSynced_WithAdditionalTargeting_ChangeGroupFields() {
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(existentTargetingsForAdGroup2);

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup2.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithAdditionalTargeting_RemoveTargetings() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup2.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup2.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithAdditionalTargeting_ChangeTargetings() {
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup2.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()))
                .withRetargetingConditions(emptyList());

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup2.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithRetargeting_WithoutChanges() {
        // prepare group
        List<RetargetingConditionBase> retargetingConditions = singletonList(createMetrikaGoalTargeting());
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(retargetingConditions);
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup1.getId()), StatusBsSynced.YES);

        // perform
        updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(retargetingConditions);
        result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // check
        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void checkStatusBsSynced_WithRetargeting_ChangeGroupFields() {
        // prepare group
        List<RetargetingConditionBase> retargetingConditions = singletonList(createMetrikaGoalTargeting());
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(retargetingConditions);
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup1.getId()), StatusBsSynced.YES);

        // perform
        updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(retargetingConditions);
        result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // check
        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithRetargeting_ChangeRetargeting() {
        // prepare group
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(singletonList(createMetrikaGoalTargeting()));
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup1.getId()), StatusBsSynced.YES);

        // perform
        updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(List.of(createCryptaTargeting()));
        result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // check
        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }


    @Test
    public void checkStatusBsSynced_WithRetargeting_AddAdditionalTargeting() {
        // prepare group
        List<RetargetingConditionBase> retargetingConditions = singletonList(createMetrikaGoalTargeting());
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(retargetingConditions);
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup1.getId()), StatusBsSynced.YES);

        // perform
        updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(singletonList(validInternalNetworkTargeting()))
                .withRetargetingConditions(retargetingConditions);
        result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // check
        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void checkStatusBsSynced_WithRetargeting_RemoveRetargeting() {
        // prepare group
        InternalAdGroupUpdateItem updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(singletonList(createMetrikaGoalTargeting()));
        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup1.getId()), StatusBsSynced.YES);

        // perform
        updateItem = new InternalAdGroupUpdateItem()
                .withAdGroupChanges(new ModelChanges<>(existentAdGroup1.getId(), InternalAdGroup.class))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());
        result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        // check
        AdGroup actualAdGroup = getActualAdGroup(existentAdGroup1.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void updateWithSameRetargetingCondition_CheckRetargetingDoesNotChange() {
        RetargetingCondition retCondition = createMetrikaGoalTargeting();
        InternalAdGroupUpdateItem updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup1.getId())
                .withRetargetingConditions(singletonList(retCondition));

        MassResult<Long> result = update(updateItem);
        assumeThat(result, isFullySuccessful());

        Retargeting retargetingBefore = getRetargetingByAdGroupId(result.get(0).getResult());

        result = update(updateItem);
        assertThat(result, isFullySuccessful());

        Retargeting retargetingAfter = getRetargetingByAdGroupId(result.get(0).getResult());

        assertThat(retargetingAfter, beanDiffer(retargetingBefore));
    }

    @Test
    public void updateWithIrrelevantTargeting_CheckBsSyncedDoesNotChange() {
        // prepare group
        var updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(existentTargetingsForAdGroup2);
        var result = update(updateItem);
        assumeThat(result, isFullySuccessful());
        adGroupRepository.updateStatusBsSynced(
                clientInfo.getShard(), List.of(existentAdGroup2.getId()), StatusBsSynced.YES);

        //perform
        updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroup2.getId())
                .withAdditionalTargetings(concat(
                        existentTargetingsForAdGroup2, allIrrelevantValidInternalAdAdditionalTargetings()));
        result = update(updateItem);

        // check
        assertThat(result, isFullySuccessful());
        checkComplexInternalAdGroupUpdateWithCustomTargetings(updateItem, existentTargetingsForAdGroup2);
        var actualAdGroup = getActualAdGroup(existentAdGroup2.getId());
        assertThat(actualAdGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void updateExistingTimeTargetingWithIrrelevantTimeTargeting_CheckTargetingIsRemoved() {
        var updateItem = internalAdGroupUpdateItemWithoutTargetings(existentAdGroupWithTimeTargeting.getId())
                .withAdditionalTargetings(List.of(irrelevantValidTimeTargeting()));
        var result = update(updateItem);

        assumeThat(result, isFullySuccessful());
        checkComplexInternalAdGroupUpdateWithCustomTargetings(updateItem, List.of());
    }

    private RetargetingCondition createMetrikaGoalTargeting() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(GoalType.GOAL), defaultGoalByType(GoalType.AUDIENCE))
        ));
        return retargetingCondition(clientInfo.getClientId(), rules);
    }

    private RetargetingCondition createInvalidMetrikaGoalTargeting() {
        List<Rule> rules = List.of(
                defaultRule(List.of((Goal) defaultGoalByType(GoalType.GOAL).withTime(null)), RuleType.NOT)
        );
        return retargetingCondition(clientInfo.getClientId(), rules);
    }

    private RetargetingCondition createCryptaTargeting() {
        List<Rule> rules = defaultRules(List.of(
                List.of(defaultGoalByType(GoalType.INTERESTS), defaultGoalByType(GoalType.INTERNAL)),
                List.of(defaultGoalByType(GoalType.SOCIAL_DEMO))
        ));
        return retargetingCondition(clientInfo.getClientId(), rules);
    }

    private RetargetingCondition createNegativeCryptaTargeting() {
        List<Rule> rules = List.of(
                defaultRule(List.of(defaultGoalByType(GoalType.INTERESTS), defaultGoalByType(GoalType.INTERNAL)),
                        RuleType.NOT)
        );
        return retargetingCondition(clientInfo.getClientId(), rules);
    }

    private MassResult<Long> update(InternalAdGroupUpdateItem updateItem) {
        return update(singletonList(updateItem));
    }

    private MassResult<Long> update(List<InternalAdGroupUpdateItem> updateItems) {
        var operationContainer =
                new InternalAdGroupOperationContainer(
                        Applicability.PARTIAL,
                        clientInfo.getUid(),
                        UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                        true,
                        RequestSource.FRONT
                );
        return serviceUnderTest.update(updateItems, operationContainer);
    }

    private void checkExistentAdGroup(AdGroup expectedAdGroup, List<AdGroupAdditionalTargeting> additionalTargetings) {
        complexInternalAdGroupServiceTestHelper.checkInternalAdGroupAndTargetingInDb(
                campaign.getShard(), expectedAdGroup, additionalTargetings);
    }

    private void checkComplexInternalAdGroupUpdate(InternalAdGroupUpdateItem updateItem) {
        checkComplexInternalAdGroupUpdateWithCustomTargetings(updateItem, updateItem.getAdditionalTargetings());
    }

    private void checkComplexInternalAdGroupUpdateWithCustomTargetings(
            InternalAdGroupUpdateItem updateItem, List<AdGroupAdditionalTargeting> expectedTargetings) {
        Long adGroupId = updateItem.getAdGroupChanges().getId();
        AdGroup expectedAdGroup = updateItem.getAdGroupChanges()
                .applyTo(new InternalAdGroup().withId(adGroupId))
                .getModel();

        complexInternalAdGroupServiceTestHelper.checkInternalAdGroupAndTargetingInDb(
                campaign.getShard(), expectedAdGroup, expectedTargetings);
    }

    private static RetargetingCondition retargetingCondition(ClientId clientId, List<Rule> rules) {
        return (RetargetingCondition) defaultRetCondition(clientId)
                .withType(ConditionType.interests)
                .withRules(rules);
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

    void checkInternalAdGroupDoesNotHaveRetargetingConditionInDb(Long adGroupId) {
        var retargetingConditions = retargetingConditionRepository.getRetConditionsByAdGroupIds(
                clientInfo.getShard(), singletonList(adGroupId)).getOrDefault(adGroupId, List.of());
        assertThat(retargetingConditions, empty());
    }

    private AdGroup getActualAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
    }

    private Retargeting getRetargetingByAdGroupId(Long adGroupId) {
        return retargetingRepository.getRetargetingsByAdGroups(clientInfo.getShard(), List.of(adGroupId)).get(0);
    }
}
