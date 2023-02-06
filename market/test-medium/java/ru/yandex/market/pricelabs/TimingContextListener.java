package ru.yandex.market.pricelabs;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.market.common.util.timing.TimingContext;

/**
 * Сбрасывает контекст секундомера перед каждым тестом
 */
public class TimingContextListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        TimingContext.newRoot();
    }

}
