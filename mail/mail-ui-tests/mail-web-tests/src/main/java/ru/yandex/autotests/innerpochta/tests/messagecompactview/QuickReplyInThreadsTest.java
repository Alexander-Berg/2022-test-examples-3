package ru.yandex.autotests.innerpochta.tests.messagecompactview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.text.ParseException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.convertDate;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_AUTOSAVE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_ENABLE_QUOTING;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

/**
 * Created by kurau
 */
@Aqua.Test
@Title("Тесты на Quick Reply")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.QR)
public class QuickReplyInThreadsTest extends BaseTest {

    private static final String REPLY_PREFIX = "Re: ";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private String subject = Utils.getRandomName();
    private String text = Utils.getRandomName();
    private String newText = Utils.getRandomName();
    private String signature = Utils.getRandomName();
    private Message msg;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, "");
        msg = user.apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), subject, text);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("Ссылка «Быстрый ответ всем участникам переписки» разворачивает QR.")
    @TestCaseId("1997")
    public void quickReplyRising() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .shouldSee(
                onMessageView().quickReply().replyText(),
                onMessageView().quickReply().openCompose(),
                onMessageView().quickReply().sendButton()
            );
    }

    @Test
    @Title("Проверка перехода в полноэкранный композ из квикреплая.")
    @TestCaseId("1644")
    public void quickReplyOpenComposeButton() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .shouldSee(onMessageView().quickReply())
            .inputsTextInElement(onMessageView().quickReply().replyText(), newText)
            .clicksOn(onMessageView().quickReply().openCompose())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), newText);
    }

    @Test
    @TestCaseId("3285")
    @Title("Проверка подстановки цитирования при переходе в полную форму ответа.")
    public void shouldShowQuoteAndSignInCompose() throws ParseException {
        user.apiSettingsSteps().changeSignsWithTextAndAmount(sign(signature))
            .callWithListAndParams(SETTINGS_ENABLE_QUOTING, of(SETTINGS_ENABLE_QUOTING, STATUS_ON));
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .clicksOn(onMessageView().quickReply().openCompose())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldHasText(
                onComposePopup().yabbleTo().yabbleText(),
                user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME)
            )
            .shouldHasText(onComposePopup().yabbleCc().yabbleText(), DEV_NULL_EMAIL)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), createQuotationString());
    }

    @Test
    @TestCaseId("3284")
    @Title("Проверка автосохранения данных.")
    public void shouldAutoSaveData() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .inputsTextInElement(onMessageView().quickReply().replyText(), newText)
            .waitInSeconds(10)
            .opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(REPLY_PREFIX + subject);
        user.defaultSteps().clicksOn(user.messagesSteps().findMessageBySubject(REPLY_PREFIX + subject).sender());
        user.composeSteps().shouldSeeTextAreaContains(newText);
    }

    @Test
    @TestCaseId("1059")
    @Title("Сохранение черновика из QR по Ctrl+S")
    public void shouldSaveDraftHotKey() {
        user.apiSettingsSteps()
            .callWithListAndParams(SETTINGS_ENABLE_AUTOSAVE, of(SETTINGS_ENABLE_AUTOSAVE, STATUS_FALSE));
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .inputsTextInElement(onMessageView().quickReply().replyText(), newText);
        user.hotkeySteps().pressHotKeysWithDestination(
            onMessageView().quickReply().replyText(),
            Keys.chord(Keys.CONTROL, "s")
        );
        user.defaultSteps().opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(REPLY_PREFIX + subject);
        user.defaultSteps().clicksOn(user.messagesSteps().findMessageBySubject(REPLY_PREFIX + subject).sender());
        user.composeSteps().shouldSeeTextAreaContains(newText);
    }

    private String createQuotationString() throws ParseException {
        return new StringBuilder(convertDate("y-MM-dd'T'HH:mm:ss", msg.getDate().getIso(), "dd.MM.y, HH:mm"))
            .append(", \"")
            .append(user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME))
            .append("\" <")
            .append(lock.firstAcc().getSelfEmail())
            .append(">:\n")
            .append(text)
            .append("\n\n\n")
            .append(signature)
            .toString();
    }
}
