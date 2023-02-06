package ui_tests.src.test.java.tests.testsTickets;

import Classes.Comment;
import Classes.deliveryOrder.DeliveryOrder;
import Classes.order.DeliveryProperties;
import Classes.order.MainProperties;
import Classes.order.Order;
import Classes.order.PaymentProperties;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Normal;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;
import unit.Orders;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

public class TestsPreviewOrders {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsPreviewOrders.class);

    @InfoTest(descriptionTest = "Проверка отображения превью заказа Покупки на карточке обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-436")
    @Category({Blocker.class})
    @Test
    public void ocrm436_checkDisplayingOrderPreviewOnTicketCard() {

        Ticket ticketFromPage = new Ticket();
        Order order = Orders.getPostpaidOrder();
        // Получаем последнее созданное обращение и меняем у него заказ
        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                "  .withFilters {\n" +
                "    not(eq('order', null))\n" +
                "    eq('archived', false)\n" +
                "    eq('service','beruQuestion')\n" +
                "  }\n" +
                "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                "  .limit(1)\n" +
                "  .get()\n" +
                "api.bcp.edit(ticket, ['order' : '" + order.getMainProperties().getOrderNumber() + "'])\n" +
                "return ticket");
        // Открываем полученное обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket + "/edit");

        // В открывшемся обращении переходим на вкладку "Сообщения"
        PageHelper.ticketPageHelper(webDriver).openTabComments();

        // Со страницы обращения получаем свойства, тему, сообщения, атрибуты
        ticketFromPage.setOrder(PageHelper.ticketPageHelper(webDriver).getAllOrderAttributes());

        Assert.assertEquals(order, ticketFromPage.getOrder());
    }

    @Ignore("выпилили ЯДО")
    @InfoTest(descriptionTest = "Проверка отображения превью логистического заказа на странице обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-631")
    @Category({Blocker.class})
    @Test
    public void ocrm631_ChangeViewLogisticOrder() {
        Ticket ticket = new Ticket();
        Properties properties = new Properties()
                .setService("Логистическая поддержка Т Я.До > Общие")
                .setDeliveryOrder(Orders.getDeliveryOrder().getMainProperties().getDeliveryOrderNumber());
        Comment comment = new Comment().setType("internal")
                .setText(Tools.other().getRandomText());
        ticket.setProperties(properties)
                .setComments(Collections.singletonList(comment))
                .setSubject("Test 37 " + new GregorianCalendar().getTime());
        // Открываем страницу со всеми обращениями
        Pages.navigate(webDriver).openLVAllTickets();
        // Создаём обращение с логистическим заказом
        PageHelper.tableHelper(webDriver).createNewTicket("Логистическая поддержка Я.До", ticket);

        DeliveryOrder deliveryOrder = PageHelper.ticketPageHelper(webDriver).getAllDeliveryOrderAttributes();
        Assert.assertEquals("Не весь логистический заказ отобразился на превью заказа обращения", Orders.getDeliveryOrder(), deliveryOrder);

    }

    @InfoTest(descriptionTest = "Проверка вывода ссылки на страницу заказа Покупки с превью заказа обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-632")
    @Category({Blocker.class})
    @Test
    public void ocrm632_ChangeViewLinkToOrderPageOnTicketPage() {
        // Открываем страницу обращений очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Выводим обращения чез заказов
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомное обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Открываем обращение для редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Изменяем поле Заказ на карточке обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getPostpaidOrder().getMainProperties().getOrderNumber()));
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();


        // Получаем ссылку на страницу заказа
        String linkToOrderPageFromPage = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().getLinkToOrderPage();

        Assert.assertEquals("Ссылка на страницу заказа беру не совпадает с оригиналом " + Orders.getPostpaidOrder().getMainProperties().getLinkToOrderPage() + ", она равна " + linkToOrderPageFromPage, Orders.getPostpaidOrder().getMainProperties().getLinkToOrderPage(), linkToOrderPageFromPage);
    }

    @Ignore("выпилили ЯДО")
    @InfoTest(descriptionTest = "Проверка вывода ссылки на страницу заказа логистики с превью заказа обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-633")
    @Category({Blocker.class})
    @Test
    public void ocrm633_ChangeViewLinkToDeliveryOrderPageOnTicketPage() {
        Ticket expectedTicket = new Ticket();
        // Создать переменную обращения
        Properties properties = new Properties()
                .setService("Логистическая поддержка Т Я.До > Общие")
                .setDeliveryOrder(Orders.getDeliveryOrder().getMainProperties().getDeliveryOrderNumber());
        Comment comment = new Comment().setType("internal")
                .setText(Tools.other().getRandomText());
        expectedTicket.setProperties(properties)
                .setComments(Collections.singletonList(comment))
                .setSubject("Test 39 ChangeViewLinkToDeliveryOrderPageOnTicketPage from " + new GregorianCalendar().getTime())
                .setDeliveryOrder(Orders.getDeliveryOrder());
        // Открыть список обращений
        Pages.navigate(webDriver).openLVAllTickets();
        // Создать обращение с логистическим заказом
        PageHelper.tableHelper(webDriver).createNewTicket("Логистическая поддержка Я.До", expectedTicket);

        //Перейти на страницу редактирования
        // В некоторых случаях возникает короткий лаг кнопки "Изменить" - автотест нажимает на кнопу "Изменить" до того,
        // как до конца выполняется запрос на view: в таком случае происходит запрос на edit, он возвращает 200, а уже
        // потом возвращается запрос view. Симптомно выглядит так, что кнопка "Изменить" нажимается, пропадает и
        // появляется снова. Автотест при этом считает, что кнопка не пропала, т.е. не удалось на нее правильно нажать.
        // Красивого решения не предлагаю, подкладываю соломку в виде 3 секунд ожидания.
        Tools.waitElement(webDriver).waitTime(3000);
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();

        // Получить ссылку на страницу логистического заказа
        String linkToDeliveryOrderPageFromPage = Pages.ticketPage(webDriver).messageTab().attributes().deliveryOrderTab().mainProperties().getLinkToDeliveryOrderPage();

        Assert.assertEquals("Ссылка на страницу заказа логистики не совпадает с оригиналом",
                expectedTicket.getDeliveryOrder().getMainProperties().getLinkToDeliveryOrderPage(), linkToDeliveryOrderPageFromPage);

    }

    @InfoTest(descriptionTest = "Проверка копирования трек-кода при нажатии на ссылку 'Трекинг' в превью заказа обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-634")
    @Category({Blocker.class})
    @Test
    public void ocrm634_ChangeCopyTrackCodeAfterClickTrackingLink() {

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters{ eq('service', 'beruQuestion')\n" +
                "            not(eq('order',null))}\n" +
                ".limit(1).get()\n" +
                "\n" +
                "api.bcp.edit(ticket,['order':'" + Orders.getPostpaidOrder().getMainProperties().getOrderNumber() + "'])");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Нажать на ссылку 'Трекинг'
        Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().clickOnTrackingLink();

        // Tools.tabsBrowser(webDriver).takeFocusMainTab();

        // Вставляем данные из буфера в комментарий
        Pages.ticketPage(webDriver).messageTab().commentsCreation().setTextComment(Keys.chord(Keys.CONTROL, "v"));
        // Получаем текст из комментария
        String trackCodeFromPage = Pages.ticketPage(webDriver).messageTab().commentsCreation().getEnteredComment();

        Assert.assertTrue("Трек-код из буфера не совпадает с трек-кодом из заказа", trackCodeFromPage.contains(Orders.getPostpaidOrder().getDeliveryProperties().getTrackCode()));
    }

    @InfoTest(descriptionTest = "Проверка открытия сайта СД при нажатии на ссылку Трекинг",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-635")
    @Category({Blocker.class})
    @Test
    public void ocrm635_ChangeOpenDeliveryServicePageWithNotYandexCourier() {

        // Открываем страницу обращений очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Выводим обращения чез заказов
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомное обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Открываем обращение для редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Изменяем поле Заказ на карточке обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getPostpaidOrder().getMainProperties().getOrderNumber()));
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        // Нажимаем на ссылку Трекинг
        Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().clickOnTrackingLinkAndSetFocusOnNewTab();
        // Получаем url страницы
        String urlDeliveryService = webDriver.getCurrentUrl();

        Assert.assertEquals("Открылся не сайт СД, Должна была открыться страница https://www.dpd.ru/ols/trace2/extended.do2 а открылась " + urlDeliveryService, "https://www.dpd.ru/ols/trace2/extended.do2", urlDeliveryService);
    }

    @InfoTest(descriptionTest = "Проверка отображения блока доставки если СД не яндекса",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-636")
    @Category({Blocker.class})
    @Test
    public void ocrm636_ChangePropertyFromDeliverySectionOrdersOnTicketPageWithNotYandexCourier() {

        // Открываем страницу обращений очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Выводим обращения чез заказов
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомное обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Открываем обращение для редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Изменяем поле Заказ на карточке обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getPostpaidOrder().getMainProperties().getOrderNumber()));
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();
        // Получаем блок Доставка у превью заказа
        DeliveryProperties deliveryPropertiesFromPage = PageHelper.ticketPageHelper(webDriver).getAllPropertyFromDeliverySectionOrders();

        Assert.assertEquals("Свойства доставки заказа со страницы обращения не совпадает с ожидаемыми данными. На странице данные:" + deliveryPropertiesFromPage + " а ожидаем " + Orders.getPostpaidOrder().getDeliveryProperties(), Orders.getPostpaidOrder().getDeliveryProperties(), deliveryPropertiesFromPage);

    }

    @InfoTest(descriptionTest = "Проверка выделения поля Плановая дата доставки если плановая дата доставки отличается от даты доставки при оформлении",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-641")
    @Category({Normal.class})
    @Test
    public void ocrm641_CheckingPlannedDeliveryDateFieldHighlight() {
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$beru').withFilters{\n" +
                        "eq('service','beruQuestion')\n" +
                        "eq('archived',false)\n" +
                        "}.limit(1).get()\n" +
                        "\n" +
                        "api.bcp.edit(ticket,['order':'" + Orders.getPostpaidOrder().getMainProperties().getOrderNumber() + "'])");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        boolean isHighlightingField = PageHelper.ticketPageHelper(webDriver).isDeliveryTimeFullFieldHighlighted();

        Assert.assertTrue("Плановая дата доставки не выделена цветом", isHighlightingField);

    }

    @InfoTest(descriptionTest = "Проверка вывода поля Расчётное время доставки если заказ с СД Яндекса",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-646")
    @Category({Blocker.class})
    @Test
    @Ignore("Пока не получим тестовые данные от 3PL этот тест будет падать")
    public void ocrm646_CheckingDisplayFieldEstimatedDeliveryTime() {
        // Открываем страницу обращений очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Выводим обращения чез заказов
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомное обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Открываем обращение для редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Изменяем поле Заказ на карточке обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getOrderWithYandexCourier().getMainProperties().getOrderNumber()));
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        String estimatedDeliveryTimeFromPage = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getEstimatedDeliveryTime();

        Assert.assertEquals("Поле Расчётное время доставки не совпадает с данными которые должны выводиться. В поле вывели - " + estimatedDeliveryTimeFromPage + ", а должны были вывести -" + Orders.getOrderWithYandexCourier().getDeliveryProperties().getEstimatedDeliveryTime(), Orders.getOrderWithYandexCourier().getDeliveryProperties().getEstimatedDeliveryTime(), estimatedDeliveryTimeFromPage);
    }

    @InfoTest(descriptionTest = "Проверка не вывода поля Расчётное время доставки если заказ с СД не Яндекса",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-648")
    @Category({Blocker.class})
    @Test
    @Ignore("Пока не получим тестовые данные от 3PL этот тест будет падать")
    public void ocrm648_CheckingNoDisplayFieldEstimatedDeliveryTime() {
        boolean result = false;

        // Открываем страницу обращений очереди Покупки общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Выводим обращения чез заказов
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомное обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Открываем обращение для редактирования
        PageHelper.ticketPageHelper(webDriver).openEditPageTicket();
        // Изменяем поле Заказ на карточке обращения
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getPostpaidOrder().getMainProperties().getOrderNumber()));
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        try {
            Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getEstimatedDeliveryTime();
        } catch (Throwable t) {
            if (t.getMessage().contains("Не удалось получить Расчётное время доставки")) {
                result = true;
            } else {
                throw new Error("Произошла неизвестная ошибка - " + t);
            }
        }
        Assert.assertTrue("На превью заказа с СД не яндекса выводится поле Расчётное время доставки", result);
    }

    @InfoTest(descriptionTest = "Проверка вывода статуса логического заказа и номера логистического заказа на превью заказа беру",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-649")
    @Category({Blocker.class})
    @Test
    public void ocrm649_CheckingDisplayFieldEstimatedDeliveryTime() {

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticketGid = api.db.of('ticket$beru')\n" +
                        "    .withFilters {\n" +
                        "      not(eq('order', null))\n" +
                        "      eq('archived', false)\n" +
                        "    }\n" +
                        "    .withOrders(api.db.orders.desc('creationTime'))\n" +
                        "    .limit(1)\n" +
                        "    .get()\n" +
                        "\n" +
                        "api.bcp.edit(ticketGid, ['order' : '" + Orders.getPostpaidOrderWithADeliveryOrder().getMainProperties().getOrderNumber() + "'])\n" +
                        "return ticketGid");
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        String deliveryOrderNumber = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getDeliveryOrderNumber();
        String deliveryOrderStatus = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getStatusDeliveryOrder();
        boolean isStatus = deliveryOrderStatus.equals(Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getStatusDeliveryOrder());
        boolean isNumber = deliveryOrderNumber.equals(Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getDeliveryOrderNumber());

        Assert.assertTrue("Статус и номер логистического заказа не совпадают с ожидаемыми. Тестовое обращение - " + ticketGid, isNumber == isStatus);

    }

    @InfoTest(descriptionTest = "Проверка вывода ссылки на логистический заказ в превью карточки заказа беру",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-649")
    @Category({Blocker.class})
    @Test
    public void ocrm649_CheckingLinkToDeliveryOrder() {
        // Получаем gid обращения
        String gidTitle = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$beru')\n" +
                        "  .withFilters {\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('archived', false)\n" +
                        "  }\n" +
                        "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                        "  .limit(1)\n" +
                        "  .get()\n" +
                        "\n" +
                        // Меняем у обращения номер заказа
                        "api.bcp.edit(ticket, ['order':'" + Orders.getPostpaidOrderWithADeliveryOrder().getMainProperties().getOrderNumber() + "'])\n" +
                        "return ticket;");
        // Открываем обращение по gid
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTitle);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        String linkToDeliveryOrderPage = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().getLinkToDeliveryOrderPage();

        Assert.assertEquals("Ссылка на страницу логистического заказа не совпадает с оригиналом. Должна быть ссылка - " + Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getLinkToDeliveryOrderPage() + ", а вывели ссылку -  " + linkToDeliveryOrderPage, Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getLinkToDeliveryOrderPage(), linkToDeliveryOrderPage);
    }

    @InfoTest(descriptionTest = "проверяем что у обращений входящей телефонии в превью заказа нет вкладок",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-651")
    @Category({Normal.class})
    @Test
    public void ocrm651_CheckingDoNotShowTabsOnPreviewOrderOnIncomingCallTicket() {

        // ищем рандомный тикет телефонии без заказа и указываем у него заказ
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beruIncomingCall')\n" +
                "  .withFilters {\n" +
                "    ne('order', null)\n" +
                "    eq('archived', false)\n" +
                "  }\n" +
                "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                "  .limit(1)\n" +
                "  .get()\n" +
                "\n" +
                "return ticket;");
        // Открываем найденный тикет
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        // Переходим на вкладку Сообщения
        PageHelper.ticketPageHelper(webDriver).openTabComments();
        // Получаем список названий вкладок
        List<String> listTabsNamePreviewOrder = PageHelper.ticketPageHelper(webDriver).getListTabsNamePreviewOrder();

        Assert.assertEquals("У превью заказа не должно быть вкладок", 0, listTabsNamePreviewOrder.size());
    }

    @InfoTest(descriptionTest = "Проверка отображения только вкладки Заказ на превью заказа карточки обращения Недозвон СД",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-655")
    @Category({Normal.class})
    @Test
    public void ocrm655_CheckingDisplayOnlyOrderTabOnPreviewOrderOnDeliveryServiceFailedToDeliverTicket() {
        String errorMessage = "";
        boolean condition = true;

        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('ticket').withFilters{\n" +
                "eq('service','beruDeliveryServiceFailedToDeliver')\n" +
                "}\n" +
                ".withOrders(api.db.orders.desc('creationTime'))\n" +
                ".limit(1)\n" +
                ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        // Получаем список названий вкладок
        List<String> listTabsNamePreviewOrder = PageHelper.ticketPageHelper(webDriver).getListTabsNamePreviewOrder();

        if (listTabsNamePreviewOrder.size() > 1) {
            errorMessage += "У превью заказа обращения очереди Недозвон СД  выводятся не только вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        } else if (!listTabsNamePreviewOrder.contains("Заказ")) {
            errorMessage += "У превью заказа обращения очереди Недозвон СД  не выводится вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        }

        Assert.assertTrue(errorMessage, condition);
    }

    @InfoTest(descriptionTest = "Проверка отображения только вкладки Заказ на превью заказа карточки обращения Кросс-док",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-655")
    @Category({Normal.class})
    @Test
    public void ocrm655_CheckingDisplayOnlyOrderTabOnPreviewOrderOnCrossdocCancellationTicket() {
        String errorMessage = "";
        boolean condition = true;

        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket').withFilters{\n" +
                        "eq('service','beruCrossdocCancellation')\n" +
                        "eq('metaclass','ticket$beruOutgoingCall')\n" +
                        "}\n" +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1)\n" +
                        ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        // Получаем список названий вкладок
        List<String> listTabsNamePreviewOrder = PageHelper.ticketPageHelper(webDriver).getListTabsNamePreviewOrder();

        if (listTabsNamePreviewOrder.size() > 1) {
            errorMessage += "У превью заказа обращения очереди Покупки > Отмена Dropshipping  выводятся не только вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        } else if (!listTabsNamePreviewOrder.contains("Заказ")) {
            errorMessage += "У превью заказа обращения очереди Покупки > Отмена Dropshipping  не выводится вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        }

        Assert.assertTrue(errorMessage, condition);
    }

    @InfoTest(descriptionTest = "Проверка отображения только вкладки Заказ на превью заказа карточки обращения Покупки > Подтверждение фрод-заказов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-655")
    @Category({Normal.class})
    @Test
    public void ocrm655_CheckingDisplayOnlyOrderTabOnPreviewOrderOnFraudConfirmationTicket() {
        String errorMessage = "";
        boolean condition = true;
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('ticket').withFilters{\n" +
                "eq('service','beruFraudConfirmation')\n" +
                "eq('status','closed')\n" +
                "}\n" +
                ".withOrders(api.db.orders.desc('creationTime'))\n" +
                ".limit(1)\n" +
                ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        //Получаем список названий вкладок
        List<String> listTabsNamePreviewOrder = PageHelper.ticketPageHelper(webDriver).getListTabsNamePreviewOrder();

        if (listTabsNamePreviewOrder.size() > 1) {
            errorMessage += "У превью заказа обращения очереди Покупки > Отмена Dropshipping  выводятся не только вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        } else if (!listTabsNamePreviewOrder.contains("Заказ")) {
            errorMessage += "У превью заказа обращения очереди Покупки > Отмена Dropshipping  не выводится вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        }

        Assert.assertTrue(errorMessage, condition);
    }

    @InfoTest(descriptionTest = "Проверка отображения только вкладки Заказ на превью заказа карточки обращения Покупки > Подтверждение предзаказов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-655")
    @Category({Normal.class})
    @Test
    public void ocrm655_CheckingDisplayOnlyOrderTabOnPreviewOrderOnPreorderConfirmation() {
        String errorMessage = "";
        boolean condition = true;

        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('ticket').withFilters{\n" +
                "eq('service','beruPreorderConfirmation')\n" +
                "}\n" +
                ".withOrders(api.db.orders.desc('creationTime'))\n" +
                ".limit(1)\n" +
                ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        // Получаем список названий вкладок
        List<String> listTabsNamePreviewOrder = PageHelper.ticketPageHelper(webDriver).getListTabsNamePreviewOrder();

        if (listTabsNamePreviewOrder.size() > 1) {
            errorMessage += "У превью заказа обращения очереди Покупки > Подтверждение предзаказов  выводятся не только вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        } else if (!listTabsNamePreviewOrder.contains("Заказ")) {
            errorMessage += "У превью заказа обращения очереди Покупки > Подтверждение предзаказов  не выводится вкладка 'Заказ'. Выводятся вкладки - " + listTabsNamePreviewOrder;
            condition = false;
        }

        Assert.assertTrue(errorMessage, condition);
    }

    @InfoTest(descriptionTest = "Проверка поиска заказов беру на превью заказа карточки обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-657")
    @Category({Blocker.class})
    @Test
    public void ocrm657_CheckingSearchForOrdersOnOrderPreviewOnTicketPage() {
        String orderNumber = Orders.getPostpaidOrder().getMainProperties().getOrderNumber();
        // Открываем страницу очереди беру общие вопросы
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Применяем предсохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");
        // Открываем рандомный заказ
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Закрываем всплывашки с ошибками
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем вкладку поиска заказов на превью заказ
        PageHelper.ticketPageHelper(webDriver).openTabSearchAllOrders();
        // Через быстрый поиск ищем заказ
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(orderNumber);

        // Получаем к-во найденных заказов
        int countOrders = Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().footer().getTotalEntity();

        if (countOrders == 1) {
            try {
                Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().content().openEntity(orderNumber);
                Assert.assertTrue(true);
            } catch (Throwable t) {
                Assert.fail("Не нашёлся заказ беру в превью карточки заказа\n" + t);
            }
        } else
            Assert.fail("В таблице с результатами поиска больше одного заказа. При поиске нашлось " + countOrders + " заказов");


    }

    @InfoTest(descriptionTest = "Проверка поиска заказа клиента на превью заказа карточки обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-665")
    @Category({Blocker.class})
    @Test
    public void ocrm665_CheckingSearchForOrdersOfClientOnOrderPreviewOnTicketPage() {
        boolean isSearchOrderForCustomer = true;
        boolean isSearchOrderForAllUsers = true;
        Order order = new Order().setMainProperties(new MainProperties().setOrderNumber("4851422"));

        // Переходим на заготовленное обращение
        webDriver.get(Config.getProjectURL() + "/entity/ticket@115272486");

        // Открываем вкладку поиска заказов клиента
        PageHelper.ticketPageHelper(webDriver).openTabSearchOrdersOfCustomer();
        // Через быстрый поиск ищем заказ не клиента из обращения клиента
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(Orders.getPostpaidOrder().getMainProperties().getOrderNumber());
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        try {
            PageHelper.tableHelper(webDriver).openTicketPage(Orders.getPostpaidOrder().getMainProperties().getOrderNumber(), 1);
        } catch (Throwable t) {
            if (t.getMessage().contains("Не нашлось обращение с темой")) {
                isSearchOrderForAllUsers = false;
            }
        }

        // Через быстрый поиск ищем заказ клиента
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(order.getMainProperties().getOrderNumber());
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        try {
            PageHelper.tableHelper(webDriver).openTicketPage(order.getMainProperties().getOrderNumber(), 1);
        } catch (Throwable t) {
            if (t.getMessage().contains("Не нашлось обращение с темой")) {
                isSearchOrderForCustomer = false;
            }
        }

        if (isSearchOrderForAllUsers) {
            Assert.fail("На вкладке поиска заказов клиента нашёлся заказ не принадлежащий клиенту обращения");
        } else if (!isSearchOrderForCustomer) {
            Assert.fail("На вкладке поиска заказов клиента не нашёлся заказ принадлежащий клиенту обращения");
        }

    }

    @Ignore("выпилили ЯДО")
    @InfoTest(descriptionTest = "Проверка поиска заказов логистики на превью заказа карточки обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-666")
    @Category({Blocker.class})
    @Test
    public void ocrm666_CheckingSearchForLogisticOrdersOnOrderPreviewOnTicketPage() {
        String orderNumber = Orders.getDeliveryOrder().getMainProperties().getDeliveryOrderNumber();
        // Открываем страницу очереди логистики Ядо общие
        Pages.navigate(webDriver).openLVDeliveryLogisticSupportQuestion();
        // Применяем предсохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("Обращения без заказов для автотестов");

        // Открываем рандомный заказ
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Закрываем всплывашки с ошибками
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        // Открываем вкладку поиска заказов на превью заказ
        PageHelper.ticketPageHelper(webDriver).openTabSearchAllOrders();
        // Через быстрый поиск ищем заказ
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(orderNumber);
        // Получаем к-во найденных заказов
        int countOrders = Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().footer().getTotalEntity();

        if (countOrders == 1) {
            try {
                Pages.ticketPage(webDriver).messageTab().attributes().searchTabs().content().openEntity(orderNumber);
                Assert.assertTrue(true);
            } catch (Throwable t) {
                Assert.fail("Не нашёлся заказ логистики в превью карточки заказа\n" + t);
            }
        } else
            Assert.fail("В таблице с результатами поиска больше одного заказа. При поиске нашлось " + countOrders + " заказов");

    }

    @InfoTest(descriptionTest = "Проверка поиска логистического заказа клиента на превью заказа карточки обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-667")
    @Category({Blocker.class})
    @Test
    public void ocrm667_CheckingSearchForLogisticOrdersOfClientOnOrderPreviewOnTicketPage() {
        boolean isSearchOrderForCustomer = true;
        boolean isSearchOrderForAllUsers = true;
        DeliveryOrder order = new DeliveryOrder().setMainProperties(new Classes.deliveryOrder.MainProperties().setDeliveryOrderNumber("42346/7057866"));

        // Переходим на заготовленное обращение
        webDriver.get(Config.getProjectURL() + "/entity/ticket@116352497");

        // Открываем вкладку поиска заказов клиента
        PageHelper.ticketPageHelper(webDriver).openTabSearchOrdersOfCustomer();
        // Через быстрый поиск ищем заказ не клиента из обращения клиента
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(Orders.getDeliveryOrder().getMainProperties().getDeliveryOrderNumber());
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        try {
            PageHelper.tableHelper(webDriver).openTicketPage(Orders.getDeliveryOrder().getMainProperties().getDeliveryOrderNumber(), 1);
        } catch (Throwable t) {
            if (t.getMessage().contains("Не нашлось обращение с темой")) {
                isSearchOrderForAllUsers = false;
            }
        }

        // Через быстрый поиск ищем заказ клиента
        PageHelper.ticketPageHelper(webDriver).quicklyFindEntity(order.getMainProperties().getDeliveryOrderNumber());
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        try {
            PageHelper.tableHelper(webDriver).openTicketPage(order.getMainProperties().getDeliveryOrderNumber(), 1);
        } catch (Throwable t) {
            if (t.getMessage().contains("Не нашлось обращение с темой")) {
                isSearchOrderForCustomer = false;
            }
        }

        if (isSearchOrderForAllUsers) {
            Assert.fail("На вкладке поиска заказов клиента нашёлся заказ не принадлежащий клиенту обращения");
        } else if (!isSearchOrderForCustomer) {
            Assert.fail("На вкладке поиска заказов клиента не нашёлся заказ принадлежащий клиенту обращения");
        }

    }

    @InfoTest(descriptionTest = "проверка вывода таблицы со Способам платежа и суммы платежа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-695")
    @Category({Blocker.class})
    @Test
    public void ocrm695_CheckingDisplayTypePaymentAndPaymentAmountOnPreviewOrder() {

        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                "  .withFilters {\n" +
                "    not(eq('order', null))\n" +
                "    eq('archived', false)\n" +
                "    eq('service', 'beruQuestion')\n" +
                "  }\n" +
                "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                "  .limit(1)\n" +
                "  .get()\n" +
                "api.bcp.edit(ticket, ['order' : '" + Orders.getOrderWithTypePaymentAndPaymentAmount().getMainProperties().getOrderNumber() + "'])\n" +
                "return ticket");

        // Открываем полученное обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket + "/edit");

        // Получаем со страницы данные таблицы способа оплаты и суммы платежа
        HashMap<String, String> mapFromPage = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().paymentProperties().getTypePaymentAndPaymentAmount();

        Assert.assertEquals("На странице не вывелась таблица заказа либо данные из таблицы вывелись не те которые мы ожидали", Orders.getOrderWithTypePaymentAndPaymentAmount().getPaymentProperties().getTypePaymentAndPaymentAmount(), mapFromPage);
    }
}
