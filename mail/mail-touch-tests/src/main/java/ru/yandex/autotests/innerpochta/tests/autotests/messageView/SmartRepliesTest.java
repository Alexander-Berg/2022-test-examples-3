package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SMART_REPLIES_TOUCH;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на смарт реплаи в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
public class SmartRepliesTest {

    private static final String GENERAL = "general";
    private static final String MSG_TEXT = "Как дела?";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(lock2)
        .around(auth2);

    @Before
    public void prep() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomName(), MSG_TEXT);
        steps.user().apiMessagesSteps().withAuth(auth2).sendMail(accLock.firstAcc().getSelfEmail(), MSG_TEXT, MSG_TEXT);
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны включить настройку умных ответов")
    @TestCaseId("1145")
    public void shouldSeeSRWhenSettingOn() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем настройку смарт реплаев",
            of(SMART_REPLIES_TOUCH, FALSE)
        );
        switchSRSetting();
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().settings().srTogglerOn())
            .opensDefaultUrlWithPostFix(
                MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
            )
            .shouldSee(steps.pages().touch().messageView().quickReply().smartReply());
    }

    @Test
    @Title("Должны отключить настройку умных ответов")
    @TestCaseId("1303")
    public void shouldNotSeeSRWhenSettingOff() {
        switchSRSetting();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(
                MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
            )
            .shouldNotSee(steps.pages().touch().messageView().quickReply().smartReply());
    }

    @Test
    @Title("Не должны видеть умных ответов на письме от самого себя")
    @TestCaseId("1143")
    public void shouldNotSeeSROnMsgFromYourself() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(
                MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(1).getMid())
            )
            .shouldNotSee(steps.pages().touch().messageView().quickReply().smartReply());
    }

    @Test
    @Title("Должны выбрать один из вариантов умных ответов")
    @TestCaseId("1294")
    public void shouldSelectSmartReplies() {
        openMsg();
        String firstSmartAnswer = steps.pages().touch().messageView().quickReply().smartReplies().get(0).getText();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().quickReply().smartReplies().get(0))
            .shouldNotSee(
                steps.pages().touch().messageView().quickReply().smartReply(),
                steps.pages().touch().messageView().quickReply().disabledSend()
            )
            .shouldContainText(steps.pages().touch().messageView().quickReply().input(), firstSmartAnswer);
    }

    @Test
    @Title("При вводе текста в квикреплай умные ответы должны исчезать")
    @TestCaseId("1299")
    public void shouldNotSeeSRWhenQRIsNotEmpty() {
        openMsgAndInputTextInQR();
    }

    @Test
    @Title("При очистке квикрепая умные ответы появляются снова")
    @TestCaseId("1300")
    public void shouldSeeSRAgain() {
        openMsgAndInputTextInQR();
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().messageView().quickReply().input(),
            Keys.chord(Keys.CONTROL, "a")
        );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().messageView().quickReply().input(),
            Keys.chord(Keys.CONTROL, "x")
        );
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().quickReply().smartReply());
    }

    @Test
    @Title("Умные ответы должны показываться под каждым письмом в треде")
    @TestCaseId("1298")
    public void shouldSeeSRInThread() {
        steps.user().apiMessagesSteps().withAuth(auth2).moveAllMessagesFromFolderToFolder(SENT, INBOX);
        steps.user().apiMessagesSteps().withAuth(auth2)
            .sendMessageToThreadWithSubjectWithoutCheck(MSG_TEXT, accLock.firstAcc(), MSG_TEXT);
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .shouldSee(steps.pages().touch().messageView().msgInThread().waitUntil(not(empty())).get(0)
                .quickReply().smartReply())
            .clicksOn(steps.pages().touch().messageView().msgInThread().get(1))
            .scrollTo(steps.pages().touch().messageView().msgInThread().get(1).quickReply().smartReply());
    }

    @Step("Открываем настройку, переключаем тумблер умных ответов")
    private void switchSRSetting() {
        steps.user().defaultSteps().opensCurrentUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .clicksOn(steps.pages().touch().settings().srToggler());
    }

    @Step("Открываем письмо, вводим текст в квикреплай")
    private void openMsgAndInputTextInQR() {
        openMsg();
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageView().quickReply().smartReply())
            .clicksAndInputsText(steps.pages().touch().messageView().quickReply().input(), Utils.getRandomString())
            .shouldNotSee(steps.pages().touch().messageView().quickReply().smartReply());
    }

    @Step("Открываем письмо")
    private void openMsg() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
        );
    }
}
