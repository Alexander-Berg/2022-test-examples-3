package ru.yandex.autotests.innerpochta.matchers.message;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomLabelBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

public class CounterMatchers {

    public static Matcher<WebDriverRule> inboxCount(int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(Integer.toString(count)),
                "total inbox count should be", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation().folderTotalCounter().get(0)
                            .getText();
                } catch (StaleElementReferenceException e) {
                    return "oops! StaleElementReferenceException";
                }
            }
        };
    }

    public static Matcher<WebDriverRule> sendCount(int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(Integer.toString(count)),
                "total send count should be", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation().sentFolderCounter()
                            .getText();
                } catch (StaleElementReferenceException e) {
                    return "oops! StaleElementReferenceException";
                }
            }
        };
    }

    public static Matcher<WebDriverRule> draftCount(int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(Integer.toString(count)),
                "draft count should be", "actual"
        ) {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation()
                            .selectedFolderCounter().getText();
                } catch (StaleElementReferenceException e) {
                    return "StaleElementReferenceException";
                }
            }
        };
    }

    public static Matcher<WebDriverRule> customLabelCount(final String labelName, int count) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(String.valueOf(count)),
                "label count should be", "actual") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(filter(having(on(CustomLabelBlock.class).labelName().getText(),
                                    containsString(labelName)),
                            new GetPagesSteps(webDriverRule).MessagePage().labelsNavigation()
                                    .userLabels())).get(0).getAttribute("title").split(": ")[1];
                } catch (StaleElementReferenceException e) {
                    return e.getMessage();
                } catch (IndexOutOfBoundsException e) {
                    return format("Нет метки с именем «%s»", labelName);
                }
            }
        };
    }

}
