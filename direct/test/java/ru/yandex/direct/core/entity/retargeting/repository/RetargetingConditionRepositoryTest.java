package ru.yandex.direct.core.entity.retargeting.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingConditionValidationData;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbschema.ppc.enums.RetargetingConditionsRetargetingConditionsType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.ab_segments;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.utils.FunctionalUtils.filterAndMapList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionRepositoryTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFieldsExcept(
            newPath("deleted"), newPath("lastChangeTime"), newPath("available"), newPath(".*", "allowToUse"));
    private static final Long NON_EXISTING_ID = 123456L;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private RetargetingGoalsRepository retGoalsRepository;

    @Autowired
    private RetargetingConditionRepository repoUnderTest;

    private RetConditionInfo retConditionInfo1;
    private RetConditionInfo retConditionInfo2;
    private ClientId clientId;
    private int shard;
    private DSLContext dslContext;
    private List<Long> retConditionIds;
    private List<Long> retConditionIdsOrdered;

    @Before
    public void before() {
        retConditionInfo1 = retConditionSteps.createDefaultRetCondition();
        retConditionInfo2 = retConditionSteps.createDefaultRetCondition(retConditionInfo1.getClientInfo());
        shard = retConditionInfo1.getShard();
        dslContext = dslContextProvider.ppc(shard);
        clientId = retConditionInfo1.getClientId();
        retConditionIds = asList(retConditionInfo1.getRetConditionId(), retConditionInfo2.getRetConditionId());
        retConditionIdsOrdered = new ArrayList<>(retConditionIds);
        retConditionIdsOrdered.sort(naturalOrder());
    }

    // get() by List<Long> ids

    @Test
    public void get_OneOfPassedIdsIsDeleted_DontReturnDeletedConditions() {
        Long deletedConditionId = retConditionIds.get(0);
        repoUnderTest.delete(shard, clientId, singletonList(deletedConditionId));

        List<RetargetingCondition> fetchedRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard,
                clientId, retConditionIds);
        List<Long> fetchedIds = mapList(fetchedRetConditions, RetargetingCondition::getId);
        assertThat("среди выбранных условий отсутствует удаленное",
                fetchedIds.contains(deletedConditionId), is(false));
    }

    @Test
    public void get_AllOfPassedIdsExists_ReturnsAllConditionsInValidOrder() {
        List<RetargetingCondition> fetchedRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard,
                clientId, retConditionIds);
        List<Long> fetchedIds = mapList(fetchedRetConditions, RetargetingCondition::getId);
        assertThat(fetchedIds, contains(retConditionIdsOrdered.toArray()));
    }

    @Test
    public void get_OneOfPassedIdsExists_ReturnsOneCondition() {
        long existingId = retConditionIds.get(0);
        List<RetargetingCondition> fetchedRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard,
                clientId, asList(existingId, NON_EXISTING_ID));
        List<Long> fetchedIds = mapList(fetchedRetConditions, RetargetingCondition::getId);
        assertThat(fetchedIds, contains(existingId));
    }

    @Test
    public void get_OneOfPassedIdsExistsAndAnotherBelongsToOtherClient_ReturnsConditionOfPassedClient() {
        long client1RetCondId = retConditionIds.get(0);
        RetConditionInfo retConditionInfo = retConditionSteps.createDefaultRetCondition();
        long client2RetCondId = retConditionInfo.getRetConditionId();

        List<Long> retCondIdsToFetch = asList(client2RetCondId, client1RetCondId);

        List<RetargetingCondition> fetchedRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard,
                clientId, retCondIdsToFetch);

        List<Long> fetchedIds = mapList(fetchedRetConditions, RetargetingCondition::getId);
        assertThat(fetchedIds, contains(client1RetCondId));
    }

    @Test
    public void get_NoneOfPassedIdsExists_ReturnsEmptyList() {
        List<RetargetingCondition> fetchedRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard,
                clientId, asList(Long.MAX_VALUE - 1, Long.MAX_VALUE - 2));
        List<Long> fetchedIds = mapList(fetchedRetConditions, RetargetingCondition::getId);
        assertThat(fetchedIds, emptyIterable());
    }

    @Test
    public void get_OneOfPassedIdsExists_DataIsValid() {
        List<RetargetingCondition> fetchedRetConditions =
                repoUnderTest.getFromRetargetingConditionsTable(shard, clientId, singletonList(retConditionIds.get(0)));

        assertThat(fetchedRetConditions, hasSize(1));
        assertThat(fetchedRetConditions.get(0),
                beanDiffer(retConditionInfo1.getRetCondition())
                        .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void get_LimitIsDefined_LimitWorksFine() {
        int limit = 1;

        List<RetargetingCondition> fetchedRetConditions =
                repoUnderTest.getFromRetargetingConditionsTable(shard, clientId, retConditionIds, limited(limit));

        assertThat(fetchedRetConditions, hasSize(limit));
    }

    @Test
    public void get_LimitAndOffsetAreDefined_OffsetWorksFine() {
        int offset = 1;

        List<RetargetingCondition> fetchedRetConditions =
                repoUnderTest.getFromRetargetingConditionsTable(shard, clientId, retConditionIds, limited(10, offset));

        assertThat(fetchedRetConditions, hasSize(retConditionIds.size() - offset));
    }

    // get() by List<RetargetingConditionsRetargetingConditionsType> types

    @Test
    public void get_OneOfPassedIsBrandSafetyType_ReturnWithoutBrandSafetyType() {
        retConditionSteps.createDefaultBrandSafetyRetCondition(retConditionInfo1.getClientInfo());
        List<RetargetingCondition> fetchedRetConditions =
                repoUnderTest.getFromRetargetingConditionsTable(shard, clientId, null, null, null,
                        asList(RetargetingConditionsRetargetingConditionsType.metrika_goals,
                                RetargetingConditionsRetargetingConditionsType.brandsafety), maxLimited());

        assertThat(fetchedRetConditions, hasSize(2));
        assertThat(fetchedRetConditions, contains(
                beanDiffer(retConditionInfo1.getRetCondition()).useCompareStrategy(COMPARE_STRATEGY),
                beanDiffer(retConditionInfo2.getRetCondition()).useCompareStrategy(COMPARE_STRATEGY)));
    }

    // getExistingIds()

    @Test
    public void getExistingIds_AllOfPassedIdsExists_ReturnsAllIds() {
        List<Long> existingIds = repoUnderTest.getExistingIds(shard, clientId, retConditionIds);
        assertThat(existingIds, containsInAnyOrder(retConditionIds.toArray()));
    }

    @Test
    public void getExistingIds_OneOfPassedIdsExists_ReturnsOneId() {
        long existingId = retConditionIds.get(0);
        List<Long> fetchedExistingIds = repoUnderTest.getExistingIds(shard, clientId, asList(12345L, existingId));
        assertThat(fetchedExistingIds, contains(existingId));
    }

    @Test
    public void getExistingIds_OneOfPassedIdsExistsAndAnotherIsDeleted_DontReturnDeletedId() {
        Long deletedConditionId = retConditionIds.get(0);
        repoUnderTest.delete(shard, clientId, singletonList(deletedConditionId));

        List<Long> fetchedIds = repoUnderTest.getExistingIds(
                shard, clientId, asList(deletedConditionId, retConditionIds.get(0)));

        assertThat("среди выбранных условий отсутствует удаленное",
                fetchedIds.contains(deletedConditionId), is(false));
    }

    @Test
    public void getExistingIds_OneOfPassedIdsExistsAndAnotherBelongsToOtherClient_ReturnsConditionIdOfPassedClient() {
        long client1RetCondId = retConditionIds.get(0);
        long client2RetCondId = retConditionSteps.createDefaultRetCondition().getRetConditionId();

        List<Long> retCondIdsToCheck = asList(client1RetCondId, client2RetCondId);

        List<Long> fetchedExistingIds = repoUnderTest.getExistingIds(shard, clientId, retCondIdsToCheck);
        assertThat(fetchedExistingIds, contains(client1RetCondId));
    }

    @Test
    public void getExistingIds_NoneOfPassedIdsExists_ReturnsEmptyList() {
        List<Long> fetchedExistingIds = repoUnderTest.getExistingIds(shard, clientId, singletonList(12345L));
        assertThat(fetchedExistingIds, emptyIterable());
    }

    @Test
    public void getExistingIds_OneOfPassedIsBrandSafety_ReturnWithoutBrandSafety() {
        RetConditionInfo brandSafetyRetConditionInfo =
                retConditionSteps.createDefaultBrandSafetyRetCondition(retConditionInfo1.getClientInfo());
        List<Long> requestedRetCondIds = new ArrayList<>(retConditionIds);
        requestedRetCondIds.add(brandSafetyRetConditionInfo.getRetConditionId());
        List<Long> fetchedExistingIds = repoUnderTest.getExistingIds(shard, clientId, requestedRetCondIds);
        assertThat(fetchedExistingIds, containsInAnyOrder(retConditionIds.toArray()));
    }

    // add

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoItems_AddsValidDataAndReturnIdsInValidOrder() {
        List<RetargetingCondition> sourceConditions =
                asList(defaultRetCondition(clientId), defaultRetCondition(clientId));

        List<Long> newRetConditionIds = repoUnderTest.add(shard, sourceConditions);
        List<RetargetingCondition> newRetConditions = repoUnderTest.getFromRetargetingConditionsTable(shard, clientId,
                newRetConditionIds);

        assertThat(newRetConditions, contains(
                beanDiffer(sourceConditions.get(0)).useCompareStrategy(COMPARE_STRATEGY),
                beanDiffer(sourceConditions.get(1)).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void add_OneItem_AddsEntriesToRetargetingGoalsTable() {
        RetargetingCondition sourceCondition = bigRetCondition(clientId);
        Set<Long> allSourceGoalIds = extractGoalIds(sourceCondition);

        Long newRetConditionId = repoUnderTest.add(shard, singletonList(sourceCondition)).get(0);

        List<Long> goalIdsInDb = retGoalsRepository.getGoalIds(shard, newRetConditionId);
        assertThat("все связки условия с целями добавлены в retargeting_goals",
                goalIdsInDb, containsInAnyOrder(allSourceGoalIds.toArray()));
    }

    // getValidationData

    @Test
    public void getValidationData_ReturnsAllExistingConditions() {
        List<RetargetingConditionValidationData> fetchedValidationDataList =
                repoUnderTest.getValidationData(shard, clientId);
        List<Long> fetchedIds = mapList(fetchedValidationDataList, vd -> vd.getId());
        assertThat(fetchedIds, containsInAnyOrder(retConditionIds.toArray()));
    }

    @Test
    public void getValidationData_DontReturnDeletedConditions() {
        Long deletedConditionId = retConditionIds.get(0);
        repoUnderTest.delete(shard, clientId, singletonList(deletedConditionId));

        List<RetargetingConditionValidationData> fetchedValidationDataList =
                repoUnderTest.getValidationData(shard, clientId);
        List<Long> fetchedIds = mapList(fetchedValidationDataList, vd -> vd.getId());
        assertThat("среди выбранных условий отсутствует удаленное",
                fetchedIds.contains(deletedConditionId), is(false));
    }

    @Test
    public void getValidationData_DontReturnInterestsConditions() {
        List<RetargetingCondition> sourceConditions
                = singletonList((RetargetingCondition) defaultRetCondition(clientId).withType(interests));

        Long newConditionId = repoUnderTest.add(shard, sourceConditions).get(0);

        List<RetargetingConditionValidationData> fetchedValidationDataList =
                repoUnderTest.getValidationData(shard, clientId);
        List<Long> fetchedIds = mapList(fetchedValidationDataList, vd -> vd.getId());
        assertThat("среди выбранных условий отсутствует с типом 'interests'",
                fetchedIds.contains(newConditionId), is(false));
    }

    @Test
    public void getValidationData_DontReturnAbSegmentsConditions() {
        List<RetargetingCondition> sourceConditions
                = singletonList((RetargetingCondition) defaultRetCondition(clientId).withType(ab_segments));

        Long newConditionId = repoUnderTest.add(shard, sourceConditions).get(0);

        List<RetargetingConditionValidationData> fetchedValidationDataList =
                repoUnderTest.getValidationData(shard, clientId);
        List<Long> fetchedIds = mapList(fetchedValidationDataList, RetargetingConditionValidationData::getId);
        assertThat("среди выбранных условий отсутствует с типом 'interests'",
                fetchedIds.contains(newConditionId), is(false));
    }

    // delete

    @Test
    public void delete_AllOfPassedIdsExists_DeletesAllConditions() {
        repoUnderTest.delete(shard, clientId, retConditionIds);

        List<Long> existingIds = repoUnderTest.getExistingIds(shard, clientId, retConditionIds);
        assertThat("все удаленные условия отсутствуют в базе", existingIds, emptyIterable());
    }

    @Test
    public void delete_OneOfPassedIdsExists_DeletesOneCondition() {
        repoUnderTest.delete(shard, clientId, asList(retConditionIds.get(0), 123L));

        List<Long> existingIds = repoUnderTest.getExistingIds(shard, clientId, retConditionIds);
        assertThat("удаленное условие отсутствует в базе", existingIds, contains(retConditionIds.get(1)));
    }

    @Test
    public void delete_DeleteOneCondition_RemovesEntriesFromRetargetingGoals() {
        Long retCondId = retConditionIds.get(0);
        repoUnderTest.delete(shard, clientId, singletonList(retCondId));

        List<Long> retGoalIds = retGoalsRepository.getGoalIds(shard, retCondId);
        assertThat(retGoalIds, emptyIterable());
    }

    @Test
    public void deleteHyperGeo_AllOfPassedIdsExistAndBelongToHyperGeoType_ClientIdIsNotPassed_DeletesAllConditions() {
        var hyperGeoRetConditionIds = createHyperGeoRetargetingConditions();
        repoUnderTest.deleteHyperGeo(dslContext, null, hyperGeoRetConditionIds);

        List<Long> existingIds = filterAndMapList(repoUnderTest.getConditions(shard, hyperGeoRetConditionIds),
                condition -> !condition.getDeleted(), RetargetingConditionBase::getId);
        assertThat("все удаленные условия отсутствуют в базе", existingIds, emptyIterable());
    }

    @Test
    public void deleteHyperGeo_AllOfPassedIdsExistAndBelongToHyperGeoType_ClientIdIsPassed_DeletesOneConditions() {
        var hyperGeoRetConditionIds = createHyperGeoRetargetingConditions();
        repoUnderTest.deleteHyperGeo(dslContext, clientId, hyperGeoRetConditionIds);

        List<Long> existingIds = filterAndMapList(repoUnderTest.getConditions(shard, hyperGeoRetConditionIds),
                condition -> !condition.getDeleted(), RetargetingConditionBase::getId);
        assertThat("удаленное условие отсутствует в базе", existingIds, contains(hyperGeoRetConditionIds.get(1)));
    }

    @Test
    public void deleteHyperGeo_OneOfPassedIdsExistsAndBelongsToHyperGeoType_DeletesOneCondition() {
        var goal = defaultGoalByType(GoalType.AUDIENCE);
        var hyperGeoRetConditionId = retConditionSteps.createDefaultRetCondition(List.of(goal),
                retConditionInfo1.getClientInfo(), ConditionType.geo_segments, RuleType.OR).getRetConditionId();
        repoUnderTest.deleteHyperGeo(dslContext, clientId, List.of(retConditionIds.get(0), hyperGeoRetConditionId));

        List<Long> existingIds = repoUnderTest.getExistingIds(shard, clientId,
                List.of(retConditionIds.get(0), hyperGeoRetConditionId));
        assertThat("удаленное условие отсутствует в базе", existingIds, contains(retConditionIds.get(0)));

        var retGoalIds = retGoalsRepository.getGoalIds(shard, hyperGeoRetConditionId);
        assertThat("удаленные цели ретаргетинга отсутствуют в базе ", retGoalIds, emptyIterable());
    }

    private List<Long> createHyperGeoRetargetingConditions() {
        var goal1 = defaultGoalByType(GoalType.AUDIENCE);
        var goal2 = defaultGoalByType(GoalType.AUDIENCE);
        var clientInfo2 = clientSteps.createDefaultClient();
        var hyperGeoRetConditionId1 = retConditionSteps.createDefaultRetCondition(List.of(goal1),
                retConditionInfo1.getClientInfo(), ConditionType.geo_segments, RuleType.OR).getRetConditionId();
        var hyperGeoRetConditionId2 = retConditionSteps.createDefaultRetCondition(List.of(goal1, goal2),
                clientInfo2, ConditionType.geo_segments, RuleType.OR).getRetConditionId();
        return List.of(hyperGeoRetConditionId1, hyperGeoRetConditionId2);
    }


    // update

    @Test
    public void update_OneItem_UpdatedConditionDataIsValid() {
        RetargetingCondition sourceCondition = defaultRetCondition(clientId);
        RetargetingCondition conditionWithUpdateData = bigRetCondition(clientId);
        Long sourceConditionId = repoUnderTest.add(shard, singletonList(sourceCondition)).get(0);

        ModelChanges<RetargetingCondition> modelChanges =
                new ModelChanges<>(sourceConditionId, RetargetingCondition.class);
        modelChanges.process(conditionWithUpdateData.getName(), RetargetingCondition.NAME);
        modelChanges.process(conditionWithUpdateData.getRules(), RetargetingCondition.RULES);

        AppliedChanges<RetargetingCondition> appliedChanges = modelChanges.applyTo(sourceCondition);

        repoUnderTest.update(shard, singletonList(appliedChanges));

        RetargetingCondition actualUpdatedCondition =
                repoUnderTest.getFromRetargetingConditionsTable(shard, clientId, singletonList(sourceConditionId)).get(0);

        RetargetingConditionBase expectedUpdatedCondition = conditionWithUpdateData;
        expectedUpdatedCondition.setId(sourceConditionId);
        expectedUpdatedCondition.setDescription(sourceCondition.getDescription());

        assertThat("данные условия ретаргетинга после обновления соответствуют ожидаемым",
                actualUpdatedCondition,
                beanDiffer(expectedUpdatedCondition).useCompareStrategy(COMPARE_STRATEGY));
    }


    // todo добавить тесты для методоа getAdGroupIds() и getUsedRetargetingConditionsIds()

    private Set<Long> extractGoalIds(RetargetingCondition retargetingCondition) {
        List<Long> allGoalIds = retargetingCondition.getRules().stream()
                .map(rule -> rule.getGoals())
                .map(goals -> mapList(goals, goal -> goal.getId()))
                .reduce(new ArrayList<>(), (acc, cur) -> {
                    acc.addAll(cur);
                    return acc;
                });
        return new HashSet<>(allGoalIds);
    }
}
