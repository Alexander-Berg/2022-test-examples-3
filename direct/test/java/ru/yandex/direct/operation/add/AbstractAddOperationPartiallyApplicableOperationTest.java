package ru.yandex.direct.operation.add;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AbstractAddOperationPartiallyApplicableOperationTest extends BaseAbstractAddOperationTest {

    @Test
    public void getValidElementIndexes_ValidItems_ReturnsAll() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();
        assertThat(operation.getValidElementIndexes(), contains(0, 1));
    }

    @Test
    public void getValidElementIndexes_InvalidSecondItem_ReturnsFirst() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();
        assertThat(operation.getValidElementIndexes(), equalTo(Collections.singleton(0)));
    }

    @Test
    public void getValidElementIndexes_InvalidFirstAndSecondItem_ReturnsEmpty() {
        twoObjectsWithErrors();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();
        assertThat(operation.getValidElementIndexes(), empty());
    }

    @Test(expected = IllegalStateException.class)
    public void apply_partial_CalledBeforePrepare_ThrowsException() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.apply(Collections.singleton(0));
    }

    @Test(expected = IllegalStateException.class)
    public void apply_partial_ApplyInvalidItem_ThrowsException() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.apply(Collections.singleton(1));
    }

    @Test(expected = IllegalStateException.class)
    public void apply_partial_ApplyNotExistingItem_ThrowsException() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.apply(Collections.singleton(2));
    }

    @Test
    public void apply_partial_ApplyOnlyFirstItem_CallsExecuteWithOnlyOneElement() {
        twoValidObjects();
        executeReturns(ImmutableMap.of(0, AD_GROUP_1_ID));

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();
        MassResult<Long> result = operation.apply(Collections.singleton(0));

        assertThat("состояние MassResult -- successful",
                result.getState(), equalTo(ResultState.SUCCESSFUL));
        assertThat("состояние первого элемента результата -- successful",
                result.get(0).getState(), equalTo(ResultState.SUCCESSFUL));
        assertThat("состояние второго элемента результата -- canceled",
                result.get(1).getState(), equalTo(ResultState.CANCELED));
    }
}
