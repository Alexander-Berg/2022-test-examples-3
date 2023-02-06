package ru.yandex.market.replenishment.autoorder.utils;

import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class StringCapturingMatcher extends TypeSafeMatcher<String> {

    private String value;
    private Function<String, Boolean> matcher;

    public StringCapturingMatcher(Function<String, Boolean> matcher) {
        this.matcher = matcher;
    }

    public String getValue() {
        return value;
    }

    @Override
    protected boolean matchesSafely(String item) {
        this.value = item;
        return true;
    }

    @Override
    public void describeTo(Description description) {
    }
}
