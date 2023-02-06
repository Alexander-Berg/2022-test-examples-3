package ru.yandex.mail.things.matchers;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;


public class Items extends TypeSafeMatcher<List<String>> {
    private List<String> expected;

    private Items(List<String> expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(List<String> actual) {
        return CollectionUtils.isEqualCollection(expected, actual);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Списки эквивалентны друг другу");
    }

    @Factory
    public static Matcher<List<String>> listsAreEqual(List<String> expected) {
        return new Items(expected);
    }
}
