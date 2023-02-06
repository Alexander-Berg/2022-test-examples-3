package ru.yandex.autotests.innerpochta.tests.screentests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;

/**
 * @author sshelgunova
 */
@Aqua.Test
@Title("Тесты на попап копирование ябблов")
@Features({FeaturesConst.MESSAGE_FULL_VIEW})
@Stories(FeaturesConst.COPY_YABBLE)
public class CopyYabbleScreenTest {

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
    public void prepare() {
        stepsProd.user().apiMessagesSteps().withAuth(auth2).sendMailToSeveralReceivers(
            Utils.getRandomString(), Utils.getRandomString(), acc.firstAcc().getSelfEmail(), DEV_NULL_EMAIL
        );
        stepsProd.user().defaultSteps().waitInSeconds(1);
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Должный увидеть результаты поиска по адресу после клика на Показать переписку в попапе копирования ябблов")
    @TestCaseId("990")
    public void shouldSeeCorrespondence() {
        Consumer<InitStepsRule> act = steps -> {
            openPopupCopyYabble(steps, 0);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().yabblePopup().btnShowCrrspndnce())
                .shouldSee(steps.pages().touch().search().header());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Test
    @Title("После клика на кнопку Скопировать адрес появляется статуслайн о копировании адреса в буфер обмена")
    @TestCaseId("858")
    public void shouldSeeStatusLineAfterCopy() {
        Consumer<InitStepsRule> act = steps -> {
            openPopupCopyYabble(steps, 0);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().yabblePopup().btnCopyAddress())
                .shouldSee(steps.pages().touch().messageView().statusLineInfo())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(steps.pages().touch().messageView().statusLineInfo());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Test
    @Title("Должны увидеть попап копирования ябблов при тапе на второй яббл из поля, в котором более одного яббла")
    @TestCaseId("861")
    public void shouldSeePopupCopy() {
        Consumer<InitStepsRule> act = steps -> openPopupCopyYabble(steps, 1);

        parallelRun.withAcc(acc.firstAcc()).withActions(act).run();
    }

    @Step("Открываем попап копирования ябблов")
    private void openPopupCopyYabble(InitStepsRule st, int number) {
        st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock().subject())
            .shouldNotSee(st.pages().touch().messageView().msgLoaderInView())
            .clicksOn(st.pages().touch().messageView().avatarToolbar())
            .shouldSee(st.pages().touch().messageView().msgDetails());
        st.user().touchSteps().longTap(st.pages().touch().messageView().yabbles().get(number));
        st.user().defaultSteps().shouldSee(st.pages().touch().messageView().yabblePopup());
    }
}