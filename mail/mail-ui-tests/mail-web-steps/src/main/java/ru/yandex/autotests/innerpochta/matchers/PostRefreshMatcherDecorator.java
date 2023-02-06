package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.openqa.selenium.WebDriver;


public class PostRefreshMatcherDecorator<T> extends TypeSafeMatcher<T> {

    private WebDriver driver;
    private Matcher<? extends T> matcher;


    @Override
    protected boolean matchesSafely(T item) {
        if (!matcher.matches(item)) {
            driver.navigate().refresh();
            return false;
        }
        return true;
    }

    public PostRefreshMatcherDecorator(Matcher<? extends T> matcher, WebDriver driver) {
        this.driver = driver;
        this.matcher = matcher;
    }


    @Override
    public void describeTo(Description description) {
        description.appendText("(with postrefresh) ").appendDescriptionOf(matcher);

    }


    @Override
    protected void describeMismatchSafely(T item, Description mismatchDescription) {
        matcher.describeMismatch(item, mismatchDescription);
    }


    @Factory
    public static <T> Matcher<T> withPostRefresh(Matcher<? extends T> matcher, WebDriver driver) {
        return new PostRefreshMatcherDecorator<T>(matcher, driver);
    }

}
