package ui_tests.src.test.java.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;
import unit.Config;

public class LoginPage {
    private WebDriver webDriver;

    public LoginPage(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "//div[@data-tid='dc9cf3d6 4e284f96']//a")
    private WebElement linkToLoginPageBy;

    @FindBy(name = "login")
    private WebElement loginInput;

    @FindBy(name = "passwd")
    private WebElement passInput;

    @FindBy(className = "passport-Button")
    private WebElement buttonLogin;
    @FindBy(xpath = "//*[text()='Другой аккаунт']")
    private WebElement linkOtherUser;


    public LoginPage clickToLinkToLoginPage() {
        Tools.clickerElement(webDriver).clickElement(linkToLoginPageBy);
        Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//div[@class='passport-Page-Content']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        return this;
    }

    public LoginPage clickOtherUsers() {
        try {
            Tools.clickerElement(webDriver).clickElement(linkOtherUser);
        } catch (Throwable throwable) {

        }

        return this;
    }

    public LoginPage setLogin(String login) {
        try {
            Tools.sendElement(webDriver).sendElement(loginInput, login);
        } catch (Throwable t) {
            throw new Error("Не удалось ввести данные в поле для ввода логина \n" + t);
        }
        return this;
    }

    public LoginPage setPass(String pass) {
        try {
            Tools.sendElement(webDriver).sendElement(passInput, pass);
        } catch (Throwable t) {
            throw new Error("Не удалось ввести данные в поле ввода пароля \n" + t);
        }
        return this;
    }

    public void loginButtonClick() {
        try {
            Tools.clickerElement(webDriver).clickElement(buttonLogin);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку авторизации \n" + t);
        }
    }
}
