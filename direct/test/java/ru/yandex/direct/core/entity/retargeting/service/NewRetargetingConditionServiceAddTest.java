package ru.yandex.direct.core.entity.retargeting.service;

import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetConditions;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.bigRetConditionsWithLalSegments;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceAddTest extends BaseRetargetingConditionServiceTest {

    private List<RetargetingCondition> conditions;

    @Before
    public void before() {
        super.before();
        conditions = bigRetConditions(clientId, 2);
    }

    @Test
    public void addRetargetingConditions_OneItemIsValid_ResultIsFullySuccessful() {
        addConditionsAndCheckResult(singletonList(conditions.get(0)), true);
    }

    @Test
    public void addRetargetingConditions_OneItemIsValid_AddsItem() {
        addConditionsAndCheckAddedConditionsNumber(singletonList(conditions.get(0)), 1);
    }

    @Test
    public void addRetargetingConditions_OneItemIsInvalid_OperationResultIsSuccessfulAndItemResultIsBroken() {
        conditions.get(0).setClientId(null);
        addConditionsAndCheckResult(singletonList(conditions.get(0)), false);
    }

    @Test
    public void addRetargetingConditions_BothItemsIsValid_ResultIsFullySuccessful() {
        addConditionsAndCheckResult(conditions, true, true);
    }

    @Test
    public void addRetargetingConditions_BothItemsIsValid_AddsAllItems() {
        addConditionsAndCheckAddedConditionsNumber(conditions, 2);
    }

    @Test
    public void addRetargetingConditions_OneOfItemsIsInvalid_OperationResultIsSuccessfulAndItemResultIsBroken() {
        conditions.get(0).setClientId(null);
        addConditionsAndCheckResult(conditions, false, true);
    }

    @Test
    public void addRetargetingConditions_OneOfItemsIsInvalid_AddsOnlyValidItem() {
        conditions.get(0).setClientId(null);
        addConditionsAndCheckAddedConditionsNumber(conditions, 1);
    }

    @Test
    public void addRetargetingConditions_BothItemsIsInvalid_OperationResultIsSuccessfulAndItemsResultsIsBroken() {
        List<RetargetingCondition> conditionsToAdd = bigRetConditions(clientId, 2);
        conditionsToAdd.forEach(c -> c.setClientId(null));
        addConditionsAndCheckResult(conditionsToAdd, false, false);
    }

    @Test
    public void addRetargetingConditions_EmptyList_OperationIsSuccessful() {
        List<RetargetingCondition> conditionsToAdd = emptyList();
        MassResult<Long> result = createRetargetingConditions(conditionsToAdd);
        assertThat(result, isSuccessful());
    }

    @Test
    public void addRetargetingConditions_WithLalSegments() {
        List<RetargetingCondition> conditionsWithLalSegments = bigRetConditionsWithLalSegments(clientId, 2);
        testLalSegmentRepository.addLalSegmentsFromRetargetingConditions(conditionsWithLalSegments);

        List<Long> addedRetConditionIds = createRetargetingConditionsReturningIds(conditionsWithLalSegments);
        assertThat("количество возвращенных id соответствует количеству валидных элементов",
                addedRetConditionIds, hasSize(2));

        List<RetargetingCondition> existingRetConditions = retConditionRepository.getConditions(
                shard, addedRetConditionIds);

        assertEquals("в первом правиле union_with_id должен быть выставлен",
                existingRetConditions.get(0).getRules().get(0).getGoals().get(0).getId(),
                existingRetConditions.get(0).getRules().get(0).getGoals().get(1).getUnionWithId());
        assertNull("во втором правиле union_with_id не должен быть выставлен",
                existingRetConditions.get(0).getRules().get(1).getGoals().get(1).getUnionWithId());
        assertEquals("в третьем правиле union_with_id должен быть выставлен",
                existingRetConditions.get(0).getRules().get(2).getGoals().get(0).getId(),
                existingRetConditions.get(0).getRules().get(2).getGoals().get(1).getUnionWithId());
    }

    private void addConditionsAndCheckResult(List<RetargetingCondition> conditions, Boolean... elementsResults) {
        MassResult<Long> result = createRetargetingConditions(conditions);
        assertThat(result, isSuccessful(elementsResults));
    }

    private void addConditionsAndCheckAddedConditionsNumber(
            List<RetargetingCondition> conditions, int validElemsNumber) {
        List<Long> addedRetConditionIds =
                createRetargetingConditionsReturningIds(conditions);
        assertThat("количество возвращенных id соответствует количеству валидных элементов",
                addedRetConditionIds, hasSize(validElemsNumber));

        List<Long> existingRetConditionIds =
                retConditionRepository.getExistingIds(shard, clientId, addedRetConditionIds);
        assertThat("id добавленного элемента, возвращенный методом addRetargetingConditions присутствуют в базе",
                existingRetConditionIds, containsInAnyOrder(addedRetConditionIds.toArray()));
    }

    private List<Long> createRetargetingConditionsReturningIds(List<RetargetingCondition> conditionsToAdd) {
        MassResult<Long> result = createRetargetingConditions(conditionsToAdd);
        assumeThat(result, isSuccessful());
        return extractIdsFromResult(result);
    }

    private MassResult<Long> createRetargetingConditions(List<RetargetingCondition> conditionsToAdd) {
        var goals = StreamEx.of(conditionsToAdd)
                .map(RetargetingCondition::collectGoals)
                .flatMap(Collection::stream)
                .filter(g -> g.getType().isMetrika())
                .append(StreamEx.of(conditionsToAdd)
                        .map(RetargetingCondition::collectGoals)
                        .flatMap(Collection::stream)
                        .filter(t -> t.getType() == GoalType.LAL_SEGMENT)
                        .map(g -> (Goal) new Goal()
                                .withId(g.getParentId())
                                .withType(GoalType.GOAL)))
                .toSet();
        metrikaClientStub.addGoals(uid, goals);
        metrikaHelperStub.addGoalsFromConditions(uid, conditionsToAdd);
        return retargetingConditionService.addRetargetingConditions(conditionsToAdd, clientId);
    }
}
