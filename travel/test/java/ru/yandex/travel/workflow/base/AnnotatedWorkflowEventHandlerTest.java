package ru.yandex.travel.workflow.base;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;

import com.google.protobuf.Message;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.travel.workflow.StateContext;

import static ru.yandex.travel.workflow.base.AnnotatedStatefulWorkflowEventHandler.validateHandlerParams;

public class AnnotatedWorkflowEventHandlerTest {

    @Test
    public void testParametersValidation() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> validateHandlerParams(getTestMethod("paramsAmountMismatch")));
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> validateHandlerParams(getTestMethod("messageTypeMismatch")));
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> validateHandlerParams(getTestMethod("contextTypeMismatch")));
        validateHandlerParams(getTestMethod("correctHandler"));
    }

    private Method getTestMethod(String name) {
        return Arrays.stream(TestHandler.class.getMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("no such method: " + name));
    }

    private interface TestHandler {
        void paramsAmountMismatch();

        void messageTypeMismatch(String message, StateContext ctx);

        void contextTypeMismatch(Message message, Object ctx);

        void correctHandler(Message message, StateContext ctx);
    }
}
