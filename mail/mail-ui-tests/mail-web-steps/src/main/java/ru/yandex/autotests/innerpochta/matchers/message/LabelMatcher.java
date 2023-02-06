package ru.yandex.autotests.innerpochta.matchers.message;

import ch.lambdaj.function.argument.InvocationException;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomLabelBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class LabelMatcher {

    public static Matcher<WebDriverRule> shouldSeeLabel(String name) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItem(containsString(name)),
                "should be label ", "") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return extract(new GetPagesSteps(webDriverRule).MessagePage().labelsNavigation().userLabels(),
                            on(CustomLabelBlock.class).labelName().getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                } catch (InvocationException e) {
                    return with(e.getMessage());
                }
            }
        };
    }
}

