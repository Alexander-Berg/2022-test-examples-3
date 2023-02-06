package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class LoginPage extends AbstractPage {

    @FindBy(xpath = "//input[@type='text']")
    private SelenideElement username;

    @FindBy(xpath = "//input[@type='password']")
    private SelenideElement password;

    public LoginPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("common/loginPage"));
        wait.until(ExpectedConditions.visibilityOf(username));
        wait.until(ExpectedConditions.visibilityOf(password));
    }

    @Step("Вводим логин и пароль")
    public MenuPage login(String username, String password) {
        refreshPageIfInputHasText();
        this.username.sendKeys(username);
        this.password.sendKeys(password);
        this.password.pressEnter();
        return new MenuPage(driver);
    }

    private LoginPage refreshPageIfInputHasText() {
        boolean inputHasText = !this.username.getAttribute("value").equals("")
                || !this.password.getAttribute("value").equals("");
        if (inputHasText) driver.navigate().refresh();
        return  this;
    }
}
