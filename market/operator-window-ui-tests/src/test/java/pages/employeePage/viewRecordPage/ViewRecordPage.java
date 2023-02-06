package ui_tests.src.test.java.pages.employeePage.viewRecordPage;

import org.openqa.selenium.WebDriver;

public class ViewRecordPage {
    private WebDriver webDriver;

    public ViewRecordPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Tabs tabs() {
        return new Tabs(webDriver);
    }

    public MainPropertiesTab mainPropertiesTab() {
        return new MainPropertiesTab(webDriver);
    }

    public Header header() {
        return new Header(webDriver);
    }

    public TicketsTab ticketsTab() {
        return new TicketsTab(webDriver);
    }
}
