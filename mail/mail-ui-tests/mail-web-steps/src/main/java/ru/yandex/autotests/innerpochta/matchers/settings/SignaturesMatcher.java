package ru.yandex.autotests.innerpochta.matchers.settings;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature.SignatureToolbarBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.ns.pages.GetPagesSteps;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class SignaturesMatcher {

    public static Matcher<WebDriver> hasSignaturesCount(Integer count) {
        return new FeatureMatcher<WebDriver, ArrayList<?>>(hasSize(count), "signatures list should be", "actual"
        ) {
            @Override
            protected ArrayList<?> featureValueOf(WebDriver wd) {
                return (ArrayList<?>) ((JavascriptExecutor) wd).executeScript("return ns.Model.get('signs').data.signs");
            }
        };
    }

    public static Matcher<WebDriverRule> signaturesCountOnPage(Integer count) {
        return new FeatureMatcher<WebDriverRule, List<SignatureToolbarBlock>>(hasSize(count), "signatures list should be", "actual"
        ) {
            @Override
            protected List<SignatureToolbarBlock> featureValueOf(WebDriverRule wd) {
                return new GetPagesSteps(wd).SenderInfoSettingsPage().blockSetupSender().signatures()
                        .signaturesList();
            }
        };
    }

}