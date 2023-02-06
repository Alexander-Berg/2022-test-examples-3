package ru.yandex.direct.operation.update;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;

public class AbstractUpdateOperationPartiallyApplicableOperationTest extends BaseAbstractUpdateOperationTest {

    @Test
    public void getValidElementIndexes_ValidItems_ReturnsAll() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        assertThat(updateOperation.getValidElementIndexes(), contains(0, 1));
    }

    @Test
    public void getValidElementIndexes_InvalidSecondItem_ReturnsFirst() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        assertThat(updateOperation.getValidElementIndexes(), equalTo(Collections.singleton(0)));
    }

    @Test
    public void getValidElementIndexes_InvalidFirstAndSecondItem_ReturnsEmpty() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationWithInvalidSecondItem();
        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        assertThat(updateOperation.getValidElementIndexes(), empty());
    }

    @Test(expected = IllegalStateException.class)
    public void apply_partial_CalledBeforePrepare_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.apply(Collections.singleton(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_partial_ValidationIsFullySuccessful_CallsExecuteWithOnlyOneElement() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply(Collections.singleton(0));

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        Map<Integer, AppliedChanges<RetargetingCondition>> validAppliedChanges =
                new HashMap<>(captor.getValue().getAppliedChangesForExecutionWithIndex());
        Assert.assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges.keySet(), hasSize(1));
        Assert.assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
    }

}
