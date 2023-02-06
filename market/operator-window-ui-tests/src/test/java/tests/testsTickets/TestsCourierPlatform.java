package ui_tests.src.test.java.tests.testsTickets;

import Classes.order.DeliveryProperties;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Orders;

public class TestsCourierPlatform {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();;
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCourierPlatform.class);

    @InfoTest(descriptionTest = "Проверка отображения блока доставки если СД яндекса",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-637")
    @Category({Blocker.class})
    @Test
    public void ocrm637_ChangePropertyFromDeliverySectionOrdersOnTicketPageWithYandexCourier() {

        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters{ eq('service', 'beruQuestion')\n" +
                "            }\n" +
                ".limit(1).get()\n" +
                "\n" +
                "api.bcp.edit(ticket,['order':'" + Orders.getOrderWithYandexCourier().getMainProperties().getOrderNumber() + "'])");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        //Получаем блок Доставка у превью заказа
        DeliveryProperties deliveryPropertiesFromPage = PageHelper.ticketPageHelper(webDriver).getAllPropertyFromDeliverySectionOrders();

        Assert.assertEquals("Свойства доставки заказа со страницы обращения не совпадает с ожидаемыми данными. На странице данные:" + deliveryPropertiesFromPage + " а ожидаем " + Orders.getOrderWithYandexCourier().getDeliveryProperties(), Orders.getOrderWithYandexCourier().getDeliveryProperties(), deliveryPropertiesFromPage);

    }

    @InfoTest(descriptionTest = "Проверка открытия сайта трекинга курьера СД Яндекса при нажатии на ссылку Трекинг",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-638")
    @Category({Blocker.class})
    @Test
//    @Ignore("Пока не получим тестовые данные от 3PL этот тест будет падать")
    public void ocrm638_ChangeOpenCourierTrackingPageWithYandexCourier() {
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters{ eq('service', 'beruQuestion')\n" +
                "           }\n" +
                ".limit(1).get()\n" +
                "\n" +
                "api.bcp.edit(ticket,['order':'" + Orders.getOrderWithYandexCourier().getMainProperties().getOrderNumber() + "'])\n" +
                "return ticket");
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        // Нажимаем на ссылку Трекинг
        Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().clickOnTrackingLinkAndSetFocusOnNewTab();
        // Получаем url страницы
        String urlDeliveryService = webDriver.getCurrentUrl();

        Assert.assertTrue(
                Tools.differ().format("Открылся не сайт с Трекингом курьера", "https://touch.pokupki.fslb.market.yandex.ru/tracking/2116630bb88a47679328a11a6ed66a8f?from_beru=1", urlDeliveryService),
                urlDeliveryService.contains("/tracking/55a59da2d4db4f7f9c5493ac80cdff7b"));
    }

    @InfoTest(descriptionTest = "Проверка отображения кнопки контактов курьера если СД Яндекса",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-639")
    @Category({Blocker.class})
    @Test
//    @Ignore("Пока не получим тестовые данные от 3PL этот тест будет падать")
    public void ocrm639_checkingDisplayOfCourierContactButton() {
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters{ eq('service', 'beruQuestion')\n" +
                "           }\n" +
                ".limit(1).get()\n" +
                "\n" +
                "api.bcp.edit(ticket,['order':'" + Orders.getOrderWithYandexCourier().getMainProperties().getOrderNumber() + "'])");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();

        // Проверяем, видна ли кнопка
        boolean isDisplayOfCourierContactButton = Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties().isCourierDataButton();

        Assert.assertTrue("Кнопка контактов курьера не видна в превью заказа", isDisplayOfCourierContactButton);
    }

    @InfoTest(descriptionTest = "Проверка не вывода ссылки на трекинг курьера если из курьерской платформы не пришла ссылка",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-640")
    @Category({Blocker.class})
    @Test
    public void ocrm640_CheckingIfLinkToTrackingCourierIsNotDisplayed() {
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def ticket = api.db.of('ticket$beru')\n" +
                ".withFilters{ eq('service', 'beruQuestion')\n" +
                " }\n" +
                ".limit(1).get()\n" +
                "\n" +
                "api.bcp.edit(ticket,['order':'" + Orders.getOrderWithYandexCourier2().getMainProperties().getOrderNumber() + "'])");

        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");
        // Открываем вкладку Заказы на превью заказа
        PageHelper.ticketPageHelper(webDriver).openTabOrderOnPreviewOrder();
        // Ожидаем пока блок доставка появится на странице
        Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().deliveryProperties();


        if (Tools.findElement(webDriver).findElements(By.xpath("//*[text()='Трекинг']")).size() > 0) {
            Assert.fail("Ссылка на трекинг курьера доставки отображается, хотя не должна была отображаться ");
        }
    }
}
