package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import org.hamcrest.Matcher;


public class NumberMatchers {

    private NumberMatchers() { }

    /**
     * Matcher for long values
     * Rest assured cannot compare long values with int numbers
     * Helps to solve the problem:
     *
     * java.lang.AssertionError: 1 expectation failed.
     * Expected: is <2032780L>
     * Actual: 2032780
     *
     */
    public static Matcher<? extends Number> is(Long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return org.hamcrest.core.Is.is(value);
        } else {
            return org.hamcrest.core.Is.is(value.intValue());
        }
    }
}
