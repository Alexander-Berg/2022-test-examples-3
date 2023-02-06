package ru.yandex.direct.core.testing.listener;


import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.log.service.metrics.MetricData;
import ru.yandex.direct.common.log.service.metrics.MetricsAddRequest;
import ru.yandex.direct.common.log.service.metrics.MetricsLogHttpSender;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * listener для замера времени подготовки к запуску теста.
 * Время логируется и отправляется в intApi ручку /metrics/add.
 * <p>
 * Метод beforeTestClass будет вызываться для каждого теста.
 * startTime - изначально равен 0. Первый поток, который выполнит эту строчку, присвоит переменной startTime текущее
 * значение System.currentTimeMillis(). Остальные потоки (или этот же поток, но позже) не смогут изменить startTime.
 * То есть запоминается время перед инициализацией первого теста. Для остальных тестов startTime не меняется.
 * <p>
 * Аналогично в beforeTestMethod, логируется время инициализации только для первого теста. Для последующих тестов время
 * логироваться не будет.
 */
public class LogTestInitTimeListener implements TestExecutionListener, Ordered {

    private static final String METRIC_NAME = "TestInitTime";
    private static final String INTAPI_URL = "https://intapi.direct.yandex.ru";
    private static final Logger logger = LoggerFactory.getLogger(LogTestInitTimeListener.class);
    private static final double MIN_SECONDS = 2;

    private static final AtomicLong startTime = new AtomicLong();
    private static final AtomicBoolean timeAlreadyLogged = new AtomicBoolean();
    private static final Set<String> TESTS_TYPES = Set.of("CoreTest", "GridCoreTest", "GridProcessingTest");

    /**
     * Этот метод вызывается до framework-specific логики.
     */
    @Override
    public void beforeTestClass(TestContext testContext) {
        startTime.compareAndSet(0, System.currentTimeMillis());
    }

    /**
     * Этот метод вызывается после framework-specific логики, но до самой логики теста
     */
    @Override
    public void beforeTestMethod(TestContext testContext) {
        boolean successSet = timeAlreadyLogged.compareAndSet(false, true);
        boolean isSpringJUnit4ClassRunnerTest = isSpringJUnit4ClassRunnerTest(testContext);

        if (successSet && isSpringJUnit4ClassRunnerTest) {
            double time = (System.currentTimeMillis() - startTime.get()) / 1000.0;
            if (isTimeLessThanMinimum(time)) {
                return;
            }

            sendMetrics(testContext, time);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Отправить метрики в intApi ручку /metrics/add
     */
    private void sendMetrics(TestContext testContext, double time) {
        try (DefaultAsyncHttpClient defaultAsyncHttpClient = new DefaultAsyncHttpClient()) {
            MetricsAddRequest request = createRequest(testContext, time);

            MetricsLogHttpSender sender = new MetricsLogHttpSender(INTAPI_URL, defaultAsyncHttpClient);
            sender.sendMetrics(request)
                    .orTimeout(5, SECONDS)
                    .join();
        } catch (RuntimeException e) {
            logger.warn("Error in sendMetrics", e);
        }
    }

    /**
     * Подготавливаем данные для отправки в intApi ручку /metrics/add
     */
    private MetricsAddRequest createRequest(TestContext testContext, double time) {
        String userName = System.getProperty("user.name");
        String isDebugMode = System.getProperty("intellij.debug.agent", "false");
        String firstTestName = testContext.getTestClass().getSimpleName();
        String testType = getTestType(testContext);

        Map<String, String> context =
                Map.of("userName", userName,
                        "testType", testType,
                        "firstTestName", firstTestName,
                        "isDebugMode", isDebugMode);

        MetricData metricData = new MetricData(METRIC_NAME, time, context);
        return new MetricsAddRequest(new MetricData[]{metricData}, null);
    }

    /**
     * Логируем время только для SpringJUnit4ClassRunner теста.
     * То есть игнорируем параметризованные и др. тесты.
     */
    private boolean isSpringJUnit4ClassRunnerTest(TestContext testContext) {
        Class<?> testClass = testContext.getTestClass();
        if (isAnnotationRunWithPresent(testClass)) {
            Class<? extends Runner> annotationValue = testClass.getAnnotation(RunWith.class).value();
            return SpringJUnit4ClassRunner.class.isAssignableFrom(annotationValue);
        }

        return false;
    }

    /**
     * Если сначала вызывается параметризованный тест, который поднимает Spring и не использует
     * LogTestInitTimeListener, а за ним вызывается тест который использует LogTestInitTimeListener, то время
     * инициализации будет совсем небольшим. Такое время не нужно лоигировать
     */
    private boolean isTimeLessThanMinimum(double time) {
        if (time < MIN_SECONDS) {
            logger.info("Too little time. Time is {}", time);
            return true;
        }
        return false;
    }

    /**
     * Определить, имеет ли тест аннотацию @RunWith
     * <p>
     * Просто написать testClass.isAnnotationPresent(RunWith.class) нельзя, т.к. есть junit5 модули, в которых нет этого
     * класса. То есть isAnnotationPresent(RunWith.class) может привести к NoClassDefFoundError.
     */
    private boolean isAnnotationRunWithPresent(Class<?> testClass) {
        return Arrays.stream(testClass.getAnnotations())
                .map(annotation -> annotation.annotationType().getSimpleName())
                .anyMatch(annotationName -> annotationName.equals("RunWith"));
    }

    /**
     * Узнать тип теста (CoreTest/GridCoreTest/GridProcessingTest)
     */
    private String getTestType(TestContext testContext) {
        return Arrays.stream(testContext.getTestClass().getAnnotations())
                .map(annotation -> annotation.annotationType().getSimpleName())
                .filter(TESTS_TYPES::contains)
                .findAny()
                .orElse("Unknown");

    }
}
