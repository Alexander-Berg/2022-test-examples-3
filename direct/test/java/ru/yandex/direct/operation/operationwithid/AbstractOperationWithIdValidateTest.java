package ru.yandex.direct.operation.operationwithid;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class AbstractOperationWithIdValidateTest extends BaseAbstractOperationWithIdTest {

    // вызов метода validate() со списком идентификаторов

    @Test
    @SuppressWarnings("unchecked")
    public void prepare_TwoIds_CallsMethodValidateModelIdsWithTwoPrePassedIds() {
        twoValidIds();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        operation.prepare();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(operation).validate(argumentCaptor.capture());

        List<Long> actualIds = (List<Long>) argumentCaptor.getValue();
        assertThat(actualIds, contains(retargetingId1, retargetingId2));
    }

    // возврат результата из метода prepare()

    @Test
    public void prepare_PartialYes_OneValidId_ReturnsEmptyOptional() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OneIdWithWarning_ReturnsEmptyOptional() {
        oneIdWithWarning();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_OneInvalidId_ReturnsResultWithError() {
        oneInvalidIdOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertTrue(resultOptional.isPresent());

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialYes_TwoValidIds_ReturnsEmptyOptional() {
        twoValidIds();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialYes_TwoInvalidIds_ReturnsResultWithErrors() {
        twoInvalidIdsOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertTrue(resultOptional.isPresent());

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialYes_OneValidIdAndOneWithInvalidId_ReturnsEmptyOptional() {
        oneValidIdAndOneInvalidIdOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneValidId_ReturnsEmptyOptional() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneIdWithWarning_ReturnsEmptyOptional() {
        oneIdWithWarning();
        TestableOperationWithId operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_TwoValidIds_ReturnsEmptyOptional() {
        twoValidIds();
        TestableOperationWithId operation = createOperation(Applicability.FULL);
        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertThat(resultOptional.isPresent(), is(false));
    }

    @Test
    public void prepare_PartialNo_OneInvalidId_ReturnsResultWithError() {
        oneInvalidIdOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertTrue(resultOptional.isPresent());

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
    }

    @Test
    public void prepare_PartialNo_TwoInvalidIds_ReturnsResultWithErrors() {
        twoInvalidIdsOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertTrue(resultOptional.isPresent());

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(0));
        assertThat(validationResult, errorMatcher(1));
    }

    @Test
    public void prepare_PartialNo_OneValidIdAndOneInvalidId_ReturnsResultWithError() {
        oneValidIdAndOneInvalidIdOnValidation();
        TestableOperationWithId operation = createOperation(Applicability.FULL);

        Optional<MassResult<Long>> resultOptional = operation.prepare();
        assertTrue(resultOptional.isPresent());

        ValidationResult<?, Defect> validationResult = resultOptional.get().getValidationResult();
        assertThat(validationResult, errorMatcher(1));
    }


}
