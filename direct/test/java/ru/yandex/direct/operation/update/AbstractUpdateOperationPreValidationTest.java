package ru.yandex.direct.operation.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.validation.result.PathHelper.index;

/**
 * Первым делом в AbstractUpdateOperation валидируется входной список объектов ModelChanges.
 * В зависимости от результата либо сразу возвращается результат из метода prepare(),
 * либо вызываются коллбэки, которым через параметры становятся доступны поля
 * modelChangesValidationResult и validModelChanges. Здесь проверяется правильность
 * вызова коллбэка onModelChangesValidated в различных ситуациях, правильность данных,
 * доступных в коллбэке, правильность вызова getModels с правильным списком id моделей,
 * а так же возвращение результата в случае провальной валидации.
 */
@SuppressWarnings("unchecked")
public class AbstractUpdateOperationPreValidationTest extends BaseAbstractUpdateOperationTest {

    // неправильный порядок вызова метода prepare

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledTwice_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        updateOperation.prepare();
    }

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledAfterApply_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
        updateOperation.apply();
        updateOperation.prepare();
    }

    // этап валидации списка ModelChanges

    @Test
    public void prepare_CallsPreValidationWithSourceModelChanges() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(updateOperation).validateModelChanges(captor.capture());

        List<ModelChanges<RetargetingCondition>> actualModelChanges =
                (List<ModelChanges<RetargetingCondition>>) captor.getValue();
        assertThat("в метод validateModelChanges должны быть переданы входные ModelChanges",
                actualModelChanges,
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
    }

    // возвращение результата из метода prepare() при провальной валидации (Applicability.FULL)

    @Test
    public void prepare_PartialNo_PreValidationHasTopLevelError_ReturnsValidResult() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkPrepareReturnsResultWithGlobalError(Applicability.FULL);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasOneInvalidItem_ReturnsValidResult() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        checkPrepareReturnsResultWithItemsErrors(Applicability.FULL, true, false);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasAllInvalidItems_ReturnsValidResult() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkPrepareReturnsResultWithItemsErrors(Applicability.FULL, true, true);
    }

    // возвращение результата из метода prepare() при провальной валидации (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_PreValidationHasTopLevelError_ReturnsValidResult() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkPrepareReturnsResultWithGlobalError(Applicability.FULL);
    }

    @Test
    public void prepare_PartialYes_PreValidationHasAllInvalidItems_ReturnsValidResult() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkPrepareReturnsResultWithItemsErrors(Applicability.FULL, true, true);
    }

    // вызов метода onModelChangesValidated в зависимости
    // от результата валидации списка ModelChanges (Applicability.FULL)

    @Test
    public void prepare_PartialNo_PreValidationIsFullyValid_CallsMethodOnModelChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.FULL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasTopLevelError_DoesNotCallMethodOnModelChangesValidated() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasOneInvalidItem_DoesNotCallMethodOnModelChangesValidated() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasAllInvalidItems_DoesNotCallMethodOnModelChangesValidated() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    // вызов метода onModelChangesValidated в зависимости
    // от результата валидации списка ModelChanges (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_PreValidationIsFullyValid_CallsMethodOnModelChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.PARTIAL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_CallsMethodOnModelChangesValidated() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.PARTIAL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_PreValidationHasTopLevelError_DoesNotCallMethodOnModelChangesValidated() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.PARTIAL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_PreValidationHasAllInvalidItems_DoesNotCallMethodOnModelChangesValidated() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkOnModelChangesValidatedCall(Applicability.PARTIAL, MUST_NOT_BE_CALLED);
    }

    private void checkOnModelChangesValidatedCall(Applicability applicability, boolean mustBeCalled) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        updateOperation.prepare();

        verify(updateOperation, mustBeCalled ? times(1) : never()).onModelChangesValidated(any());
    }

    // доступность и правильность данных, доступных в коллбэке onModelChangesValidated (Applicability.FULL)

    @Test
    public void prepare_PartialNo_PreValidationIsFullyValid_ModelChangesAreAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        List<ModelChanges<RetargetingCondition>> actualModelChanges =
                captor.getValue().getModelChanges();
        assertThat("список ModelChanges должен содержать исходные объекты ModelChanges",
                actualModelChanges,
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
    }

    @Test
    public void prepare_PartialNo_PreValidationIsFullyValid_ModelChangesValidationResultIsAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actualValidationResult =
                captor.getValue().getModelChangesValidationResult();
        assertThat("результат валидации ModelChanges должен содержать исходные ModelChanges",
                actualValidationResult.getValue(),
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
    }

    @Test
    public void prepare_PartialNo_PreValidationIsFullyValid_ValidModelChangesAreAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        Collection<ModelChanges<RetargetingCondition>> actualValidModelChanges =
                captor.getValue().getValidModelChanges();
        assertThat("список валидных ModelChanges не соответствует ожидаемому",
                actualValidModelChanges,
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
    }

    // доступность и правильность данных, доступных в коллбэке onModelChangesValidated (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_ModelChangesAreAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        List<ModelChanges<RetargetingCondition>> actualModelChanges =
                captor.getValue().getModelChanges();
        assertThat("список ModelChanges должен содержать исходные ModelChanges",
                actualModelChanges,
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_ValidationResultIsAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> actualValidationResult =
                captor.getValue().getModelChangesValidationResult();
        assertThat("результат валидации ModelChanges должен содержать исходные ModelChanges",
                actualValidationResult.getValue(),
                contains(sameInstance(retCond1Changes), sameInstance(retCond2Changes)));
        assertThat("результат валидации ModelChanges должен содержать ошибку в первом элементе",
                actualValidationResult.getSubResults().get(index(0)).getErrors(),
                hasSize(1));
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_ValidModelChangesAreAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        Collection<ModelChanges<RetargetingCondition>> actualValidModelChanges =
                captor.getValue().getValidModelChanges();
        assertThat("список валидных ModelChanges не соответствует ожидаемому",
                actualValidModelChanges,
                contains(sameInstance(retCond2Changes)));
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_ValidModelChangesWithIndexAreAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ModelChangesValidatedStep> captor = ArgumentCaptor.forClass(ModelChangesValidatedStep.class);
        verify(updateOperation).onModelChangesValidated(captor.capture());

        Map<Integer, ModelChanges<RetargetingCondition>> actualValidModelChanges =
                captor.getValue().getValidModelChangesWithIndex();
        assertThat("мапа валидных ModelChanges и индексами содержит только один элемент",
                actualValidModelChanges.size(), equalTo(1));
        assertThat("мапа валидных ModelChanges и индексами не соответствует ожидаемому",
                actualValidModelChanges,
                hasEntry(equalTo(1), sameInstance(retCond2Changes)));
    }
}
