package ru.yandex.direct.operation.update;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.validation.result.PathHelper.index;

/**
 * Валидация моделей с примененными к ним изменениями.
 * <p>
 * Проверяется, что в валидацию моделей передается правильный ValidationResult,
 * построенный из результата валидации ModelChanges и загруженных моделей.
 */
@SuppressWarnings("unchecked")
public class AbstractUpdateOperationValidationTest extends BaseAbstractUpdateOperationTest {

    // вызов шаблонного метода validateAppliedChanges с правильным параметром ValidationResult

    @Test
    public void prepare_PreValidationHasOneInvalidItem_CallsValidateAppliedChangesWithValidValidationResult() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ValidationResult> argumentCaptor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(updateOperation).validateAppliedChanges(argumentCaptor.capture());

        ValidationResult<List<RetargetingCondition>, Defect> validationResult =
                (ValidationResult<List<RetargetingCondition>, Defect>) argumentCaptor.getValue();

        assertThat("размер списка моделей в ValidationResult "
                        + "должен соответствовать размеру входного списка ModelChanges",
                validationResult.getValue(), hasSize(modelChangesList.size()));


        RetargetingCondition validationResultItem1 = validationResult.getValue().get(0);
        RetargetingCondition validationResultItem2 = validationResult.getValue().get(1);

        assertThat("в ValidationResult первый элемент списка (для которого не прошла валидация ModelChanges) - "
                        + "должен быть заглушкой в виде пустой модели с выставленным id",
                validationResultItem1.getId().equals(retCond1.getId()) &&
                        validationResultItem1.getRules() == null, is(true));
        assertThat("в ValidationResult должна присутствовать ошибка для элемента, "
                        + "для которого не прошла валидация ModelChanges",
                validationResult.getSubResults().get(index(0)).flattenErrors(),
                hasSize(1));

