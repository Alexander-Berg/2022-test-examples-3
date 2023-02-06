package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.beruOrderTab;

import org.openqa.selenium.WebDriver;

public class BeruOrderTab {
    private final WebDriver webDriver;

    public final String block = "//*[@style=\"grid-column: span 4 / auto;\"]";

    public BeruOrderTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * свойства доставки
     *
     * @return
     */
    public final DeliveryProperties deliveryProperties() {
        return new DeliveryProperties(webDriver);
    }

    /**
     * Основные свойства
     *
     * @return
     */
    public final MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }

    /**
     * Свойства оплаты
     *
     * @return
     */
    public final PaymentProperties paymentProperties() {
        return new PaymentProperties(webDriver);
    }

}
