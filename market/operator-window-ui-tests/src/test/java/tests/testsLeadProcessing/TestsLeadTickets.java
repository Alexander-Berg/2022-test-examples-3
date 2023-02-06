package ui_tests.src.test.java.tests.testsLeadProcessing;

import entity.Entity;
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

public class TestsLeadTickets {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsLeadTickets.class);

    @Test
    @InfoTest(descriptionTest = "Ручное изменение поля ответственный в обращениях B2B лидов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1427",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1426")
    @Category(Blocker.class)
    public void ocrm1427_ChangeResponsibleEmployeeField() {
        String responsibleEmployee = "";
        boolean result = false;
        //Получить случайного пользователя
        //Найти обращение в очереди 01 КД: Обработка лидов где ответственный есть и это не найденный пользователь
        //Если обращение не найдено создаем обращение проставив Максимуса ответственным
        //Элементы массива 0 - Имя найденного сотрудника 1 - Обращение
        String testArray[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def employee = api.db.of('employee').limit(1).get()\n" +
                        "def ticket = api.db.of('ticket$b2bLead').withFilters{\n" +
                        "eq('service','b2b-commercial-department-leads')\n" +
                        " not( eq('responsibleEmployee',null))\n" +
                        " not( eq('responsibleEmployee',employee))\n" +
                        "eq('archived',false)\n" +
                        "}\n" +
                        ".limit(1).get()\n" +
                        "\n" +
                        "if (ticket == null) {\n" +
                        "  ticket = api.bcp.create('ticket$b2bLead', [\n" +
                        "    'title': 'Autotest-OCRM-1427',\n" +
                        "    'service': 'b2b-commercial-department-leads',\n" +
                        "    'responsibleEmployee': '80822602',\n" +
                        "  ])\n" +
                        "}\n" +
                        "return \\\"${employee.title},${ticket}\\\"").split(",");
        //Переходим на страницу редактирования обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(testArray[1] + "/edit");
        //Изменяем сотрудника в поле ответственный сотрудник
        Entity.properties(webDriver).setPropertiesOfSuggestTypeField("responsibleEmployee", testArray[0]);
        //СОхраняем изменения
        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();

        //получаем значение поля Ответственный сотрудник
        for (int i = 0; i < 5; i++) {
            responsibleEmployee = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.'" + testArray[1] + "'.responsibleEmployee.title"
            );
            if (responsibleEmployee.equals(testArray[0])) {
                result = true;
                break;
            }
        }

        //responsibleEmployee = Pages.ticketPage(webDriver).properties().getResponsibleEmployee();
        Assert.assertTrue("У обращения " + testArray[1] + " не получилось изменить поле Ответственный сотрудник", result);

    }


}
