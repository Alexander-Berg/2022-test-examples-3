package ru.yandex.autotests.innerpochta.matchers.message;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import static org.hamcrest.Matchers.equalTo;

public class FolderListMatcher {

    public static Matcher<WebDriverRule> currentFolderName(String folderName) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(folderName),
                "Current folder on message page should have name", "") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation()
                            .currentFolder().getText();
                } catch (StaleElementReferenceException e) {
                    return e.getMessage();
                }
            }
        };
    }

    public static Matcher<WebDriverRule> inboxFolderName(String folderTitle) {
        return new FeatureMatcher<WebDriverRule, String>(equalTo(folderTitle),
                "Folder title should have name", "") {
            @Override
            protected String featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return new GetPagesSteps(webDriverRule).MessagePage().foldersNavigation().inbox().getText();
                } catch (StaleElementReferenceException e) {
                    return e.getMessage();
                }
            }
        };
    }
}
