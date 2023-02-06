package ru.yandex.direct.operation.add;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class AbstractAddOperationValidationTest extends BaseAbstractAddOperationTest {

    // вызов метода validate() с результатом превалидации

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoObjects_CallsMethodValidateWithTwoPassedObjects() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();

        ArgumentCaptor<ValidationResult> argumentCaptor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(operation).validate(argumentCaptor.capture());

        ValidationResult<List<AdGroup>, Defect> preValidationResult = argumentCaptor.getValue();

        assertThat("размер списка моделей в ValidationResult "
                        + "должен соответствовать размеру входного списка models",
                preValidationResult.getValue(), hasSize(adGroups.size()));

        AdGroup validationResultItem1 = preValidationResult.getValue().get(0);
        AdGroup validationResultItem2 = preValidationResult.getValue().get(1);

        assertThat("в ValidationResult первый элемент списка (для которого успешно прошла предварительная валидация) - "
                        + "должен быть тем же инстансом модели, который был передан в валидацию",
                validationResultItem1,
                sameInstance(adGroup1));
        assertThat("в ValidationResult не должно присутствовать ошибкок для первого элемента",
                preValidationResult.getSubResults().get(index(0)).flattenErrors().size() == 0,
                is(true));

        assertThat("в ValidationResult второй элемент списка (для которого успешно прошла валидация) - "
                        + "должен быть тем же инстансом модели, который был передан в валидаци",
                validationResultItem2,
                sameInstance(adGroup2));
        assertThat("в ValidationResult не должно присутствовать ошибкок для второго элемента",
                preValidationResult.getSubResults().get(index(1)).flattenErrors().size() == 0,
                is(true));
    }

    // возврат результата из метода prepare()

    @Test
    public void prepare_PartialYes_OneValidObject_ReturnsEmptyOptional() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OneObjectWithWarning_ReturnsEmptyOptional() {
        oneObjectWithWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_TwoValidObjects_ReturnsEmptyOptional() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OneInvalidObject_ReturnsResultWithError() {
        oneObjectWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialYes_TwoInvalidObjects_ReturnsResultWithErrors() {
        twoObjectsWithErrors();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialYes_OneValidObjectAndOneWithError_ReturnsEmptyOptional() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneValidObject_ReturnsEmptyOptional() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneObjectWithWarning_ReturnsEmptyOptional() {
        oneObjectWithWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_TwoValidObjects_ReturnsEmptyOptional() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneInvalidObject_ReturnsResultWithError() {
        oneObjectWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialNo_TwoInvalidObjects_ReturnsResultWithErrors() {
        twoObjectsWithErrors();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialNo_OneValidObjectAndOneWithError_ReturnsResultWithError() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(1));
    }

    // условие вызова prepare()

    @Test
    public void prepare_OneValidObject_CallsMethodOnModelsValidated() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        verify(operation).onModelsValidated(any());
    }

    @Test
    public void prepare_OneObjectWithWarning_CallsMethodOnModelsValidated() {
        oneObjectWithWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        verify(operation).onModelsValidated(any());
    }

    @Test
    public void prepare_OneObjectWithError_DoesNotCallMethodOnModelsValidated() {
        oneObjectWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(resultOptional.isPresent(), "prepare() в данном случае должен возвращать результат");

        verify(operation, times(0)).onModelsValidated(any());
    }

    // доступность и валидность данных в коллбэке onModelsValidated

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoValidObjects_CallsMethodOnModelsValidatedWithValidValidationResult() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsValidatedStep.class);
        verify(operation).onModelsValidated(argumentCaptor.capture());

        ModelsValidatedStep<AdGroup> modelsValidatedStep = argumentCaptor.getValue();
        ValidationResult<List<AdGroup>, Defect> validationResult = modelsValidatedStep.getValidationResult();

        assertThat("результат валидации должен содержать список входных объектов в правильном порядке",
                validationResult.getValue(),
                contains(sameInstance(adGroup1), sameInstance(adGroup2)));

        assertThat("результат валидации списка объектов должен содержать в саб-результате [0] "
                        + "результат валидации первого объекта",
                validationResult.getSubResults().get(index(0)).getValue(),
                sameInstance(adGroup1));
        assertThat("результат валидации списка объектов должен содержать в саб-результате [1] "
                        + "результат валидации второго объекта",
                validationResult.getSubResults().get(index(1)).getValue(),
                sameInstance(adGroup2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_OneValidAndOneInvalidObject_CallsMethodOnModelsValidatedWithValidValidationResult() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsValidatedStep.class);
        verify(operation).onModelsValidated(argumentCaptor.capture());

        ModelsValidatedStep<AdGroup> modelsValidatedStep = argumentCaptor.getValue();
        ValidationResult<List<AdGroup>, Defect> validationResult = modelsValidatedStep.getValidationResult();

        assertThat("результат валидации должен содержать список входных объектов в правильном порядке",
                validationResult.getValue(),
                contains(sameInstance(adGroup1), sameInstance(adGroup2)));

        assertThat("результат валидации списка объектов должен содержать в саб-результате [0] "
                        + "результат валидации первого объекта",
                validationResult.getSubResults().get(index(0)).getValue(),
                sameInstance(adGroup1));
        assertThat("результат валидации списка объектов должен содержать в саб-результате [1] "
                        + "результат валидации второго объекта",
                validationResult.getSubResults().get(index(1)).getValue(),
                sameInstance(adGroup2));

        assertThat("результат валидации должен содержать ошибку для второго объекта",
                validationResult,
                errorMatcher(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoValidObjects_CallsMethodOnModelsValidatedWithValidMapOfValidModels() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsValidatedStep.class);
        verify(operation).onModelsValidated(argumentCaptor.capture());

        ModelsValidatedStep<AdGroup> modelsValidatedStep = argumentCaptor.getValue();
        Map<Integer, AdGroup> validModelsMap = modelsValidatedStep.getValidModelsMap();

        assertThat(validModelsMap.keySet(), hasSize(2));
        assertThat(validModelsMap.get(0), sameInstance(adGroup1));
        assertThat(validModelsMap.get(1), sameInstance(adGroup2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_OneValidAndOneInvalidObject_CallsMethodOnModelsValidatedWithValidMapOfValidModels() {
        oneValidObjectAndOneWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsValidatedStep.class);
        verify(operation).onModelsValidated(argumentCaptor.capture());

        ModelsValidatedStep<AdGroup> modelsValidatedStep = argumentCaptor.getValue();
        Map<Integer, AdGroup> validModelsMap = modelsValidatedStep.getValidModelsMap();

        assertThat(validModelsMap.keySet(), hasSize(1));
        assertThat(validModelsMap.get(0), sameInstance(adGroup1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_OneValidAndOnePreInvalidAndOneInvalidObject_CallsMethodOnModelsValidatedWithValidMapOfValidModels() {
        oneObjectWithPreErrorAndOneWithErrorAndOneValid();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsValidatedStep.class);
        verify(operation).onModelsValidated(argumentCaptor.capture());

        ModelsValidatedStep<AdGroup> modelsValidatedStep = argumentCaptor.getValue();
        Map<Integer, AdGroup> validModelsMap = modelsValidatedStep.getValidModelsMap();

        assertThat(validModelsMap.keySet(), hasSize(1));
        assertThat(validModelsMap.get(2), sameInstance(adGroup3));
    }
}
