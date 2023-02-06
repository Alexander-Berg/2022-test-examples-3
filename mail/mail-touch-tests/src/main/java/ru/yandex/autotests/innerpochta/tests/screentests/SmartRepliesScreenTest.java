package ru.yandex.autotests.innerpochta.tests.screentests;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Скриночные тесты на смарт реплаи")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.QR)
@RunWith(DataProviderRunner.class)
public class SmartRepliesScreenTest {

    private static final String MSG_TEXT = "Как дела?";
    private static final String LONG_MSG_TEXT = "Как дела?" +
        "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nКак дела?";

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

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
        stepsProd.user().apiMessagesSteps().withAuth(auth2)
            .sendMailWithNoSaveWithoutCheck(acc.firstAcc().getSelfEmail(), MSG_TEXT, LONG_MSG_TEXT);
        stepsProd.user().apiMessagesSteps().withAuth(auth2)
            .sendMailWithNoSaveWithoutCheck(acc.firstAcc().getSelfEmail(), MSG_TEXT, MSG_TEXT);
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Должны видеть блок со смарт реплаями")
    @TestCaseId("1293")
    public void shouldSeeSmartReplies() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().quickReply().smartReply());

        String mid = stepsProd.user().apiMessagesSteps().getAllMessages().get(0).getMid();
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(mid)).run();
    }

    @Test
    @Title("Блок со смарт реплаями должен залипать внизу страницы")
    @TestCaseId("1292")
    @DoTestOnlyForEnvironment("iOS")
    public void shouldAlwaysSeeSR() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageView().quickReply().smartReply());

        String mid = stepsProd.user().apiMessagesSteps().getAllMessages().get(1).getMid();
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).withUrlPath(MSG_FRAGMENT.makeTouchUrlPart(mid)).run();
    }
}
