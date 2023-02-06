package ru.yandex.market.pricelabs.tms;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;

public class TmsResetListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
        testContext.getApplicationContext().getBean(TestControls.class).resetShops();
        TimingUtils.resetTime();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext().getBean(ExecutorSources.class).resetDefaults();
        TimingUtils.addTime(1);
    }
}
