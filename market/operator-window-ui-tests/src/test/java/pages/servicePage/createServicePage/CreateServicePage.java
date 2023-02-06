package ui_tests.src.test.java.pages.servicePage.createServicePage;

import org.openqa.selenium.WebDriver;


public class CreateServicePage {
    private WebDriver webDriver;

    public CreateServicePage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Header header() {
        return new Header(webDriver);
    }

    public Properties properties() {
        return new Properties(webDriver);
    }


}
