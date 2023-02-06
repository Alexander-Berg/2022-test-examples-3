package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.deliveryOrderTab;

import org.openqa.selenium.WebDriver;

public class DeliveryOrderTab {
    private final WebDriver webDriver;

    public DeliveryOrderTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Основные свойства
     *
     * @return
     */
    public final MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }
}
