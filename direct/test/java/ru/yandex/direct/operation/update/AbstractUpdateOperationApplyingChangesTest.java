package ru.yandex.direct.operation.update;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Этап применения изменений.
 * <p>
 * После первого этапа валидации становится известно, какие из ModelChanges валидны.
 * Для них загружаются модели, и к этим моделям применяются изменения, эти примененные изменения
 * сохраняются в поле appliedChangesForValidModelChanges.
 * <p>
 * Здесь проверяется условный вызов метода getModels для загрузки моделей и список id, передаваемый в него,
 * а так же примененные к загруженным моделям изменения - appliedChangesForValidModelChanges.
 */
@SuppressWarnings("unchecked")
public class AbstractUpdateOperationApplyingChangesTest extends BaseAbstractUpdateOperationTest {

    // условный вызов метода getModels в зависимости от результата валидации списка modelChanges (Applicability.FULL)

    @Test
    public void prepare_PartialNo_PreValidationIsFullyValid_CallsMethodGetModelsWithValidIds() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.FULL, MUST_BE_CALLED, retCond1.getId(), retCond2.getId());
    }

    @Test
    public void prepare_PartialNo_PreValidationHasTopLevelError_DoesNotCallMethodGetModels() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasOneInvalidItem_DoesNotCallMethodGetModels() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialNo_PreValidationHasAllInvalidItems_DoesNotCallMethodGetModels() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.FULL, MUST_NOT_BE_CALLED);
    }

    // условный вызов метода getModels в зависимости от результата валидации списка modelChanges (Applicability.PARTIAL)

    @Test
    public void prepare_PartialYes_PreValidationIsFullyValid_CallsMethodGetModelsWithValidIds() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.PARTIAL, MUST_BE_CALLED, retCond1.getId(), retCond2.getId());
    }

    @Test
    public void prepare_PartialYes_PreValidationHasOneInvalidItem_CallsMethodGetModelsWithValidIds() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.PARTIAL, MUST_BE_CALLED, retCond2.getId());
    }

    @Test
    public void prepare_PartialYes_PreValidationHasTopLevelError_DoesNotCallMethodGetModels() {
        modelChangesValidationWithTopLevelError();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.PARTIAL, MUST_NOT_BE_CALLED);
    }

    @Test
    public void prepare_PartialYes_PreValidationHasAllInvalidItems_DoesNotCallMethodGetModels() {
        modelChangesValidationWithAllInvalidItems();
        appliedChangesValidationIsFullyValid();
        checkGetModelsCall(Applicability.PARTIAL, MUST_NOT_BE_CALLED);
    }

    private void checkGetModelsCall(Applicability applicability, boolean mustBeCalled, Long... ids) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        updateOperation.prepare();

        if (!mustBeCalled) {
            verify(updateOperation, never()).getModels(any());
        } else {
            ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
            verify(updateOperation).getModels(captor.capture());

            Collection<Long> actualIds = captor.getValue();
            assertThat("список id моделей, переданный в метод getModels не соответствует ожидаемому",
                    actualIds,
                    contains(ids));
        }
    }

    // проверка применения изменений к загруженным моделям

    @Test
    public void prepare_AllModelChangesAreValid_ChangesAreAppliedToBothItems() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationIsFullyValid();

        checkAppliedChangesViaCallbackOnChangesApplied(Applicability.FULL, true, true);
    }

    @Test
    public void prepare_OneModelChangesItemIsInvalid_ChangesAreAppliedToValidItem() {
        modelChangesValidationWithInvalidFirstItem();
        appliedChangesValidationIsFullyValid();

        checkAppliedChangesViaCallbackOnChangesApplied(Applicability.PARTIAL, false, true);
    }

    private void checkAppliedChangesViaCallbackOnChangesApplied(Applicability applicability, boolean firstItemValid,
                                                                boolean secondItemValid) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        updateOperation.prepare();

        ArgumentCaptor<ChangesAppliedStep> captor = ArgumentCaptor.forClass(ChangesAppliedStep.class);
        verify(updateOperation).onChangesApplied(captor.capture());

        ChangesAppliedStep<RetargetingCondition> changesAppliedStep =
                (ChangesAppliedStep<RetargetingCondition>) captor.getValue();
        Collection<AppliedChanges<RetargetingCondition>> appliedChanges =
                changesAppliedStep.getAppliedChangesForValidModelChanges();

        int expectedSize = 0;
        if (firstItemValid) {
            expectedSize++;
        }
        if (secondItemValid) {
            expectedSize++;
        }

        assertThat("размер списка appliedChangesForValidModelChanges должен совпадать "
                        + "с количеством успешно провалидированных ModelChanges",
                appliedChanges, hasSize(expectedSize));

        Iterator<AppliedChanges<RetargetingCondition>> iterator = appliedChanges.iterator();
        if (firstItemValid) {
            AppliedChanges<RetargetingCondition> firstItemAppliedChanges = iterator.next();
            checkFirstItemAppliedChanges(firstItemAppliedChanges);
        }
        if (secondItemValid) {
            AppliedChanges<RetargetingCondition> secondItemAppliedChanges = iterator.next();
            checkSecondItemAppliedChanges(secondItemAppliedChanges);
        }
    }

    private void checkFirstItemAppliedChanges(AppliedChanges<RetargetingCondition> appliedChanges) {
        assertThat("в проверяемом appliedChanges должен быть инстанс первого объекта",
                appliedChanges.getModel(), sameInstance(retCond1));
        assertThat("в проверяемом appliedChanges должен быть соответствующий объект с примененными к нему изменениями",
                appliedChanges.getModel(), beanDiffer(createRetCond1().withName(NEW_NAME)));
        assertThat("в appliedChanges первого объекта должно быть зарегистрировано изменение поля name",
                appliedChanges.changed(RetargetingCondition.NAME), is(true));
        assertThat("в appliedChanges первого объекта должно быть зарегистрировано изменение одного поля",
                appliedChanges.getActuallyChangedProps(), hasSize(1));
    }

    private void checkSecondItemAppliedChanges(AppliedChanges<RetargetingCondition> appliedChanges) {
        assertThat("в проверяемом appliedChanges должен быть инстанс второго объекта",
                appliedChanges.getModel(), sameInstance(retCond2));
        assertThat("в проверяемом appliedChanges должен быть соответствующий объект с примененными к нему изменениями",
                appliedChanges.getModel(), beanDiffer(createRetCond2().withDescription(NEW_DESCRIPTION)));
        assertThat("в appliedChanges второго объекта должно быть зарегистрировано изменение поля description",
                appliedChanges.changed(RetargetingCondition.DESCRIPTION), is(true));
        assertThat("в appliedChanges второго объекта должно быть зарегистрировано изменение одного поля",
                appliedChanges.getActuallyChangedProps(), hasSize(1));
    }
}
