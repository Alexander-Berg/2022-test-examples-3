package ui_tests.src.test.java.pages.ticketPage.clientTicketsTab;

import org.openqa.selenium.WebDriver;

public class ClientTicketsTab {

    private WebDriver webDriver;

    public ClientTicketsTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public RelatedTicketsTable relatedTicketsTable() {
        return new RelatedTicketsTable(webDriver);
    }
}
