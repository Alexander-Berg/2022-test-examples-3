package ui_tests.src.test.java.pages.dataImportPage;

import org.openqa.selenium.WebDriver;
import pages.dataImportPage.beruOutgoingCallTicketByClientPhonePage.BeruOutgoingCallTicketByClientPhonePage;
import pages.dataImportPage.beruOutgoingCallTicketByOrderIdPage.BeruOutgoingCallTicketByOrderIdPage;
import pages.dataImportPage.beruOutgoingTicketByEmailPage.BeruOutgoingTicketByEmailPage;
import pages.dataImportPage.beruOutgoingTicketByOrderIdPage.BeruOutgoingTicketByOrderIdPage;
import pages.dataImportPage.calcelOrdersPage.CancelOrdersPage;
import pages.dataImportPage.orderTicketsManualCreation.OrderTicketsManualCreation;
import pages.dataImportPage.ticketChangeStatusAddCommentPage.TicketChangeStatusAddCommentPage;


public class DataImportPage {
    private WebDriver webDriver;

    public DataImportPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Импорт отмены заказов
     *
     * @return
     */
    public CancelOrdersPage cancelOrdersPage() {
        return new CancelOrdersPage(webDriver);
    }

    /**
     * Импорт "Создание исходящих обращений телефонии по номеру заказа и внутреннему комментарию"
     *
     * @return
     */
    public BeruOutgoingCallTicketByOrderIdPage beruOutgoingCallTicketByOrderIdPage() {
        return new BeruOutgoingCallTicketByOrderIdPage(webDriver);
    }

    /**
     * Импорт "Создание исходящих обращений по e-mail и внешнему комментарию"
     *
     * @return
     */
    public BeruOutgoingTicketByEmailPage beruOutgoingTicketByEmailPage() {
        return new BeruOutgoingTicketByEmailPage(webDriver);
    }

    /**
     * Импорт "Смена статуса с добавлением комментариев"
     *
     * @return
     */
    public TicketChangeStatusAddCommentPage ticketChangeStatusAddComment() {
        return new TicketChangeStatusAddCommentPage(webDriver);
    }

    /**
     * Импорт "Создание исходящих обращений по номеру заказа и внешнему комментарию"
     *
     * @return
     */
    public BeruOutgoingTicketByOrderIdPage beruOutgoingTicketByOrderId() {
        return new BeruOutgoingTicketByOrderIdPage(webDriver);
    }

    /**
     * Импорт "Создание исходящих обращений телефонии по номеру телефона и внутреннему комментарию"
     *
     * @return
     */
    public BeruOutgoingCallTicketByClientPhonePage beruOutgoingCallTicketByClientPhonePage() {
        return new BeruOutgoingCallTicketByClientPhonePage(webDriver);
    }

    /**
     * Импорт "Создание обращений по ручным задачам"
     *
     * @return
     */
    public OrderTicketsManualCreation orderTicketsManualCreation() {
        return new OrderTicketsManualCreation(webDriver);
    }

}
