package ru.yandex.direct.operation.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Применение изменений.
 * <p>
 * Проверяется, что в метод execute передается правильный список appliedChanges,
 * что вызывается метод onExecute, что метод apply возвращает правильный результат,
 * а так же негативные кейсы.
 */
@SuppressWarnings("unchecked")
public class AbstractUpdateOperationExecutionTest extends BaseAbstractUpdateOperationTest {

    // неправильный порядок вызова apply()

    @Test(expected = IllegalStateException.class)
    public void apply_CalledBeforePrepare_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledTwice_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        prepareAndApply(Applicability.PARTIAL).apply();
    }

    // вызов apply(), когда операцию выполнить невозможно

    @Test(expected = IllegalStateException.class)
    public void apply_PartialNo_ResultIsReadyAfterModelChangesValidationStage_ThrowsException() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        prepareAndApply(Applicability.FULL);
    }

    @Test(expected = IllegalStateException.class)
    public void apply_PartialNo_ResultIsReadyAfterAppliedChangesValidationStage_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        prepareAndApply(Applicability.FULL);
    }

    @Test(expected = IllegalStateException.class)
    public void apply_PartialYes_ResultIsReady_ThrowsException() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationWithInvalidSecondItem();
        prepareAndApply(Applicability.PARTIAL);
    }

    private TestableUpdateOperation prepareAndApply(Applicability applicability) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        updateOperation.prepare();
        updateOperation.apply();
        return updateOperation;
    }

    // вызов шаблонного метода execute с правильным набором валидных AppliedChanges

    @Test
    public void apply_PartialYes_PreValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChangesWithIndex() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        Map<Integer, AppliedChanges<RetargetingCondition>> validAppliedChanges =
                new HashMap<>(captor.getValue().getAppliedChangesForExecutionWithIndex());
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges.keySet(), hasSize(1));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                validAppliedChanges.get(1).getModel(), sameInstance(retCond2));
    }

    @Test
    public void apply_PartialYes_ValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChangesWithIndex() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        Map<Integer, AppliedChanges<RetargetingCondition>> validAppliedChanges =
                new HashMap<>(captor.getValue().getAppliedChangesForExecutionWithIndex());
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges.keySet(), hasSize(1));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
    }

    @Test
    public void apply_PartialNo_ValidationIsFullySuccessful_CallsExecuteWithValidAppliedChangesWithIndex() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        Map<Integer, AppliedChanges<RetargetingCondition>> validAppliedChanges =
                new HashMap<>(captor.getValue().getAppliedChangesForExecutionWithIndex());
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 2 элемента",
                validAppliedChanges.keySet(), hasSize(2));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                validAppliedChanges.get(1).getModel(), sameInstance(retCond2));
    }

    @Test
    public void apply_PartialYes_PreValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> appliedChanges =
                new ArrayList<>(captor.getValue().getAppliedChangesForExecution());
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                appliedChanges, hasSize(1));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                appliedChanges.get(0).getModel(), sameInstance(retCond2));
    }

    @Test
    public void apply_PartialYes_ValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> appliedChanges =
                new ArrayList<>(captor.getValue().getAppliedChangesForExecution());
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                appliedChanges, hasSize(1));
        assertThat("map валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                appliedChanges.get(0).getModel(), sameInstance(retCond1));
    }

    @Test
    public void apply_PartialNo_ValidationIsFullySuccessful_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<ExecutionStep> captor = ArgumentCaptor.forClass(ExecutionStep.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> appliedChanges =
                new ArrayList<>(captor.getValue().getAppliedChangesForExecution());
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 2 элемента",
                appliedChanges, hasSize(2));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                appliedChanges.get(0).getModel(), sameInstance(retCond1));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                appliedChanges.get(1).getModel(), sameInstance(retCond2));
    }

    // вызов метода onExecuted

    @Test
    public void apply_CallsOnExecuted() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply();

        verify(updateOperation).afterExecution(any());
    }

    // вызов метода postProcessResult

    @Test
    public void apply_CallsPostProcessResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepareAndApply();

        verify(updateOperation).postProcessResult(any());
    }
}
