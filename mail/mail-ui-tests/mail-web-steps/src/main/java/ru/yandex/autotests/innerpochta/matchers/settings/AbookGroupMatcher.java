package ru.yandex.autotests.innerpochta.matchers.settings;

import ch.lambdaj.collection.LambdaList;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook.AbookCustomUserGroupsBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import java.util.List;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class AbookGroupMatcher {

    public static Matcher<WebDriverRule> abookGroup(Matcher<List<AbookCustomUserGroupsBlock>> matcher) {
        return new FeatureMatcher<WebDriverRule, List<AbookCustomUserGroupsBlock>>(matcher,
                "should has group ", "actual ") {
            @Override
            protected List<AbookCustomUserGroupsBlock> featureValueOf(WebDriverRule webDriverRule) {
                return new GetPagesSteps(webDriverRule).AbookSettingsPage().blockSetupAbook().groupsManage()
                        .createdGroups();
            }
        };
    }

    public static Matcher<List<AbookCustomUserGroupsBlock>> withName(String name) {
        return new FeatureMatcher<List<AbookCustomUserGroupsBlock>, LambdaList<String>>(hasItem(equalTo(name)),
                "with name: ", "actual: ") {
            @Override
            protected LambdaList<String> featureValueOf(List<AbookCustomUserGroupsBlock> groups) {
                try {
                    return with(groups).extract(on(AbookCustomUserGroupsBlock.class).groupName().getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }
}
