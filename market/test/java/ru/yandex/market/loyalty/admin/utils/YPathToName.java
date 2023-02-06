package ru.yandex.market.loyalty.admin.utils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.inside.yt.kosher.cypress.YPath;

public class YPathToName extends TypeSafeMatcher<YPath> {
    private final Matcher<String> matcher;

    public YPathToName(Matcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(YPath s) {
        return matcher.matches(s.name());
    }

    @Override
    public void describeTo(Description description) {
        matcher.describeTo(description);
    }
}
