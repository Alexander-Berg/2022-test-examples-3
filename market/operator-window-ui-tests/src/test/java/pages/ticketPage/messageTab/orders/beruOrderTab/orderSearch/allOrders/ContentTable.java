package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.beruOrderTab.orderSearch.allOrders;

import entity.entityTable.Content;
import org.openqa.selenium.WebDriver;

public class ContentTable {
    private final WebDriver webDriver;

    public ContentTable(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Content Content() {
        return new Content(webDriver);
    }
}
