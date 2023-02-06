package tools;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class Action {

    public static void moveToElementAndClick(WebElement element, WebDriver driver) {
        new Actions(driver).moveToElement(element).click().build().perform();
    }
}
