package ui_tests.src.test.java.pages.ticketPage.messageTab;

import org.openqa.selenium.WebDriver;
import pages.ticketPage.messageTab.orders.Orders;

public class MessageTab {
    private final WebDriver webDriver;

    public MessageTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Блок комментариев
     *
     * @return
     */
    public entity.comments.Comments comments() {
        return new entity.comments.Comments(webDriver);
    }

    /**
     * Блок создания комментариев
     *
     * @return
     */
    public CommentCreation commentsCreation() {
        return new CommentCreation(webDriver);
    }

    /**
     * Блок Атрибуты (заказы)
     *
     * @return
     */
    public Orders attributes() {
        return new Orders(webDriver);
    }


}
