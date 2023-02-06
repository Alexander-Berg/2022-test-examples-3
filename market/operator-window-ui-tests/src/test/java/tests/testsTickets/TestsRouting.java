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

import java.util.HashMap;


public class TestsRouting {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsRouting.class);


    @InfoTest(descriptionTest = "Роутинг по 'Кому'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1243",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1269")
    @Category({Blocker.class})
    @Test
    public void ocrm1269_RoutingUsingToMail() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1269 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("testb2bpokupki@yandex.ru")
        ;

        // Проверить, что есть нужное правило роутинга. Если нет - создать
        String routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', 'testb2bpokupki@yandex.ru')\n" +
                        "  eq('title', null)\n" +
                        "  eq('service', '8a5fa6f5-f092-9fe4-02ed-5bf2890dcd77')\n" +
                        "}.get()");
        if (routingRule == null) {
            routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.bcp.create('b2bTicketRoutingRule', [\n" +
                            "  'replyTo': 'testb2bpokupki@yandex.ru',\n" +
                            "  'title': null,\n" +
                            "  'service': '8a5fa6f5-f092-9fe4-02ed-5bf2890dcd77',\n" +
                            "])");
        }

        // Получить gid ожидаемой очереди из правила
        String expectedService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", routingRule)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", gid));

        Assert.assertEquals("Правило роутинга не сработало", expectedService, serviceFromPage);
    }

    @InfoTest(descriptionTest = "Роутинг по X-заголовку",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1244",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1319")
    @Category({Blocker.class})
    @Test
    public void ocrm1319_RoutingUsingXHeader() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1319 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-otrs-partnerform", "ocrm-1244");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setHeaders(headers)
        ;

        // Проверить, что есть нужное правило роутинга. Если нет - создать
        String routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', null)\n" +
                        "  eq('title', 'ocrm-1244')\n" +
                        "  eq('service', 'bddab754-2cad-d928-94bb-5be2d5bcf0c5')\n" +
                        "}.get()");
        if (routingRule == null) {
            routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.bcp.create('b2bTicketRoutingRule', [\n" +
                            "  'replyTo': null,\n" +
                            "  'title': 'ocrm-1244',\n" +
                            "  'service': 'bddab754-2cad-d928-94bb-5be2d5bcf0c5',\n" +
                            "])");
        }

        // Получить gid ожидаемой очереди из правила
        String expectedService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", routingRule)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", gid));

        Assert.assertEquals("Правило роутинга не сработало", expectedService, serviceFromPage);
    }

    @InfoTest(descriptionTest = "Роутинг по 'Кому' и X-заголовку",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1242",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1347")
    @Category({Blocker.class})
    @Test
    public void ocrm1347_RoutingUsingMailAndXHeader() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1347 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-otrs-partnerform", "ocrm-1242");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setHeaders(headers)
                .setTo("cc-test-crm@yandex.ru")
        ;

        // Проверить, что есть нужное правило роутинга. Если нет - создать
        String routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', 'cc-test-crm@yandex.ru')\n" +
                        "  eq('title', 'ocrm-1242')\n" +
                        "  eq('service', '344ebb83-2ff6-3505-172c-5bd8329c5c81')\n" +
                        "}.get()");
        if (routingRule == null) {
            routingRule = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.bcp.create('b2bTicketRoutingRule', [\n" +
                            "  'replyTo': 'cc-test-crm@yandex.ru',\n" +
                            "  'title': 'ocrm-1242',\n" +
                            "  'service': '344ebb83-2ff6-3505-172c-5bd8329c5c81',\n" +
                            "])");
        }

        // Получить gid ожидаемой очереди из правила
        String expectedService = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", routingRule)
        );

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service", gid));

        Assert.assertEquals("Правило роутинга не сработало", expectedService, serviceFromPage);
    }

    @InfoTest(descriptionTest = "При отсуствии правил роутинга выбираем очередь по умолчанию",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1240",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1399")
    @Category({Blocker.class})
    @Test
    public void ocrm1399_RoutingWithoutRules() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1399 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("cc-test-crm@yandex.ru")
        ;

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить код очереди из созданного обращения
        String serviceFromTicket = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service.code", gid));

        Assert.assertEquals("Обращение создалось не в дефолтной очереди", "b2bDefaultService", serviceFromTicket);
    }

    @InfoTest(descriptionTest = "Роутинг происходит по правилу с наибольшим количеством критериев",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1245",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1408")
    @Category({Blocker.class})
    @Test
    public void ocrm1408_RoutingUsingRuleWithMostCriteria() {
        // Проверить, что есть правила роутинга.
        // Если нет - создать.
        String routingRule2 = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def rule1 = api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', 'wwtest-van-anast@yandex.ru')\n" +
                        "  eq('title', 'ocrm-1245')\n" +
                        "  eq('service', '27459b06-4168-ddef-f5c6-5d1df4355bf9')\n" +
                        "}.get()\n" +
                        "\n" +
                        "\n" +
                        "if (rule1 == null) {\n" +
                        "  api.bcp.create('b2bTicketRoutingRule', [\n" +
                        "    'replyTo': 'wwtest-van-anast@yandex.ru',\n" +
                        "    'title': 'ocrm-1245',\n" +
                        "    'service': '27459b06-4168-ddef-f5c6-5d1df4355bf9',\n" +
                        "  ])\n" +
                        "}\n" +
                        "\n" +
                        "def rule2 = api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', 'wwtest-van-anast@yandex.ru')\n" +
                        "  eq('title', null)\n" +
                        "  eq('service', 'ace160f6-d553-d4ae-dcd5-5be2cfd87e4c')\n" +
                        "}.get()\n" +
                        "\n" +
                        "\n" +
                        "if (rule2 == null) {\n" +
                        "  api.bcp.create('b2bTicketRoutingRule', [\n" +
                        "    'replyTo': 'wwtest-van-anast@yandex.ru',\n" +
                        "    'title': null,\n" +
                        "    'service': 'ace160f6-d553-d4ae-dcd5-5be2cfd87e4c',\n" +
                        "  ])\n" +
                        "}\n" +
                        "\n" +
                        "def rule3 = api.db.of('b2bTicketRoutingRule').withFilters{\n" +
                        "  eq('replyTo', null)\n" +
                        "  eq('title', 'ocrm-1245')\n" +
                        "  eq('service', '5d5060ec-c34d-5236-dfe8-5be2d2b82836')\n" +
                        "}.get()\n" +
                        "\n" +
                        "\n" +
                        "if (rule3 == null) {\n" +
                        "  api.bcp.create('b2bTicketRoutingRule', [\n" +
                        "    'replyTo': null,\n" +
                        "    'title': 'ocrm-1245',\n" +
                        "    'service': '5d5060ec-c34d-5236-dfe8-5be2d2b82836',\n" +
                        "  ])\n" +
                        "}");

        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1408 - ocrm-1245 -"
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-otrs-partnerform", "ocrm-1245");
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setHeaders(headers)
                .setTo("wwtest-van-anast@yandex.ru")
        ;

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++) {
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить очередь из созданного обращения
        String serviceFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').service.code", gid));

        Assert.assertEquals("Правило роутинга не сработало",
                "27459b06-4168-ddef-f5c6-5d1df4355bf9", serviceFromPage);

    }

}
