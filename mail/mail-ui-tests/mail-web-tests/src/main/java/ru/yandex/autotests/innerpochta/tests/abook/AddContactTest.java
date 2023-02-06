package ru.yandex.autotests.innerpochta.tests.abook;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Birthdate;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Создание/удаление контактов")
@Features(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
@Tag(FeaturesConst.ABOOK)
public class AddContactTest extends BaseTest {

    public static final String EMAIL = "testbot2@yandex.ru";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private Contact contactWithBD, contactWithEmail, contactWithoutEmail;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        contactWithBD = user.abookSteps().createDefaultContact()
            .withBirthdate(new Birthdate().withDay("19").withMonth("февраля").withYear("1993").withMonthNumb(2L));
        contactWithEmail = user.abookSteps().createDefaultContact()
            .withBirthdate(contactWithBD.getBirthdate())
            .withPhone(contactWithBD.getPhone())
            .withEmail(singletonList(new Email().withValue(EMAIL)));
        contactWithoutEmail = user.abookSteps().createContactWithParametrs(getRandomString(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.CONTACTS);
        user.abookSteps().addsContact(contactWithBD)
            .shouldSeeContact(contactWithBD);
    }

    @Test
    @Title("Добавление и удаление нового контакта в АК")
    @TestCaseId("1176")
    public void shouldAddContact() {
        user.abookSteps().clicksOnContact(contactWithBD)
            .shouldSeeContactDetails(contactWithBD)
            .deleteContact(contactWithBD);
        user.defaultSteps().shouldSeeElementsCount(onAbookPage().contacts(), 0);
    }

    @Test
    @Title("Создание и удаление нескольких контактов в АК")
    @TestCaseId("1175")
    public void shouldMergeContacts() {
        user.abookSteps().addsContact(contactWithEmail);
        user.defaultSteps().clicksOn(user.pages().HomePage().mail360HeaderBlock().closeContactInput())
            .shouldNotSee(onAbookPage().searchResultsHeader())
            .clicksOn(onAbookPage().contacts().waitUntil(not(empty())).get(0).contactAvatarWithCheckBox())
            .clicksOn(user.pages().AbookPage().toolbarBlock().deleteContactButton())
            .shouldNotSee(user.pages().AbookPage().toolbarBlock().deleteContactButton())
            .shouldSeeElementsCount(onAbookPage().contacts(), 1)
            .clicksOn(onAbookPage().contacts().get(0).contactAvatarWithCheckBox())
            .clicksOn(user.pages().AbookPage().toolbarBlock().deleteContactButton())
            .shouldNotSee(onAbookPage().contacts());
    }

    @Test
    @Title("Проверяем валидацию поля для email")
    @TestCaseId("2201")
    public void testValidationFieldEmail() {
        user.defaultSteps().shouldSee(onAbookPage().toolbarBlock().addContactButton())
            .clicksOn(onAbookPage().toolbarBlock().addContactButton())
            .inputsTextInElement(onAbookPage().addContactPopup().addNewAddress(), Utils.getRandomName())
            .clicksOn(onAbookPage().addContactPopup().addPhoneNumber())
            .shouldSee(onAbookPage().addContactPopup().error())
            .clicksOn(onAbookPage().addContactPopup().addContactButton())
            .shouldSee(onAbookPage().addContactPopup())
            .inputsTextInElement(
                onAbookPage().addContactPopup().addNewAddress(),
                contactWithBD.getEmail().get(0).getValue()
            )
            .clicksOn(onAbookPage().addContactPopup().addPhoneNumber())
            .shouldNotSee(onAbookPage().addContactPopup().error())
            .clicksOn(onAbookPage().addContactPopup().addContactButton())
            .shouldNotSee(onAbookPage().addContactPopup());
    }

    @Test
    @Title("Добавляем и редактируем контакт без email")
    @TestCaseId("3936")
    public void shouldEditContactWithoutEmail() {
        user.abookSteps().addsContact(contactWithoutEmail)
            .clicksOnContact(contactWithoutEmail);
        user.defaultSteps().clicksOn(onAbookPage().contactPopup().editContactBtn())
            .inputsTextInElement(onAbookPage().contactPopup().abookAddEmail(), EMAIL)
            .clicksOn(onAbookPage().contactPopup().saveChangesBtn())
            .shouldSee(onAbookPage().contactPopup().editContactBtn());
        user.abookSteps().shouldSeeEmail(EMAIL);
    }

}
