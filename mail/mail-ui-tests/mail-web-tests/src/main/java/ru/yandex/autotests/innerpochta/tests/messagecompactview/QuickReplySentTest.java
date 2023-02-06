package ru.yandex.autotests.innerpochta.tests.messagecompactview;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created by mabelpines on 29.02.16.
 */
@Aqua.Test
@Title("Тест на отправку и закрытие писем в QR")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.QR)
public class QuickReplySentTest extends BaseTest {

    private static final int THREAD_SIZE = 2;
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private String subject = Utils.getRandomName();
    private String msgText = Utils.getRandomName();
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), subject, THREAD_SIZE);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-horizontal",
            of(SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .inputsTextInElement(onMessageView().quickReply().replyText(), msgText);
    }

    @Test
    @Title("Ответить на письмо через QR")
    @TestCaseId("1996")
    public void shouldSentLetterFromQR() {
        user.defaultSteps().clicksOn(onMessageView().quickReply().sendButton())
            .shouldSee(onMessageView().quickReplyPlaceholder())
            .refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 4);
    }

    @Test
    @Title("Закрыть QR по крестику")
    @TestCaseId("1998")
    public void shouldCloseQR() {
        user.defaultSteps().clicksOn(onMessageView().quickReply().closeQrBtn())
            .shouldSee(onMessageView().quickReplyPlaceholder());
        user.messagesSteps().shouldSeeThreadCounter(subject, THREAD_SIZE);
    }

    @Test
    @Title("Отправить ответ из QR по хоткею")
    @TestCaseId("3379")
    public void shouldSentLetterFromQRByHotkey() {
        user.hotkeySteps().pressCombinationOfHotKeys(
            onMessageView().quickReply().replyText(),
            key(Keys.CONTROL), key(Keys.ENTER)
        );
        user.defaultSteps().shouldSee(onMessageView().quickReplyPlaceholder())
            .refreshPage();
        user.messagesSteps().shouldSeeThreadCounter(subject, 4);
    }
}
