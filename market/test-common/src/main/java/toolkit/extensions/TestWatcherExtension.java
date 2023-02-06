package toolkit.extensions;

import java.util.Arrays;
import java.util.LinkedList;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import toolkit.OkClient;

@Slf4j
public class TestWatcherExtension implements TestWatcher, LifecycleMethodExecutionExceptionHandler {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        LinkedList<StackTraceElement> stackTraceElements = new LinkedList<>(Arrays.asList(cause.getStackTrace()));
        stackTraceElements.addFirst(new StackTraceElement("Response is\n" + OkClient.getResponse() + "\n", "", "", 0));
        stackTraceElements.addFirst(new StackTraceElement("Request is\n" + OkClient.getRequest() + "\n", "", "", 0));
        cause.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) {
        log.error(ExceptionUtils.getStackTrace(throwable));
    }
}
