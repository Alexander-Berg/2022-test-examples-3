package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.WebDriver;

public class UriMatcher {

    public static Matcher<WebDriver> urlShould(Matcher<String> matcher) {
        return new FeatureMatcher<WebDriver, String>(matcher, "url should:",
                "") {
            @Override
            protected String featureValueOf(WebDriver webDriver) {
                return webDriver.getCurrentUrl();
            }
        };
    }
}
