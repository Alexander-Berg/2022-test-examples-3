package ui_tests.src.test.java.tests;

import Classes.customer.Customer;
import Classes.order.MainProperties;
import Classes.order.Order;
import Classes.order.PaymentProperties;
import Classes.yaDeliveryOrder.GeneralProperties;
import Classes.yaDeliveryOrder.YaDeliveryOrder;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Normal;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;

/**
 * Функциональность Второй экран
 */

public class TestsSecondView {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();;

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsSecondView.class);

    @InfoTest(
            descriptionTest = "Проверка отображения логистического заказа на втором мониторе",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-778",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-733"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm778_ShowYaDeliveryOrderOnSecondScreen() {
        // Создать переменную с ожидаемыми значениями
        YaDeliveryOrder expectedYaDeliveryOrder = new YaDeliveryOrder()
                .setMainProperties(new Classes.yaDeliveryOrder.MainProperties().setNumber("№56599/7099626"))
                .setGeneralProperties(new GeneralProperties()
                        .setOrderCreationDate("11.09.2020 11:50")
                        .setBarcode("7099626")
                        .setDeliveryType("Доставка курьером")
                        .setDeliveryInterval("12.09.2020 - 12.09.2020 (10:00 - 22:00)")
                        .setClientFullName("Ванина Анастасия")
                        .setClientNumber("+7 (910) 399-55-61 \n" +
                                "+7 910 399-55-61")
                        .setClientEmail("lulufoxx@yandex.ru")
                        .setClientsAddress(
                                "Область: Москва и Московская область\n" +
                                        "Населенный пункт: Москва\n" +
                                        "Улица: улица Земляной Вал\n" +
                                        "Дом: 1\n" +
                                        "Квартира или офис: 2\n" +
                                        "Почтовый индекс: 105064"));

        // Создать переменную для значений со страницы
        YaDeliveryOrder yaDeliveryOrderFromPage = new YaDeliveryOrder();

        // Зайти на карточку обращения со связанным логистическим заказом
        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                "eq('rawYaDeliveryOrderId','7099626')}" +
                        ".limit(1)" +
                        ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket);

        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();

        // Дождаться появления заказа на втором мониторе
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Собрать данные со страницы в переменную
        yaDeliveryOrderFromPage
                .setMainProperties(new Classes.yaDeliveryOrder.MainProperties()
                        .setNumber(Pages.yaDeliveryOrderPage(webDriver).mainProperties().getOrderNumber()))
                .setGeneralProperties(new GeneralProperties()
                        .setOrderCreationDate(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getOrderCreationDate())
                        .setBarcode(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getBarcode())
                        .setDeliveryType(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getDeliveryType())
                        .setDeliveryInterval(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getDeliveryInterval())
                        .setClientFullName(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientFullName())
                        .setClientNumber(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientNumber())
                        .setClientEmail(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientEmail())
                        .setClientsAddress(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientsAddress()));

        // Сравнить ожидаемый заказ с тем, который отображается на странице
        Assert.assertEquals("На втором мониторе информация о заказе не соответствует ожидаемой",
                expectedYaDeliveryOrder, yaDeliveryOrderFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка обновления логистического заказа на втором мониторе",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-827",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-733"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm827_UpdateYaDeliveryOrderOnSecondScreen() {
        // Создать переменную с ожидаемыми значениями
        YaDeliveryOrder expectedYaDeliveryOrder = new YaDeliveryOrder()
                .setMainProperties(new Classes.yaDeliveryOrder.MainProperties().setNumber("№130394/7328818"))
                .setGeneralProperties(new GeneralProperties()
                        .setOrderCreationDate("15.10.2020 10:23")
                        .setBarcode("7328818")
                        .setDeliveryType("Доставка в пункт выдачи заказа")
                        .setDeliveryInterval("16.10.2020 - 16.10.2020")
                        .setClientFullName("последнееимя первоеимя среднееимя")
                        .setClientNumber("+7 (777) 777-77-77")
                        .setClientEmail("ymail@y.mail")
                        .setClientsAddress(
                                "Область: Москва и Московская область\n" +
                                        "Населенный пункт: Москва\n" +
                                        "Улица: Авиаконструктора Миля ул.\n" +
                                        "Дом: 1\n" +
                                        "Почтовый индекс: 109156"));

        // Создать переменную для значений со страницы
        YaDeliveryOrder yaDeliveryOrderFromPage = new YaDeliveryOrder();

        // Зайти на карточку первого обращения со связанным логистическим заказом
        String gidRandomTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                        "not(eq('rawYaDeliveryOrderId',null))\n" +
                        "ne('rawYaDeliveryOrderId','130396')}\n" +
                        ".limit(1)\n" +
                        ".get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidRandomTicket);

        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        // Вернуться в основную вкладку
        Tools.tabsBrowser(webDriver).takeFocusMainTab();

        // Зайти на карточку второго обращения со связанным логистическим заказом
        String gidTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                        "eq('rawYaDeliveryOrderId','130396')}" +
                        ".limit(1)" +
                        ".get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTicket);

        // Переключиться на второй экран
        Tools.tabsBrowser(webDriver).takeFocusNewTab();
        // Дождаться появления заказа на втором мониторе
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Собрать данные со страницы в переменную
        yaDeliveryOrderFromPage
                .setMainProperties(new Classes.yaDeliveryOrder.MainProperties()
                        .setNumber(Pages.yaDeliveryOrderPage(webDriver).mainProperties().getOrderNumber()))
                .setGeneralProperties(new GeneralProperties()
                        .setOrderCreationDate(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getOrderCreationDate())
                        .setBarcode(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getBarcode())
                        .setDeliveryType(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getDeliveryType())
                        .setDeliveryInterval(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getDeliveryInterval())
                        .setClientFullName(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientFullName())
                        .setClientNumber(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientNumber())
                        .setClientEmail(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientEmail())
                        .setClientsAddress(Pages.yaDeliveryOrderPage(webDriver).orderTabs().generalProperties().getClientsAddress()));

        // Сравнить ожидаемый заказ с тем, который отображается на странице
        Assert.assertEquals("На втором мониторе информация о заказе не соответствует ожидаемой",
                expectedYaDeliveryOrder, yaDeliveryOrderFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка отображения сообщения 'Нет связанных данных' на втором экране, " +
                    "если у обращения нет связанного логистического заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-830",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-733"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm830_ShowNoYaDeliveryOrderOnSecondScreen() {
        // Зайти на карточку обращения со связанным логистическим заказом
        String gidRandomTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                        "not(eq('rawYaDeliveryOrderId',null))}\n" +
                        ".limit(1)\n" +
                        ".get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidRandomTicket);

        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        // Вернуться в основную вкладку
        Tools.tabsBrowser(webDriver).takeFocusMainTab();

        // Зайти на карточку обращения, у которого нет связанного логистического заказа
        String gidSecondRandomTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                        "eq('rawYaDeliveryOrderId',null)}\n" +
                        ".limit(1)\n" +
                        ".get()");
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidSecondRandomTicket);

        // Переключиться на второй экран
        Tools.tabsBrowser(webDriver).takeFocusNewTab();

        // Проверить, что на втором экране отображается надпись "Нет связанных данных"
        Assert.assertFalse("На втором экране отображаются данные, хотя их быть не должно",
                Pages.secondScreenPage(webDriver).checkRelatedData());
    }

    @InfoTest(
            descriptionTest = "Проверка пустого результата на втором мониторе, " +
                    "если в основной вкладке открыта не сущность",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-833",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-431"
    )
    @Category({Normal.class})
    @Test
    public void ocrm833_ShowNothingOnSecondScreenWithNoEntity() {

        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();

        // Проверить, что на втором экране отображается надпись "Нет связанных данных"
        Assert.assertFalse("На втором экране отображаются данные, хотя их быть не должно",
                Pages.secondScreenPage(webDriver).checkRelatedData());
    }

    @InfoTest(
            descriptionTest = "Проверка отображения связанного заказа на втором мониторе",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-834",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-431"
    )
    @Category({Normal.class})
    @Test
    public void ocrm834_ShowOrderOnSecondScreen() {
        // Создать переменную с ожидаемыми значениями
        Order expectedOrder = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7423244")
                        .setTypeMarket("Покупки")
                        .setDateCreate("от 13.11.2020, 16:05:09 (мск)"))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setFullName("Pupkin Vasily")
                                .setPhone("+7 (964) 269-39-33 ")
                                .setEmail("spbtester43081@yandex.ru")))
                .setPaymentProperties(new PaymentProperties()
                        .setPayer("Пупкин Василий")
                        .setCostDelivery("49,00 ₽")
                        .setOrderAmount("737,00 ₽"));

        // Создать переменную для значений со страницы
        Order orderFromPage = new Order();
        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(expectedOrder.getMainProperties().getOrderNumber());

        // Открываем обращение с заказом
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$firstLine').withFilters{\n" +
                "eq('order','"+expectedOrder.getMainProperties().getOrderNumber()+"')}\n" +
                ".limit(1)\n" +
                ".get()\n" +
                "\n" +
                "if (ticket==null){\n" +
                "  ticket = api.db.of('ticket$beru').withFilters{\n" +
                "  ne('channel','qualityManagement')\n" +
                "    eq('service','beruQuestion')\n" +
                "  }.limit(1).get()\n" +
                "  \n" +
                "  api.bcp.edit(ticket, ['order' : '"+expectedOrder.getMainProperties().getOrderNumber()+"'])\n" +
                "}\n" +
                "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid+"/edit");

        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        // Дождаться появления заказа на втором мониторе
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Собрать данные со страницы в переменную
        orderFromPage
                .setMainProperties(PageHelper.orderHelper(webDriver).getMainProperties())
                .setCustomer(PageHelper.orderHelper(webDriver).getCustomer())
                .setPaymentProperties(PageHelper.orderHelper(webDriver).getPaymentProperties());

        // Сравнить ожидаемый заказ с тем, который отображается на странице
        Assert.assertEquals(
                Tools.differ().format("На втором мониторе информация о заказе не соответствует ожидаемой",expectedOrder,orderFromPage),
                expectedOrder, orderFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка отображения связанного заказа на втором мониторе",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-840",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-431"
    )
    @Category({Normal.class})
    @Test
    public void ocrm840_ShowUpdatedOrderOnSecondScreen() {
        // Создать переменные с ожидаемыми значениями
        Order expectedOrder = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7427711")
                        .setTypeMarket("Покупки")
                        .setDateCreate("от 16.11.2020, 17:54:53 (мск)"))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setFullName("не указано")
                                .setPhone("+7 (287) 517-89-14 ")
                                .setEmail("spbtester32268@yandex.ru")))
                .setPaymentProperties(new PaymentProperties()
                        .setPayer("Василий Пупкин")
                        .setCostDelivery("49,00 ₽")
                        .setOrderAmount("701,00 ₽"));

        // Создать переменную для значений со страницы
        Order orderFromPage = new Order();

        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(expectedOrder.getMainProperties().getOrderNumber());

        // Открываем обращение с заказом
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$firstLine').withFilters{\n" +
                "eq('order','"+expectedOrder.getMainProperties().getOrderNumber()+"')}\n" +
                ".limit(1)\n" +
                ".get()\n" +
                "\n" +
                "if (ticket==null){\n" +
                "  ticket = api.db.of('ticket$beru').withFilters{\n" +
                "  ne('channel','qualityManagement')\n" +
                "    eq('service','beruQuestion')\n" +
                "  }.limit(1).get()\n" +
                "  \n" +
                "  api.bcp.edit(ticket, ['order' : '"+expectedOrder.getMainProperties().getOrderNumber()+"'])\n" +
                "}\n" +
                "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid+"/edit");


        // Нажать на кнопку "Второй монитор" в боковом меню
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        // Вернуться в основную вкладку
        Tools.tabsBrowser(webDriver).takeFocusMainTab();

        // Привязать к обращению другой заказ
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.bcp.edit('"+gid+"', ['order' : '"+expectedOrder.getMainProperties().getOrderNumber()+"'])");
        webDriver.navigate().refresh();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();

        // Перейти на второй монитор
        Tools.tabsBrowser(webDriver).takeFocusNewTab();
        // Дождаться появления заказа на втором мониторе
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Собрать данные со страницы в переменную
        orderFromPage
                .setMainProperties(PageHelper.orderHelper(webDriver).getMainProperties())
                .setCustomer(PageHelper.orderHelper(webDriver).getCustomer())
                .setPaymentProperties(PageHelper.orderHelper(webDriver).getPaymentProperties());

        // Сравнить ожидаемый заказ с тем, который отображается на странице
        Assert.assertEquals("На втором мониторе информация о заказе не соответствует ожидаемой",
                expectedOrder, orderFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка отображения последнего связанного заказа на втором мониторе," +
                    "если в основной вкладке больше нет приложения.",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-844",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-431"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm844_ShowOrderOnSecondScreenWithoutAppTab() {
        // Создать переменную с ожидаемыми значениями
        Order expectedOrder = new Order()
                .setMainProperties(new MainProperties()
                        .setOrderNumber("7423244")
                        .setTypeMarket("Покупки")
                        .setDateCreate("от 13.11.2020, 16:05:09 (мск)"))
                .setCustomer(new Customer()
                        .setMainProperties(new Classes.customer.MainProperties()
                                .setFullName("Pupkin Vasily")
                                .setPhone("+7 (964) 269-39-33 ")
                                .setEmail("spbtester43081@yandex.ru")))
                .setPaymentProperties(new PaymentProperties()
                        .setPayer("Пупкин Василий")
                        .setCostDelivery("49,00 ₽")
                        .setOrderAmount("737,00 ₽"));

        PageHelper.otherHelper(webDriver).buyerAndRecipientAreOnePerson(expectedOrder.getMainProperties().getOrderNumber());
        // Создать переменную для значений со страницы
        Order orderFromPage = new Order();

        // Перейти на страницу не приложения
        webDriver.get("https://yandex.ru/");

        // В новой вкладке открыть обращение со связанным заказом
        Tools.tabsBrowser(webDriver).openUrlInNewTab(Config.getProjectURL());
        Tools.tabsBrowser(webDriver).takeFocusNewTab();

        // Открываем обращение с заказом
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$firstLine').withFilters{\n" +
                "eq('order','"+expectedOrder.getMainProperties().getOrderNumber()+"')}\n" +
                ".limit(1)\n" +
                ".get()\n" +
                "\n" +
                "if (ticket==null){\n" +
                "  ticket = api.db.of('ticket$beru').withFilters{\n" +
                "  ne('channel','qualityManagement')\n" +
                "    eq('service','beruQuestion')\n" +
                "  }.limit(1).get()\n" +
                "  \n" +
                "  api.bcp.edit(ticket, ['order' : '"+expectedOrder.getMainProperties().getOrderNumber()+"'])\n" +
                "}\n" +
                "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        // Нажать на кнопку "Второй монитор" в боковом меню и дождаться появления заказа
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Вернуться на вкладку с главной Яндекса
        Tools.tabsBrowser(webDriver).takeFocusMainTab();
        Tools.waitElement(webDriver).waitTime(3000);

        // Переключиться на вкладку со вторым экраном
        Tools.tabsBrowser(webDriver).takeFocusNewTab();

        // Собрать данные со страницы в переменную
        orderFromPage
                .setMainProperties(PageHelper.orderHelper(webDriver).getMainProperties())
                .setCustomer(PageHelper.orderHelper(webDriver).getCustomer())
                .setPaymentProperties(PageHelper.orderHelper(webDriver).getPaymentProperties());

        // Сравнить ожидаемый заказ с тем, который отображается на странице
        Assert.assertEquals("На втором мониторе не отобразился последний открытый заказ",
                expectedOrder, orderFromPage);
    }

    @InfoTest(
            descriptionTest = "Проверка отображения надписи 'Нет связанных данных' в случае, " +
                    "если вкладка приложения была закрыта",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-845",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-431"
    )
    @Category({Normal.class})
    @Test
    public void ocrm845_ShowNothingOnSecondScreenWhenClosingAppTab() {
        // Открываем обращение с заказом
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$firstLine').withFilters{\n" +
                "eq('order','7423244')}\n" +
                ".limit(1)\n" +
                ".get()\n" +
                "\n" +
                "if (ticket==null){\n" +
                "  ticket = api.db.of('ticket$beru').withFilters{\n" +
                "  ne('channel','qualityManagement')\n" +
                "    eq('service','beruQuestion')\n" +
                "  }.limit(1).get()\n" +
                "  \n" +
                "  api.bcp.edit(ticket, ['order' : '7423244'])\n" +
                "}\n" +
                "return ticket");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        // Нажать на кнопку "Второй монитор" в боковом меню и дождаться появления заказа
        Pages.mainMenuOfPage(webDriver).openSecondScreen();
        Assert.assertTrue("На втором мониторе не отобразился связанный заказ",
                Pages.secondScreenPage(webDriver).waitForOrder());

        // Вернуться на основную вкладку и закрыть ее
        Tools.tabsBrowser(webDriver).takeFocusMainTab();
        Tools.tabsBrowser(webDriver).closeCurrentTab();

        // Проверить, что на втором экране отображается надпись "Нет связанных данных"
        Tools.tabsBrowser(webDriver).takeFocusNewTab();
        Assert.assertFalse("На втором экране отображаются данные, хотя их быть не должно",
                Pages.secondScreenPage(webDriver).checkRelatedData());

    }

}
