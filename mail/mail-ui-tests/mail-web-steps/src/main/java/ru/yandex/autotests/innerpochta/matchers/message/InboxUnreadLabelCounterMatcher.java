package ru.yandex.autotests.innerpochta.matchers.message;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import static org.hamcrest.Matchers.equalTo;

public class InboxUnreadLabelCounterMatcher {

    public static Matcher<WebDriverRule> inboxUnreadLabelCount(int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo((Integer.toString(count))),
                "inbox unread label count should be", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation().inboxUnreadCounter()
                            .getText();
                } catch (StaleElementReferenceException e) {
                    return "StaleElementReferenceException";
                }
            }
        };
    }

}
