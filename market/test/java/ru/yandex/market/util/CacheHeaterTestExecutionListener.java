package ru.yandex.market.util;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.security.util.heater.HeatableService;

/**
 * Listener для прогревания кэшей {@link HeatableService} перед каждым тестом.
 */
public class CacheHeaterTestExecutionListener extends AbstractTestExecutionListener {
    @Override
    public void beforeTestMethod(final TestContext testContext) {
        testContext.getApplicationContext()
                .getBeansOfType(HeatableService.class)
                .values()
                .forEach(HeatableService::heatUp);
    }
}

