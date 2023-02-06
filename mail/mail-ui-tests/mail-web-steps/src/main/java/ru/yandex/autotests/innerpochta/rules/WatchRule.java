package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class WatchRule implements TestRule {

    public WatchRule() {

    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    WatchRule.this.starting(description);
                    base.evaluate();
                } catch (Throwable t) {
                    throw t;
                }
            }
        };
    }

    protected void starting(Description description) {
    }
}
