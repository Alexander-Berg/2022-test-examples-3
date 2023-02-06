package ui_tests.src.test.java.pages.ticketPage.messageTab.orders;

import entity.Tabs;
import entity.entityTable.EntityTable;
import org.openqa.selenium.WebDriver;
import pages.ticketPage.messageTab.orders.beruOrderTab.BeruOrderTab;
import pages.ticketPage.messageTab.orders.deliveryOrderTab.DeliveryOrderTab;

public class Orders {
    private final WebDriver webDriver;

    public Orders(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Блок с вкладками
     *
     * @return
     */
    public Tabs tabs() {
        return new Tabs(webDriver, "//*[@style='grid-column: span 4 / auto;']");
    }

    /**
     * Вкладки поиска
     *
     * @return
     */
    public EntityTable searchTabs() {
        return new EntityTable(webDriver);
    }

    /**
     * Вкладка  заказа беру
     *
     * @return
     */
    public BeruOrderTab beruOrderTab() {
        return new BeruOrderTab(webDriver);
    }

    /**
     * Вкладка заказа логистики
     *
     * @return
     */
    public DeliveryOrderTab deliveryOrderTab() {
        return new DeliveryOrderTab(webDriver);
    }
}
