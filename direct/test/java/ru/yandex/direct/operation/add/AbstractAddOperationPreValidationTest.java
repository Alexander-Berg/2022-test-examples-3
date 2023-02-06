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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class AbstractAddOperationPreValidationTest extends BaseAbstractAddOperationTest {

    // вызов метода preValidate() со списком моделей

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoObjects_CallsMethodPreValidateWithTwoPrePassedObjects() {
        twoValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        operation.prepare();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(operation).preValidate(argumentCaptor.capture());

        List<AdGroup> actualAdGroups = (List<AdGroup>) argumentCaptor.getValue();
        assertThat(actualAdGroups, contains(sameInstance(adGroup1), sameInstance(adGroup2)));
    }

    // возврат результата из метода prepare()

    @Test
    public void prepare_PartialYes_OnePreValidObject_ReturnsEmptyOptional() {
        onePreValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OneObjectWithPreWarning_ReturnsEmptyOptional() {
        oneObjectWithPreWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_TwoPreValidObjects_ReturnsEmptyOptional() {
        twoPreValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OnePreInvalidObject_ReturnsResultWithError() {
        oneObjectWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialYes_TwoPreInvalidObjects_ReturnsResultWithErrors() {
        twoObjectsWithPreErrors();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialYes_OnePreValidObjectAndOneWithPreError_ReturnsEmptyOptional() {
        onePreValidObjectAndOneWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OnePreValidObject_ReturnsEmptyOptional() {
        onePreValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneObjectWithPreWarning_ReturnsEmptyOptional() {
        oneObjectWithPreWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_TwoPreValidObjects_ReturnsEmptyOptional() {
        twoPreValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OnePreInvalidObject_ReturnsResultWithError() {
        oneObjectWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialNo_TwoPreInvalidObjects_ReturnsResultWithErrors() {
        twoObjectsWithPreErrors();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialNo_OnePreValidObjectAndOneWithPreError_ReturnsResultWithError() {
        onePreValidObjectAndOneWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(true));

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(1));
    }

    // условие вызова onPreValidated()

    @Test
    public void prepare_OnePreValidObject_CallsMethodOnPreValidate() {
        onePreValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        verify(operation).onPreValidated(any());
    }

    @Test
    public void prepare_OneObjectWithPreWarning_CallsMethodOnPreValidate() {
        oneObjectWithPreWarning();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        verify(operation).onPreValidated(any());
    }

    @Test
    public void prepare_OneObjectWithPreError_DoesNotCallMethodOnPreValidate() {
        oneObjectWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(resultOptional.isPresent(), "prepare() в данном случае должен возвращать результат");

        verify(operation, times(0)).onPreValidated(any());
    }

    // доступность и валидность данных в коллбэке onPreValidated

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoPreValidObjects_CallsMethodOnPreValidateWithValidValidationResult() {
        twoPreValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsPreValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsPreValidatedStep.class);
        verify(operation).onPreValidated(argumentCaptor.capture());

        ModelsPreValidatedStep<AdGroup> modelsPreValidatedStep = argumentCaptor.getValue();
        ValidationResult<List<AdGroup>, Defect> validationResult =
                modelsPreValidatedStep.getValidationResult();

        assertThat("результат валидации должен содержать список входных объектов в правильном порядке",
                validationResult.getValue(),
                contains(sameInstance(adGroup1), sameInstance(adGroup2)));

        assertThat("результат валидации списка объектов должен содержать в саб-результате [0] "
                        + "при успешной валидации пустое значение",
                validationResult.getSubResults().get(index(0)),
                nullValue());
        assertThat("результат валидации списка объектов должен содержать в саб-результате [1] "
                        + "при успешной валидации пустое значение",
                validationResult.getSubResults().get(index(1)),
                nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_OnePreValidAndOnePreInvalidObject_CallsMethodOnPreValidateWithValidValidationResult() {
        onePreValidObjectAndOneWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsPreValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsPreValidatedStep.class);
        verify(operation).onPreValidated(argumentCaptor.capture());

        ModelsPreValidatedStep<AdGroup> modelsPreValidatedStep = argumentCaptor.getValue();
        ValidationResult<List<AdGroup>, Defect> validationResult =
                modelsPreValidatedStep.getValidationResult();

        assertThat("результат валидации должен содержать список входных объектов в правильном порядке",
                validationResult.getValue(),
                contains(sameInstance(adGroup1), sameInstance(adGroup2)));

        assertThat("результат валидации списка объектов должен содержать в саб-результате [0] "
                        + "при успешной валидации пустое значение",
                validationResult.getSubResults().get(index(0)),
                nullValue());
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
    public void prepare_TwoPreValidObjects_CallsMethodOnPreValidateWithValidMapOfValidModels() {
        twoPreValidObjects();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsPreValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsPreValidatedStep.class);
        verify(operation).onPreValidated(argumentCaptor.capture());

        ModelsPreValidatedStep<AdGroup> modelsPreValidatedStep = argumentCaptor.getValue();
        Map<Integer, AdGroup> validModelsMap = modelsPreValidatedStep.getPreValidModelsMap();

        assertThat(validModelsMap.keySet(), hasSize(2));
        assertThat(validModelsMap.get(0), sameInstance(adGroup1));
        assertThat(validModelsMap.get(1), sameInstance(adGroup2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_OnePreValidAndOnePreInvalidObject_CallsMethodOnPreValidateWithValidMapOfValidModels() {
        onePreValidObjectAndOneWithPreError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        checkState(!resultOptional.isPresent(), "prepare() в данном случае не должен возвращать результат");

        ArgumentCaptor<ModelsPreValidatedStep<AdGroup>> argumentCaptor =
                ArgumentCaptor.forClass(ModelsPreValidatedStep.class);
        verify(operation).onPreValidated(argumentCaptor.capture());

        ModelsPreValidatedStep<AdGroup> modelsPreValidatedStep = argumentCaptor.getValue();
        Map<Integer, AdGroup> validModelsMap = modelsPreValidatedStep.getPreValidModelsMap();

        assertThat(validModelsMap.keySet(), hasSize(1));
        assertThat(validModelsMap.get(0), sameInstance(adGroup1));
    }
}
