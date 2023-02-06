package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.cthul.matchers.CthulMatchers.and;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.matchers.webdriver.AttributeMatcher.attribute;

/**
 * User: lanwen
 * Date: 21.11.13
 * Time: 0:01
 */
public class CommonMatchers {

    public static Matcher disabledButton() {
        return and(isPresent(), anyOf(attribute("aria-disabled", "true"), hasClass(containsString("is-disabled"))));
    }

    public static Matcher enabledButton() {
        return and(isPresent(), attribute("aria-disabled", "false"));
    }

    public static Matcher<String> trimmed(Matcher<String> matcher) {
        return new FeatureMatcher<String, String>(matcher, "trimmed", "trimmed") {
            @Override
            protected String featureValueOf(String s) {
                return s.trim();
            }
        };
    }
}
