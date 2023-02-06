package ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import lombok.extern.log4j.Log4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.platform.commons.util.Preconditions;
import org.opentest4j.TestAbortedException;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

@Log4j
public class RetryableTestExtension implements TestTemplateInvocationContextProvider, BeforeEachCallback,
        TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private int currentIndex = 0;
    private int totalRepeats = 0;
    private int maxRumTimeMin;
    private int testRunTimeMin;
    private List<Class<? extends Throwable>> retryableExceptions;
    private List<Class<? extends Throwable>> nonRetryableExceptions;
    private boolean testShouldBeRetried = false;
    private boolean nonRetryableExceptionAppeared = false;
    private String displayName;

    private static final long testrunStartTimestamp = System.currentTimeMillis();
    private long testFailTimestemp = 0;

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return isAnnotated(extensionContext.getTestMethod(), RetryableTest.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        Preconditions.notNull(extensionContext.getTestMethod().orElse(null), "Test method must not be null");

        RetryableTest annotationParams = extensionContext.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetryableTest.class))
                .orElseThrow(() -> new RetryableTestException("Annotation not found on executed test."));

        totalRepeats = annotationParams.repeats();
        maxRumTimeMin = annotationParams.maxRunTime();
        testRunTimeMin = annotationParams.duration();

        displayName = extensionContext.getDisplayName();

        Spliterator<TestTemplateInvocationContext> spliterator =
                spliteratorUnknownSize(new TestTemplateIterator(), Spliterator.ORDERED);
        return stream(spliterator, false);
    }

    @Override
    public void beforeEach(ExtensionContext context) {

        //Сброс флага, чтобы после успеха не повторять тест снова
        testShouldBeRetried = false;

        //Возможность задать кол-во повторений в пропертях
        String strTotalRepeats = System.getProperty("totalRepeats");
        if(strTotalRepeats != null) {
            try {
                totalRepeats = Integer.parseInt(strTotalRepeats);
            }catch(Exception e){
                log.warn("Could not read totalRepeats from system properties", e);
            }
        }

        retryableExceptions = Stream.of(context.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetryableTest.class))
                .orElseThrow(() -> new IllegalStateException("The extension should not be executed "))
                .retryOnExceptions()
        ).collect(Collectors.toList());
        retryableExceptions.add(TestAbortedException.class);

        nonRetryableExceptions = Stream.of(context.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, RetryableTest.class))
                .orElseThrow(() -> new IllegalStateException("The extension should not be executed "))
                .failOnExceptions()
        ).collect(Collectors.toList());

        ResourceLock resourceLockParams = context.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, ResourceLock.class))
                .orElseThrow(() -> new RetryableTestException("@RetryableTest cannot be used " +
                        "without @ResourceLock annotation."));

        DisplayName displayNameParams = context.getTestMethod()
                .flatMap(testMethods -> findAnnotation(testMethods, DisplayName.class))
                .orElseThrow(() -> new RetryableTestException("@RetryableTest cannot be used " +
                        "without @DisplayName annotation."));

        String displayName = displayNameParams.value();
        if (!displayName.equals(resourceLockParams.value())) {
            throw new RetryableTestException("Values for annotations @DisplayName " +
                    "and @ResourceLock should be the same.");
        }
    }

    private boolean appearedExceptionDoesNotAllowRepetitions(final Throwable appearedException) {
        return retryableExceptions.stream().noneMatch(ex -> ex.isAssignableFrom(appearedException.getClass()))
                || nonRetryableExceptions.stream().anyMatch(ex -> ex.isAssignableFrom(appearedException.getClass()));
    }

    /**
     * Общая обработка исключений: для before, after и самого теста
     */
    private void handleException(ExtensionContext context, Throwable throwable) throws Throwable {
        //Запоминаем время окончания теста здесь, чтобы в двух проверках использовалось одинаковое
        testFailTimestemp = System.currentTimeMillis();

        if (appearedExceptionDoesNotAllowRepetitions(throwable)) {
            Allure.step("Retry suppressed. Non retryable exception appeared: \n" + throwable.getClass(),
                    Status.FAILED);
            nonRetryableExceptionAppeared = true;
            throw throwable;
        }

        if (repeatLimitNotReached()) {
            testShouldBeRetried = true;
            throw new TestAbortedException("Test failed, rerunning", throwable);
        } else {
            throw throwable;
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        handleException(context, throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        handleException(context, throwable);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        handleException(context, throwable);
    }

    /**
     * До таймаута на large тесты осталось достаточно времени для перезапуска теста
     */
    private boolean timeLimitNotReached() {
        return (testFailTimestemp - testrunStartTimestamp) / (60 * 1000) < maxRumTimeMin - testRunTimeMin;
    }

    /**
     * Истекло количество попыток или остается мало времени для ещё одного повторения
     */
    private boolean repeatLimitNotReached() {
        return timeLimitNotReached() && currentIndex < totalRepeats;
    }

    /**
     * Итератор для работы с шаблоном теста помеченного RetryableTest
     */
    class TestTemplateIterator implements Iterator<TestTemplateInvocationContext> {

        @Override
        public boolean hasNext() {
            if (currentIndex == 0) {
                return true;
            }
            return testShouldBeRetried && !nonRetryableExceptionAppeared && repeatLimitNotReached();
        }

        @Override
        public TestTemplateInvocationContext next() {
            if (hasNext()) {
                currentIndex++;
                return new RetryableTestInvocationContext(currentIndex, totalRepeats, displayName);
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
