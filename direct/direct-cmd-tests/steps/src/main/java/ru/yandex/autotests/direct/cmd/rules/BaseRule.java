package ru.yandex.autotests.direct.cmd.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 *         TestWatcher не прокидывает эксепшны из методов starting и finished
 *         из-за этого получаются неожиданные ошибки в тестах, использующих ресурсы этих рул
 */
public abstract class BaseRule implements TestRule {

    private Throwable exception;

    protected final Logger log = LoggerFactory.getLogger(BaseRule.class);

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    starting(description);
                    base.evaluate();
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    finishedQuietly(description);
                }
            }
        };
    }

    protected void start() {

    }

    protected void finish() {

    }

    protected void starting(Description description) {
        start();
    }

    protected void finished(Description description) {
        finish();
    }

    private void finishedQuietly(Description description) {
        try {
            finished(description);
        } catch (Exception t) {
            log.info("Cannon perform shutdown actions", t);
        }
    }
}
