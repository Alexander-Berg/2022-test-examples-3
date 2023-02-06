package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.beruOrderTab;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

public class MainProperties {
    private final WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    private final String block = "//*[@style=\"grid-column: span 4 / auto;\"]";

    /**
     * Получить статус заказа
     *
     * @return
     */
    public String getOrderStatus() {
        try {
            return Entity.properties(webDriver).getValueField("//*[@style='grid-column: span 4 / auto;']", "status");
        } catch (Throwable t) {
            throw new Error("Не удалось получить статус заказа\n" + t);
        }
    }

    /**
     * Получить Номер заказа на превью заказа
     *
     * @return
     */
    public String getOrderNumberFromPreview() {
        try {
            return Entity.properties(webDriver).getValueField("orderNumberLink");
        } catch (Throwable t) {
            throw new Error("Не удалось получить номер заказа с превью заказа:\n" + t);
        }
    }

    /**
     * Получить ссылку на страницу заказа Беру
     *
     * @return
     */
    public String getLinkToOrderPage() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("orderNumberLink");
        } catch (Throwable t) {
            throw new Error("Не удалось полйчить ссылку на страницу заказа Беру\n" + t);
        }
    }

    /**
     * Получить тип маркета с превью заказа
     *
     * @return
     */
    public String getTypeMarketFromPreview() {
        try {
            return Entity.properties(webDriver).getValueField("color");
        } catch (Throwable t) {
            throw new Error("Не удалось получить тип маркета заказа:\n" + t);
        }
    }

    /**
     * Получить тип оплаты
     *
     * @return
     */
    public String getPaymentType() {
        try {
            return Entity.properties(webDriver).getValueField("paymentType");
        } catch (Throwable t) {
            throw new Error("Не удалось получить тип оплаты заказа\n" + t);
        }
    }

    /**
     * Получить дату создания с превью заказа
     *
     * @return
     */
    public String getDateCreateFromPreview() {
        try {
            return Entity.properties(webDriver).getValueField("creationDate");
        } catch (Throwable t) {
            throw new Error("Не удалось получить дату создания заказа:\n" + t);
        }
    }

    /**
     * Нажать на кнопку "Создать задачу в СТ"
     */
    public void clickCreateSTTicketButton() {
        try {
            Entity.buttons(webDriver).clickButton(block, "Создать задачу в СТ");
            Tools.waitElement(webDriver).waitVisibilityElementTheTime
                    (By.xpath("//div[text()='Создание задачи в стартрек']"), 10);
        } catch (Throwable e) {
            throw new Error("Не удалось открыть форму создания задачи в СТ \n" + e);
        }
    }

}
