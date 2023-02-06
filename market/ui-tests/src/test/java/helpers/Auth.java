package helpers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static tools.Elements.getElementById;

public class Auth {

    public static void auth(String login, String password, String testStand, WebDriver driver) {
        driver.get(testStand);
        driver.manage().window().maximize();
        WebElement loginField = getElementById("username", driver);
        WebElement passwordFiled = getElementById("password", driver);
        WebElement loginButton = getElementById("Login", driver);
        loginField.sendKeys(login);
        passwordFiled.sendKeys(password);
        loginButton.click();
    }
}
