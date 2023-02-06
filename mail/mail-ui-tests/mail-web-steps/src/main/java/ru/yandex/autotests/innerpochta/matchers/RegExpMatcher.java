package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

public class RegExpMatcher extends TypeSafeMatcher<String> {

    private String pattern;

    public boolean matchesSafely(String text) {
        return text.matches(pattern);
    }

    public RegExpMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Factory
    public static RegExpMatcher withPattern(String pattern) {
        return new RegExpMatcher(pattern);
    }

    @Override
    public void describeMismatchSafely(String text, Description description) {
        description.appendText("Текст: ").appendText(text);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Текст подходит под паттерн: ").appendText(pattern);
    }
}
