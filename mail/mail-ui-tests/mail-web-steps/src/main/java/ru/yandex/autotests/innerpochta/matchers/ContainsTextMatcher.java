package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import static org.hamcrest.Matchers.containsString;

public class ContainsTextMatcher {

    public static Matcher<WebDriverRule> containsText(String text) {
        return new FeatureMatcher<WebDriverRule, String>(containsString(text),
                "text of the element should contains", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().statusLineBlock().getText();
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
        };
    }
}
