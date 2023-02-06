package ru.yandex.autotests.innerpochta.matchers.settings;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import java.util.List;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.hasItems;

/**
 * Created by mabelpines on 15.03.16.
 */
public class FilterMatchers {

    public static Matcher<WebDriverRule> customFiltersNames(String... name) {
        return new FeatureMatcher<WebDriverRule, List<String>>(hasItems(name),
            "Custom filters name is", "") {
            @Override
            protected List<String> featureValueOf(WebDriverRule webDriverRule) {
                try {
                    return with(new GetPagesSteps(webDriverRule).FiltersOverviewSettingsPage().createdFilterBlocks())
                        .extract(on(ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter
                            .AlreadyCreatedFilterBlock.class).filterName().getText());
                } catch (StaleElementReferenceException e) {
                    return with(e.getMessage());
                }
            }
        };
    }
}
