package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
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
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на квикреплай в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
public class QuickReplyTest {

    private static final String TEXT = "Knowledge is power";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().apiMessagesSteps().sendThread(accLock.firstAcc(), Utils.getRandomString(), 2);
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomString(), "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Отправить ответ на одиночное письмо")
    @TestCaseId("265")
    public void shouldSendAnswerOnMsg() {
        openMsg(0);
        sendMsgFromQuickreply();
        openMsgWithRefresh();
        steps.user().defaultSteps()
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 3);
    }

    @Test
    @Title("Кнопка отправки дизэйблится, если очистить поле")
    @TestCaseId("269")
    public void shouldDisableBtn() {
        openMsg(0);
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().messageView().quickReply().input(), TEXT)
            .shouldNotSee(steps.pages().touch().messageView().quickReply().disabledSend())
            .inputsTextInElement(steps.pages().touch().messageView().quickReply().input(), "\b")
            //кнопка не гаснет по триггеру empty(), нужно именно "стирать"
            .clicksOn(steps.pages().touch().messageView().quickReply().input())
            .shouldSee(steps.pages().touch().messageView().quickReply().disabledSend());
    }

    @Test
    @Title("Отправить в треде несколько писем подряд")
    @TestCaseId("562")
    public void shouldSendFewMsges() {
        openMsg(1);
        sendMsgFromQuickreply();
        openMsgWithRefresh();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().threadHeader())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 4);
        sendMsgFromQuickreply();
        openMsgWithRefresh();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().threadHeader())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 6);
    }

    @Test
    @Title("Сохранение черновика после переходе в композ")
    @TestCaseId("676")
    public void shouldSaveDraftAfterQuickreply() {
        openMsg(0);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageView().quickReply())
            .clicksOn(steps.pages().touch().messageView().quickReply().expandCompose())
            .shouldSee(steps.pages().touch().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldSee(steps.pages().touch().messageView().header());
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(DRAFT, 1);
    }

    @Test
    @Title("Должна загореться кнопка отправки при вставке из буфера обмена")
    @TestCaseId("591")
    public void shouldSeeActiveSendBtnInQR() {
        openMsg(0);
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().touch().messageView().quickReply().input(), Utils.getRandomName());
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().messageView().quickReply().input(),
            Keys.chord(Keys.CONTROL, "a")
        );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().messageView().quickReply().input(),
            Keys.chord(Keys.CONTROL, "x")
        );
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().quickReply().disabledSend());
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().messageView().quickReply().input(),
            Keys.chord(Keys.CONTROL, "v")
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().quickReply().disabledSend());
    }

    @Test
    @Title("Кнопка отправки дизэйблится после отправки")
    @TestCaseId("1255")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDisableBtnAfterSent() {
        openMsg(0);
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().messageView().quickReply().input(), TEXT)
            .clicksOn(steps.pages().touch().messageView().quickReply().send())
            .shouldSee(steps.pages().touch().messageView().quickReply().disabledSend());
    }

    @Test
    @Title("Квикреплай должен отображаться в письмах в табах")
    @TestCaseId("1341")
    public void shouldSeeQRInTabs() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
        steps.user().defaultSteps().refreshPage();
        openMsg(0);
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().quickReply());
    }

    @Step("Обновляем инбокс и открываем письмо")
    private void openMsgWithRefresh() {
        steps.user().defaultSteps().refreshPage();
        openMsg(0);
    }

    @Step("Грузим инбокс и открываем письмо")
    private void openMsg(int num) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(num));
    }

    @Step("Отправляем письмо из квикреплая")
    private void sendMsgFromQuickreply() {
        steps.user().defaultSteps().inputsTextInElement(steps.pages().touch().messageView().quickReply().input(), TEXT)
            .clicksOn(steps.pages().touch().messageView().quickReply().send())
            .shouldNotSee(steps.pages().touch().messageView().quickReplyOverlay());
    }
}
