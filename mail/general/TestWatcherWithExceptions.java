package ru.yandex.autotests.innerpochta.wmi.core.rules;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * User: lanwen
 * Date: 25.03.14
 * Time: 14:55
 */
public abstract class TestWatcherWithExceptions implements TestRule {
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<Throwable>();

                starting(description);
                try {
                    base.evaluate();
                    succeeded(description);
                } catch (AssumptionViolatedException e) {
                    errors.add(e);
                    skipped(e, description);
                } catch (Throwable t) {
                    errors.add(t);
                    failed(t, description);
                } finally {
                    finished(description);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    /**
     * Invoked when a test succeeds
     */
    protected void succeeded(Description description) throws Exception {
    }

    /**
     * Invoked when a test fails
     */
    protected void failed(Throwable e, Description description) {
    }

    /**
     * Invoked when a test is skipped due to a failed assumption.
     */
    protected void skipped(AssumptionViolatedException e, Description description) {
    }

    /**
     * Invoked when a test is about to start
     */
    protected void starting(Description description) throws Exception {
    }

    /**
     * Invoked when a test method finishes (whether passing or failing)
     */
    protected void finished(Description description) throws Exception {
    }
}