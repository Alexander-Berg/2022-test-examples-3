package ru.yandex.autotests.innerpochta.matchers.compose;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import java.util.List;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;

public class ComposeMatcher {

    public static Matcher<WebDriverRule> hasAddresses(String... values) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItems(values),
            "Addresses in field to should be", "actual") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).ComposePopup().yabbleToNamesList())
                        .extract(on(MailElement.class).getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<WebDriverRule> hasClearAddress() {
        return new FeatureMatcher<WebDriverRule, List<String>>(everyItem(equalTo("")),
            "Addresses in field to should be clear", "actual count of addresses ") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).ComposePopup().yabbleToNamesList())
                        .extract(on(MailElement.class).getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }
}
