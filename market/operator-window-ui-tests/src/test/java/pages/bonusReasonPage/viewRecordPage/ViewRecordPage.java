package ui_tests.src.test.java.pages.bonusReasonPage.viewRecordPage;

import org.openqa.selenium.WebDriver;

public class ViewRecordPage {

    private WebDriver webDriver;

    public ViewRecordPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Properties properties() {
        return new Properties(webDriver);
    }

    public Header toolBar() {
        return new Header(webDriver);
    }
}
