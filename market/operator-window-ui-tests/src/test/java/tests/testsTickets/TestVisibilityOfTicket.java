package ui_tests.src.test.java.tests.testsTickets;

import Classes.Comment;
import Classes.ticket.Properties;
import Classes.ticket.Ticket;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
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

import java.util.Arrays;
import java.util.List;

public class TestVisibilityOfTicket {
    private static WebDriver webDriver;
    @Rule
    public final TestName name = new TestName();

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestVisibilityOfTicket.class);

    @After
    public void after() {

        if (name.getMethodName().contains("ocrm1277_")) {
            PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def service = api.db.of('service').withFilters{\n" +
                    "  eq('code','beruQuestion')}.get()\n" +
                    "api.bcp.edit(service,['sharedService':false,'serviceOnResolved':null])", false);
        }
    }

    @Test
    @InfoTest(descriptionTest = "Проверка видимости обращений в статусе \"Решено\"",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-571",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1277")
    @Category(Blocker.class)
    public void ocrm1277_CheckingVisibilityOfTicketsWithResolvedStatus() {
        PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage("def service = api.db.of('service').withFilters{\n" +
                "  eq('code','beruOutgoingMail')}.get()\n" +
                "api.bcp.edit(service,['sharedService':true,'serviceOnResolved':null])", false);

        Ticket newTicket = new Ticket()
                .setProperties(new Properties()
                        .setContactEmail(Tools.other().getRandomText() + "yandex.ru")
                        .setCategory(Arrays.asList("test sanity")))
                .setSubject(Tools.date().getDateNow() + " " + Tools.other().getRandomText())
                .setComments(Arrays.asList(new Comment()
                        .setText("Проверка тест-кейса https://testpalm2.yandex-team.ru/testcase/ocrm-571 от "+Tools.date().getDateNow())
                        .setType("internal")));

        Pages.navigate(webDriver).openPageByMetaClassAndID("root@1");
        PageHelper.tableHelper(webDriver).createNewTicket("Покупки - исходящее", newTicket);
        Tools.waitElement(webDriver).waitTime(5000);
        Pages.navigate(webDriver).openPageByMetaClassAndID("root@1");
        List<String> titles = Entity.entityTable(webDriver).content().getTitlesEntityOnPage();
        Assert.assertTrue("Среди обращений не нашлось обращения из очереди с включенным флагом 'единая очередь'", titles.contains(newTicket.getSubject()));

    }
}
