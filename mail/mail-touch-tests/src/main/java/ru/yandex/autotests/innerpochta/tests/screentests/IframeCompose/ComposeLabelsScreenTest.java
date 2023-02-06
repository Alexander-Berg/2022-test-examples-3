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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_RED_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап меток в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeLabelsScreenTest {

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
    @Title("Верстка попапа меток без пользовательских меток")
    @TestCaseId("1511")
    public void shouldSeeLabelsPopupWitOutCustomLabels() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().labels())
                .shouldSee(st.pages().touch().composeIframe().labelsPopup());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Верстка попапа меток с пользовательскими метками")
    @TestCaseId("1511")
    public void shouldSeeLabelsPopupWithCustomLabels() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().labels())
                .shouldSee(st.pages().touch().composeIframe().labelsPopup());
        };
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_RED_COLOR);
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Верстка композа с меткой")
    @TestCaseId("1512")
    public void shouldSeeLabelsInCompose() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().switchToComposeIframe();
            addLabel(st);
            addLabel(st);
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().labels());
        };
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_RED_COLOR);
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(COMPOSE.makeTouchUrlPart()).run();
    }

    @Step("Добавить метку")
    private void addLabel(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().touch().composeIframe().header().labels())
            .shouldSee(st.pages().touch().composeIframe().labelsPopup())
            .clicksOn(st.pages().touch().composeIframe().labelsPopup().labels().get(0));
    }
}
