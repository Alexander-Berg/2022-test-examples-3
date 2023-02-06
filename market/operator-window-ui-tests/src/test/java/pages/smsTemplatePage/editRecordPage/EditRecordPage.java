package ui_tests.src.test.java.pages.smsTemplatePage.editRecordPage;

import org.openqa.selenium.WebDriver;

public class EditRecordPage {
    private WebDriver webDriver;

    public EditRecordPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Properties properties() {
        return new Properties(webDriver);
    }

    public Header header() {
        return new Header(webDriver);
    }
}
