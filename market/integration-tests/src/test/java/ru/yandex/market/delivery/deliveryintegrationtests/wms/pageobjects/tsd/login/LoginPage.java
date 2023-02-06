package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.login;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class LoginPage extends AbstractTsdPage {
    private final String loginFormId = "user";
    private final String passwordFormId = "password";

    @FindBy(id = loginFormId)
    private HtmlElement user;

    @FindBy(id = passwordFormId)
    private HtmlElement password;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим логин и пароль")
    public void logIn (String username, String password) {
        waitOverlayHiddenIfPresent();
        wait.until(ExpectedConditions.elementToBeClickable(By.id(loginFormId)));
        this.user.sendKeys(username);
        this.password.sendKeys(password);
        this.password.sendKeys(Keys.ENTER);
        waitSpinnerIfPresent();
    }
}
