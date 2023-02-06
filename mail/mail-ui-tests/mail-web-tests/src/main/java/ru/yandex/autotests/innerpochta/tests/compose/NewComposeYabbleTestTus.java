package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Ябблы")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewComposeYabbleTestTus extends BaseTest {
    private static final String CONTACT_WITH_TWO_EMAILS = "Два Адреса";
    private static final String CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL = "testbot3@yandex.ru";
    private static final String CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL = "testbot4@yandex.ru";
    private Contact contact;
    String msgSubject;
    String msgTo;

    List<Email> emails = Arrays.asList(
        new Email().withValue(CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL),
        new Email().withValue(CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL)
    );

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
        contact = user.abookSteps().createContactWithParametrs(
            CONTACT_WITH_TWO_EMAILS,
            CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL
        ).withEmail(emails);
        user.apiAbookSteps().addContactWithTwoEmails(CONTACT_WITH_TWO_EMAILS, contact);
        msgSubject = getRandomString();
        msgTo = lock.firstAcc().getSelfEmail();
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем треды и сбрасываем свёрнутые композы",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, false,
                SETTINGS_HEAD_FULL_EDITION, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Редактируем имя контакта через попап яббла")
    @TestCaseId("5697")
    public void shouldChangeContactFromYabbleMenu() {
        user.apiAbookSteps().addContact(CONTACT_WITH_TWO_EMAILS, contact);
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().yabbleTo())
            .clicksOn(onComposePopup().yabbleDropdown().editYabble())
            .shouldSeeThatElementHasText(
                onComposePopup().expandedPopup().popupTo(),
                String.format("\"%s\" <%s>", CONTACT_WITH_TWO_EMAILS, CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            )
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .clicksOn(
                onComposePopup().expandedPopup().sbjInput(),
                onComposePopup().expandedPopup().sendBtn()
            );
        waitForMessageToBeSendAndCheckRecipient(DEV_NULL_EMAIL, msgSubject);
    }

    @Test
    @Title("Отправка письма после выбора другого адреса из «От кого»")
    @TestCaseId("5710")
    public void shouldSendChangedSenderEmail() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), msgTo)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().yabbleFrom())
            .shouldSee(onComposePopup().fromSuggestList().get(0));
        String email = onComposePopup().fromSuggestList().get(1).fromEmail().getText();
        user.defaultSteps().clicksOn(onComposePopup().fromSuggestList().get(1))
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps().shouldSeeThatElementHasText(onMessageView().messageHead().fromAddress(), email);
    }

    @Test
    @Title("Копируем email из попапа яббла")
    @TestCaseId("5705")
    public void shouldCopyEmailFromYabbleMenu() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().yabbleTo())
            .clicksOn(onComposePopup().yabbleDropdown().copyEmail());
        user.hotkeySteps().pressHotKeysWithDestination(
            onComposePopup().expandedPopup().bodyInput(),
            Keys.chord(Keys.CONTROL, "v")
        );
        user.defaultSteps().shouldSeeThatElementHasText(
            onComposePopup().expandedPopup().bodyInput(),
            CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL
        );
    }

    @Test
    @Title("Пишем «Только этому получателю» в попапе яббла")
    @TestCaseId("5709")
    public void shouldSendOnlyThisContactFromYabbleMenu() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), DEV_NULL_EMAIL)
            .offsetFromRightCornerClick(onComposePopup().expandedPopup().sbjInput(), 5, 5)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .clicksOn(onComposePopup().yabbleCc())
            .clicksOn(onComposePopup().yabbleDropdown().singleTarget())
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .shouldNotContainText(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            .shouldNotContainText(onComposePopup().expandedPopup().popupCc(), DEV_NULL_EMAIL)
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        waitForMessageToBeSendAndCheckRecipient(DEV_NULL_EMAIL, msgSubject);
    }

    @Step("Ждём отправки сообщения и проверяем получателя в отправленных")
    private void waitForMessageToBeSendAndCheckRecipient(String recipient, String subject) {
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.SENT).refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeAddressOnMessageWithSubject(recipient, subject);
    }
}
