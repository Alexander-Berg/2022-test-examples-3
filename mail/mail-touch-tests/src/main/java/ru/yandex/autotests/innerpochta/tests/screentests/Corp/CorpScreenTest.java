package ru.yandex.autotests.innerpochta.tests.screentests.Corp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

@Aqua.Test
@Title("Общие скриночные тесты на корп")
@Features({FeaturesConst.CORP_PACK})
@Stories(FeaturesConst.CORP)
public class CorpScreenTest {

    private static final String SIDEBAR = "?show-left-panel=1";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        String subj = Utils.getRandomString();
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, Utils.getRandomString());
    }

    @Test
    @Title("Не должны видеть блок Настройки в сайдбаре на корпе")
    @TestCaseId("620")
    public void shouldNotSeeSettingsOnCorp() {
        Message msg = stepsProd.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0);
        String mid = msg.getMid();
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().sidebar().leftPanelBox())
                .scrollTo(st.pages().touch().sidebar().sidebarPromo())
                .shouldSee(st.pages().touch().sidebar().leftPanelItems().get(0));

        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc())
            .withUrlPath(FOLDER_ID.makeTouchUrlPart("1/") + MSG_FRAGMENT.fragment(mid) + SIDEBAR).run();
    }

    @Test
    @Title("Открываем письмо на просмотр")
    @TestCaseId("1017")
    public void shouldSeeCorpMsg() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().subjectList().get(0))
                .shouldSee(st.pages().touch().messageView().header());

        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть пользователя без аватарки")
    @TestCaseId("641")
    public void shouldSeeUserWithoutAvatar() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().subjectList().get(0))
                .shouldSee(st.pages().touch().messageView().header())
                .clicksOn(st.pages().touch().messageView().toolbar())
                .shouldSee(st.pages().touch().messageView().msgDetails());

        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть аватарки от разных сервисов")
    @TestCaseId("642")
    public void shouldSeeAvatarsFromServices() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock());

        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart("7")).run();
    }

    @Test
    @Title("Должны увидеть аватарки сервисов в деталях письма ")
    @TestCaseId("695")
    public void shouldSeeServiceAvatarsInMsgDetails() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().subjectList().get(0))
                .shouldSee(st.pages().touch().messageView().header())
                .clicksOn(st.pages().touch().messageView().toolbar())
                .shouldSee(st.pages().touch().messageView().msgDetails());

        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart("7")).run();
    }

    @Test
    @Title("Должны увидеть яббл внешнего юзера в композе")
    @TestCaseId("1407")
    public void shouldSeeExternalAddressInCompose() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().touch().composeIframe().inputTo(), DEV_NULL_EMAIL)
                .clicksOn(st.pages().touch().composeIframe().inputSubject())
                .shouldSee(st.pages().touch().composeIframe().externalYabble());
        };
        parallelRun.withActions(act).withCorpAcc(accLock.firstAcc())
            .withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}
