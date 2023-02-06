package ru.yandex.autotests.innerpochta.tests.screentests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.FORWARD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Дополнительные функции тулбара в просмотре письма у планшетов")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
@RunWith(DataProviderRunner.class)
public class ToolbarInViewScreenTest {

    private Message msg;

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @DataProvider
    public static Object[][] button() {
        return new Object[][]{
            {REPLY.btn()},
            {REPLY_ALL.btn()},
            {FORWARD.btn()}
        };
    }

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void sendMsg() {
        msg = stepsProd.user().apiMessagesSteps()
            .addCcEmails(DEV_NULL_EMAIL_2).addBccEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(accLock.firstAcc().getSelfEmail(), getRandomName(), Utils.getRandomString());
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Разворачиваем письмо на весь экран")
    @TestCaseId("552")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeMessageFullScreen() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().toolbar().fullScreenOpenBtn())
                .shouldNotSee(st.pages().touch().messageView().toolbar().fullScreenOpenBtn());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Сворачиваем письмо из полноэкранного просмотра в режим 2pane")
    @TestCaseId("553")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeMessageIn2pane() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().toolbar().fullScreenOpenBtn())
                .waitInSeconds(1) //пауза для анимации и смены иконок
                .clicksOn(st.pages().touch().messageView().toolbar().fullScreenCloseBtn())
                .shouldNotSee(st.pages().touch().messageView().toolbar().fullScreenCloseBtn());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Отвечаем на письмо из тулбара в просмотре письма")
    @TestCaseId("551")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldAnswerMsgFromMsgViewToolbar() {
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps()
                .shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().toolbar().replyAllBtn());
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputCc());
        };
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("По клику на кнопки ответить переходим в композ")
    @TestCaseId("372")
    @UseDataProvider("button")
    public void shouldSeeFilledCompose(String btn) {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), btn)
                .shouldBeOnUrl(containsString(COMPOSE.fragment()));
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputTo());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions)
            .withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(msg.getMid())).run();
    }
}
