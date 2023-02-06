package ui_tests.src.test.java.tools;


import org.openqa.selenium.WebDriver;
import tools.aShot.Screenshoter;

public class Tools {

    public static ClickElement clickerElement(WebDriver webDriver) {
        return new ClickElement(webDriver);
    }

    public static Other other() {
        return new Other();
    }

    public static WaitElement waitElement(WebDriver webDriver) {
        return new WaitElement(webDriver);
    }

    public static Email email() {
        return new Email();
    }

    public static FindElement findElement(WebDriver webDriver) {
        return new FindElement(webDriver);
    }

    public static Screenshoter screenshoter(WebDriver webDriver) {
        return new Screenshoter(webDriver);
    }

    public static TabsBrowser tabsBrowser(WebDriver webDriver) {
        return new TabsBrowser(webDriver);
    }

    public static SendElement sendElement(WebDriver webDriver) {
        return new SendElement(webDriver);
    }

    public static File file() {
        return new File();
    }

    public static Scripts scripts(WebDriver webDriver) {
        return new Scripts(webDriver);
    }

    public static Alerts alerts(WebDriver webDriver) {
        return new Alerts(webDriver);
    }

    public static Differ differ() {
        return new Differ();
    }

    public static Date date() {
        return new Date();
    }

}
