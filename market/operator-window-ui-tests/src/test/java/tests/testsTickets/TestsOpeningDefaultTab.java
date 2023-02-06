package ui_tests.src.test.java.tests.testsTickets;

import Classes.Email;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Critical;
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

import java.util.HashMap;


public class TestsOpeningDefaultTab {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsOpeningDefaultTab.class);

    @Test
    @InfoTest(descriptionTest = "Проверка открытия превью обращения на вкладке 'Партнер' если обращение связано с партнером - vendorid",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1168",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1065")
    @Category(Critical.class)
    public void ocrm1168_OpeningATicketOnPartnerTab() {

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-vendorid", "8821");
        newEmail.setSubject("Автотест ocrm1168 от" + Tools.date().getDateNow())
                .setText(Tools.other().getRandomText())
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // создаем обращение на основе письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");
        // ищем и открываем созданное обращение
        String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", newEmail.getSubject());

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        String openTab = Pages.ticketPage(webDriver).messageTab().attributes().tabs().whichTabActive();
        Assert.assertEquals("Превью партнера открылось не на вкладке 'Партнер'", "Партнер", openTab);
    }

    @Test
    @InfoTest(descriptionTest = "Проверка открытия превью обращения на вкладке 'Партнер' если обращение связано с партнером - shopid",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1169",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1063")
    @Category(Critical.class)
    public void ocrm1169_OpeningATicketOnPartnerTab() {

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-shopid", "10750309");
        newEmail.setSubject("Автотест ocrm1169 от " + Tools.date().getDateNow())
                .setText(Tools.other().getRandomText())
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // создаем обращение на основе письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");
        // ищем и открываем созданное обращение
        String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", newEmail.getSubject());

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        String openTab = Pages.ticketPage(webDriver).messageTab().attributes().tabs().whichTabActive();
        Assert.assertEquals("Превью партнера открылось не на вкладке 'Партнер'", "Партнер", openTab);
    }

    @Test
    @InfoTest(descriptionTest = "Проверка открытия превью обращения на вкладке 'Партнер' если обращение связано с партнером - supplierid",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1170",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1064")
    @Category(Critical.class)
    public void ocrm1170_OpeningATicketOnPartnerTab() {

        // Создать отправляемое письмо
        Email newEmail = new Email();
        HashMap<String, String> replays = new HashMap<>();
        replays.put("x-market-supplierid", "10424056");
        newEmail.setSubject("Автотест ocrm1170 от " + Tools.date().getDateNow())
                .setText(Tools.other().getRandomText())
                .setFromAlias(Tools.other().getRandomText() + "@yandex.ru")
                .setTo("b2b")
                .setHeaders(replays);
        // создаем обращение на основе письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "b2b");
        // ищем и открываем созданное обращение
        String gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket", newEmail.getSubject());

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid);

        String openTab = Pages.ticketPage(webDriver).messageTab().attributes().tabs().whichTabActive();
        Assert.assertEquals("Превью партнера открылось не на вкладке 'Партнер'", "Партнер", openTab);
    }
}
