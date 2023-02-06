package ui_tests.src.test.java.tests.testsPickupPointPotentialTickets;

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

public class TestsCreatingLinks {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(tests.testsPickupPointPotentialTickets.TestsCreatingLinks.class);


    @InfoTest(descriptionTest = "Если не удалось определить партнера - не указывать его",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1284",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1308")
    @Category({Blocker.class})
    @Test
    public void ocrm1308_DontSetPartnerIfCantDetermineIt() {
        String gid = "null";
        // Сгенерировать тестовый e-mail
        String emailAddress = Tools.other().getRandomText() + "@yandex.ru";

        // Сгенерировать тему
        String subject = "Автотест ocrm-1308 - "
                + Tools.date().generateCurrentDateAndTimeStringOfFormat("dd.MM.yy HH:mm ")
                + Tools.other().getRandomText();

        // Создать отправляемое письмо
        Email newEmail = new Email();
        newEmail.setSubject(subject)
                .setText("Я тикет для автотеста. Трогай меня, только если видишь, что я не новый.")
                .setFromAlias(emailAddress)
                .setTo("pickupPointPotential");

        // Создать обращение из письма
        PageHelper.otherHelper(webDriver).createTicketFromMail(newEmail, "pickupPointPotential");

        // Дождаться создания обращения
        for (int i = 0; i < 10; i++){
            Tools.waitElement(webDriver).waitTime(2000);
            gid = PageHelper.otherHelper(webDriver).findEntityByTitle("ticket$b2b", subject);
            if (gid != null) break;
        }

        // Получить партнера из созданного обращения
        String accountFromPage = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(String.format(
                "api.db.get('%s').partner", gid));

        Assert.assertNull("Партнер определился, хотя должен был остаться пустым", accountFromPage);
    }
}
