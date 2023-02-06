package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class LoginPage extends AbstractWsPage {

    @FindBy(xpath = "//input[@attribute = 'userName']")
    private HtmlElement user;

    @FindBy(xpath = "//input[@attribute = 'password']")
    private HtmlElement password;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим логин и пароль на странице логина")
    public void logIn (String username, String password) {
        overlayBusy.waitUntilHidden();
        this.user.sendKeys(username);
        this.password.sendKeys(password);
        this.password.sendKeys(Keys.ENTER);

        //Для того, чтобы эту ошибку можно было поретраить
        try {
            overlayBusy.waitUntilHidden();
        } catch (TimeoutException e) {
            throw new AssertionError(e.getMessage(), e);
        }
    }
}
