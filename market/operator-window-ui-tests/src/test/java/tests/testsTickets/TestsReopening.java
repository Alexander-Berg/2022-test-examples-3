package ui_tests.src.test.java.tests.testsTickets;

import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Normal;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;

import java.util.GregorianCalendar;

public class TestsReopening {
    private static WebDriver webDriver;

    @Rule
    public final TestName name = new TestName();
    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsReopening.class);

    @After
    public void after() {
        if (name.getMethodName().contains("ocrm1375_")) {
            // Отключаем таймер времени выполнения обращения у очереди Покупки >  Исходящие звонки
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "def service = api.db.of('service').withFilters{\n" +
                            "eq('code','beruOutgoingCall')\n" +
                            "}.limit(1).get()\n" +
                            "api.bcp.edit(service, ['needCheckTicketProcessingTime': false])"
            ));
        }
    }

    @Ignore("Ядо выпилили")
    @InfoTest(descriptionTest = "Автоматическое переоткрытие обращений",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-584",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1348")
    @Category({Blocker.class})
    @Test
    public void ocrm1348_AutomaticReopening() {
        String status = "";
        // Получить обращение в статусе "Новый" из очереди "Логистическая поддержка Т Я.До > Общие"
        String ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                " def ticket = api.db.of('ticket$yandexDeliveryLogisticSupport').withFilters{\n" +
                        "  eq('service', 'yandexDeliveryLogisticSupportQuestion')\n" +
                        "  eq('status', 'registered')\n" +
                        "}.limit(5).list()[2]\n" +
                        "api.bcp.edit(ticket, \n" +
                        "             ['status': 'processing',\n" +
                        "             'categories': ['\tyandexDeliveryLogisticSupportOcrm-1348']])\n" +
                        "api.bcp.edit(ticket, \n" +
                        "             ['status': 'waitingDeliveryService'])\n" +
                        "return ticket"
        );

        // Проверить, что в обращении был создан таймер переоткрытия на +3 сек
        String reopenTime = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').reopenTime", ticketGid));

        // Проверить, что переоткрытие сработало

        for (int i = 0; i < 10; i++) {
            status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "api.db.get('%s').status", ticketGid));
            if (status.equals("reopened")) {
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(1000);
            }
        }

        Assert.assertTrue(String.format(
                "Назначился таймер для переоткрытия: %s, \n" +
                        "переоткрытие произошло: %s",
                reopenTime.equals("PT3S"), status.equals("reopened")),
                reopenTime.equals("PT3S") && status.equals("reopened")
        );

    }

    @InfoTest(descriptionTest = "Переоткрытие обращений, взятых в работу не в play-режиме",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-839",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1375")
    @Category({Normal.class})
    @Test
    public void ocrm1375_AutomaticReopeningWhenTimeIsOutNoPlayMode() {
        Ticket newTicket = new Ticket();
        String status = new String();
        String processingTime = new String();
        // Включаем проверку времени выполнения тикета и ставим её равной 3 секундам
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "def service = api.db.of('service').withFilters{\n" +
                        "eq('code','beruOutgoingCall')\n" +
                        "}.limit(1).get()\n" +
                        "api.bcp.edit(service,['needChangeServiceOnCreateTicket': false,\n" +
                        "             'needCheckTicketProcessingTime': true,\n" +
                        "             'serviceTime': 30013503,\n" +
                        "             'supportTime': 30013503,\n" +
                        "             'ticketProcessingTime': 3])"));

        // Задаем данные для будущего обращения
        newTicket.setProperties(new Properties()
                .setContactPhoneNumber("4999384362")
                .setService("Покупки > Исходящие звонки"))
                .setSubject(Tools.other().getRandomText() + new GregorianCalendar().getTime().toString());

        // Открываем страницу со всеми обращениями, создаём обращение исходящей телефонии и получаем его gid
        Pages.navigate(webDriver).openLVAllTickets();
        PageHelper.tableHelper(webDriver).createNewTicket("Покупки - исходящий звонок", newTicket);
        String ticketGid = Tools.other().getGidFromCurrentPageUrl(webDriver);


        // Взять обращение в работу
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.bcp.edit('%s', \n" +
                        "             ['status': 'processing'])",
                ticketGid)
        );

        // Проверить, что в обращении был создан таймер времени выполнения на +3 сек

        for (int i = 0; i < 5; i++) {
            processingTime = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "api.db.get('%s').processingTime", ticketGid));
            if (!processingTime.equals("PT3S")) {
                Tools.waitElement(webDriver).waitTime(5000);
            }
        }

        // Проверить, что переоткрытие сработало
        for (int i = 0; i < 5; i++) {
            status = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                    "api.db.get('%s').status", ticketGid));
            if (!status.equals("reopened")) {
                Tools.waitElement(webDriver).waitTime(5000);
            }
        }

        Assert.assertTrue(String.format(
                "Назначился таймер для переоткрытия: %s, \n" +
                        "переоткрытие произошло: %s",
                processingTime.equals("PT3S"), status.equals("reopened")),
                processingTime.equals("PT3S") && status.equals("reopened")
        );

    }
}
