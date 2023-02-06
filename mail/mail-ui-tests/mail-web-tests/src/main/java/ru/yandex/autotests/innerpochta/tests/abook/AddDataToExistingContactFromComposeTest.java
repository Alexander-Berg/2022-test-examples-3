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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Добавление данных в существующий контакт через написание письма")
@RunWith(Parameterized.class)
@Stories(FeaturesConst.GENERAL)
@Features({FeaturesConst.ABOOK, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.ABOOK)
@UseCreds(AddDataToExistingContactFromComposeTest.CREDS)
public class AddDataToExistingContactFromComposeTest extends BaseTest {

    public static final String CREDS = "AddDataToContactFromComposeTest";
    private static final String EMAIL = "testbot2@yandex.ru";

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @Parameterized.Parameter(0)
    public String address;
    @Parameterized.Parameter(1)
    public Contact contact;

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {"FirstName Middle LastName <testbot2@yandex.ru>",
                new Contact()
                    .withName(new Name().withFirst("FirstName").withMiddle("Middle").withLast("LastName"))
                    .withEmail(singletonList(new Email().withValue(EMAIL)))
            },
            {"OnlyName <testbot2@yandex.ru>",
                new Contact()
                    .withName(new Name().withFirst("OnlyName").withMiddle("").withLast(""))
                    .withEmail(singletonList(new Email().withValue(EMAIL)))
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
    public void setUp() {
        Contact defaultContact = user.abookSteps().createContactWithParametrs("", EMAIL);
        user.apiAbookSteps().addNewContacts(defaultContact);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Обновление данных контакта при отправлении письма из поля TO")
    @TestCaseId("1177")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddDataToExistingContactFromFieldTo() {
        user.apiMessagesSteps().addCcEmails(lock.firstAcc().getSelfEmail())
            .sendMailWithCcAndBcc(address, Utils.getRandomName(), "");
        user.abookSteps().checksContactInAbook(contact);
    }

    @Test
    @Title("Обновление данных контакта при отправлении письма из поля СС")
    @TestCaseId("1178")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddDataToExistingContactFromFieldCc() {
        user.apiMessagesSteps().addCcEmails(address)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), Utils.getRandomName(), "");
        user.abookSteps().checksContactInAbook(contact);
    }

    @Test
    @Title("Обновление данных контакта при отправлении письма из поля BСС")
    @TestCaseId("1179")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-60978")
    public void testAddDataToExistingContactFromFieldBcc() {
        user.apiMessagesSteps().addBccEmails(address)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), Utils.getRandomName(), "");
        user.abookSteps().checksContactInAbook(contact);
    }
}
