package ru.yandex.autotests.innerpochta.tests.abook;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Карточка контакта")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.CONTACT_CARD)
public class ContactCardTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        Contact contact = user.abookSteps().createContactWithParametrs(
            getRandomString(),
            lock.firstAcc().getSelfEmail()
        );
        user.apiAbookSteps().addContact(Utils.getRandomName(), contact)
            .addNewAbookGroupWithContacts(
                Utils.getRandomString(),
                user.apiAbookSteps().getPersonalContacts().get(0)
            );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomString(), getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Не должны увидеть удаленную группу в карточке контакта")
    @TestCaseId("1087")
    public void shouldDeleteGroupFromContact() {
        openContactCard();
        user.defaultSteps().clicksOn(onMessageView().mailCard().editContactBtn())
            .clicksOn(onMessageView().mailCard().groupDeleteCrossList().get(0))
            .shouldNotSee(onMessageView().mailCard().groupDeleteCrossList())
            .clicksOn(onMessageView().mailCard().saveChangesBtn())
            .shouldSee(onMessageView().mailCard().editContactBtn())
            .shouldNotSee(onMessageView().mailCard().contactGroups())
            .opensDefaultUrl();
        openContactCard();
        user.defaultSteps().shouldNotSee(onMessageView().mailCard().contactGroups());
    }

    @Test
    @Title("Проверяем кнопку “Написать письмо“ из карточки контактов в просмотре письма")
    @TestCaseId("2042")
    public void shouldOpenComposeFromContactCard() {
        openContactCard();
        user.defaultSteps().clicksOn(onMessageView().mailCard().composeBtn())
            .shouldSee(onComposePopup().expandedPopup());
    }

    @Step("Открываем карточку контакта через просмотр письма")
    private void openContactCard() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSee(onMessageView().messageTextBlock().text())
            .clicksOn(onMessageView().messageHead().fromName())
            .shouldSee(onMessageView().mailCard().contactName());
    }
}
