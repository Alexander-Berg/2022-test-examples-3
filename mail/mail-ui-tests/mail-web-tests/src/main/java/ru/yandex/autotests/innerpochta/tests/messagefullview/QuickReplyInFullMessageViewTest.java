package ru.yandex.autotests.innerpochta.tests.messagefullview;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Math.toIntExact;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на Quick Reply в письме на отдельной странице")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
@RunWith(DataProviderRunner.class)
public class QuickReplyInFullMessageViewTest extends BaseTest {

    private static final String MORE_RECEIVERS = "и ещё 1 получатель";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final String text = Utils.getRandomName();
    private final String sbj = getRandomString();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] userInterfaceLayout() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        msg = user.apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), sbj, "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
    }

    @Test
    @Title("Проверяем, что есть все кнопки в свернутом QR")
    @TestCaseId("2177")
    public void shouldSeeReplyAllInQuickReplyText() {
        user.defaultSteps().shouldSee(onMessageView().quickReplyPlaceholder());
    }

    @Test
    @Title("Ответить всем на письмо через QR")
    @TestCaseId("2555")
    public void shouldSentLetterToAllFromQR() {
        user.messageViewSteps().openQRAndInputText(text);
        user.defaultSteps()
            .shouldHasText(onMessageView().quickReply().yabbleMore(), MORE_RECEIVERS)
            .clicksOn(onMessageView().quickReply().sendButton())
            .shouldSee(onMessageView().doneQrMessage())
            .opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(1);
    }

    @Test
    @Title("Проверка перехода в полноэкранный композ из QR")
    @TestCaseId("2168")
    public void shouldOpenComposeButton() {
        user.messageViewSteps().openQRAndInputText(text);
        user.defaultSteps().clicksOn(onMessageView().quickReply().openCompose())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), text);
    }

    @Test
    @Title("Закрыть QR по крестику")
    @TestCaseId("2169")
    public void shouldCloseQR() {
        user.messageViewSteps().openQRAndInputText(text);
        user.defaultSteps().clicksOn(onMessageView().quickReply().closeQrBtn())
            .shouldSee(onMessageView().quickReplyPlaceholder())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(1);
    }

    @Test
    @Title("Отправить ответ из QR по хоткею")
    @TestCaseId("3379")
    public void shouldSentLetterFromQRByHotkey() {
        user.messageViewSteps().openQRAndInputText(text);
        user.hotkeySteps().pressCombinationOfHotKeys(
            onMessageView().quickReply().replyText(),
            key(Keys.CONTROL), key(Keys.ENTER)
        );
        user.defaultSteps().shouldSee(onMessageView().doneQrMessage())
            .opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeInboxUnreadCounter(1);
    }

    @Test
    @Title("Должны видеть QR, когда письмо превращает в тред")
    @UseDataProvider("userInterfaceLayout")
    @TestCaseId("2392")
    public void shouldSeeQuickReplyAfterConvertMessageToThread(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Переключаем интерфейс",
            of(
                SETTINGS_PARAM_LAYOUT, layout,
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
        user.defaultSteps().opensDefaultUrl();
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .inputsTextInElement(onMessageView().quickReply().replyText(), getRandomString());
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(sbj, lock.firstAcc(), "");
        user.defaultSteps().shouldSeeThatElementHasTextWithCustomWait(
            onMessageView().messageSubject().threadCount(),
            "2",
            toIntExact(MILLISECONDS.toSeconds(XIVA_TIMEOUT))
        ).shouldSee(onMessageView().quickReply().replyText());
    }
}
