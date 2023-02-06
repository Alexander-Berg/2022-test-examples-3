package ru.yandex.market.mbi.logprocessor;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Листенер, который определяет запущен есть ли переменная окружения YT_PROXY.
 * Если такая переменная есть, то можно считать, что был поднят рецепт для локального YT.
 * Это нужно для запуска тестов, где используется YT, в Sandbox/Linux окружениях.
 */
@Order(1)
public class YaMakeTestExecutionListener implements TestExecutionListener {
    @Override
    public void beforeTestClass(TestContext testContext) {
        Optional.ofNullable(System.getenv("YT_PROXY")).ifPresent(proxy -> {
            // Запускаемся из тестового окружения Sandbox/Linux
            System.setProperty("YT_PROXY", proxy);
            System.setProperty("YT_REPLICAS", "");
            System.setProperty("YT_USERNAME", "root");
            System.setProperty("YT_TOKEN", "");
        });
    }

}

