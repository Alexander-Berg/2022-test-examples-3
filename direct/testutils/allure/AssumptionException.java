package ru.yandex.autotests.irt.testutils.allure;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;
import org.hamcrest.StringDescription;
import org.junit.AssumptionViolatedException;

public class AssumptionException extends RuntimeException implements SelfDescribing {
    private AssumptionViolatedException exception;

    public <T> AssumptionException(String assumption, T value, Matcher<? super T> matcher) {
        super(value instanceof Throwable ? (Throwable) value : null);
        this.exception = new AssumptionViolatedException(assumption, value, matcher);
    }

    public <T> AssumptionException(T value, Matcher<? super T> matcher) {
        this(null, value, matcher);
    }

    public AssumptionException(String assumption) {
        this(assumption, null, null);
    }

    public AssumptionException(String assumption, Throwable t) {
        this(assumption, t, null);
    }

    @Override
    public String getMessage() {
        return StringDescription.asString(this.exception);
    }

    public void describeTo(Description description) {
        this.exception.describeTo(description);
    }
}
