package ru.yandex.direct.operation.operationwithid;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.testing.matchers.result.MassResultMatcher;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertThat;

public class AbstractOperationWithIdMethodCallOrderTest extends BaseAbstractOperationWithIdTest {

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledTwice_ThrowsException() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        operation.prepare();
        operation.prepare();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledBeforePrepare_ThrowsException() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledAgainAfterApply_ThrowsException() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        operation.prepare();
        operation.apply();
        operation.prepare();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledAgainAfterApply_ThrowsException() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        operation.prepare();
        operation.apply();
        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledWhenPrepareFailed_ThrowsException() {
        oneIdWithError();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> massResult = operation.prepare();
        checkState(massResult.isPresent(), "в данном случае prepare() должен вернуть результат");

        operation.apply();
    }

    @Test
    public void prepareAndApply_ValidCall_ResultIsSuccessful() {
        oneValidId();
        TestableOperationWithId operation = createOperation(Applicability.PARTIAL);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, MassResultMatcher.isSuccessful());
    }
}
