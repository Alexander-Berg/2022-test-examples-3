package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import static org.hamcrest.Matchers.equalTo;

public class MessageInThreadCountMatcher {

    public static Matcher<WebDriverRule> messagesInThreadCount(int count) {
        return new FeatureMatcher<WebDriverRule, Integer>(equalTo(count),
                "opened thread should be", "actual") {
            @Override
            protected Integer featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().displayedMessages().messagesInThread()
                            .size();
                } catch (StaleElementReferenceException e) {
                    return -1;
                }
            }
        };
    }
}
