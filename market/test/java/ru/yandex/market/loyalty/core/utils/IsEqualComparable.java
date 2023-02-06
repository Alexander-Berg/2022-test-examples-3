package ru.yandex.market.loyalty.core.utils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.lang.reflect.Array;
import java.util.function.BiPredicate;

public class IsEqualComparable<T> extends BaseMatcher<T> {
    private final Object expectedValue;
    private final BiPredicate<Object, Object> comparator;

    public IsEqualComparable(Object equalArg, BiPredicate<Object, Object> comparator) {
        this.expectedValue = equalArg;
        this.comparator = comparator;
    }

    @Override
    public boolean matches(Object actualValue) {
        return areEqual(actualValue, expectedValue);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(expectedValue);
    }

    private boolean areEqual(Object actual, Object expected) {
        if (actual == null) {
            return expected == null;
        }

        if (expected != null && isArray(actual)) {
            return isArray(expected) && areArraysEqual(actual, expected);
        }

        return comparator.test(actual, expected);
    }

    private boolean areArraysEqual(Object actualArray, Object expectedArray) {
        return areArrayLengthsEqual(actualArray, expectedArray) && areArrayElementsEqual(actualArray, expectedArray);
    }

    private static boolean areArrayLengthsEqual(Object actualArray, Object expectedArray) {
        return Array.getLength(actualArray) == Array.getLength(expectedArray);
    }

    private boolean areArrayElementsEqual(Object actualArray, Object expectedArray) {
        for (int i = 0; i < Array.getLength(actualArray); i++) {
            if (!areEqual(Array.get(actualArray, i), Array.get(expectedArray, i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArray(Object o) {
        return o.getClass().isArray();
    }

}
