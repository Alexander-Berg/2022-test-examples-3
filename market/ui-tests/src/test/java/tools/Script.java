package tools;


import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Script {

    static public void scrollPageDown(WebDriver driver) {
        JavascriptExecutor jsExecuter = (JavascriptExecutor) driver;
        jsExecuter.executeScript("window.scrollTo(0, 800)");
    }

    static public void clickElement(WebElement element, WebDriver driver) {
        JavascriptExecutor jsExecuter = (JavascriptExecutor) driver;
        jsExecuter.executeScript("arguments[0].click();", element);
    }

}
