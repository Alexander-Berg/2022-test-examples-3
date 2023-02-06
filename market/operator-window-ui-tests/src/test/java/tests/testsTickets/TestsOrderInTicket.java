package ui_tests.src.test.java.tests.testsTickets;

import Classes.order.MainProperties;
import Classes.ticket.Properties;
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
import unit.Orders;

public class TestsOrderInTicket {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsOrderInTicket.class);

    @InfoTest(descriptionTest = "Проверка отображения заказа DSBS на превью карточки обращения",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-690")
    @Category({Blocker.class})
    @Test
    public void ocrm690_CheckingDisplayDSBSOrderOnTicketPage() {
        // Открываем страницу очереди Маркет > DSBS
        Pages.navigate(webDriver).openYandexMarketDSBS();
        // Применяем предсохранённый фильтр
        PageHelper.tableHelper(webDriver).setSavedFilter("[Автотест] Обращения метакласса Маркет");
        // Открываем рандомную запись
        PageHelper.tableHelper(webDriver).openRandomEntity();
        // Переходим на страницу редактирования
        Pages.ticketPage(webDriver).header().clickOnEditTicketButton();
        // Меняем номер заказа
        PageHelper.ticketPageHelper(webDriver).editProperties(new Properties().setOrder(Orders.getDSBSOrder().getMainProperties().getOrderNumber()));
        // Сохраняем изменения
        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();
        // Получаем данные из раздела основных свойств превью карточки заказа
        MainProperties mainPropertiesAttributeFromPage = PageHelper.ticketPageHelper(webDriver).getMainPropertiesAttribute();

        Assert.assertEquals("На странице обращения не отобразился заказ DSBS, отобразился заказ " + mainPropertiesAttributeFromPage, Orders.getDSBSOrder().getMainProperties(), mainPropertiesAttributeFromPage);

    }

    @InfoTest(descriptionTest = "Привязка заказа к обращению при клике на ссылку с заказом",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1055",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-288"
    )
    @Category({Normal.class})
    @Test
    public void ocrm1055_linkOrderToTicketOnClick() {
        // Найти в очереди "Покупки > Общие вопросы" обращение в статусе "Новый"
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$firstLine').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "    eq('order',null)\n" +
                        "}\n" +
                        ".limit(1).get()");

        // Открыть обращение в режиме редактирования
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid + "/edit");

        // Запомнить вкладку с обращением как основную
        String originalWindow = webDriver.getWindowHandle();

        // Нажать на любой заказ из списка
        PageHelper.tableHelper(webDriver).openRandomEntity();

        // Перейти на вкладку с открытым заказом
        Tools.tabsBrowser(webDriver).takeFocusNewTab();

        // Получить номер открытого заказа
        String order = Tools.other().getGidFromCurrentPageUrl(webDriver).replaceAll(".*T", "");

        // Закрыть вкладку с заказом и вернуться на оригинальную на вкладку с обращением, переведя её в режим просмотра
        webDriver.close();
        webDriver.switchTo().window(originalWindow);
        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();

        // Проверить, что в поле "Заказ" находится номер выбранного ранее заказа
        String orderFieldValue = Pages.ticketPage(webDriver).properties().getOrderNumber();
        Assert.assertEquals(("Номера заказов не совпадают - " + order + " не равно " + orderFieldValue), order, orderFieldValue);
    }
}
