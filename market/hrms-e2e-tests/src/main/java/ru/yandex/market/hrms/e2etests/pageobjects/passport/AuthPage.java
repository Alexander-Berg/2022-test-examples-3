package ru.yandex.market.hrms.e2etests.pageobjects.passport;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import io.qameta.allure.model.Parameter;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.hrms.e2etests.pageobjects.AbstractPage;

import static io.qameta.allure.Allure.getLifecycle;

public class AuthPage extends AbstractPage {

    @FindBy(xpath = "//input[@type='text' and @name='login']")
    private SelenideElement loginInput;

    @FindBy(xpath = "//input[@type='password' and @name='passwd']")
    private SelenideElement passwordInput;

    @FindBy(xpath = "//button[@type='submit' and @class='passport-Button']")
    private SelenideElement signInButton;

    @Override
    protected void checkPageElements() {
    }

    @Override
    protected String urlCheckRegexp() {
        return "/auth";
    }

    @Step("Вводим логин")
    public AuthPage SendKeysLogin(String input) {
        loginInput.sendKeys(input);
        return this;
    }

    @Step("Вводим пароль")
    public AuthPage SendKeysPassword(String input) {
        //Не показываем пароль в отчете
        getLifecycle().updateStep(step -> step.getParameters().clear());
        getLifecycle().updateStep(step -> step.getParameters()
                .add(new Parameter().setName("input").setValue("*****")));
        passwordInput.sendKeys(input);
        return this;
    }

    @Step("Нажимаем кнопку Войти")
    public ProfilePage ClickSignInButton() {
        signInButton.click();
        return Selenide.page(ProfilePage.class);
    }
}
