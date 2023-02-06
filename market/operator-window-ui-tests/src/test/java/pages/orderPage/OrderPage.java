package ui_tests.src.test.java.pages.orderPage;


import org.openqa.selenium.WebDriver;
import pages.orderPage.customer.CustomerProperties;
import pages.orderPage.orderPage.BonusesTab;
import pages.orderPage.orderPage.MainProperties;
import pages.orderPage.orderPage.Tabs;
import pages.orderPage.orderPage.generalInformationTab.GeneralInformationTab;
import pages.orderPage.orderPage.historyTab.HistoryTab;
import pages.ticketPage.messageTab.orders.beruOrderTab.DeliveryProperties;
import pages.ticketPage.messageTab.orders.beruOrderTab.PaymentProperties;


public class OrderPage {

    private final WebDriver webDriver;

    public OrderPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Клиент
     */
    public CustomerProperties customerProperties() {
        return new CustomerProperties(webDriver);
    }

    /**
     * Свойства доставки
     */
    public DeliveryProperties deliveryProperties() {
        return new DeliveryProperties(webDriver);
    }

    /**
     * Свойства оплаты
     */
    public PaymentProperties paymentProperties() {
        return new PaymentProperties(webDriver);
    }

    /**
     * Основные свойства заказа
     */
    public MainProperties mainProperties() {
        return new MainProperties(webDriver);
    }


    public Header header() {
        return new Header(webDriver);
    }


    /**
     * Вкладки
     */
    public Tabs tabs() {
        return new Tabs(webDriver);
    }

    /**
     * Вкладка Общая информация
     *
     * @return
     */
    public GeneralInformationTab generalInformationTab() {
        return new GeneralInformationTab(webDriver);
    }

    /**
     * Вкладка "Бонусы"
     */
    public BonusesTab bonusesTab() {
        return new BonusesTab(webDriver);
    }

    /**
     * Модальное окно редактирования даты доставки
     *
     * @return
     */
    public ModalWindowEditDateDelivery modalWindowEditDateDelivery() {
        return new ModalWindowEditDateDelivery(webDriver);
    }

    /**
     * Модальное окно создания тикета в СТ
     */
    public ModalWindowCreateOrderYandexForm createOrderYandexFormPage() {
        return new ModalWindowCreateOrderYandexForm(webDriver);
    }

    /**
     * Модальное окно отмены заказа
     */
    public ModalWindowCancelOrder modalWindowCancelOrder() {
        return new ModalWindowCancelOrder(webDriver);
    }

    /**
     * Модальное окно выдачи купона
     */
    public ModalWindowGiveCoupon modalWindowGiveCoupon() {
        return new ModalWindowGiveCoupon(webDriver);
    }

    /**
     * Вкладка "История заказ"
     *
     * @return
     */
    public HistoryTab historyTab() {
        return new HistoryTab(webDriver);
    }
}
