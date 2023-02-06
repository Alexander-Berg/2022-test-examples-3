package ui_tests.src.test.java.tests;

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

import javax.tools.Tool;

public class TestsML {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();

    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsML.class);

    @Test
    @InfoTest(descriptionTest = "Предположение о действии от ML (у тикета меньше 20 комментариев)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1392",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-893")
    @Category(Blocker.class)
    public void ocrm1392_AssumptionOfAnActionFromML() {
        String taxiMLSupport = "";
        Email email = new Email()
                .setTo("beru")
                .setText("Пришлите мне чек по заказу");
        String ticketGID[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "eq('color','BLUE')\n" +
                        String.format("between ('creationDate',%s)}\n", Tools.date().getDateRangeForSeveralMonths(2)) +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1).get()\n" +
                "def tickets = api.db.of('ticket$firstLine').withFilters{\n" +
                        "  eq('channel','mail')\n" +
                        "eq('taximlSupport',null)\n" +
                        "  eq('service','beruQuestion')\n" +
                        "}.withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(100).list()\n" +
                        "\n" +
                        "for(def ticket:tickets){\n" +
                        "  def comments= api.db.of('comment').withFilters{\n" +
                        "  \teq('entity',ticket)\n" +
                        "  }.list()\n" +
                        "  if(comments.size()<20){\n" +
                        "api.bcp.edit(ticket,['order':order])\n" +
                        "  return \\\"${ticket},${ticket.id},${ticket.title},${ticket.clientEmail}\\\"\n" +
                        "  }\n" +
                        "}"
        ).split(",");

        email.setSubject(ticketGID[2] + ", № " + ticketGID[1])
                .setFromAlias(ticketGID[3]);
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");
        for (int i = 0; i < 7; i++) {
            taxiMLSupport = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                    "api.db.'" + ticketGID[0] + "'.taximlSupport"
            );
            if (taxiMLSupport.equals(301968902)){
                break;
            } else {
                Tools.waitElement(webDriver).waitTime(1000);
            }
        }
        Assert.assertEquals("У обращения не проставилось значение 301968902 в поле 'Предположение о действии от ML'", "301968902", taxiMLSupport);
    }

    @Test
    @InfoTest(descriptionTest = "Предположение о действии от ML (у тикета с 20 комментариями)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1393",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-894")
    @Category(Blocker.class)
    public void ocrm1393_AssumptionOfAnActionFromML() {
        Email email = new Email()
                .setTo("beru")
                .setText("Пришлите мне чек по заказу");
        String ticketGID[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def order = api.db.of('order').withFilters{\n" +
                        "eq('color','BLUE')\n" +
                        String.format("between ('creationDate',%s)}\n", Tools.date().getDateRangeForSeveralMonths(2)) +
                        ".withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(1).get()\n" +
                        "def tickets = api.db.of('ticket$beru').withFilters{\n" +
                        "  eq('channel','mail')\n" +
                        "  eq('taximlSupport',null)\n" +
                        "  eq('service','beruQuestion')\n" +
                        "}.withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(100).list()\n" +
                        "\n" +
                        "for(def ticket:tickets){\n" +
                        "  def comments= api.db.of('comment').withFilters{\n" +
                        "      eq('entity',ticket)\n" +
                        "  }.list()\n" +
                        "  if(comments.size()<20){\n" +
                        "    for (def i=0;i<(20-comments.size());i++){\n" +
                        "    api.bcp.create('comment$internal',['body':'random','entity':ticket])\n" +
                        "    }\n" +
                        "api.bcp.edit(ticket,['order':order])\n" +
                        "    return \\\"${ticket},${ticket.id},${ticket.title},${ticket.clientEmail}\\\"\n" +
                        "  }\n" +
                        "}"
        ).split(",");

        email.setSubject(ticketGID[2] + ", № " + ticketGID[1])
                .setFromAlias(ticketGID[3]);
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");
        Tools.waitElement(webDriver).waitTime(5000);
        String taxiMLSupport = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.'" + ticketGID[0] + "'.taximlSupport", true, 3
        );
        Assert.assertNull("У обращения проставилось значение в поле 'Предположение о действии от ML', а должно было остаться пустым", taxiMLSupport);
    }

    @Test
    @InfoTest(descriptionTest = "Предположение о действии от ML (ограничение по каналу)",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1394",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-895")
    @Category(Blocker.class)
    public void ocrm1394_AssumptionOfAnActionFromML() {
        Email email = new Email()
                .setTo("beru")
                .setText("Пришлите мне чек по заказу");
        String ticketGID[] = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def tickets = api.db.of('ticket$beru').withFilters{\n" +
                        "  eq('channel','mail')\n" +
                        "  eq('taximlSupport',null)\n" +
                        "  eq('service','beruQuestion')\n" +
                        "}.withOrders(api.db.orders.desc('creationTime'))\n" +
                        ".limit(100).list()\n" +
                        "\n" +
                        "for(def ticket:tickets){\n" +
                        "  def comments= api.db.of('comment').withFilters{\n" +
                        "      eq('entity',ticket)\n" +
                        "  }.list()\n" +
                        "  if(comments.size()<20){\n" +
                        "    api.bcp.edit(ticket,['channel':'qualityManagement'])\n" +
                        "    return \\\"${ticket},${ticket.id},${ticket.title},${ticket.clientEmail}\\\"\n" +
                        "  }\n" +
                        "}"
        ).split(",");

        email.setSubject(ticketGID[2] + ", № " + ticketGID[1])
                .setFromAlias(ticketGID[3]);
        PageHelper.otherHelper(webDriver).createTicketFromMail(email, "beru");
        Tools.waitElement(webDriver).waitTime(5000);
        String taxiMLSupport = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.'" + ticketGID[0] + "'.taximlSupport", true, 3
        );
        Assert.assertNull("У обращения проставилось значение в поле 'Предположение о действии от ML', а должно было остаться пустым", taxiMLSupport);
    }
}
