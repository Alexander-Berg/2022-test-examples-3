package ui_tests.src.test.java.tests;

import Classes.Comment;
import Classes.LoyaltyCoupon;
import Classes.OrderItem;
import Classes.order.DeliveryProperties;
import Classes.order.Order;
import Classes.order.PaymentProperties;
import basicClass.AfterAndBeforeMethods;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Critical;
import interfaces.testPriorities.Minor;
import interfaces.testPriorities.Normal;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;
import unit.Orders;

import java.util.*;

/**
 * Функциональность Карточка заказа
 */
public class TestsOrderPage {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    ;

    @BeforeClass
    public static void before() {
        webDriver = new AfterAndBeforeMethods().beforeClases(TestsOrderPage.class.getName());
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def obj = api.db.get('configuration@1') \n api.bcp.edit(obj, ['showCashback' : true])", false);
        Tools.waitElement(webDriver).waitTime(5000);
    }

    @AfterClass
    public static void after() {
        new AfterAndBeforeMethods().afterClases(webDriver);
    }

    @InfoTest(descriptionTest = "Проверка вывода маркера Предзаказ, на странице заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-706",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-607")
    @Category({Blocker.class})
    @Test
    public void ocrm706_CheckingDisplayMarkersOrderPreOrderOnOrderPages() {
        String orderNumber = "6823803";
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(orderNumber);
        // Получаем со страницы маркеры заказа
        List<String> markersFromPage = Pages.orderPage(webDriver).header().getMarkers();

        Assert.assertTrue("На странице заказа не выводится маркер 'Предзаказ'", markersFromPage.contains("Предзаказ"));
    }

    @InfoTest(descriptionTest = "проверка вывода маркера \"подозрение на фрод\", на странице заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-709",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-618")
    @Category({Blocker.class})
    @Test
    public void ocrm709_CheckingDisplayMarkersOrderAntiFraudOnOrderPages() {
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarkersOrderAntiFraudAndMarkersCustomerVIP();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Получаем со страницы маркеры заказа
        List<String> markersFromPage = Pages.orderPage(webDriver).header().getMarkers();

        Assert.assertTrue("На странице заказа не выводится маркер 'подозрение на фрод'", markersFromPage.contains("Подозрение на фрод"));
    }

    @InfoTest(descriptionTest = "проверка вывода маркера клиента \"Заблокирован\", на странице заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-710",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-625")
    @Category({Blocker.class})
    @Test
    public void ocrm710_CheckingDisplayMarkersCustomerLockedOnOrderPages() {
        String orderNumber = "32377845";
        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(orderNumber);
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(orderNumber);
        // Получаем со страницы маркеры заказа
        List<String> markersFromPage = Pages.orderPage(webDriver).customerProperties().mainProperties().getMarkers();

        Assert.assertTrue("На странице заказа не выводится маркер 'Заблокирован'", markersFromPage.contains("Заблокирован"));
    }

    @InfoTest(descriptionTest = "проверка вывода маркера клиента \"VIP\", на странице заказа по UID клиента",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-712"
            , linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-567")
    @Category({Blocker.class})
    @Test
    public void ocrm712_CheckingDisplayMarkersCustomerVIPByUIDOnOrderPages() {
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarkersCustomerVIPByUID();

        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(order.getMainProperties().getOrderNumber());
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Получаем со страницы маркеры заказа
        List<String> markersFromPage = Pages.orderPage(webDriver).customerProperties().mainProperties().getMarkers();

        Assert.assertTrue("На странице заказа не выводится маркер 'VIP'", markersFromPage.containsAll(order.getCustomer().getMainProperties().getMarkers()));
    }

    @InfoTest(descriptionTest = "проверка вывода маркера клиента \"VIP\", на странице заказа по номеру телефона",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-713",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-568")
    @Category({Blocker.class})
    @Test
    public void ocrm713_CheckingDisplayMarkersCustomerVIPByPhoneNumberOnOrderPages() {
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarkersCustomerVIPByPhoneNumber();
        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(order.getMainProperties().getOrderNumber());
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Получаем со страницы маркеры заказа
        List<String> markersFromPage = Pages.orderPage(webDriver).customerProperties().mainProperties().getMarkers();

        Assert.assertTrue("На странице заказа не выводится маркер 'VIP'", markersFromPage.containsAll(order.getCustomer().getMainProperties().getMarkers()));
    }

    @InfoTest(
            descriptionTest = "Проверяем подтверждение заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-28",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-811"
    )
    @Category({Blocker.class})
    @Test
    @Ignore
    public void ocrm811_CheckingOrderConfirmation() {
        String statusFromPage;
        // Получаем заказ в статусе Pending и подстатусе AWAIT_CONFIRMATION
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{  \n" +
                "eq('status', '811') \n" +
                "eq('subStatus','6603')\n" +
                "eq('preorder',true)\n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(1).get()");
        // Открываем полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Нажимаем на кнопку подтверждения заказа
        Pages.orderPage(webDriver).header().clickConfirmOrderButton();
        try {
            // Подтверждаем изменение даты доставки
            Pages.orderPage(webDriver).modalWindowEditDateDelivery().clickConfirmDeliveryDateButton();
        } catch (Throwable throwable) {

        }

        // Получаем статус заказа после подтверждения заказа
        statusFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.get('" + gidOrder + "').status.title");
        // Проверяем статус заказа
        Assert.assertEquals("После подтверждения заказа не изменился статус заказа", "в обработке: Заказ комплектуется на складе партнера", statusFromPage);

    }

    @InfoTest(
            descriptionTest = "Добавление комментария при подтверждении заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-28",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-842"
    )
    @Category({Blocker.class})
    @Test
    @Ignore("Валимся из-за того что чекаутер отдает неверные даты")
    public void ocrm842_AddingACommentWhenConfirmingAnOrder() {
        List<Comment> commentsFromPage;
        Comment expectedComment;
        // Получаем заказ в статусе Pending и подстатусе AWAIT_CONFIRMATION
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{  \n" +
                "eq('status', '811') \n" +
                "eq('subStatus','6603')\n" +
                "eq('preorder',true)\n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(1).get()");
        expectedComment = new Comment()
                .setType("internal")
                .setText("Заказ " + gidOrder.replaceAll(".*T", "") + " подтвержден")
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");

        // Открываем полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Нажимаем на кнопку подтверждения заказа
        Pages.orderPage(webDriver).header().clickConfirmOrderButton();

        // Подтверждаем изменение даты доставки
        Pages.orderPage(webDriver).modalWindowEditDateDelivery().clickConfirmDeliveryDateButton();
        // Получаем список сообщений заказа
        commentsFromPage = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();


        Assert.assertTrue("Среди сообщений нет сообщения о том что заказ был подтвержден. Заказ который подтверждали - " + gidOrder, commentsFromPage.contains(expectedComment));
    }

    @InfoTest(
            descriptionTest = "Проверка создания задачи в ST с карточки заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-846",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-467"
    )
    @Category({Blocker.class})
    @Test
    @Ignore
    public void ocrm846_CreateTicketToSTFromOrderPage() {
        // Перейти на карточку заказа
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{  \n" +
                "eq('status', '806') \n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(1).get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Нажать на "Создать задачу в СТ"
        Pages.orderPage(webDriver).header().clickCreateSTTicketButton();

        // Выбрать ФОС
        Pages.orderPage(webDriver).createOrderYandexFormPage().chooseForm("Форма для автотестов");

        // Проверить, что загрузилось содержимое ФОС
        Tools.other().takeFocusIFrame(webDriver, "orderYandexForm@127938984");
        Pages.orderPage(webDriver).createOrderYandexFormPage().waitFormContent();

        // Нажать кнопку отправки ответа
        Pages.orderPage(webDriver).createOrderYandexFormPage().submitButtonClick();

        // Проверить, что отображается сообщение от трекера
        Assert.assertTrue("Не отобразился ответ от трекера",
                Pages.orderPage(webDriver).createOrderYandexFormPage().trackerResponcePrecense());
    }

    @InfoTest(
            descriptionTest = "Проверка создания комментария в карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-30",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-850"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm850_CreateCommentOnOrderPage() {
        // Список комментариев со страницы
        List<Comment> commentsFromPage;
        // Комментарий который должен получиться
        Comment expectedComment = new Comment()
                .setType("internal")
                .setText("Проверка добавления комментария " + Tools.date().generateCurrentDateAndTimeStringOfFormat("yyyy.MM.dd mm:ss"))
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");

        // Получаем рандомный заказ в статусе Pending и подстатусе AWAIT_CONFIRMATION
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{  \n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(1).get()");
        // Открываем заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // У заказа добавляем комментарий
        PageHelper.orderHelper(webDriver).addComment(expectedComment.getText());
        // Получаем список комментариев заказа
        commentsFromPage = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();

        Assert.assertTrue("У заказа не добавился комментарий который мы добавляли", commentsFromPage.contains(expectedComment));

    }

    @InfoTest(
            descriptionTest = "Проверка выдачи купона при отмене заказа в статусе 'В обработке'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-848",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-55"
    )
    @Ignore("Невозможно стабилизировать")
    @Category({Normal.class})
    @Test
    public void ocrm848_GiveCouponAfterCancellingOrderInPending() {
        boolean result = false;
        // Получить заказ в статусе "В обработке"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def idStatus = api.db.of('orderSubStatus').withFilters{\n" +
                        "  or{\n" +
                        "    eq('code','DELIVERY_PROBLEMS')\n" +
                        "    eq('code','MISSING_ITEM')\n" +
                        "  }\n" +
                        "}.list()\n" +
                        "\n" +
                        "api.db.of('order').withFilters{  \n" +
                        "  eq('status', '806') \n" +
                        "  or{\n" +
                        "    eq('subStatus',idStatus[0])\n" +
                        "    eq('subStatus',idStatus[1])\n" +
                        "  }\n" +
                        "  eq('cancellationRequestSubStatus', null)\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDateRangeForSeveralMonths(8)) +
                        "  }.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Нажать на кнопку отмены заказа
        Pages.orderPage(webDriver).header().clickCancelOrderButton();

        // Выбрать причину отмены 'Товара не оказалось в наличии' и отменить заказ
        Pages.orderPage(webDriver).modalWindowCancelOrder().setCancellationReason("Товара не оказалось в наличии");
        Pages.orderPage(webDriver).modalWindowCancelOrder().cancelButtonClick();

        // Обновить страницу
        webDriver.navigate().refresh();

        // Перейти на вкладку "Бонусы"
        Pages.orderPage(webDriver).tabs().clickBonusesTab();

        List<HashMap<String, String>> table = Entity.simpleTable(webDriver).getDateFromTable("//div[text()='Выданные купоны']/..");

        if (table.get(0).get("promoValue").equals("300,00")) {
            if (table.get(0).get("bonusReason").equals("Автоматическое начисление компенсации")) {
                result = true;
            }

        }

        // Проверить, что появился автоматически начисленный бонус
        Assert.assertTrue("Начисленный бонус имеет неверный номинал или причину выдачи", result);
    }

    @InfoTest(
            descriptionTest = "Проверка выдачи купона при отмене заказа в статусе 'Передан в доставку'",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-849",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-55"
    )
    @Category({Normal.class})
    @Test
    public void ocrm849_GiveCouponAfterCancellingOrderInDelivery() {
        boolean result = false;
        // Получить заказ в статусе "Передан в доставку"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def orders = api.db.of('order').withFilters{  \n" +
                        "eq('status', '807') \n" +
                        "eq('subStatus','76659303')\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(150).list()\n" +
                        "for(def order:orders){\n" +
                        "if (order.cancellationRequestSubStatus==null){\n" +
                        "return order;}\n" +
                        "}");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Нажать на кнопку отмены заказа
        Pages.orderPage(webDriver).header().clickCancelOrderButton();

        // Выбрать причину отмены 'Возникли проблемы во время доставки' и отменить заказ
        Pages.orderPage(webDriver).modalWindowCancelOrder().setCancellationReason("Возникли проблемы во время доставки");
        Pages.orderPage(webDriver).modalWindowCancelOrder().cancelButtonClick();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        // Ожидаем что данные прорастут на все сервера
        Tools.waitElement(webDriver).waitTime(5000);

        // Обновить страницу
        webDriver.navigate().refresh();

        // Перейти на вкладку "Бонусы"
        Pages.orderPage(webDriver).tabs().clickBonusesTab();

        List<HashMap<String, String>> table = Entity.simpleTable(webDriver).getDateFromTable("//div[text()='Выданные купоны']/..");

        if (table.get(table.size() - 1).get("promoValue").equals("300,00")) {
            if (table.get(table.size() - 1).get("bonusReason").equals("Автоматическое начисление компенсации")) {
                result = true;
            }
        }
        // Проверить, что появился автоматически начисленный бонус
        Assert.assertTrue("Начисленный бонус имеет неверный номинал или причину выдачи", result);
    }

    @InfoTest(descriptionTest = "Проверка вывода номера логистического заказа и статуса логистического заказа на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-440",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-860")
    @Category({Normal.class})
    @Test
    public void ocrm860_CheckingDisplayFieldEstimatedDeliveryTimeOnOrderPage() {
        // Переменная для свойств доставки со страницы
        DeliveryProperties deliveryPropertiesFromPage = new DeliveryProperties();
        // Эталонная переменная со свойствами заказа доставки
        DeliveryProperties expectedDeliveryProperties = new DeliveryProperties()
                .setDeliveryOrderNumber(Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getDeliveryOrderNumber())
                .setStatusDeliveryOrder(Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getStatusDeliveryOrder());
        // Получаем gid заказа со связанным заказом логистики
        String orderGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','" + Orders.getPostpaidOrderWithADeliveryOrder().getMainProperties().getOrderNumber() + "')\n" +
                "}.get()");
        // Переходим на страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(orderGid);
        // Со страницы заказа получаем номер логистического заказа и его статус
        deliveryPropertiesFromPage
                .setDeliveryOrderNumber(Pages.orderPage(webDriver).deliveryProperties().getDeliveryOrderNumber())
                .setStatusDeliveryOrder(Pages.orderPage(webDriver).deliveryProperties().getStatusDeliveryOrder());

        Assert.assertEquals("Номер либо статус логистического заказа не равен тому что мы ожидали", expectedDeliveryProperties, deliveryPropertiesFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода ссылки заказа логистики на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-440",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-861")
    @Category({Normal.class})
    @Test
    public void ocrm861_CheckingDisplayLinkToDeliveryOrderOnOrderPage() {
        // Переменная для свойств доставки со страницы
        DeliveryProperties deliveryPropertiesFromPage = new DeliveryProperties();
        // Эталонная переменная со свойствами заказа доставки
        DeliveryProperties expectedDeliveryProperties = new DeliveryProperties()
                .setLinkToDeliveryOrderPage(Orders.getPostpaidOrderWithADeliveryOrder().getDeliveryProperties().getLinkToDeliveryOrderPage());
        // Получаем gid заказа со связанным заказом логистики
        String orderGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','" + Orders.getPostpaidOrderWithADeliveryOrder().getMainProperties().getOrderNumber() + "')\n" +
                "}.get()");
        // Переходим на страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(orderGid);
        // Со станицы заказа получаем ссылку на логистический заказ
        deliveryPropertiesFromPage
                .setLinkToDeliveryOrderPage(Pages.orderPage(webDriver).deliveryProperties().getLinkToDeliveryOrderPage());

        Assert.assertEquals("Ссылка на логистический заказа не равна ожидаемой ссылке", expectedDeliveryProperties, deliveryPropertiesFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка автоматического добавления комментария в карточке заказа при отмене заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-858",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-734"
    )
    @Ignore("Невозможно стабилизировать")
    @Category({Blocker.class})
    @Test
    public void ocrm858_AutomaticCommentOnOrderPageAfterCancelling() {
        // Переменная для списка комментариев со страницы
        List<Comment> commentsFromPage = null;

        // Получить заказ в статусе PENDING с подстатусом AWAIT_CONFIRMATION
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('order').withFilters{  \n" +
                        "eq('status', '811') \n" +
                        "eq('color', 'BLUE') \n" +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(3).list()[1]");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Создать переменную для комментарий, который должен получиться
        String commentText = String.format("%s - ocrm-858",
                Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm"));
        Comment expectedComment = new Comment()
                .setType("internal")
                .setText(String.format(
                        "Заказ %s отменен с причиной: \"покупатель передумал\". Комментарий: \"%s\"",
                        Pages.orderPage(webDriver).header().getOrderNumber(), commentText))
                .setNameAndEmail("Локи Одинсон (robot-loki-odinson)");

        // Нажать на кнопку отмены заказа
        Pages.orderPage(webDriver).header().clickCancelOrderButton();

        // Выбрать причину отмены 'Покупатель передумал', оставить комментарий и отменить заказ
        Pages.orderPage(webDriver).modalWindowCancelOrder().setCancellationReason("Покупатель передумал");
        Pages.orderPage(webDriver).modalWindowCancelOrder().setComment(commentText);
        Pages.orderPage(webDriver).modalWindowCancelOrder().cancelButtonClick();


        for (int i = 0; i < 3; i++) {
            // Получить комментарии
            commentsFromPage = Pages.orderPage(webDriver).generalInformationTab().comments().getComments();
            if (commentsFromPage.contains(expectedComment)) {
                break;
            }
            webDriver.navigate().refresh();
            Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        }
        // Проверить, что среди комментариев есть автоматический
        Assert.assertTrue("У заказа после отмены не появился автоматический комментарий",
                commentsFromPage.contains(expectedComment));
    }

    @InfoTest(
            descriptionTest = "Проверка маркера 'Заказ в процессе отмены' в карточке заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-869",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-619"
    )
    @Category({Blocker.class})
    @Test
    @Ignore
    public void ocrm869_CancellingMarkerOnOrderPage() {
        // Получить заказ в статусе "В обработке", у которого пока нет маркера отмены
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{  \n" +
                        "eq('status', '806') \n" +
                        "eq('subStatus','1701')\n" +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "api.security.doAsSuperUser\n" +
                        "{\n" +
                        "  api.bcp.edit(order,['cancellationRequestSubStatus':'1007'])}\n" +
                        "return order;");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Проверить, что появился маркер "заказ в процессе отмены"
        Assert.assertTrue("Не появился маркер 'Заказ в процессе отмены'",
                Pages.orderPage(webDriver).header().getMarkers().contains("заказ в процессе отмены"));
    }

    @InfoTest(descriptionTest = "Проверка вывода статуса клиента 'неавторизован'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-555",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-872")
    @Category({Normal.class})
    @Test
    public void ocrm872_DisplayAccountStatusCustomerNotAuthorizedOnOrderPage() {
        String gid = "order@2006T6351640";
        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(gid);
        // Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);
        // Со страницы получаем статус аккаунта
        String statusFromPage = Pages.orderPage(webDriver).customerProperties().mainProperties().getStatusAuthorisation();

        Assert.assertTrue("Статус аккаунта не равен статусу который мы ожидаем. Ожидаем статус 'неавторизован (1152921504811731219)' а получили статус " + statusFromPage, statusFromPage.equals("неавторизован (1152921504811731219)"));
    }

    @InfoTest(descriptionTest = "Проверка вывода статуса клиента 'авторизован'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-555",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-874")
    @Category({Normal.class})
    @Test
    public void ocrm874_DisplayAccountStatusCustomerAuthorizedOnOrderPage() {
        // Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID("order@2006T6351647");
        // Со страницы получаем статус аккаунта
        String statusFromPage = Pages.orderPage(webDriver).customerProperties().mainProperties().getStatusAuthorisation();

        Assert.assertTrue("Статус аккаунта не равен статусу который мы ожидаем. Ожидаем статус 'авторизован (4004661923)' а получили статус " + statusFromPage, statusFromPage.equals("авторизован (4004661923)"));
    }

    @InfoTest(
            descriptionTest = "Проверка отмены заказа Покупок",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-873",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-563"
    )
    @Ignore("не удается стабилизировать")
    @Category({Blocker.class})
    @Test
    public void ocrm873_BlueOrderCancellation() {
        // Получить заказ в PENDING, не в процессе отмены, с типом маркета - Покупки
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('order').withFilters{  \n" +
                        "eq('status', '811') \n" +
                        "eq('color', 'BLUE')\n" +
                        "eq('cancellationRequestSubStatus', null)\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(2).list()[1]");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Отменить заказ
        Pages.orderPage(webDriver).header().clickCancelOrderButton();
        Pages.orderPage(webDriver).modalWindowCancelOrder().setCancellationReason("Изменяется состав заказа");
        Pages.orderPage(webDriver).modalWindowCancelOrder().cancelButtonClick();

        boolean result = false;
        String status = "";
        for (int i = 0; i < 5; i++) {
            status = Pages.orderPage(webDriver).header().getOrderStatus();
            if (status.equals("отменен")) {
                result = true;
                break;
            }
            Tools.waitElement(webDriver).waitTime(10000);
        }

        // Проверить, что заказ перешёл в статус "Отменен"
        Assert.assertTrue("Заказ не отменился. У заказа должен быть статус - 'отменен', а у обращения статус - " + status, result);
    }

    @InfoTest(
            descriptionTest = "Проверка отмены заказа - Белый DSBS",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-876",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-808"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm876_WhiteDSBSOrderCancellation() {
        // Получить заказ в обработке, не в процессе отмены, с типом маркета - Белый DSBS
        String script = "api.db.of('order').withFilters{  \n" +
                "eq('status', '806') \n" +
                "eq('color', 'WHITE')\n" +
                String.format("between ('creationDate',%s)\n", Tools.date().getWideDatesInterval()) +
                "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                ".limit(2).list()[1]";
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(script);

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Отменить заказ
        Pages.orderPage(webDriver).header().clickCancelOrderButton();
        Pages.orderPage(webDriver).modalWindowCancelOrder().setCancellationReason("Изменяется состав заказа");
        Pages.orderPage(webDriver).modalWindowCancelOrder().cancelButtonClick();

        // Проверить, что заказ перешёл в статус "Отменен"
        Tools.waitElement(webDriver).waitTime(5000);
        Assert.assertEquals("Заказ не отменился", "отменен",
                Pages.orderPage(webDriver).header().getOrderStatus());

    }

    @InfoTest(
            descriptionTest = "Проверка вывода накопленного кэшбека на странице заказа с несколькими товарами в блоке оплаты",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-686",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-863")
    @Category({Blocker.class})
    @Test
    public void ocrm863_DisplayCashBackOnOrderPageInBlockPayment() {
        // Переменная для свойств полученных со страницы
        PaymentProperties paymentPropertiesFromPage = new PaymentProperties();
        // Открытие заказа
        Pages.navigate(webDriver).openOrderPageByOrderNumber(Orders.getOrderWithCashBack1().getMainProperties().getOrderNumber());
        // Получение кэшбека из блока оплаты
        paymentPropertiesFromPage.setAccruedCashBack(Pages.orderPage(webDriver).paymentProperties().getAccruedCashBack());

        Assert.assertEquals("В блоке оплаты не вывели сумму начисления кэшбека либо она не равна той которую мы ожидаем", Orders.getOrderWithCashBack1().getPaymentProperties(), paymentPropertiesFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка вывода накопленного кэшбека на странице заказа в блоке с товарами",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-686",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-868"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm868_DisplayCashBackOnOrderPageInBlockOrderItems() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Получение gid заказа
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','" + Orders.getOrderWithCashBack1().getMainProperties().getOrderNumber() + "')\n" +
                "}.get()\n");
        // Открытие заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("У товаров указан не тот кэшбек который мы ожидаем", orderItemsFromPage.containsAll(Orders.getOrderWithCashBack1().getOrderItem()));
    }

    @InfoTest(
            descriptionTest = "Проверка вывода списанного кэшбека на странице заказа в блоке с товарами",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-686",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-871"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm871_DisplayCashBackOnOrderPageInBlockPayment() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Переменная для свойств полученных со страницы
        // Получение gid заказа
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','" + Orders.getOrderWithCashBack2().getMainProperties().getOrderNumber() + "')\n" +
                "}.get()\n");
        // Открытие заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("В блоке с товарами не вывели сумму списанного кэшбека либо она не равна той которую мы ожидаем", orderItemsFromPage.containsAll(Orders.getOrderWithCashBack2().getOrderItem()));
    }

    @InfoTest(
            descriptionTest = "Проверка создания купона из карточки заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-900",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-12"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm900_GiveCouponFromOrderPage() {
        String couponDiscountAmount;
        // Получить заказ не в процессе отмены
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def orders = api.db.of('order').withFilters{  \n" +
                        "eq('status', '806') \n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(15).list()\n" +
                        "for(def order:orders){\n" +
                        "  def res = api.db.of('loyaltyCoupon').withFilters{\n" +
                        "  eq('couponOrder',order)}.list();\n" +
                        "  if(!res){\n" +
                        "  return order;}\n" +
                        "}");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Нажать на кнопку "Выдать купон"
        // upd. 21.10.2021 Пытается нажать на кнопку когда она задизейблена, быстро фиксим 10 секундной паузой перед нажатием
        Tools.waitElement(webDriver).waitTime(10000);
        Pages.orderPage(webDriver).header().clickGiveCouponButton();

        // Выбрать причину начисления
        Pages.orderPage(webDriver).modalWindowGiveCoupon().setReason("Компенсация разницы стоимости за доставку");
        // Запомнить сумму начисления (привести к тому виду, в котором она будет отображаться в таблице)
        couponDiscountAmount = Pages.orderPage(webDriver).modalWindowGiveCoupon().getCouponDiscountAmount();
        couponDiscountAmount = couponDiscountAmount.replaceAll("\\s.*", "").replace(".", ",");
        // Выдать купон
        Pages.orderPage(webDriver).modalWindowGiveCoupon().saveButtonClick();

        // Проверить, что купон появился в карточке заказа (если отображается не сразу, проверка раз в 2 секунды 5 раз)
        for (int i = 0; i < 15; i++) {
            String codeCoupon = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('loyaltyCoupon').withFilters{\n" +
                    "eq('couponOrder','" + Tools.other().getGidFromCurrentPageUrl(webDriver) + "')\n" +
                    "}.get().code");
            if (codeCoupon == null) {
                Tools.waitElement(webDriver).waitTime(2000);
                continue;
            }
            webDriver.navigate().refresh();
            Pages.orderPage(webDriver).tabs().clickBonusesTab();
            List<HashMap<String, String>> table = Entity.simpleTable(webDriver).getDateFromTable("//div[text()='Выданные купоны']/..");
            if (table.get(0).get("code").equals(codeCoupon)) {
                boolean b1 = couponDiscountAmount.equals(table.get(0).get("promoValue"));
                boolean b2 = "Компенсация разницы стоимости за доставку".equals(table.get(0).get("bonusReason"));
                Assert.assertTrue("Данные купона в карточке отличаются от ожидаемых", b1 & b2);
                return;
            }
        }
        // Если новый купон не появился - тест не пройден
        throw new Error("Выданный купон не появился в карточке клиента");
    }

    @InfoTest(descriptionTest = "Проверка вывода значка нативного приложения на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-901")
    @Test
    @Category({Minor.class})
    public void ocrm901_AppLogoWEBVIEWIstDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage;
        //Получаем заказ и у него меняем платформу на "нативная платформа"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'APP_WEBVIEW', 'text':'APP_WEBVIEW']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");


        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с APP_WEBVIEW");
        }

        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Получаем название приложения
        platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();

        Assert.assertEquals("Приложение с которого был сделан заказ должно быть 'нативные приложения', а отобразилось " + platformFromPage, "нативные приложения", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода значка десктопного клиента на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-902")
    @Category({Minor.class})
    @Test
    public void ocrm902_AppLogoDESKTOPIstDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage;
        //Получаем заказ и у него меняем платформу на "DESKTOP"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'DESKTOP', 'text':'DESKTOP']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");

        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с DESKTOP");
        }
        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Получаем название приложения
        platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();

        Assert.assertEquals("Приложение с которого был сделан заказ должно быть 'десктопный клиент', а вывели " + platformFromPage, "десктопный клиент", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода значка мобильного браузера на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-903")
    @Category({Minor.class})
    @Test
    public void ocrm903_AppLogoMOBILEBROWSERIstDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage;
        //Получаем заказ и у него меняем платформу на "мобильный браузер"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'MOBILE_BROWSER', 'text':'MOBILE_BROWSER']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");
        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с MOBILE_BROWSER");
        }

        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Получаем название приложения
        platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();

        Assert.assertEquals("Приложение с которого был сделан заказ должно быть 'мобильный браузер', а вывели " + platformFromPage, "мобильный браузер", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода значка IOS на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-904")
    @Category({Minor.class})
    @Test
    public void ocrm904_AppLogoIOSIstDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage;
        //Получаем заказ и у него меняем платформу на "IOS"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'IOS', 'text':'IOS']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");
        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с IOS");
        }
        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Получаем название приложения
        platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();

        Assert.assertEquals("Приложение с которого был сделан заказ должно быть 'приложение IOS', а вывели " + platformFromPage, "приложение IOS", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка вывода значка Android на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-905")
    @Category({Minor.class})
    @Test
    public void ocrm905_AppLogoANDROIDIstDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage;
        //Получаем заказ и у него меняем платформу на "ANDROID"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'ANDROID', 'text':'ANDROID']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");
        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с ANDROID");
        }

        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Получаем название приложения
        platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();

        Assert.assertEquals("Приложение с которого был сделан заказ должно быть 'приложение Android', а вывели " + platformFromPage, "приложение Android", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка не вывода значка, если не смогли определить приложение с которого сделан заказ на карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-524",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-906")
    @Category({Minor.class})
    @Test
    public void ocrm906_AppLogoIsNotDisplayedOnOrderPage() {
        //название приложения с которого был сделан заказ
        String platformFromPage = "";
        //Получаем заказ и у него меняем платформу на "UNKNOWN"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(12) + ")\n" +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "def newValue = [:]\n" +
                        "def newValue2 = [:]\n" +
                        "newValue2<<['value':'UNKNOWN', 'text':'UNKNOWN']\n" +
                        "newValue << order.properties\n" +
                        "newValue.platform << newValue2\n" +
                        "api.bcp.edit(order, ['properties': newValue])");
        if (gidOrder == null) {
            Assert.fail("На стенде нет заказов созданных с UNKNOWN");
        }

        //Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        //Ожидаем падения метода получения названия приложения
        try {
            platformFromPage = Pages.orderPage(webDriver).header().getNameOfDistributionPlatform();
        } catch (Throwable throwable) {
            if (!throwable.getMessage().contains("Не удалось получить название платформы с которой был создан заказ")) {
                throw new Error(throwable);
            }
        }

        Assert.assertEquals("Приложение с которого был сделан заказ не должно выводиться, а вывели " + platformFromPage, "", platformFromPage);
    }

    @InfoTest(descriptionTest = "Проверка ссылки в ABO карточки заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-579",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-907")
    @Category({Blocker.class})
    @Test
    public void ocrm907_ABOLinkOnOrderPage() {
        String orderNumber;
        // Переменные для ожидаемых ссылок
        String expectedHref;
        String expectedLink;
        // Переменные для фактических ссылок
        String hrefFromPage;
        String linkFromPage;

        //Получить любой актуальный заказ
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('order').withFilters{  \n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()");

        // Открыть полученный заказ и получить его номер
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        orderNumber = Pages.orderPage(webDriver).header().getOrderNumber();

        // Ожидаемая ссылка под кнопкой "показать заказ в AБО"
        expectedHref = String.format("https://abo.market.yandex-team.ru/order/%s",
                orderNumber);
        // Ожидаемая ссылка в АБО
        expectedLink = "https://abo.market.yandex-team.ru/order/" + orderNumber;

        // Получить адрес ссылки под кнопкой  со страницы "показать заказ в AБО"
        hrefFromPage = Pages.orderPage(webDriver).header().getABOButtonHref();

        // Нажать на кнопку "показать заказ в АБО"
        Pages.orderPage(webDriver).header().clickABOButton();

        // Переключиться на открывшуюся вкладку и получить адрес открытой в ней страницы
        linkFromPage = webDriver.getCurrentUrl();

        // Сверить значения
        Assert.assertEquals("Неверная ссылка в ABO", expectedHref, hrefFromPage);
        Assert.assertEquals("Открылась неверная страница ABO", expectedLink, linkFromPage);
    }

    @InfoTest(descriptionTest = "Вывод корректных данных в поле Также в составе мультизаказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-561",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-942")
    @Category({Normal.class})
    @Test
    public void ocrm942_CorrectDataInMultiorderField() {
        // Переменная для текста из поля Также в составе мультизаказа
        String multiOrderFieldText;

        // Получаем эталонный заказ
        Order order = Orders.getMultiOrder();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Получаем со страницы значение поля Также в составе мультизаказа
        multiOrderFieldText = Pages.orderPage(webDriver).deliveryProperties().getMultiOrderFieldText();

        Assert.assertEquals("Значение в поле Также в составе мультизаказа совпадает с ожидаемым", multiOrderFieldText, order.getDeliveryProperties().getNumberOfMultiOrder());
    }

    @InfoTest(descriptionTest = "В поле Также в составе мультизаказа ссылка на нужный заказ",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-561",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-943")
    @Category({Normal.class})
    @Test
    public void ocrm943_CorrectLinkInMultiorderField() {
        // Переменная для ссылки из поля Также в составе мультизаказа
        String multiOrderFieldLink;

        // Получаем эталонный заказ
        Order order = Orders.getMultiOrder();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Получаем со страницы ссылку из поля Также в составе мультизаказа
        multiOrderFieldLink = Pages.orderPage(webDriver).deliveryProperties().getMultiOrderLinks();

        Assert.assertEquals("Ссылка в поле Также в составе мультизаказа совпадает с ожидаемой", multiOrderFieldLink, order.getDeliveryProperties().getLinkToMultiOrderPage());
    }

    @InfoTest(descriptionTest = "Открытие заказа в новой вкладке при нажатии на ссылку в поле Также в составе мультизаказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-561",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-944")
    @Category({Normal.class})
    @Test
    public void ocrm944_OpenOrderInNewTabOnClickToMultiOrderLink() {
        // Переменная для url страницы
        String urlDeliveryService;

        // Получаем эталонный заказ
        Order order = Orders.getMultiOrder();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());
        // Нажимаем на ссылку из поля Также в составе мультизаказа
        Pages.orderPage(webDriver).deliveryProperties().clickOnMultiOrderLinkAndSetFocusOnNewTab();
        // Получаем url открывшейся страницы
        urlDeliveryService = webDriver.getCurrentUrl();

        Assert.assertEquals("Открылась карточка заказа с url из поля Также в составе мультизаказа", urlDeliveryService, order.getDeliveryProperties().getLinkToMultiOrderPage());
    }

    @InfoTest(
            descriptionTest = "Проверка вывода планового кэшбека на странице заказа в блоке с товарами",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-969",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-971"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm971_DisplayCashBackOnOrderPageInBlockOrderItems() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Получение gid заказа
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','" + Orders.getOrderWithCashBack3().getMainProperties().getOrderNumber() + "')\n" +
                "}.get()\n");
        // Открытие заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("У товаров указан не тот кэшбек который мы ожидаем. Ожидали что будет " + Orders.getOrderWithCashBack3().getOrderItem().toString() + ", а вывелось " + orderItemsFromPage.toString(), orderItemsFromPage.containsAll(Orders.getOrderWithCashBack3().getOrderItem()));
    }

    @InfoTest(
            descriptionTest = "Проверка отображения маркера товарных позиций – Click&Collect (Витрина)",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-789",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-981"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm981_DisplayOrderMarketTitleClickCollect() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarketTitleClickCollect();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());

        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("У товара указан не Click&Collect-маркер товарной позиции", orderItemsFromPage.containsAll(order.getOrderItem()));
    }

    @InfoTest(
            descriptionTest = "Проверка отображения маркера товарных позиций – 1P и Товар 3P",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-789",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-986"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm986_DisplayOrderMarketTitle1PAnd3p() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarketTitle1PAnd3p();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());

        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("У товаров указаны не 1p и Товар 3P-маркер товарной позиции", orderItemsFromPage.containsAll(order.getOrderItem()));
    }

    @InfoTest(
            descriptionTest = "Проверка отображения маркера товарных позиций – Dropshipping (Витрина + Доставка)",
            linkFromTestCaseSanityTest = "https://testpalm.yandex-team.ru/testcase/ocrm-789",
            linkFromTestCaseAutoTest = "https://testpalm.yandex-team.ru/testcase/ocrm-987"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm987_DisplayOrderMarketTitleDropshipping() {
        // Переменная для свойств товаров со страницы
        List<OrderItem> orderItemsFromPage;
        // Получаем эталонный заказ
        Order order = Orders.getOrderWithMarketTitleDropshipping();
        // Переходим на страницу заказа по его номеру
        Pages.navigate(webDriver).openOrderPageByOrderNumber(order.getMainProperties().getOrderNumber());

        // Получение свойств товаров заказа
        orderItemsFromPage = Pages.orderPage(webDriver).generalInformationTab().orderItems().getOrderItems();

        Assert.assertTrue("У товара указан не Dropshipping-маркер товарной позиции", orderItemsFromPage.containsAll(order.getOrderItem()));
    }

    @InfoTest(descriptionTest = "Вывод подстатуса логистического заказа в статусе `обработка заказа` в карточке заказа",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1045",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1046")
    @Category({Normal.class})
    @Test
    public void ocrm1046_LogisticOrderSubstatusOnOrderPage() {
        // Переменная для свойств доставки со страницы
        DeliveryProperties deliveryPropertiesFromPage = new DeliveryProperties();

        // Эталонная переменная со свойствами заказа доставки
        DeliveryProperties expectedDeliveryProperties = new DeliveryProperties()
                .setDeliveryOrderNumber("328453/32122161")
                .setStatusDeliveryOrder("заказ доставлен");

        // Получаем gid заказа со связанным заказом логистики
        String orderGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("api.db.of('order').withFilters{\n" +
                "eq('title','32122161')\n" +
                "}.get()");

        // Переходим на страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(orderGid);

        // Со страницы заказа получаем номер логистического заказа и его подстатус
        deliveryPropertiesFromPage
                .setDeliveryOrderNumber(Pages.orderPage(webDriver).deliveryProperties().getDeliveryOrderNumber())
                .setStatusDeliveryOrder(Pages.orderPage(webDriver).deliveryProperties().getStatusDeliveryOrder());

        Assert.assertEquals("Номер либо подстатус логистического заказа не равен тому что мы ожидали", expectedDeliveryProperties, deliveryPropertiesFromPage);
    }

    @InfoTest(descriptionTest = "Скрытие ссылки на скачивание документа подтверждения покупки'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1037",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1193")
    @Category({Critical.class})
    @Test
    public void ocrm1193_HidingPurchaseConfirmationDownloadLink() {
        //Находим заказ в статусе "доставлен" и тип маркета не "Покупки"
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('order').withFilters{  \n" +
                        "eq('status', '809') \n" +
                        "not(eq('color', 'BLUE'))\n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getWideDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()");
        // Открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);
        // Находим поле "Подтверждение оплаты"
        String purchaseConfirmationFromPage = Pages.orderPage(webDriver).generalInformationTab().paymentInfo().getPurchaseConfirmation();
        // Проверяем что в поле "Подтверждение оплаты" ничего нет
        Assert.assertEquals("Поле 'Подтверждение оплаты' не пустое у заказа " + gidOrder, "", purchaseConfirmationFromPage);
    }

    @InfoTest(descriptionTest = "Проверка блока доставки если СД не яндекса",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-594, https://testpalm2.yandex-team.ru/testcase/ocrm-599, https://testpalm2.yandex-team.ru/testcase/ocrm-660",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1207")
    @Category({Blocker.class})
    @Test
    public void ocrm1207_CheckPropertyFromDeliverySectionOrdersWithNotYandexCourier() {
        // Открываем заказ
        Pages.navigate(webDriver).openOrderPageByOrderNumber(Orders.getPostpaidOrder().getMainProperties().getOrderNumber());
        // Получаем блок Доставка из заказа
        DeliveryProperties deliveryPropertiesFromPage = PageHelper.ticketPageHelper(webDriver).getAllPropertyFromDeliverySectionOrders();

        Assert.assertEquals("Свойства доставки заказа со страницы обращения не совпадает с ожидаемыми данными. На странице данные:" + deliveryPropertiesFromPage + " а ожидаем " + Orders.getPostpaidOrder().getDeliveryProperties(), Orders.getPostpaidOrder().getDeliveryProperties(), deliveryPropertiesFromPage);
    }


    @Test
    @InfoTest(descriptionTest = "Копирование номера телефона клиента",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-725",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1382")
    @Category(Minor.class)
    public void ocrm1382_CheckCopyingOfClientNumberWhenClickingOnClientNumber() {
        String order[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "between('creationDate'," + Tools.date().getDateRangeForSeveralMonths(Tools.other().getRandomNumber(1, 10)) + ")\n" +
                        "}\n" +
                        ".withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(1).get()\n" +
                        "\n" +
                        "def customer = order.customer\n" +
                        "api.security.doAsSuperUser {\n" +
                        "  api.bcp.edit(customer,['email':order.buyerEmail,'phone':order.buyerPhone])\n" +
                        "}\n" +
                        "return \\\"${order},${order.buyerPhone}\\\" "
        ).split(",");
        Tools.waitElement(webDriver).waitTime(4000);
        Pages.navigate(webDriver).openPageByMetaClassAndID(order[0]);
        Pages.orderPage(webDriver).customerProperties().mainProperties().clickPhoneNumberButton();

        Pages.orderPage(webDriver).generalInformationTab().comments().setTextComment(Keys.chord(Keys.CONTROL, "v"));

        String phoneFromPage = Pages.orderPage(webDriver).generalInformationTab().comments().getEnteredComment();
        Assert.assertEquals(order[1], phoneFromPage);
    }

    @Test
    @InfoTest(
            descriptionTest = "Проверка отображения блока \"Оплата\" на карточке заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1381",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-704"
    )
    @Category({Blocker.class})
    public void ocrm1381_ShowingPaymentInfoInOrderPage() {

        // Создать переменную с ожидаемыми данными
        HashMap<String, String> typePaymentAndPaymentAmountMap = new HashMap();
        typePaymentAndPaymentAmountMap.put("Наличными при получении", "13 890,00 ₽");
        PaymentProperties expectedPaymentProperties = new PaymentProperties()
                .setPayer("Rudal' denis")
                .setOrderAmount("13 690,00 ₽")
                .setCostDelivery("200,00 ₽")
                .setTotalCostOrder("13 890,00 ₽")
                .setCashBackSpent("685.00")
                .setTypePaymentAndPaymentAmount(typePaymentAndPaymentAmountMap);

        // Зайти на карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID("order@2104T32360899");

        // Проверить, что блок "Оплата" отображается только 1 раз
        Pages.orderPage(webDriver).paymentProperties().checkRenderingOfPaymentPropertiesBlock();

        // Создать переменную со страницы
        PaymentProperties paymentPropertiesFromPage = new PaymentProperties()
                .setPayer(Pages.orderPage(webDriver).paymentProperties().getPayer())
                .setOrderAmount(Pages.orderPage(webDriver).paymentProperties().getOrderAmount())
                .setCostDelivery(Pages.orderPage(webDriver).paymentProperties().getCostDelivery())
                .setTotalCostOrder(Pages.orderPage(webDriver).paymentProperties().getTotalCostOrder())
                .setCashBackSpent(Pages.orderPage(webDriver).paymentProperties().getAccruedCashBack())
                .setTypePaymentAndPaymentAmount(Pages.orderPage(webDriver).paymentProperties().getTypePaymentAndPaymentAmount());

        // Сравнить 2 переменные
        Assert.assertEquals("На карточке заказа информация об оплате не соответствует ожидаемой",
                expectedPaymentProperties, paymentPropertiesFromPage);
    }

    @Test
    @InfoTest(descriptionTest = "Проверка вывода примененного купона",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1376",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-661")
    @Category(Normal.class)
    public void ocrm1376_CheckingDisplayLoyaltyUsedCoupon() {
        boolean result = false;
        //Получаем gid заказа и данные примененного купона
        String s[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def loyaltyCoupons = api.db.of('loyaltyCoupon').limit(500).list()\n" +
                        "\n" +
                        "for(def loyaltyCoupon:loyaltyCoupons){\n" +
                        "\n" +
                        "def loyaltyCoupon1 = beanFactory\n" +
                        " .getBean(ru.yandex.market.ocrm.module.loyalty.MarketLoyaltyService)\n" +
                        " .getIssuedAndUsedCoinsForOrder(loyaltyCoupon.couponOrder.title).usedCoupon\n" +
                        "  if(loyaltyCoupon1){\n" +
                        "   \n" +
                        "   //return loyaltyCoupon1\n" +
                        "    return \\\"${loyaltyCoupon.couponOrder},${loyaltyCoupon1.discount},${loyaltyCoupon1.couponNominal},${loyaltyCoupon1.couponValueType},${loyaltyCoupon1.startDate},${loyaltyCoupon1.dueDate},${loyaltyCoupon1.couponRestrictions.description}\\\"\n" +
                        "  }\n" +
                        "\n" +
                        "}\n" +
                        "\n" +
                        "return false").split(",");
        String orderGid = s[0];
        //Формируем эталонный купон
        LoyaltyCoupon expectedLoyaltyCoupon = new LoyaltyCoupon()
                .setDiscount(s[1].replace(".", ","))
                .setNominal(s[2] + ",00")
                .setStartDate(Tools.date().generateCurrentDateAndTimeStringOfFormat(s[4], "E MMM dd HH:mm:ss z yyyy", "dd.MM.yyyy HH:mm"))
                .setDueDate(Tools.date().generateCurrentDateAndTimeStringOfFormat(s[5], "E MMM dd HH:mm:ss z yyyy", "dd.MM.yyyy HH:mm"));
        String couponValueType = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('loyaltyCouponValueType').withFilters{\n" +
                        "eq('code','" + s[3] + "')}.get().title"
        );
        expectedLoyaltyCoupon.setCouponValueType(couponValueType);
        // открываем страницу заказа
        Pages.navigate(webDriver).openPageByMetaClassAndID(orderGid);
        //Открываем вкладку Бонусов
        Pages.orderPage(webDriver).tabs().clickBonusesTab();
        //Получаем все данные из таблицы с примененными купонами
        List<HashMap<String, String>> table = Entity.simpleTable(webDriver).getDateFromTable("//div[text()='Примененные купоны']/..");
        //Проверяем что среди купонов со страницы есть тот который мы ожидаем
        for (HashMap<String, String> valueFromTable : table) {
            LoyaltyCoupon loyaltyCoupon = new LoyaltyCoupon()
                    .setDiscount(valueFromTable.get("discount"))
                    .setNominal(valueFromTable.get("nominal"))
                    .setCouponValueType(valueFromTable.get("couponValueType"))
                    .setStartDate(valueFromTable.get("startDate"))
                    .setDueDate(valueFromTable.get("dueDate"));
            if (expectedLoyaltyCoupon.equals(loyaltyCoupon)) {
                result = true;
                break;
            }
        }
        Assert.assertTrue("На карточке заказа неверно вывелся примененный купон или он вообще не вывелся\n" +
                "Карточка заказа - " + s[0], result);
    }

    @Test
    @InfoTest(descriptionTest = "Поиск купона по названию из карточки заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1406",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-498")
    @Category(Normal.class)
    public void ocrm1406_SearchForCouponUsingName() {
        // Получить заказ не в процессе отмены
        String gidOrder = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def orders = api.db.of('order').withFilters{  \n" +
                        "eq('status', '806') \n" +
                        String.format("between ('creationDate',%s)\n", Tools.date().getDatesInterval()) +
                        "}.withOrders(api.db.orders.desc('creationDate'))\n" +
                        ".limit(150).list()\n" +
                        "for(def order:orders){\n" +
                        "  if (order.cancellationRequestSubStatus==null){\n" +
                        "def res = api.db.of('loyaltyCoupon').withFilters{\n" +
                        "  eq('couponOrder',order)}.list();\n" +
                        "  if(!res){\n" +
                        "  return order;}\n" +
                        "}\n" +
                        "}");

        // Открыть полученный заказ
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidOrder);

        // Нажать на кнопку "Выдать купон"
        // Втыкаю затычку на 10 секунд т.к. жмет кнопку пока она не активна
        Tools.waitElement(webDriver).waitTime(10000);
        Pages.orderPage(webDriver).header().clickGiveCouponButton();

        // Выбрать причину начисления
        Pages.orderPage(webDriver).modalWindowGiveCoupon().setReason("Компенсация разницы стоимости за доставку");

        // Начать вводить часть названия в сумму начисления
        Pages.orderPage(webDriver).modalWindowGiveCoupon().setCouponDiscountAmount("актуальная",
                "300.00 (Актуальная акция)");

        // Проверить, что подставилась верная сумма начисления
        Assert.assertEquals("300.00 (Актуальная акция)", Pages.orderPage(webDriver).modalWindowGiveCoupon().getCouponDiscountAmount());

    }
}
