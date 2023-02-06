package ui_tests.src.test.java.tests.testsTickets;

import Classes.Email;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
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

public class TestsTicketsInST {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsTicketsInST.class);

    @InfoTest(
            descriptionTest = "Проверка создания задачи в ST с превью заказа",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-847",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-467"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm847_CreateTicketToSTFromOrderPreview() {
        // Сгенерировать тему письма
        String subject = "Автотест 112 - ocrm-844 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");
        // Создать письмо, из которого будет сгенерировано обращение
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("beru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.\n" +
                        "Номер заказа: 7423244")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        //Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        //Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Нажать на "Создать задачу в СТ"
        Pages.ticketPage(webDriver).messageTab().attributes().beruOrderTab().mainProperties().clickCreateSTTicketButton();

        // Выбрать ФОС
        Pages.ticketPage(webDriver).createOrderYandexFormPage().chooseForm("Форма для автотестов");

        // Проверить, что содержимое ФОС отображается
        Tools.other().takeFocusIFrame(webDriver, "orderYandexForm@127938984");
        Pages.ticketPage(webDriver).createOrderYandexFormPage().waitFormContent();

        // Нажать кнопку отправки ответа
        Pages.ticketPage(webDriver).createOrderYandexFormPage().submitButtonClick();

        // Проверить, что отображается сообщение от трекера
        Assert.assertTrue("Не отобразился ответ от трекера",
                Pages.ticketPage(webDriver).createOrderYandexFormPage().trackerResponcePrecense());
    }

    @InfoTest(
            descriptionTest = "Задача к смежникам - привязанные к бренду формы отображаются в обращениях этого бренда",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1411",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1360"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1411_DisplayFosWhenTheyLinkingToBrand() {

        //Проставить бренд тестовой ФОС, получить её gid и случайное обращение бренда B2B - Покупки
        String testArray[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket').withFilters{\n" +
                        "  eq('brand', 'b2bBeru')\n" +
                        "eq('service','b2bBeru')" +
                        "}.limit(1).get()\n" +
                        "def fos = api.db.of('orderYandexForm').withFilters{\n" +
                        "  eq('code', 'autotest1411')\n" +
                        "}.get()\n" +
                        "api.bcp.edit(fos,['brands': 'b2bBeru'])\n" +
                        "return \\\"${ticket},${fos.title}\\\"").split(",");
        //Открыть обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(testArray[0]);

        // Нажать на "Задача к смежникам"
        Pages.ticketPage(webDriver).header().clickCreateOuterTicketButton();

        // Проверить наличие ФОС
        Assert.assertTrue("Нужная ФОС не отобразилась в списке", Pages.ticketPage(webDriver).createOrderYandexFormPage().findForm(testArray[1]));
    }

    @InfoTest(
            descriptionTest = "Задача к смежникам - формы не привязанные к бренду не отображаются",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1412",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1360"
    )
    @Category({Blocker.class})
    @Test
    public void ocrm1412_DisplayFosWhenTheyLinkingToBrand() {

        //Удалить бренд тестовой ФОС, получить её gid и случайне обращение бренда B2B - Покупки
        String testArray[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket').withFilters{\n" +
                        "  eq('brand', 'b2bBeru')\n" +
                        "}.limit(1).get()\n" +
                        "def fos = api.db.of('orderYandexForm').withFilters{\n" +
                        "  eq('code', 'autotest1411')\n" +
                        "}.get()\n" +
                        "api.bcp.edit(fos,['brands': null])\n" +
                        "return \\\"${ticket},${fos.title}\\\"").split(",");
        //Открыть обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(testArray[0]);

        // Нажать на "Задача к смежникам"
        Pages.ticketPage(webDriver).header().clickCreateOuterTicketButton();

        // Проверить наличие ФОС
        Assert.assertFalse("Фос отобразилась в списке хотя не должна была",
                Pages.ticketPage(webDriver).createOrderYandexFormPage().findForm(testArray[1]));
    }
}
