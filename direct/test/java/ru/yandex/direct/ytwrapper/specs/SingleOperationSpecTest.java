package ru.yandex.direct.ytwrapper.specs;

import java.time.Duration;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.specs.MapReduceSpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleOperationSpecTest {
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(3);
    private Yt yt;

    @Before
    public void before() {
        yt = mock(Yt.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void testMapReduce() {
        OperationSpec spec = new SingleOperationSpec(mock(MapReduceSpec.class), TEST_TIMEOUT);
        Operation operation = mock(Operation.class);
        when(yt.operations().mapReduceAndGetOp(ArgumentMatchers.<Optional<GUID>>any(), anyBoolean(), any()))
                .thenReturn(operation);
        spec.run(yt);

        verify(yt.operations()).mapReduceAndGetOp(eq(Optional.empty()), eq(false), isA(MapReduceSpec.class));
        verify(operation)
                .awaitAndThrowIfNotSuccess(eq(TEST_TIMEOUT));
    }

    @Test
    public void testMapReduceTransaction() {
        OperationSpec spec = new SingleOperationSpec(mock(MapReduceSpec.class), TEST_TIMEOUT);
        spec.run(yt);

        spec.run(yt, GUID.valueOf("1234-1234-1234-1234"));
        verify(yt.operations()).mapReduceAndGetOp(eq(Optional.of(GUID.valueOf("1234-1234-1234-1234"))), eq(true),
                isA(MapReduceSpec.class));
    }
}
