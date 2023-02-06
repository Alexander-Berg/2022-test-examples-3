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
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;

public class TestsExpressDeliveryYandexGo {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsExpressDeliveryYandexGo.class);

    @InfoTest(descriptionTest = "Создание обращений в очереди 'Покупки > Экспресс доставка (Яндекс.GО)'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1198",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1357")
    @Category({Blocker.class})
    @Test
    public void ocrm1357_CreatingTicketsInBeruExpressDeliveryGoService() {
        String gid = "null";

        // Установить у тестового заказа службу доставки для теста
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def obj = api.db.get('order@2103T32290205') \n" +
                "api.bcp.edit(obj, ['deliveryServiceId' : '1006360'])");

        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1357 - заказ 32290205 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
        ;

        // Получить gid ожидаемой очереди
        String expectedService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('service').withFilters{\n" +
                "  eq('code', 'beruExpressDeliveryGo')\n" +
                "}.get()");

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beru");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", gid));

        Assert.assertEquals("Обращение создалось не в очереди 'Покупки > Экспресс доставка (Яндекс.GО)'",
                expectedService, serviceFromPage);
    }
}
