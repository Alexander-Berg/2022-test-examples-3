package ru.yandex.direct.operation.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
public class SimpleAbstractUpdateOperationExecutionTest extends BaseAbstractUpdateOperationTest {
    // вызов шаблонного метода execute с правильным набором валидных AppliedChanges

    @Test
    public void apply_PartialYes_PreValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableSimpleUpdateOperation updateOperation = createSimpleUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> validAppliedChanges = new ArrayList<>(captor.getValue());
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges, hasSize(1));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond2));
    }

    @Test
    public void apply_PartialYes_ValidationWithOneInvalidItem_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();

        TestableSimpleUpdateOperation updateOperation = createSimpleUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> validAppliedChanges = new ArrayList<>(captor.getValue());
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges, hasSize(1));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
    }

    @Test
    public void apply_PartialNo_ValidationIsFullySuccessful_CallsExecuteWithValidAppliedChanges() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableSimpleUpdateOperation updateOperation = createSimpleUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> validAppliedChanges = new ArrayList<>(captor.getValue());
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 2 элемента",
                validAppliedChanges, hasSize(2));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
        assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для второго условия ретаргетинга",
                validAppliedChanges.get(1).getModel(), sameInstance(retCond2));
    }

    @Test
    public void apply_partial_ValidationIsFullySuccessful_CallsExecuteWithOnlyOneElement() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableSimpleUpdateOperation updateOperation = createSimpleUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply(Collections.singleton(0));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(updateOperation).execute(captor.capture());

        List<AppliedChanges<RetargetingCondition>> validAppliedChanges = new ArrayList<>(captor.getValue());
        Assert.assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать 1 элемент",
                validAppliedChanges, hasSize(1));
        Assert.assertThat("список валидных AppliedChanges, передаваемых в метод execute, "
                        + "должен содержать объект для первого условия ретаргетинга",
                validAppliedChanges.get(0).getModel(), sameInstance(retCond1));
    }
}
