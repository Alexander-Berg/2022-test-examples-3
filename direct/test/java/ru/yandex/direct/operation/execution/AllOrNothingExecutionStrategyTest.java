package ru.yandex.direct.operation.execution;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.operation.Operation;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AllOrNothingExecutionStrategyTest {
    private Operation<Object> operation1;
    private Operation<Object> operation2;

    @Before
    public void setUp() throws Exception {
        operation1 = createOperationMock();
        operation2 = createOperationMock();
    }

    @Test
    public void executeForValidNonPartialOperationsReturnsTrue() {
        operationDoesNotReturnResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        boolean executed = new AllOrNothingExecutionStrategy(false)
                .execute(asList(operation1, operation2));
        assertThat(executed).isTrue();
    }

    @Test
    public void executeForValidPartialOperationsReturnsTrue() {
        operationDoesNotReturnResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        boolean executed = new AllOrNothingExecutionStrategy(false, true)
                .execute(asList(operation1, operation2));
        assertThat(executed).isTrue();
    }

    @Test
    public void executeForValidNonPartialOperationsCallApplyOnAllOperations() {
        operationDoesNotReturnResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(false)
                .execute(asList(operation1, operation2));
        verify(operation1).apply();
        verify(operation2).apply();
    }

    @Test
    public void executeForSemiValidNonPartialFirstOperationsDoesNotCallApplyOnAnyOperations() {
        operationReturnsSuccessResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(false)
                .execute(asList(operation1, operation2));
        verify(operation1, never()).apply();
        verify(operation2, never()).apply();
    }

    @Test
    public void executeForSemiValidNonPartialFirstOperationsDoesNotCallPrepareOnSecondIfForcePrepareAllFalse() {
        operationReturnsSuccessResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(false)
                .execute(asList(operation1, operation2));
        verify(operation1).prepare();
        verify(operation2, never()).prepare();
    }

    @Test
    public void executeForSemiValidNonPartialFirstOperationsCallsPrepareOnSecondIfForcePrepareAllTrue() {
        operationReturnsSuccessResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(true)
                .execute(asList(operation1, operation2));
        verify(operation1).prepare();
        verify(operation2).prepare();
    }

    @Test
    public void executeForSemiValidPartialOperationsReturnsTrue() {
        operationReturnsSuccessResultOnPrepare(operation1);
        operationReturnsSuccessResultOnPrepare(operation2);

        boolean executed = new AllOrNothingExecutionStrategy(false, true)
                .execute(asList(operation1, operation2));
        assertThat(executed).isTrue();
    }

    @Test
    public void executeForInvalidPartialOperationsReturnsFalse() {
        operationReturnsBrokenResultOnPrepare(operation1);
        operationReturnsSuccessResultOnPrepare(operation2);

        boolean executed = new AllOrNothingExecutionStrategy(false, true)
                .execute(asList(operation1, operation2));
        assertThat(executed).isFalse();
    }

    @Test
    public void executeForInvalidPartialOperationsDoesNotCallApply() {
        operationReturnsBrokenResultOnPrepare(operation1);
        operationReturnsSuccessResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(false, true)
                .execute(asList(operation1, operation2));
        verify(operation1, never()).apply();
        verify(operation2, never()).apply();
    }

    @Test
    public void executeForInvalidNonPartialOperationsCallCancel() {
        operationReturnsBrokenResultOnPrepare(operation1);
        operationDoesNotReturnResultOnPrepare(operation2);

        new AllOrNothingExecutionStrategy(false, false)
                .execute(asList(operation1, operation2));
        verify(operation1, never()).apply();
        verify(operation2, never()).apply();
        verify(operation2, atLeastOnce()).cancel();
    }

    @SuppressWarnings("unchecked")
    private Operation<Object> createOperationMock() {
        return (Operation<Object>) mock(Operation.class);
    }

    private void operationDoesNotReturnResultOnPrepare(Operation<Object> operation) {
        when(operation.prepare()).thenReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void operationReturnsSuccessResultOnPrepare(Operation<Object> operation) {
        MassResult successfulMassResult = mock(MassResult.class);
        when(successfulMassResult.isSuccessful()).thenReturn(true);
        when(operation.prepare()).thenReturn(Optional.of(successfulMassResult));
    }

    @SuppressWarnings("unchecked")
    private void operationReturnsBrokenResultOnPrepare(Operation<Object> operation) {
        MassResult successfulMassResult = mock(MassResult.class);
        when(successfulMassResult.isSuccessful()).thenReturn(false);
        when(operation.prepare()).thenReturn(Optional.of(successfulMassResult));
    }
}
