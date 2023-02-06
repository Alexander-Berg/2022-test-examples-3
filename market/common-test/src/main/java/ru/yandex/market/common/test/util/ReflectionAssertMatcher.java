package ru.yandex.market.common.test.util;

import junit.framework.AssertionFailedError;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.unitils.reflectionassert.ReflectionAssert;

public final class ReflectionAssertMatcher<T> extends TypeSafeMatcher<T> {

    private final T expected;

    public ReflectionAssertMatcher(T expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(T actual) {
        try {
            ReflectionAssert.assertReflectionEquals(expected, actual);
            return true;
        } catch (AssertionFailedError e) {
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expected);
    }
}
