package ru.yandex.direct.core.entity.retargeting.service;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetConditions;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingConditionServiceDeleteTest extends BaseRetargetingConditionServiceTest {

    private List<Long> conditionIds;
    private Long conditionIdNotExist1;
    private Long conditionIdNotExist2;

    @Before
    public void before() {
        super.before();
        // создаем условия в базе
        List<RetargetingCondition> conditions = defaultRetConditions(clientId, 2);
        conditionIds = retConditionRepository.add(shard, conditions);
        conditionIdNotExist1 = conditionIds.get(0) + conditionIds.get(1);
        conditionIdNotExist2 = conditionIds.get(0) + 2 * conditionIds.get(1);
    }

    @Test
    public void deleteRetargetingConditions_OneItemExists_ResultIsFullySuccessful() {
        deleteConditionsAndCheckResult(singletonList(conditionIds.get(0)), true);
    }

    @Test
    public void deleteRetargetingConditions_OneItemExists_DeletesItem() {
        deleteConditionsAndCheckItemsDeleted(singletonList(conditionIds.get(0)), singletonList(conditionIds.get(1)));
    }

    @Test
    public void deleteRetargetingConditions_OneItemDoesNotExist_OperationResultIsSuccessfulAndItemResultIsInvalid() {
        deleteConditionsAndCheckResult(singletonList(conditionIdNotExist1), false);
    }

    @Test
    public void deleteRetargetingConditions_BothItemsExists_ResultIsFullySuccessful() {
        deleteConditionsAndCheckResult(conditionIds, true, true);
    }

    @Test
    public void deleteRetargetingConditions_BothItemsExists_DeletesBothItems() {
        deleteConditionsAndCheckItemsDeleted(conditionIds, new ArrayList<>());
    }

    @Test
    public void deleteRetargetingConditions_OneOfItemsExists_OperationResultIsSuccessfulAndItemResultIsBroken() {
        deleteConditionsAndCheckResult(asList(conditionIds.get(1), conditionIdNotExist1), true, false);
    }

    @Test
    public void deleteRetargetingConditions_OneOfItemsExists_DeletesOneOfItems() {
        deleteConditionsAndCheckItemsDeleted(asList(conditionIds.get(1), conditionIdNotExist1), singletonList(conditionIds.get(0)));
    }

    @Test
    public void deleteRetargetingConditions_BothItemsDontExist_OperationResultIsSuccessfulAndItemResultsIsBroken() {
        deleteConditionsAndCheckResult(asList(conditionIdNotExist2, conditionIdNotExist1), false, false);
    }

    @Test
    public void deleteRetargetingConditions_BothItemsDontExist_DeletesNothing() {
        deleteConditionsAndCheckItemsDeleted(asList(conditionIdNotExist2, conditionIdNotExist1), conditionIds);
    }

    @Test
    public void addRetargetingConditions_EmptyList_OperationIsSuccessful() {
        List<Long> conditionsToDelete = emptyList();
        MassResult<Long> result = retargetingConditionOperationFactory
                .deleteRetargetingConditions(conditionsToDelete, clientId);
        assertThat("результат операции успешен", result.isSuccessful(), is(true));
    }

    @Test
    public void deleteRetargetingConditions_OneOfItemsExists_FlagFalse_OperationResultIsSuccessfulAndItemResultIsBroken() {
        MassResult<Long> result =
                retargetingConditionOperationFactory
                        .deleteRetargetingConditions(asList(conditionIds.get(1), conditionIdNotExist1), clientId, Applicability.FULL);
        assertThat("результат операции соответствует ожидаемому", result.isSuccessful(), is(true));
        assertThat("поэлементные результаты соответствуют ожидаемым",
                mapList(result.getResult(), Result::isSuccessful), contains(true, false));
    }

    @Test
    public void deleteRetargetingConditions_OneOfItemsExists_FlagFalse_DeletesNothing() {
        MassResult<Long> result = retargetingConditionOperationFactory
                .deleteRetargetingConditions(asList(conditionIds.get(1), conditionIdNotExist1), clientId, Applicability.FULL);
        assumeThat(result.isSuccessful(), is(true));

        List<Long> existingIds = retConditionRepository.getExistingIds(shard, clientId, conditionIds);
        assertThat("список оставшихся у клиента условий соответствует ожидаемому",
                existingIds, contains(conditionIds.toArray()));
    }

    private void deleteConditionsAndCheckResult(
            List<Long> conditionsToDelete, Boolean... elementsResults) {
        MassResult<Long> result = retargetingConditionOperationFactory
                .deleteRetargetingConditions(conditionsToDelete, clientId);
        assertThat(result, isSuccessful(elementsResults));
    }

    @SuppressWarnings("unchecked")
    private void deleteConditionsAndCheckItemsDeleted(
            List<Long> conditionsToDelete, List<Long> leftConditionIds) {
        MassResult<Long> result = retargetingConditionOperationFactory
                .deleteRetargetingConditions(conditionsToDelete, clientId);
        assumeThat(result, isSuccessful());

        List<Long> existingIds = retConditionRepository.getExistingIds(shard, clientId, conditionIds);
        Matcher matcher = leftConditionIds.size() > 0 ? contains(leftConditionIds.toArray()) : emptyIterable();
        assertThat("список оставшихся у клиента условий соответствует ожидаемому", existingIds, matcher);
    }
}
