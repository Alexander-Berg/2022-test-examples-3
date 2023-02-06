package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriver;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;

/**
 * User: alex89
 * Date: 10.10.12
 */
public class CurrentUrlMatcher extends TypeSafeMatcher<WebDriver> {
    private Matcher<? super String> stringMatcher;

    public CurrentUrlMatcher(Matcher<? super String> stringMatcher) {
        this.stringMatcher = stringMatcher;
    }

    public boolean matchesSafely(WebDriver driver) {
        return stringMatcher.matches(driver.getCurrentUrl());

    }

    @Override
    public void describeMismatchSafely(WebDriver driver, Description description) {
        description.appendText("current url ");
        stringMatcher.describeMismatch(driver.getCurrentUrl(), description);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("url ").appendDescriptionOf(stringMatcher);
    }

    @Factory
    public static Matcher<WebDriver> containsInCurrentUrl(String url) {
        return currentUrl(containsString(url));
    }

    @Factory
    public static Matcher<WebDriver> currentUrlEnds(String url) {
        return currentUrl(endsWith(url));
    }

    @Factory
    public static Matcher<WebDriver> currentUrl(Matcher<? super String> matcher) {
        return withWaitFor(new CurrentUrlMatcher(matcher), SECONDS.toMillis(15), SECONDS.toMillis(1));
    }


}
