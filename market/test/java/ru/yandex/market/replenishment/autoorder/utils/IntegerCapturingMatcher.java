package ru.yandex.market.replenishment.autoorder.utils;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IntegerCapturingMatcher extends TypeSafeMatcher<Integer> {

    private Integer value;

    public Integer getValue() {
        return value;
    }

    @Override
    protected boolean matchesSafely(Integer item) {
        this.value = item;
        return true;
    }

    @Override
    public void describeTo(Description description) {
    }
}
