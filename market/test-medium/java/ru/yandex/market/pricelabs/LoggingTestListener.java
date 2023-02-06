package ru.yandex.market.pricelabs;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.market.pricelabs.misc.Utils;

/**
 * Выполнить логирование методов (до и после их начала), что позволяет легко определить начало и конец
 * вывода тестового метода.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingTestListener implements TestExecutionListener {

    private long onTestClass;
    private long onTestMethod;

    @Override
    public void beforeTestClass(TestContext testContext) {
        log.info(" >>> Begin Test Class [{}] ===", testContext.getTestClass());
        onTestClass = System.currentTimeMillis();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        log.info("  >> Begin Test Method [{}] ==", testContext.getTestMethod());
        onTestMethod = System.currentTimeMillis();
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        long diff = System.currentTimeMillis() - onTestMethod;
        @Nullable var t = testContext.getTestException();
        String exception = t != null ? Utils.extractErrorText(t) : "";
        log.info("  << End Test Method in {} msec [{}] [{}] ==", diff, testContext.getTestMethod(), t);
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        long diff = System.currentTimeMillis() - onTestClass;
        log.info(" <<< End Test Class in {} msec [{}] ===", diff, testContext.getTestClass());
    }

}
