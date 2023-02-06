package ui_tests.src.test.java.tests.testsTickets;

import Classes.ticket.Properties;
import Classes.ticket.Ticket;
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

import java.util.List;

public class TestsCheckingRequiredFieldComments {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCheckingRequiredFieldComments.class);

    @InfoTest(descriptionTest = "Проверяем обязательность поля \"комментарий\" при создании тикета",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-445")
    @Category({Blocker.class})
    @Test
    public void ocrm445_CheckingRequiredField() {
        List<String> dangerMessages;

        Ticket newTicket = new Ticket();

        // Создаём обращение, которое должно получиться
        newTicket.setProperties(new Properties().setContactEmail(Tools.other().getRandomText() + "@yandex.ru"));

        // Открываем страницу "Письменная коммуникация"
        Pages.navigate(webDriver).openLVAllTickets();

        // Создаём обращение в CRM
        try {
            PageHelper.tableHelper(webDriver).createNewTicket("Покупки - исходящее", newTicket);
        } catch (Throwable t) {
            if (!t.toString().contains("Не удалось нажать на кнопку изменения/сохранения страницы")) {
                throw new Error(t);
            }
            if (!t.toString().contains("не пропал со страницы за")) {
                throw new Error(t);
            }
        }

        // Получаем все ошибки системы
        dangerMessages = Pages.ticketPage(webDriver).alertDanger().getAlertDangerMessages();

        boolean b = false;
        // Проверяем, что среди ошибок есть ошибка поля комментария
        for (String messages : dangerMessages) {
            b = Tools.other().isContainsSubstring("есть обязательные незаполненные поля.*Комментарий",messages);
            if (b) {
                break;
            }
        }
        Assert.assertTrue("Не вывелась ошибка об обязательности заполнения поля \"Комментарий\"",b);
    }

    @InfoTest(descriptionTest = "Проверяем обязательность  поля \"комментарий\" при решении тикета;",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-446")
    @Category({Blocker.class})
    @Test
    public void ocrm446_CheckingRequiredField() {
        // Список предупреждений со страницы
        List<String> dangerMessages;
        // Ищем gid обращения с очередью "Покупки > Общие вопросы", статусом "Новый" и не архивные
        // Переводим это обращение в статус "В работе"
        String gidTitle = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket$beru')\n" +
                        "  .withFilters {\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('archived', false)\n" +
                        "    eq('status','registered')\n" +
                        "    not(eq('channel',null))\n" +
                        "  }\n" +
                        "  .withOrders(api.db.orders.desc('creationTime'))\n" +
                        "  .limit(2)\n" +
                        "  .list()[1]\n" +
                        "\n" +
                        //Переводим обращение в статус "В работе"
                        "api.bcp.edit(ticket, ['status':'processing'])\n" +
                        "return ticket;");
        // Открываем найденное обращение
        Pages.navigate(webDriver).openPageByMetaClassAndID(gidTitle + "/edit");
        // Не вводя текст комментария, нажимаем на кнопку решения обращения
        Pages.ticketPage(webDriver).messageTab().commentsCreation()
                .clickCloseButton()
                .clickSendAResponseActionButton();
        // Получаем все предупреждения со страницы
        dangerMessages = Pages.ticketPage(webDriver).alertDanger().getAlertDangerMessages();
        boolean b = false;
        for (String messages : dangerMessages) {
            b = Tools.other().isContainsSubstring("есть обязательные незаполненные поля.*Комментарий",messages);
            if (b) {
                break;
            }
        }
        Assert.assertTrue("Не появилось сообщение с ошибкой незаполненного поля \"Комментарий\"", b);
    }
}
