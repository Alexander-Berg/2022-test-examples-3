package ru.yandex.autotests.innerpochta.tests.screentests.IframeCompose;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап с напоминаниями")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.REMINDER)
public class ComposeRemindersScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Вёрстка попапа с напониманиями")
    @TestCaseId("1409")
    public void shouldSeeReminderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().reminders())
                .shouldSee(st.pages().touch().composeIframe().remindersPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Вёрстка кнопки и попапа с включенными напониманиями")
    @TestCaseId("1409")
    public void shouldSeeTurnedOnReminder() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().reminders())
                .shouldSee(st.pages().touch().composeIframe().remindersPopup())
                .clicksOn(st.pages().touch().composeIframe().remindersPopup().checkbox().get(1))
                .shouldSee(st.pages().touch().composeIframe().header().turnedOnReminders());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть пресеты для напоминания о неответе на письмо")
    @TestCaseId("1410")
    public void shouldSeeReminderPresets() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().reminders())
                .shouldSee(st.pages().touch().composeIframe().remindersPopup())
                .clicksOn(st.pages().touch().composeIframe().remindersPopup().checkbox().get(0))
                .shouldSee(
                    st.pages().touch().composeIframe().header().turnedOnReminders(),
                    st.pages().touch().composeIframe().remindersPopup().presets()
                    );
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }
}
