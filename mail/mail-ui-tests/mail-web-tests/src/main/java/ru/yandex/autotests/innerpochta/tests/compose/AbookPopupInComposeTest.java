package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Новый композ - Попап абука")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
public class AbookPopupInComposeTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private String subject;
    private Contact contact;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        contact = user.abookSteps().createDefaultContact()
            .withEmail(Collections.singletonList(new Email().withValue(lock.firstAcc().getSelfEmail())));
        subject = Utils.getRandomString();
        user.apiAbookSteps().addNewContacts(contact);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
        user.composeSteps().inputsSubject(subject);
    }

    @Test
    @Title("Добавляем получателя из абука")
    @TestCaseId("2823")
    public void shouldAddContactFromAbookPopup() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupTo())
            .shouldSee(onComposePopup().suggestList())
            .clicksOn(onComposePopup().abookBtn())
            .shouldSee(onComposePopup().abookPopup())
            .turnTrue(onComposePopup().abookPopup().abookAdressesCheckboxList().get(0))
            .clicksOn(onComposePopup().abookPopup().selectButton());
        user.composeSteps().shouldSeeSendToAreaContains(user.abookSteps().getFullName(contact))
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.defaultSteps().clicksOn(onHomePage().checkMailButton());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Добавляем получателя кликом в «Копия»")
    @TestCaseId("2824")
    public void shouldAddContactFromClick() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().expandedPopup().popupCc())
            .clicksOn(onComposePopup().abookBtn())
            .shouldSee(onComposePopup().abookPopup())
            .turnTrue(onComposePopup().abookPopup().abookAdressesCheckboxList().get(0))
            .clicksOn(onComposePopup().abookPopup().selectButton());
        user.composeSteps().inputsAddressInFieldTo(DEV_NULL_EMAIL)
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.defaultSteps().clicksOn(onHomePage().checkMailButton());
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("Закрываем попап контактов по крестику")
    @TestCaseId("2827")
    public void shouldCloseAllContactsPopup() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupToInput())
            .clicksOn(onComposePopup().abookBtn())
            .shouldSee(onComposePopup().abookPopup())
            .clicksOn(onComposePopup().abookCloseBtn())
            .shouldNotSee(onComposePopup().abookPopup());
    }

    @Test
    @Title("Выбираем все контакты в попапе Абука")
    @TestCaseId("2831")
    public void shouldSelectAllContacts() {
        user.apiAbookSteps().addCoupleOfContacts(5);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupToInput())
            .shouldSee(onComposePopup().suggestList())
            .clicksOn(onComposePopup().abookBtn())
            .shouldSee(onComposePopup().abookPopup())
            .clicksOn(onComposePopup().abookPopup().checkAllContacts())
            .shouldSeeThatEvery(onComposePopup().abookPopup().abookAdressesCheckboxList(), true);
    }
}
