package ui_tests.src.test.java.pages.ticketPage;

import entity.AlertDanger;
import entity.Tabs;
import entity.Toast;
import org.openqa.selenium.WebDriver;
import pages.orderPage.ModalWindowCreateOrderYandexForm;
import pages.ticketPage.createTicketPage.CreateTicketPage;
import pages.ticketPage.messageTab.MessageTab;
import pages.ticketPage.properties.Properties;
import pages.ticketPage.scriptsTab.ScriptsTab;
import pages.ticketPage.сlientTicketsTab.ClientTicketsTab;

public class TicketPage {
    private WebDriver webDriver;

    public TicketPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Раздел с свойствами обращения
     *
     * @return
     */
    public Properties properties() {
        return new Properties(webDriver);
    }

    /**
     * Раздел с вкладками обращения
     *
     * @return
     */
    public Tabs tabs() {
        return new Tabs(webDriver);
    }

    /**
     * Раздел вкладки Сообщения
     *
     * @return
     */
    public MessageTab messageTab() {
        return new MessageTab(webDriver);
    }

    /**
     * Вкладка со скриптами
     *
     * @return
     */
    public ScriptsTab scriptsTab() {
        return new ScriptsTab(webDriver);
    }

    /**
     * Вкладка "Обращения клиента"
     */
    public ClientTicketsTab clientTicketsTab() {
        return new ClientTicketsTab(webDriver);
    }

    /**
     * Блок с кнопками обращений
     *
     * @return
     */
    public Header header() {
        return new Header(webDriver);
    }

    /**
     * Блок с ошибками
     *
     * @return
     */
    public AlertDanger alertDanger() {
        return new AlertDanger(webDriver);
    }

    /**
     * Блок с тостами (уведомлениями)
     *
     * @return
     */
    public Toast toast() {
        return new Toast(webDriver);
    }

    public CreateTicketPage createTicketPage() {
        return new CreateTicketPage(webDriver);
    }

    /**
     * Модальное окно создания тикета в СТ
     */
    public ModalWindowCreateOrderYandexForm createOrderYandexFormPage() {
        return new ModalWindowCreateOrderYandexForm(webDriver);
    }

    /**
     * Модальное окно добавления связи
     */
    public ModalWindowLinkTicket modalWindowLinkTicket() {
        return new ModalWindowLinkTicket(webDriver);
    }

    /**
     * Превью партнера
     *
     * @return
     */
    public PartnerPreviewTab partnerPreviewTab() {
        return new PartnerPreviewTab(webDriver);
    }
}
