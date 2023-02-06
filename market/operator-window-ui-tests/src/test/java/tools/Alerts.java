package ui_tests.src.test.java.tools;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;

public class Alerts {
    private WebDriver webDriver;
    private Alert alert;

    public Alerts(WebDriver webDriver) {
        this.webDriver = webDriver;
        Tools.waitElement(webDriver).waitBrowserAlert(1);
        alert = webDriver.switchTo().alert();
    }

    public Alerts accept() {
        alert.accept();
        return this;
    }

    public Alerts dismiss() {
        alert.dismiss();
        return this;
    }

    public String getText() {
        return alert.getText();
    }

    public Alerts sendKeys(String text) {
        alert.sendKeys(text);
        return this;
    }
}
