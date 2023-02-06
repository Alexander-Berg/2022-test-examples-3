package ru.yandex.direct.ytwrapper.specs;

import java.time.Duration;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SetTableAttributesOperationSpecTest {
    private static final String TEST_PATH = "//tmp/test";
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(3);

    private OperationSpec spec;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Yt yt;
    @Mock
    private MergeSpec mergeSpec;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        spec = new SetTableAttributesOperationSpec(YPath.simple(TEST_PATH), mergeSpec, TEST_TIMEOUT);
    }

    @Test
    public void testSpec() {
        when(yt.cypress().exists(eq(Optional.empty()), eq(false), eq(YPath.simple(TEST_PATH))))
                .thenReturn(false);
        Operation operation = mock(Operation.class);
        when(yt.operations().mergeAndGetOp(ArgumentMatchers.<Optional<GUID>>any(), anyBoolean(), any()))
                .thenReturn(operation);

        spec.run(yt);
        verify(yt.cypress(), never()).remove(eq(Optional.empty()), eq(false), eq(YPath.simple(TEST_PATH)));
        verify(yt.operations()).mergeAndGetOp(eq(Optional.empty()), eq(false), eq(mergeSpec));
        verify(operation)
                .awaitAndThrowIfNotSuccess(eq(TEST_TIMEOUT));
    }

    @Test
    public void testSpecDeletes() {
        when(yt.cypress().exists(eq(Optional.empty()), eq(false), eq(YPath.simple(TEST_PATH))))
                .thenReturn(true);

        spec.run(yt);
        verify(yt.cypress()).remove(eq(Optional.empty()), eq(false), eq(YPath.simple(TEST_PATH)));
        verify(yt.operations()).mergeAndGetOp(eq(Optional.empty()), eq(false), eq(mergeSpec));
    }

    @Test
    public void testSpecTransaction() {
        when(yt.cypress()
                .exists(eq(Optional.of(GUID.valueOf("1234-1234-1234-1234"))), eq(true), eq(YPath.simple(TEST_PATH))))
                .thenReturn(true);
        Operation operation = mock(Operation.class);
        when(yt.operations().mergeAndGetOp(ArgumentMatchers.<Optional<GUID>>any(), anyBoolean(), any()))
                .thenReturn(operation);

        spec.run(yt, GUID.valueOf("1234-1234-1234-1234"));
        verify(yt.cypress())
                .remove(eq(Optional.of(GUID.valueOf("1234-1234-1234-1234"))), eq(true), eq(YPath.simple(TEST_PATH)));
        verify(yt.operations())
                .mergeAndGetOp(eq(Optional.of(GUID.valueOf("1234-1234-1234-1234"))), eq(true), eq(mergeSpec));
        verify(operation)
                .awaitAndThrowIfNotSuccess(eq(TEST_TIMEOUT));
    }
}
