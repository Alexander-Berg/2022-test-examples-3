package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriver;
import ru.yandex.autotests.innerpochta.ns.pages.Pages;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;

/**
 * Created by kurau.
 */
public class InitStepsRule extends ExternalResource {

    private WebDriverRule webDriverRule;
    private RestAssuredAuthRule auth;

    private AllureStepStorage user;

    public InitStepsRule(WebDriverRule webDriverRule, RestAssuredAuthRule auth) {
        this.webDriverRule = webDriverRule;
        this.auth = auth;
    }

    @Override
    protected void before() {
        user = new AllureStepStorage(webDriverRule, auth);
    }

    public AllureStepStorage user() {
        if (user == null) {
            user = new AllureStepStorage(webDriverRule, auth);
        }
        return user;
    }

    public Pages pages() {
        return new Pages(webDriverRule);
    }

    public WebDriver getDriver() {
        return webDriverRule.getDriver();
    }
}
