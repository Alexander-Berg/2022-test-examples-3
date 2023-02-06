package ru.yandex.direct.operation.update;

import org.junit.Test;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AbstractUpdateOperationCancelTest extends BaseAbstractUpdateOperationTest {

    @Test(expected = IllegalStateException.class)
    public void cancel_CalledBeforePrepare_ThrowsException() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.cancel();
    }

    @Test
    public void cancel_ReturnCanceledMassResult() {
        modelChangesValidationIsFullyValid();
        appliedChangesValidationWithInvalidSecondItem();
        TestableUpdateOperation updateOperation = createUpdateOperationMock(Applicability.PARTIAL);
        updateOperation.prepare();
        MassResult<Long> result = updateOperation.cancel();
        assertThat("состояние MassResult -- successful",
                result.getState(), equalTo(ResultState.SUCCESSFUL));
        assertThat("состояние первого элемента результата -- canceled",
                result.get(0).getState(), equalTo(ResultState.CANCELED));
        assertThat("состояние второго элемента результата -- broken",
                result.get(1).getState(), equalTo(ResultState.BROKEN));
    }
}
