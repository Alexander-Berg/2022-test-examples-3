package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.deliveryOrderTab;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class MainProperties {
    private final WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        // Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//*[text()='Основные свойства']/../.."), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Получить идентификатор заказа
     *
     * @return
     */
    public String getDeliveryOrderNumber() {
        try {
            return Entity.properties(webDriver).getValueField("orderLink");
        } catch (Throwable t) {
            throw new Error("Не удалось получить идентификатор заказа \n" + t);
        }

    }

    /**
     * Получить статус заказа
     *
     * @return статус заказа
     */
    public String getStatusOrder() {
        try {
            return Entity.properties(webDriver).getValueField("status");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Статус заказа \n" + t);
        }
    }

    /**
     * Получить ФИО получателя
     *
     * @return ФИО получателя
     */
    public String getRecipientFullName() {
        try {
            return Entity.properties(webDriver).getValueField("recipientFullName");
        } catch (Throwable t) {
            throw new Error("Не удалось получить ФИО получателя \n" + t);
        }
    }

    /**
     * Получить Адрес получателя
     *
     * @return Адрес получателя
     */
    public String getRecipientAddress() {
        try {
            return Entity.properties(webDriver).getValueField("recipientAddress");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Адрес получателя \n" + t);
        }
    }

    /**
     * Получить ссылку на страницу заказа логистики
     *
     * @return
     */
    public String getLinkToDeliveryOrderPage() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("orderLink");
        } catch (Throwable t) {
            throw new Error("Не удалось полйчить ссылку на страницу заказа логистики\n" + t);
        }
    }


}
