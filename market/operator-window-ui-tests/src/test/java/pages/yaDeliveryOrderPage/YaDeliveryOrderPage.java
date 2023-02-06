package ui_tests.src.test.java.pages.yaDeliveryOrderPage;

import org.openqa.selenium.WebDriver;

public class YaDeliveryOrderPage {
    private WebDriver webDriver;

    public YaDeliveryOrderPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Основная информация заказа
     */
    public MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }

    /**
     * Табы заказа
     */
    public OrderTabs orderTabs() {
        return new OrderTabs(webDriver);
    }


}
