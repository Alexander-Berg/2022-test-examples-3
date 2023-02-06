package ru.yandex.autotests.innerpochta.utils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static java.lang.Thread.sleep;

public class RetryRule implements TestRule {
    private int retryCount;

    public RetryRule (int retryCount) {
        this.retryCount = retryCount;
    }

    public Statement apply(Statement base, Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable caughtThrowable = null;
                for (int i = 0; i < retryCount; i++) {
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        caughtThrowable = t;
                        System.err.println(description.getDisplayName() + ": run " + (i + 1) + " failed.");
                        sleep(3000);
                    }
                }
                System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failures.");
                throw caughtThrowable;
            }
        };
    }
}
