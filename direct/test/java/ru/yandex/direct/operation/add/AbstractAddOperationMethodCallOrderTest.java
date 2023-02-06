package ru.yandex.direct.operation.add;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AbstractAddOperationMethodCallOrderTest extends BaseAbstractAddOperationTest {

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledTwice_ThrowsException() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        operation.prepare();
        operation.prepare();
    }

    @Test(expected = IllegalStateException.class)
    public void prepare_CalledTwiceAfterApplied_ThrowsException() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        when(operation.execute(any(Map.class))).thenReturn(singletonMap(0, AD_GROUP_1_ID));

        operation.prepare();
        operation.apply();
        operation.prepare();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledBeforePrepare_ThrowsException() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledTwice_ThrowsException() {
        oneValidObject();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        when(operation.execute(any(Map.class))).thenReturn(singletonMap(0, AD_GROUP_1_ID));

        operation.prepare();
        operation.apply();
        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_CalledWhenPrepareFailed_ThrowsException() {
        oneObjectWithError();
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> massResult = operation.prepare();
        checkState(massResult.isPresent(), "в данном случае prepare() должен вернуть результат");

        operation.apply();
    }
}
