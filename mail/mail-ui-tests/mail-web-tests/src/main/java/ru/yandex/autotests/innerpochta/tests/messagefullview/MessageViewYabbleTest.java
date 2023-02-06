package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Проверяем выпадушку из Yabble.")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.YABBLE)
public class MessageViewYabbleTest extends BaseTest {

    private static final String CONTACT_EMAIL = "newtestbot6@yandex.ru";
    private static final String CONTACT_NAME = "newtestbot6";
    private static final String BLACK_LIST_NOTIFICATION_TEXT = "Письма с него не будут больше приходить в ваш ящик";
    private static final String RECIPIENTS_COUNT = "2 получателя";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Contact contact;
    private final String sbj = getRandomString();
    private final AccLockRule lock = AccLockRule.use().useTusAccount(2);
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        contact = user.abookSteps().createContactWithParametrs(CONTACT_NAME, CONTACT_EMAIL);
        Message msg = user.apiMessagesSteps().addCcEmails(contact.getEmail().get(0).getValue())
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()));
    }

    @Test
    @Title("Проверяем кнопку «В черный список»")
    @TestCaseId("2040")
    public void shouldAddToABlackMailList() {
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .clicksOn(onMessageView().messageHead().contactsInCC().waitUntil(not(empty())).get(0))
            .shouldSee(onMessageView().contactBlockPopup())
            .clicksOn(onMessageView().contactBlockPopup().addToBlacklistBtn())
            .shouldSee(onMessagePage().statusLineBlock().textBox())
            .shouldContainText(onMessagePage().statusLineBlock().textBox(), BLACK_LIST_NOTIFICATION_TEXT);
        user.apiFiltersSteps().shouldContainAdressInBlackList(contact.getEmail().get(0).getValue());
    }

    @Test
    @Title("В выпадушке должен быть прописан адрес получателя, по которому кликнули")
    @TestCaseId("2041")
    public void shouldSeeReceiversEmail() {
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .clicksOn(onMessageView().messageHead().contactsInCC().waitUntil(not(empty())).get(0))
            .shouldSee(onMessageView().contactBlockPopup())
            .shouldContainText(
                onMessageView().contactBlockPopup().emailAddress(),
                contact.getEmail().get(0).getValue()
            );
    }

    @Test
    @Title("Выпадушка должна закрываться кликом по любой области кроме самой выпадушки")
    @TestCaseId("2044")
    public void shouldClosePopupByClick() {
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .clicksOn(onMessageView().messageHead().contactsInCC().waitUntil(not(empty())).get(0))
            .shouldSee(onMessageView().contactBlockPopup())
            .clicksOn(onMessageView().messageSubjectInFullView())
            .shouldNotSee(onMessageView().contactBlockPopup());
    }

    @Test
    @Title("Проверяем отображение получателей при получении скрытой копии")
    @TestCaseId("3959")
    public void shouldNotSeeSomeoneElsesAddress() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), DEV_NULL_EMAIL_2)
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), lock.accNum(1).getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.loginSteps().forAcc(lock.accNum(1)).logins();
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps()
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldNotSeeElementInList(
                onMessageView().messageHead().contactsInCC(),
                lock.accNum(1).getSelfEmail()
            )
            .shouldSeeThatElementTextEquals(
                onMessageView().messageHead().recipientsCount(),
                RECIPIENTS_COUNT
            );
    }
}
