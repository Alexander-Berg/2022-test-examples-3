package ru.yandex.direct.dbutil.wrapper;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteType;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfilingListenerTest {
    @Test(expected = ProfilingListenerException.class)
    public void start_throwsIfCalledOnAnAlreadyInitializedObject() {
        ProfilingListener listener = new ProfilingListener("test");
        ExecuteContext context = mock(ExecuteContext.class);
        when(context.type()).thenReturn(ExecuteType.READ);

        // Initialize listener
        listener.start(context);

        // Test
        listener.start(context);
    }

    @Test(expected = ProfilingListenerException.class)
    public void end_throwsIfCalledOnAnUninitializedObject() {
        ProfilingListener listener = new ProfilingListener("test");
        ExecuteContext context = mock(ExecuteContext.class);

        listener.end(context);
    }
}
