package ui_tests.src.test.java.tests;

import basicClass.AfterAndBeforeMethods;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Critical;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.CustomRuleChain;
import tools.Tools;

import java.util.List;

public class TestsSupervisorHelp {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @BeforeClass
    public static void before() {
        webDriver = new AfterAndBeforeMethods().beforeClases(TestsSupervisorHelp.class.getName());
        createRequestOnHelps();
    }

    @AfterClass
    public static void after() {
        deletedRequestOnHelps();
        new AfterAndBeforeMethods().afterClases(webDriver);

    }

    /**
     * Создание 5 запросов на помощь
     */
    private static void createRequestOnHelps() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def empls = api.db.of('employee')\n" +
                ".withFilters {\n" +
                "eq('ou', 'ou@29020711')\n" +
                "eq('archived', false)\n" +
                "}\n" +
                ".limit(5).list();\n" +
                "\n" +
                "for(def e : empls) {\n" +
                "api.tx.doInNewTx{ api.bcp.create('needsHelpAlert$default', ['employee': e])}\n" +
                "}", false);
        Tools.waitElement(webDriver).waitTime(4000);
    }

    /**
     * Удаление всех запросов на помощь
     */
    private static void deletedRequestOnHelps() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def emplsActive = api.db.of('needsHelpAlert$default')\n" +
                "                .withFilters {\n" +
                "                eq('archived', false)\n" +
                "                  eq('status','active')\n" +
                "                }.list();\n" +
                "                \n" +
                "for(def e : emplsActive){\n" +
                "api.bcp.edit(e, ['status' : 'processing'])\n" +
                "api.bcp.edit(e, ['status' : 'archived'])\n" +
                "}", false);
    }


    @Test
    @InfoTest(descriptionTest = "Перевод запроса на помощь супервизора 'В работу'",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-316",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1038")
    @Category(Critical.class)
    public void ocrm1038_TranslationOfSupervisorHelpRequestToInProgressSection() {
        boolean result;
        //Нажимаем на кнопку в запросами на помощь
        Pages.mainMenuOfPage(webDriver).clickSupervisorHelp();
        //Получаем список запросов на помощь
        List<String> employeeWhoAreYouHelping = Pages.mainMenuOfPage(webDriver).getHelpRequestsInProgress();
        //Берем первый запрос "В работу"
        Pages.mainMenuOfPage(webDriver).helpTheUser(employeeWhoAreYouHelping.get(0));
        //Получаем список запросов на помощь в статусе "В работе"
        List<String> employeeWhoRequestedHelp = Pages.mainMenuOfPage(webDriver).getListEmployeeWhoRequestedHelp();

        //Проверяем что запрос пользователя на помощь перешел в раздел "В работе"
        result = employeeWhoRequestedHelp.contains(employeeWhoAreYouHelping.get(0));

        Assert.assertTrue("Запрос о помощи который мы взяли в работу не перешел в раздел 'В работе'", result);

    }


}
