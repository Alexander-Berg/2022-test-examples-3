package ui_tests.src.test.java.tests.testsTickets;

import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Minor;
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

public class TestsNoSubFunctionality {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsNoSubFunctionality.class);

    @InfoTest(
            descriptionTest = "Очистка всех чекбоксов в инпуте тэгов",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-972",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-837"
    )
    @Category({Minor.class})
    @Test
    public void ocrm972_ClearingTagInputWhenClickingOnTheCross() {
        String ticketGid;

        // Найти в очереди "Покупки > Общие вопросы" обращение в статусе "Новый"
        ticketGid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def ticket = api.db.of('ticket').withFilters{\n" +
                        "    eq('service','beruQuestion')\n" +
                        "    eq('status','registered')\n" +
                        "}\n" +
                        ".limit(3).list()[2]\n" +
                        "def gid = api.db.of('ticketTag').get('yandexTeam').gid\n" +
                        "\n" +
                        "api.bcp.edit(ticket,['tags':['gid':gid]])");

       // Открыть карточку обращения
        Pages.navigate(webDriver).openPageByMetaClassAndID(ticketGid+"/edit");

        // Нажать на кнопку с иконкой крестика в поле "Тэги"
        Entity.properties(webDriver).clearPropertiesOfMultiSuggestTypeField("", "tags");

        // Поле "Тэги" пустое
        Assert.assertTrue(Entity.properties(webDriver).checkEmptyValueField(Pages.ticketPage(webDriver).properties().block,"tags"));
    }
}
