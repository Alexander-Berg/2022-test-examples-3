package ru.yandex.autotests.innerpochta.matchers.settings;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.CustomLabelBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class LabelMatcher {

    public static Matcher<WebDriverRule> customLabelWithName(String labelName) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItem(equalTo(labelName)),
                "Custom labels list on settings page is", "") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return extract(new GetPagesSteps(webDriverRule).FoldersAndLabelsSettingPage().setupBlock()
                            .labels().userLabelsList(), on(MailElement.class).getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

    public static Matcher<WebDriverRule> customLabelCountOnMessagePage(Matcher<Integer> matcher) {
        return new FeatureMatcher<WebDriverRule, Integer>(matcher,
            "Custom labels count on message page is", "") {
            @Override
            protected Integer featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).MessagePage().labelsNavigation().userLabels())
                        .size();
                } catch (StaleElementReferenceException e) {
                    return -1;
                }
            }
        };
    }

    public static Matcher<WebDriverRule> customLabelNameOnMessagePage(String name) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItem(name),
            "Custom labels list on right table is", "") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).MessagePage().labelsNavigation().userLabels())
                        .extract(on(CustomLabelBlock.class).getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }

}
