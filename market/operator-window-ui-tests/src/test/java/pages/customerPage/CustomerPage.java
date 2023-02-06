package ui_tests.src.test.java.pages.customerPage;

import org.openqa.selenium.WebDriver;

public class CustomerPage {

    private WebDriver webDriver;

    public CustomerPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Header header() {
        return new Header(webDriver);
    }

    public MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }
}
