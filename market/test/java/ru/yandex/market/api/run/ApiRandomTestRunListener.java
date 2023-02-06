package ru.yandex.market.api.run;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * @author dimkarp93
 */
public class ApiRandomTestRunListener extends RunListener {
    private boolean alreadyUsing = false;

    public static final ApiRandomTestRunListener API_RANDOM_TEST_RUN_LISTENER = new ApiRandomTestRunListener();

    private TestStatusFormatter formatter;

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        formatter.formatTestFinished(description);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        formatter.formatTestFailure(failure);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
        formatter.formatTestIgnored(description);
    }

    public void use() {
        alreadyUsing = true;
    }

    public boolean isUse() {
        return alreadyUsing;
    }

    public TestStatusFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(TestStatusFormatter formatter) {
        this.formatter = formatter;
    }
}
