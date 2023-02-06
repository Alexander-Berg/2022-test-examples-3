package ru.yandex.autotests.innerpochta.tests.messageView;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Плашка спама в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.MESSAGE_HEADER)
@RunWith(DataProviderRunner.class)
public class SpamReasonMessageViewTest {

    private ScreenRulesManager rules = screenRulesManager();
    private AccLockRule lock = rules.getLock().className();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Должны видеть плашку «Почему в спаме»")
    @TestCaseId("6288")
    public void shouldSeeSpamReasonBar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.user().pages().MessageViewPage().spamReasonBar());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(QuickFragments.SPAM).run();
    }

    @Test
    @Title("Должны видеть плашку «Картинки отключены», если закрыть плашку «Почему в спаме»")
    @TestCaseId("6289")
    public void shouldSeeDisabledImagesBar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.user().pages().MessageViewPage().spamReasonBar())
                .clicksOn(st.user().pages().MessageViewPage().spamReasonBarCross())
                .shouldSee(st.user().pages().MessageViewPage().spamLinksNotification());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(QuickFragments.SPAM).run();
    }
}
