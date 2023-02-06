package ru.yandex.autotests.innerpochta.matchers.message;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

public class MessageThreadMatcher {

    public static Matcher<WebDriverRule> threadCount(final String subject, int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(String.valueOf(count)),
                "thread count should be", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(filter(having(on(MessageBlock.class).subject().getText(),
                            containsString(subject)), new GetPagesSteps(webDriverRule).MessagePage()
                            .displayedMessages().list())).get(0).threadCounter().getText();
                } catch (StaleElementReferenceException e) {
                    return e.getMessage();
                } catch (IndexOutOfBoundsException e) {
                    return format("Нет треда с темой «%s»", subject);
                }
            }
        };
    }
}
