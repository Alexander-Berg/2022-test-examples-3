package ru.yandex.direct.operation.operationwithid;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

public class AbstractOperationWithIdExecutionTest extends BaseAbstractOperationWithIdTest {

    @Test
    public void apply_OneValidItem_CallsExecuteWithValidObject() {
        oneValidId();
        List<Long> validIds = callApplyAndExtractModelsPassedToExecute();
        assertThat(validIds, contains(retargetingId1));
    }

    @Test
    public void apply_OneItemWithWarning_CallsExecuteWithValidObject() {
        oneIdWithWarning();
        List<Long> validIds = callApplyAndExtractModelsPassedToExecute();
        assertThat(validIds, contains(retargetingId1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_TwoValidItems_CallsExecuteWithValidObjects() {
        twoValidIds();
        List<Long> validIds = callApplyAndExtractModelsPassedToExecute();
        assertThat(validIds, contains(retargetingId1, retargetingId2));
    }

    @Test
    public void apply_OneValidAndOneInvalidItem_CallsExecuteWithValidObjects() {
        oneValidIdAndOneInvalidIdOnValidation();
        List<Long> validIds = callApplyAndExtractModelsPassedToExecute();
        assertThat(validIds, contains(retargetingId1));
    }

    @Test
    public void apply_FirstInvalidAndSecondValidItem_CallsExecuteWithValidObjects() {
        firstWithErrorSecondValid();
        List<Long> validIds = callApplyAndExtractModelsPassedToExecute();
        assertThat(validIds, contains(retargetingId2));
    }

    private List<Long> callApplyAndExtractModelsPassedToExecute() {
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        operation.apply();
        verify(operation).execute(argumentCaptor.capture());

        return argumentCaptor.getValue();
    }

    @Test
    public void apply_PartialYes_OneValidItem_ReturnsValidResult() {
        oneValidId();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> result = operation.apply();
        assertThat(result, isSuccessfulWithItems(retargetingId1));
    }

    @Test
    public void apply_PartialYes_TwoValidItems_ReturnsValidResult() {
        twoValidIds();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> result = operation.apply();
        assertThat(result, isSuccessfulWithItems(retargetingId1, retargetingId2));
    }

    @Test
    public void apply_PartialYes_OneValidItemAndOneWithError_ReturnsValidResult() {
        oneValidIdAndOneInvalidIdOnValidation();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> result = operation.apply();
        assertThat(result, isSuccessfulWithMatchers(equalTo(retargetingId1), null));
    }

    @Test
    public void apply_PartialYes_OneValidItem_CallsMethodBeforeExecution() {
        oneValidId();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        operation.apply();

        verify(operation).beforeExecution(any());
    }

    @Test
    public void apply_PartialYes_OneValidItem_CallsMethodAfterExecution() {
        oneValidId();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        operation.apply();

        verify(operation).afterExecution(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_PartialYes_OneValidItemAndOneWithError_CallsMethodOnExecutedWithValidItemOnly() {
        oneValidIdAndOneInvalidIdOnValidation();

        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        operation.apply();

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(operation).afterExecution(argumentCaptor.capture());

        List<Long> actualValidItems = (List<Long>) argumentCaptor.getValue();
        assertThat(actualValidItems, hasSize(1));
        assertThat(actualValidItems.get(0), is(retargetingId1));
    }
}
