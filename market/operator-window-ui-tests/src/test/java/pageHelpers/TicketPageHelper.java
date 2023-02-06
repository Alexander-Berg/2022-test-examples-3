package ui_tests.src.test.java.pageHelpers;

import Classes.Comment;
import Classes.RelatedTicket;
import Classes.deliveryOrder.DeliveryOrder;
import Classes.order.DeliveryProperties;
import Classes.order.MainProperties;
import Classes.order.Order;
import Classes.order.PaymentProperties;
import Classes.ticket.Properties;
import org.openqa.selenium.WebDriver;
import pages.Pages;
import pages.ticketPage.TicketPage;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class TicketPageHelper {
    private WebDriver webDriver;
    public TicketPageHelper(WebDriver webDriver){
        this.webDriver = webDriver;
    }

    /**
     * Поиск обращения через скрипты админки и открытие страницы обращения
     * @param ticketTitle заголовок обращения
     */
    public void findATicketByTitleAndOpenIt(String ticketTitle){
        try {
        String idRecord = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket",ticketTitle);
        Pages.navigate(webDriver).openPageByMetaClassAndID(idRecord);}
        catch (Throwable throwable){
            throw new Error("Не нашлось обращения с темой "+ticketTitle +"\n"+throwable);
        }
    }

    /**
     * Получить основную информацию с превью заказа
     *
     * @return
     */
    public MainProperties getMainPropertiesAttribute() {
        MainProperties mainProperties = new MainProperties();
        // Получаем основную информацию
        mainProperties
                .setDateCreate(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getDateCreateFromPreview())
                .setOrderNumber(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getOrderNumberFromPreview())
                .setPaymentType(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getPaymentType())
                .setTypeMarket(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getTypeMarketFromPreview())
                .setLinkToOrderPage(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getLinkToOrderPage())
                .setStatus(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getOrderStatus());
        return mainProperties;
    }

    /**
     * Получить всю информацию о заказе
     *
     * @return
     */
    public Order getAllOrderAttributes() {
        Order orders = new Order();
        MainProperties mainProperties = new MainProperties();
        DeliveryProperties deliveryProperties = new DeliveryProperties();
        PaymentProperties paymentProperties = new PaymentProperties();

        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем вкладку с заказом
        Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Заказ");

        // Получаем основную информацию
        mainProperties = getMainPropertiesAttribute();

        // Получаем информацию о доставке
        deliveryProperties = getAllPropertyFromDeliverySectionOrders();

        // Получаем информацию об оплате
        paymentProperties
                .setCostDelivery(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getCostDelivery())
                .setOrderAmount(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getOrderAmount())
                .setPayer(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getPayer())
                .setTotalCostOrder(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getTotalCostOrder())
                .setTypePayment(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getTypePayment());

        orders
                .setDeliveryProperties(deliveryProperties)
                .setMainProperties(mainProperties)
                .setPaymentProperties(paymentProperties);

        return orders;
    }

    /**
     * Получить все данные из раздела доставки превью заказа карточки обращения
     *
     * @return
     */
    public DeliveryProperties getAllPropertyFromDeliverySectionOrders() {
        DeliveryProperties deliveryProperties = new DeliveryProperties();
        deliveryProperties
                .setAddress(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getAddress())
                .setRegion(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getRegion())
                .setComment(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getComment())
                .setConsignee(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getConsignee())
                .setOriginalDeliveryTimeFull(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getOriginalDeliveryTimeFull())
                .setDeliveryTimeFull(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getPlanningDateDelivery())
                .setTypeDelivery(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getTypeDelivery())
                .setTrackCode(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getTrackCode())
                .setCourierDataButton(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().isCourierDataButton())
                .setCountOfBoxes(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getCountOfBoxes())
                .setWeightOfOrder(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getWightOfOrder())
                .setDateOfShipment(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getDateOfShipment());

        // Получение контактов курьера, выполняется только если есть кнопка с контактами курьера
        if (Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().isCourierDataButton()) {
            Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().clickOnButtonContactCourier();
            deliveryProperties
                    .setFirstNameCourier(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getNameCourier())
                    .setPhoneCourier(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getPhoneCourier())
                    .setEstimatedDeliveryTime(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getEstimatedDeliveryTime());
        }

        // Получение номера и статуса логистического заказа с превью заказа беру
        try {
            deliveryProperties.setStatusDeliveryOrder(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getStatusDeliveryOrder())
                    .setDeliveryOrderNumber(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getDeliveryOrderNumber())
                    .setLinkToDeliveryOrderPage(Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getLinkToDeliveryOrderPage());
        } catch (Throwable t) {

        }

        return deliveryProperties;
    }

    /**
     * Получить всю информацию о логистическом заказе
     *
     * @return
     */
    public DeliveryOrder getAllDeliveryOrderAttributes() {
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        Classes.deliveryOrder.MainProperties mainProperties = new Classes.deliveryOrder.MainProperties();
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем вкладку с заказом
        Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Заказ");
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        mainProperties
                .setDeliveryOrderNumber(Pages.ticketPage(webDriver).messageTab().attributes().deliveryOrderTab().mainProperties().getDeliveryOrderNumber())
                .setStatus(Pages.ticketPage(webDriver).messageTab().attributes().deliveryOrderTab().mainProperties().getStatusOrder())
                .setRecipientFullName(Pages.ticketPage(webDriver).messageTab().attributes().deliveryOrderTab().mainProperties().getRecipientFullName())
                .setRecipientAddress(Pages.ticketPage(webDriver).messageTab().attributes().deliveryOrderTab().mainProperties().getRecipientAddress());

        deliveryOrder.setMainProperties(mainProperties);
        return deliveryOrder;

    }

    /**
     * Получить все свойства
     *
     * @return
     */
    public Properties getAllProperties() {
        Properties properties = new Properties();

        properties
                .setContactEmail(Pages.ticketPage(webDriver).properties().getContactEmail())
                .setPriority(Pages.ticketPage(webDriver).properties().getPriority())
                .setCategory(Pages.ticketPage(webDriver).properties().getCategory())
                .setOrder(Pages.ticketPage(webDriver).properties().getOrderNumber())
                .setService(Pages.ticketPage(webDriver).properties().getService())
                .setTeam(Pages.ticketPage(webDriver).properties().getTeam())
                .setStatus(Pages.ticketPage(webDriver).properties().getStatus());

        return properties;
    }

    /**
     * Получить все комментарии
     *
     * @return
     */
    public List<Comment> getAllComments() {
        return Pages.ticketPage(webDriver).messageTab().comments().getComments();
    }

    /**
     * Изменить свойства обращения на странице редактирования
     *
     * @param properties свойства на которые нужно изменить
     * @return
     */
    public TicketPage editProperties(Properties properties) {

        if (properties.getCategory().size() > 0) {
            Pages.ticketPage(webDriver).properties().setCategory(properties.getCategory());
        }
        if (properties.getOrder() != null) {
            Pages.ticketPage(webDriver).properties().setOrderNumber(properties.getOrder());
        }

        if (properties.getService() != null) {
            Pages.ticketPage(webDriver).properties().setService(properties.getService());
        }

        return new TicketPage(webDriver);
    }

    /**
     * Открыть вкладку Скрипты
     *
     * @return
     */
    public void openTabScripts() {
        Pages.ticketPage(webDriver).tabs().openTab("Скрипты");
    }

    /**
     * Открыть вкладку Заказ у превью обращения
     */
    public void openTabOrderOnPreviewOrder() {
        Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Заказ");
    }

    /**
     * Открыть вкладку Сообщения
     *
     * @return
     */
    public void openTabComments() {
        Pages.ticketPage(webDriver).tabs().openTab("Сообщения");
    }

    /**
     * Открыть вкладку на вкладке Скрипты
     *
     * @param tabName имя кладки
     */
    public void openTabOnScriptTab(String tabName) {
        Pages.ticketPage(webDriver).scriptsTab().tabs().openTab(tabName);
    }

    /**
     * Открыть страницу редактирования
     */
    public void openEditPageTicket() {
        // Открываем страницу редактирования
        Pages.ticketPage(webDriver).header().clickOnEditTicketButton();
    }


    /**
     * Сохранить изменения обращения
     */
    public void saveChangesToTicket() {
        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();
    }


    /**
     * Взять тикет в работу
     */
    public void takeTicketInWork() {
        // Открываем вкладку внешнего письма
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickOutputMailTab();
        // Берем тикет в работу через кнопку комментариев
        try {
            Pages.ticketPage(webDriver).messageTab().commentsCreation()
                    .clickCloseButton()
                    .clickTackInWorkActionButton();
        } catch (Throwable b) {
            Pages.ticketPage(webDriver).messageTab().commentsCreation().clickInWorkButton();
        }
    }


    /**
     * Получить список названий вкладок
     *
     * @return список названий вкладок
     */
    public List<String> getListTabsNamePreviewOrder() {
        try {
            return Pages.ticketPage(webDriver).messageTab().attributes().tabs().getListTabsName();
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    /**
     * Открыть вкладку поиска всех заказов
     */
    public void openTabSearchAllOrders() {
        try {
            Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Поиск заказа");
            Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Все заказы");
        } catch (Throwable t) {
            throw new Error("Не получилось открыть вкладку поиска всех заказов\n" + t);
        }
    }

    /**
     * Открыть вкладку поиска  заказов клиента
     */
    public void openTabSearchOrdersOfCustomer() {
        try {
            Pages.ticketPage(webDriver).toast().hideNotificationError();
            Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Поиск заказа");
            Pages.ticketPage(webDriver).messageTab().attributes().tabs().openTab("Заказы клиента");
        } catch (Throwable t) {
            throw new Error("Не получилось открыть вкладку поиска заказов клиента\n" + t);
        }
    }

    /**
     * Найти элемент через быстрый поиск на превью заказа
     *
     * @param title - заголовок сервера для поиска
     * @return
     */
    public void quicklyFindEntity(String title) {
        try {
            Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().toolBar().clearQuickSearch();
            Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().toolBar()
                    .setQuickSearch(title)
                    .quickSearchButtonClick();
        } catch (Throwable t) {
            throw new Error("Не удалось сделать быстрый поиск заказа \n" + t);
        }
    }

    /**
     * Открыть вкладку Внешнее письмо в блоке создания письма
     */
    public void openOutputMailTabOnMailTab() {
        Pages.ticketPage(webDriver).messageTab().commentsCreation().clickOutputMailTab();
    }

    /**
     * Выделено ли поле Плановая дата доставки
     *
     * @return
     */
    public Boolean isDeliveryTimeFullFieldHighlighted() {
        return Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().isDeliveryTimeFullFieldHighlighted();
    }

    /**
     * Добавить связь с обращением или тикетом СТ
     */
    public void addRelatedTicket(String relationType, String objectType, String searchText, String expectedSuggest) {
        Pages.ticketPage(webDriver).modalWindowLinkTicket()
                .setRelationType(relationType)
                .setObjectType(objectType)
                .setObject(searchText, expectedSuggest)
                .linkTicketButtonClick();
    }

    /**
     * Получить все связанные объекты из таблицы "Связанные обращения"
     */
    public List<RelatedTicket> getAllRelatedTickets() {
        List<RelatedTicket> res = new ArrayList<>();
        int numberOfRows = Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getNumberOfRows();
        for (int i = 1; i <= numberOfRows; i++) {
            RelatedTicket relatedTicket = new RelatedTicket();
            relatedTicket
                    .setRelationType(Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getRelationType(i))
                    .setLink(Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getLink(i))
                    .setRelatedObject(Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getRelatedObject(i))
                    .setService(Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getService(i))
                    .setStatus(Pages.ticketPage(webDriver).clientTicketsTab().relatedTicketsTable().getStatus(i))
            ;
            res.add(relatedTicket);
        }
        return res;
    }

}
