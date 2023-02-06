package ui_tests.src.test.java.tests;

import Classes.Comment;
import Classes.Email;
import entity.Entity;
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
import tools.Other;
import tools.Tools;
import unit.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Функциональность Рекламации
 */
public class TestsBeruComplaints {
    private static WebDriver webDriver;
    static String codeServicesForTests = "beruComplaintsVip";
    static String codeTeamForTest = "beruComplaints";
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsBeruComplaints.class);

    @BeforeClass
    public static void beforeClass() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def employee = api.db.of('employee').withFilters{\n" +
                "eq('staffLogin','" + Config.getMainUserLogin() + "')}.limit(1).get()\n" +
                "\n" +
                "def addServices = '" + codeServicesForTests + "'.split(',') as List\n" +
                "def addTeam = '" + codeTeamForTest + "'\n" +
                "\n" +
                "def newServices = []\n" +
                "def newTeams = []\n" +
                "\n" +
                "for (def team:employee.teams)\n" +
                "{ \n" +
                "newTeams<<['gid':team.gid]\n" +
                "}\n" +
                "for (def service:employee.services)\n" +
                "{ \n" +
                "newServices<<['gid':service.gid]\n" +
                "}\n" +
                "def t = api.db.of('team').withFilters{\n" +
                "  eq('code',addTeam)}.get()\n" +
                "newTeams<<['gid':t.gid]\n" +
                "for(def service:addServices){\n" +
                "  def s = api.db.of('service').withFilters{\n" +
                "  eq('code',service)}.get()\n" +
                "    newServices<<['gid':s.gid]\n" +
                "  println (s.title)\n" +
                "}\n" +
                "api.bcp.edit(employee,['services':newServices,teams:newTeams])\n");
        boolean b;
        do {
            Tools.waitElement(webDriver).waitTime(3000);
            String s = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "def teams = api.db.of('employee').withFilters{\n" +
                            "eq('staffLogin','robot-loki-odinson')}.limit(1).get().teams.code.toString()\n" +
                            "\n" +
                            "def addServices = 'beruComplaintsVip,beruComplaintsUnmarked'.split(',') as List\n" +
                            "\n" +
                            "for (def service:addServices){\n" +
                            "  if (!teams.contains(service)){\n" +
                            "  return true\n" +
                            "  }\n" +
                            "}\n" +
                            "return false"
            );
            b = Boolean.getBoolean(s);
        } while (b);


    }

    @AfterClass
    public static void afterClass() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def employee = api.db.of('employee').withFilters{\n" +
                        "eq('staffLogin','" + Config.getMainUserLogin() + "')}.limit(1).get()\n" +
                        "\n" +
                        "def newServices  = []\n" +
                        "def newTeams = []\n" +
                        "\n" +
                        "for(def team:employee.teams){\n" +
                        "if (!'" + codeTeamForTest + "'.contains(team.code))\n" +
                        "  {\n" +
                        "    newTeams<<['gid':team.gid]\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "for(def service:employee.services){\n" +
                        "if (!'" + codeServicesForTests + "'.contains(service.code))\n" +
                        "  {\n" +
                        "    newServices<<['gid':service.gid]\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "api.bcp.edit(employee,['services':newServices,'teams':newTeams])");
    }

    @After
    public void after() {
        if (name.getMethodName().contains("ocrm1344_")) {
            beforeClass();
        }
    }

    @InfoTest(
            descriptionTest = "Проверка отображения автоматических сообщений",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-870",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-746"
    )
    @Category({Normal.class})
    @Test
    public void ocrm870_DisplayOfAutomaticMessages() {
        // Переменная с ожидаемым комментарием
        Comment expectedComment = new Comment()
                .setText("Деф. отписка1")
                .setType("public")
                .setNameAndEmail("Корбен Даллас (robot-lilucrm-prod)");

        // Сгенерировать тему письма
        String subject = "Автотест 130 - ocrm-870 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm");
        // Создать письмо, из которого будет сгенерировано обращение
        Email newEmail = new Email();
        newEmail
                .setSubject(subject)
                .setTo("beru")
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я старый.\n" +
                        "Тип клиента: угрожает_судом")
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru");

        // Отправить письмо
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "beruComplaints");

        // Открыть обращение
        PageHelper.ticketPageHelper(webDriver).findATicketByTitleAndOpenIt(subject);

        // Проверить, что в списке сообщений есть автоматическое сообщение "Деф. отписка"
        Assert.assertTrue("В списке сообщений нет автоматического сообщения от системы",
                Pages.ticketPage(webDriver).messageTab().comments().getComments().contains(expectedComment));
    }

    @Test
    @InfoTest(descriptionTest = "Ручная очистка поля ответственный сотрудник",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1309",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-843")
    @Category(Blocker.class)
    public void ocrm1309_ClearResponsibleEmployeeField() {
        //Получаем обращение рекламации у которого есть ответственный
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket').withFilters{\n" +
                        "eq('service','beruComplaintsVip')\n" +
                        " not( eq('responsibleEmployee',null))\n" +
                        "}\n" +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1).get()\n" +
                        "\n" +
                        "if(ticket==null){\n" +
                        "\tticket = api.db.of('ticket').withFilters{\n" +
                        "\t\teq('service','beruComplaintsVip')\n" +
                        "      eq('status','registered')\n" +
                        "\t}\n" +
                        "\t.withOrders(api.db.orders.desc('creationTime'))\n" +
                        "\t.limit(1).get()\n" +
                        "\tapi.bcp.edit(ticket,['status':'processing'])  \n" +
                        "}\n" +
                        "return ticket"
        );
        //Переходим на страницу редактирования обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");
        //очищаем поле Ответственный сотрудник
        Entity.properties(webDriver).clearPropertiesOfSuggestTypeField("", "responsibleEmployee");
        //СОхраняем изменения
        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();
        Tools.waitElement(webDriver).waitTime(5000);
        //получаем значение поля Ответственный сотрудник
        String responsibleEmployee = Pages.ticketPage(webDriver).properties().getResponsibleEmployee();
        Assert.assertEquals("У обращения " + gid + " не получилось очистить поле Ответственный сотрудник", "", responsibleEmployee);
    }

}
