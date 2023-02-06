package ru.yandex.autotests.innerpochta.tests.abook;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Birthdate;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllContactsFromAbookRule.removeAllContactsFromAbookRule;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Добавление контактов через написание письма")
@RunWith(Parameterized.class)
@Stories(FeaturesConst.GENERAL)
@Features({FeaturesConst.ABOOK, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.ABOOK)
@UseCreds({AddContactFromComposeTest.CREDS})
public class AddContactFromComposeTest extends BaseTest {

    public static final String CREDS = "AddContactFromComposeTest";

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Parameterized.Parameter(0)
    public String address;
    @Parameterized.Parameter(1)
    public Contact contact;

    @Parameterized.Parameters(name = "address: {0}, contact{1}")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {"FirstName Middle LastName <testbot2@yandex.ru>",
                new Contact()
                    .withName(new Name().withFirst("FirstName").withMiddle("Middle").withLast("LastName"))
                    .withEmail(singletonList(new Email().withValue("testbot2@yandex.ru")))
                    .withBirthdate(new Birthdate().withDay("3").withMonth("января").withYear("2000"))},
            {"OnlyName <testbot7@yandex.ru>",
                new Contact()
                    .withName(new Name().withFirst("OnlyName").withMiddle("").withLast(""))
                    .withEmail(singletonList(new Email().withValue("testbot7@yandex.ru")))
                    .withBirthdate(new Birthdate().withDay("1").withMonth("февраля").withYear("1900"))
            }
        };
        return Arrays.asList(data);
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllContactsFromAbookRule(user))
        .around(removeAllMessages(() -> user,  INBOX, TRASH));

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Автоматическое добавление контакта при написании письма из поля TO")
    @TestCaseId("1172")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddContactFromFieldTo() {
        user.apiMessagesSteps().sendMail(address, getRandomName(), getRandomName());
        user.abookSteps().checksContactInAbook(contact);
    }

    @Test
    @Title("Автоматическое добавление контакта при написании письма из поля CC")
    @TestCaseId("1173")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddContactFromFieldCc() {
        user.apiMessagesSteps().addCcEmails(address)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), getRandomName());
        user.abookSteps().checksContactInAbook(contact);
    }

    @Test
    @Title("Автоматическое добавление контакта при написании письма из поля BCC")
    @TestCaseId("1174")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddContactFromFieldBcc() {
        user.apiMessagesSteps().addBccEmails(address)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), getRandomName());
        user.abookSteps().checksContactInAbook(contact);
    }
}
