package ru.yandex.travel.workflow.base;

import org.junit.Test;

import ru.yandex.travel.test.fake.proto.TTestSomeEvent;
import ru.yandex.travel.test.fake.proto.TTestStartEvent;
import ru.yandex.travel.workflow.StateContext;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

public class AnnotatedStatefulWorkflowEventHandlerTest {

    @Test
    public void testMessageIgnored() {
        assertThatCode(() ->
                new TestHandler().handleEvent(TTestStartEvent.newBuilder().build(), mock(StateContext.class))
        ).doesNotThrowAnyException();
    }

    @Test
    public void testMessageThrowsException() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new TestHandler().handleEvent(TTestSomeEvent.newBuilder().build(), mock(StateContext.class))
        );
    }

    // use version without generics for the test
    @IgnoreEvents(types = TTestStartEvent.class)
    private static class TestHandler extends AnnotatedStatefulWorkflowEventHandler {
    }
}