        assertThat("в ValidationResult второй элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть моделью с примененными к ней изменениями",
                validationResultItem2,
                beanDiffer(createRetCond2().withDescription(NEW_DESCRIPTION)));
        assertThat("в ValidationResult второй элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть тем же инстансом модели, которая вернулась из getModels",
                validationResultItem2,
                sameInstance(retCond2));
        assertThat("в ValidationResult не должно присутствовать ошибкок для второго элемента",
                validationResult.getSubResults().get(index(1)) == null
                        || validationResult.getSubResults().get(index(1)).flattenErrors().size() == 0,
                is(true));
    }

    @Test
    public void prepare_PreValidationIsFullyValid_CallsValidateAppliedChangesWithValidValidationResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<ValidationResult> argumentCaptor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(updateOperation).validateAppliedChanges(argumentCaptor.capture());

        ValidationResult<List<RetargetingCondition>, Defect> validationResult =
                (ValidationResult<List<RetargetingCondition>, Defect>) argumentCaptor.getValue();

        assertThat("размер списка моделей в ValidationResult "
                        + "должен соответствовать размеру входного списка ModelChanges",
                validationResult.getValue(), hasSize(modelChangesList.size()));


        RetargetingCondition validationResultItem1 = validationResult.getValue().get(0);
        RetargetingCondition validationResultItem2 = validationResult.getValue().get(1);

        assertThat("в ValidationResult первый элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть моделью с примененными к ней изменениями",
                validationResultItem1,
                beanDiffer(createRetCond1().withName(NEW_NAME)));
        assertThat("в ValidationResult первый элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть тем же инстансом модели, которая вернулась из getModels",
                validationResultItem1,
                sameInstance(retCond1));
        assertThat("в ValidationResult не должно присутствовать ошибкок для первого элемента",
                validationResult.getSubResults().get(index(0)) == null
                        || validationResult.getSubResults().get(index(0)).flattenErrors().size() == 0,
                is(true));

        assertThat("в ValidationResult второй элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть моделью с примененными к ней изменениями",
                validationResultItem2,
                beanDiffer(createRetCond2().withDescription(NEW_DESCRIPTION)));
        assertThat("в ValidationResult второй элемент списка (для которого успешно прошла валидация ModelChanges) - "
                        + "должен быть тем же инстансом модели, которая вернулась из getModels",
                validationResultItem2,
                sameInstance(retCond2));
        assertThat("в ValidationResult не должно присутствовать ошибкок для второго элемента",
                validationResult.getSubResults().get(index(1)) == null
                        || validationResult.getSubResults().get(index(1)).flattenErrors().size() == 0,
                is(true));
    }

    // возвращение результата из prepare() в случае провальной валидации (Applicability.FULL)

    @Test(expected = IllegalStateException.class)
    public void prepare_PartialNo_ValidationHasTopLevelError_FailsWithException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithTopLevelError();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();
    }

    @Test
    public void prepare_PartialNo_ValidationHasInvalidItem_ReturnsValidResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        checkPrepareReturnsResultWithItemsErrors(Applicability.FULL, false, true);
    }

    @Test
    public void prepare_PartialNo_ValidationHasAllInvalidItems_ReturnsValidResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithAllInvalidItems();
        checkPrepareReturnsResultWithItemsErrors(Applicability.FULL, true, true);
    }

    // возвращение результата из prepare() в случае провальной валидации (Applicability.PARTIAL)

    @Test(expected = IllegalStateException.class)
    public void prepare_PartialYes_ValidationHasTopLevelError_FailsWithException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithTopLevelError();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
    }

    @Test
    public void prepare_PartialYes_ValidationHasAllInvalidItems_ReturnsValidResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithAllInvalidItems();
        checkPrepareReturnsResultWithItemsErrors(Applicability.PARTIAL, true, true);
    }

    @Test
    public void prepare_PartialYes_PreValidationAndValidationMarksAllItemsAsInvalid_ReturnsValidResult() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationWithInvalidSecondItem();
        checkPrepareReturnsResultWithItemsErrors(Applicability.PARTIAL, true, true);
    }

    // возвращение пустого Optional из prepare() в случае успешной валидации

    @Test
    public void prepare_PartialNo_ValidationIsFullyValid_ReturnsEmptyOptional() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkEmptyOptionalResult(Applicability.FULL);
    }

    @Test
    public void prepare_PartialYes_ValidationIsFullyValid_ReturnsEmptyOptional() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkEmptyOptionalResult(Applicability.PARTIAL);
    }

    @Test
    public void prepare_PartialYes_ValidationHasOneInvalidItem_ReturnsEmptyOptional() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        checkEmptyOptionalResult(Applicability.PARTIAL);
    }

    private void checkEmptyOptionalResult(Applicability applicability) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        Optional<MassResult<Long>> resultOptional = updateOperation.prepare();

        assertThat(resultOptional.isPresent(), is(false));
    }

    // вызов метода onAppliedChangesValidated в зависимости от результатов валидации (Applicability.FULL)

    @Test
    public void prepare_PartialNo_ValidationIsFullyValid_CallsOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkOnAppliedChangesValidatedCall(Applicability.FULL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_ValidationHasInvalidItem_DoesNotCallOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        checkOnAppliedChangesValidatedCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_ValidationHasAllInvalidItems_DoesNotCallOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithAllInvalidItems();
        checkOnAppliedChangesValidatedCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    // вызов метода onAppliedChangesValidated в зависимости от результатов валидации (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_ValidationIsFullyValid_CallsOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkOnAppliedChangesValidatedCall(Applicability.PARTIAL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_ValidationHasInvalidItem_CallsOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        checkOnAppliedChangesValidatedCall(Applicability.PARTIAL, MUST_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_ValidationHasAllInvalidItems_DoesNotCallOnAppliedChangesValidated() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithAllInvalidItems();
        checkOnAppliedChangesValidatedCall(Applicability.PARTIAL, MUST_NOT_BE_CALLED);
    }

    private void checkOnAppliedChangesValidatedCall(Applicability applicability, boolean mustBeCalled) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        updateOperation.prepare();

        verify(updateOperation, mustBeCalled ? times(1) : never()).onAppliedChangesValidated(any());
    }

    // доступность и правильность данных, доступных в коллбэке onAppliedChangesValidated (Applicability.FULL)

    @Test
    public void prepare_PartialNo_ValidationIsFullyValid_AppliedChangesValidationResultIsAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        ValidationResult<List<RetargetingCondition>, Defect> validationResult =
                captor.getValue().getValidationResult();

        assertThat("результат валидации примененных изменений должен содержать те же инстансы моделей, "
                        + "которые вернул метод getModels",
                validationResult.getValue(),
                contains(sameInstance(retCond1), sameInstance(retCond2)));
    }

    @Test
    public void prepare_PartialNo_ValidationIsFullyValid_ValidAppliedChangesIsAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.FULL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        Collection<AppliedChanges<RetargetingCondition>> validAppliedChanges =
                captor.getValue().getValidAppliedChanges();

        assertThat("коллекция валидных AppliedChanges должна содержать 2 объекта",
                validAppliedChanges, hasSize(2));

        Iterator<AppliedChanges<RetargetingCondition>> iterator = validAppliedChanges.iterator();
        AppliedChanges<RetargetingCondition> validAppliedChangesItem1 = iterator.next();
        AppliedChanges<RetargetingCondition> validAppliedChangesItem2 = iterator.next();
        assertThat("коллекция валидных AppliedChanges должна содержать объект для первого условия ретаргетинга",
                validAppliedChangesItem1.getModel(),
                sameInstance(retCond1));
        assertThat("коллекция валидных AppliedChanges должна содержать объект для второго условия ретаргетинга",
                validAppliedChangesItem2.getModel(),
                sameInstance(retCond2));
    }

    // доступность и правильность данных, доступных в коллбэке onAppliedChangesValidated (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_AppliedChangesValidationResultIsAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        ValidationResult<List<RetargetingCondition>, Defect> validationResult =
                captor.getValue().getValidationResult();

        RetargetingCondition item1 = validationResult.getValue().get(0);
        assertThat("первый элемент результата валидации (ModelChanges для него не валидны) должен содержать "
                        + "пустую модель с выставленным id",
                retCond1.getId().equals(item1.getId()) && item1.getRules() == null,
                is(true));
        assertThat("второй элемент результата валидации (ModelChanges для него валидны) должен содержать "
                        + "инстанс модели, которую вернул метод getModels",
                validationResult.getValue().get(1),
                sameInstance(retCond2));
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_AppliedChangesValidationResultWithIndexIsAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        Map<Integer, AppliedChanges<RetargetingCondition>> validAppliedChangesWithIndex =
                captor.getValue().getValidAppliedChangesWithIndex();

        assertThat("мапа валидных AppliedChanges должна содержать 1 элемент",
                validAppliedChangesWithIndex.values(), hasSize(1));

        assertThat("мапа валидных AppliedChanges c индексами должна содержать запись для второго условия ретаргетинга",
                validAppliedChangesWithIndex,
                hasEntry(equalTo(1), hasProperty("model", sameInstance(retCond2))));
    }

    @Test
    public void prepare_PartialYes_ValidationHasOneInvalidItem_AppliedChangesValidationResultIsAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        ValidationResult<List<RetargetingCondition>, Defect> validationResult =
                captor.getValue().getValidationResult();

        assertThat("результат валидации примененных изменений должен содержать те же инстансы моделей, "
                        + "которые вернул метод getModels",
                validationResult.getValue(),
                contains(sameInstance(retCond1), sameInstance(retCond2)));
    }

    @Test
    public void prepare_PartialYes_ValidationHasOneInvalidItem_ValidAppliedChangesIsAvailable() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        Collection<AppliedChanges<RetargetingCondition>> validAppliedChanges =
                captor.getValue().getValidAppliedChanges();

        assertThat("коллекция валидных AppliedChanges должна содержать 1 объект",
                validAppliedChanges, hasSize(1));

        AppliedChanges<RetargetingCondition> validAppliedChangesItem = validAppliedChanges.iterator().next();
        assertThat("в коллекции валидных AppliedChanges должен быть объект для первого условия ретаргетинга",
                validAppliedChangesItem.getModel(), sameInstance(retCond1));
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_ValidAppliedChangesIsAvailable() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();

        ArgumentCaptor<AppliedChangesValidatedStep> captor = ArgumentCaptor.forClass(AppliedChangesValidatedStep.class);
        verify(updateOperation).onAppliedChangesValidated(captor.capture());

        Collection<AppliedChanges<RetargetingCondition>> validAppliedChanges =
                captor.getValue().getValidAppliedChanges();

        assertThat("коллекция валидных AppliedChanges должна содержать 1 объект",
                validAppliedChanges, hasSize(1));

        AppliedChanges<RetargetingCondition> validAppliedChangesItem = validAppliedChanges.iterator().next();
        assertThat("в коллекции валидных AppliedChanges должен быть объект для второго условия ретаргетинга",
                validAppliedChangesItem.getModel(), sameInstance(retCond2));
    }
}
