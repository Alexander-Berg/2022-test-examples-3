package ru.yandex.direct.operation.add;

import org.junit.Test;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.ResultState;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AbstractAddOperationCancelTest extends BaseAbstractAddOperationTest {

    @Test(expected = IllegalStateException.class)
    public void cancel_CalledBeforePrepare_ThrowsException() {
        oneValidObjectAndOneWithError();

        TestableAddOperation addOperation = createOperation(Applicability.PARTIAL);
        addOperation.cancel();
    }

    @Test
    public void cancel_ReturnCanceledMassResult() {
        oneValidObjectAndOneWithError();

        TestableAddOperation addOperation = createOperation(Applicability.PARTIAL);
        addOperation.prepare();
        MassResult<?> result = addOperation.cancel();
        assertThat("состояние MassResult -- successful",
                result.getState(), equalTo(ResultState.SUCCESSFUL));
        assertThat("состояние первого элемента результата -- canceled",
                result.get(0).getState(), equalTo(ResultState.CANCELED));
        assertThat("состояние второго элемента результата -- broken",
                result.get(1).getState(), equalTo(ResultState.BROKEN));
    }
}
