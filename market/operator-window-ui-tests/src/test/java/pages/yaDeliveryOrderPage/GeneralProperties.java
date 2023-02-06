package ui_tests.src.test.java.pages.yaDeliveryOrderPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class GeneralProperties {
    private WebDriver webDriver;

    public GeneralProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * @return Время создания заказа
     */
    public String getOrderCreationDate() {
        return Entity.properties(webDriver).getValueField("created");
    }

    /**
     * @return Barcode заказа
     */
    public String getBarcode() {
        return Entity.properties(webDriver).getValueField("barcode");
    }

    /**
     * @return Тип доставки
     */
    public String getDeliveryType() {
        return Entity.properties(webDriver).getValueField("deliveryType");
    }

    /**
     * @return Интервал доставки
     */
    public String getDeliveryInterval() {
        return Entity.properties(webDriver).getValueField("deliveryInterval");
    }

    /**
     * @return ФИО получателя
     */
    public String getClientFullName() {
        return Entity.properties(webDriver).getValueField("recipientFullName");
    }

    /**
     * @return Телефон получателя
     */
    public String getClientNumber() {
        return Entity.properties(webDriver).getValueField("recipientPhone");
    }

    /**
     * @return Email получателя
     */
    public String getClientEmail() {
        return Entity.properties(webDriver).getValueField("recipientEmail");
    }

    /**
     * @return Адрес получателя
     */
    public String getClientsAddress() {
        return Entity.properties(webDriver).getValueField("recipientAddress");
    }
}
