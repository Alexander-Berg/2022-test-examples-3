package ru.yandex.autotests.innerpochta.steps;

import org.openqa.selenium.By;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import static junit.framework.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;

public class WizardPageSteps {

    AllureStepStorage user;
    private WebDriverRule webDriverRule;

    WizardPageSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Открываем визард")
    public WizardPageSteps opensNewWizard() {
        user.defaultSteps().opensDefaultUrlWithPostFix("/u2709/?wizard=welcome")
            .shouldSee(user.pages().WizardPage().newWelcomeWizard());
        return this;
    }

    @Step("Закрываем окно визарда, если оно появилось")
    public WizardPageSteps clicksOnCloseButtonIfCan() {
        if (withWaitFor(isPresent(), 5000).matches(user.pages().WizardPage().closeWizard())) {
            user.defaultSteps().clicksOn(user.pages().WizardPage().closeWizard());
        }
        return this;
    }

    //Theme page steps

    @Step("Выбираем случайную тему")
    public String selectsRandomTheme() {
        int size = user.pages().WizardPage().newThemeStep().themes().size();
        int numberOfTheme = Utils.getRandomNumber(size - 1, 0);
        String name = user.pages().WizardPage().newThemeStep().themes().get(numberOfTheme)
            .findElement(By.cssSelector(".radio-button__control")).getAttribute("value");
        user.defaultSteps().turnTrue(user.pages().WizardPage().newThemeStep().themes().get(numberOfTheme));
        return name;
    }

    @Step("Должна быть выбрана тема {0}")
    public WizardPageSteps shouldSeeSelectedTheme(String theme) {
        String actual_name = user.apiSettingsSteps().getUserSettings(COLOR_SCHEME);
        user.defaultSteps().shouldSee(user.pages().WizardPage().newThemeStep().selectedTheme());
        String nameActual = user.pages().WizardPage().newThemeStep().selectedTheme()
            .findElement(By.cssSelector(".radio-button__control")).getAttribute("value");
        assertEquals("Тема главной страницы изменилась неверно!", theme, nameActual);
        assertEquals("Тема главной страницы не сохранилась!", theme, actual_name);
        return this;
    }

    //Collector page steps

    @Step("Должен появиться попап о Максимуме сборщиков")
    public boolean isPresentPopUpAboutMaxCollectors() {
        return withWaitFor(isPresent())
            .matches(user.pages().CollectorSettingsPage().maxCollectorCountPopUp().closePopUpButton());
    }
}