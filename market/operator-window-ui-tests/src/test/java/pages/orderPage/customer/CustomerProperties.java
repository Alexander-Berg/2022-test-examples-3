package ui_tests.src.test.java.pages.orderPage.customer;

import org.openqa.selenium.WebDriver;

public class CustomerProperties {
    private final WebDriver webDriver;

    public CustomerProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }
}
