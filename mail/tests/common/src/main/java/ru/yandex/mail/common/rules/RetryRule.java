package ru.yandex.mail.common.rules;

import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.instanceOf;

public class RetryRule implements TestRule {

    private static final Logger LOG = Logger.getLogger(RetryRule.class);

    private Matcher<Object> matcher;
    private int attempts = 3;
    private int delay = 10;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    public static RetryRule retry() {
        return new RetryRule();
    }

    public RetryRule every(int delay, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.delay = delay;
        return this;
    }

    public RetryRule times(int attempts) {
        this.attempts = attempts;
        return this;
    }

    @SuppressWarnings("unchecked")
    public RetryRule ifException(Matcher<?> newMatcher) {
        if (matcher == null)
            matcher = (Matcher<Object>) newMatcher;
        else
            matcher = either(matcher).or((Matcher<Object>) newMatcher);
        return this;
    }

    /**
     * Adds to the list of requirements for any thrown exception that it
     * should be an instance of {@code type}
     */
    public RetryRule ifException(Class<? extends Throwable> type) {
        return ifException(instanceOf(type));
    }

    /**
     * Adds to the list of requirements for any thrown exception that it
     * should <em>contain</em> string {@code substring}
     */
    public RetryRule ifMessage(String substring) {
        return ifMessage(containsString(substring));
    }

    /**
     * Adds {@code matcher} to the list of requirements for the message
     * returned from any thrown exception.
     */
    public RetryRule ifMessage(Matcher<String> matcher) {
        return ifException(hasMessage(matcher));
    }

    public RetryRule or() {
        return this;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable e = null;
                for (int i = 0; i <= attempts; i++) {
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        e = t;
                        if (matcher != null && matcher.matches(e)) {
                            LOG.info("Attempt [" + i + "] failed, sleeping for "
                                    + delay + " " + timeUnit.name() + " to retry...", e);
                            Thread.sleep(timeUnit.toMillis(delay));
                        } else {
                            throw e;
                        }
                    }
                }
                LOG.info("All [" + attempts + "] attempts failed, forgiving...");
                throw e;
            }
        };
    }

    private Matcher<Throwable> hasMessage(final Matcher<String> matcher) {
        return new TypeSafeMatcher<Throwable>() {
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("exception with message ");
                description.appendDescriptionOf(matcher);
            }

            @Override
            public boolean matchesSafely(Throwable item) {
                return matcher.matches(item.getMessage());
            }
        };
    }
}
