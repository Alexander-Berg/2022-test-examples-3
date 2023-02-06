package ui_tests.src.test.java.pages.yaDeliveryOrderPage;

import org.openqa.selenium.WebDriver;

public class OrderTabs {
    private WebDriver webDriver;

    public OrderTabs(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Общая информация заказа
     */
    public GeneralProperties generalProperties() {
        return new GeneralProperties(webDriver);
    }
}
