package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Различные попапы в футере")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeFooterTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Открываем попап напоминания о неответе")
    @TestCaseId("3224")
    public void shouldSeeNotifyPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().notifyBtn())
                .turnTrue(st.pages().mail().composePopup().expandedPopup().notifyPopup().options().get(0));

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Включаем чекбокс «Сообщить о доставке письма»")
    @TestCaseId("3225")
    public void shouldSeeSelectedNotify() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().notifyBtn())
                .turnTrue(st.pages().mail().composePopup().expandedPopup().notifyPopup().options().get(1));

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап отложенной отправки")
    @TestCaseId("3226")
    public void shouldSeeSendInTimePopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().delaySendBtn())
                .onMouseHover(st.pages().mail().composePopup().expandedPopup().delaySendPopup());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем переводчик")
    @TestCaseId("5882")
    public void shouldSeeTranslator() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().translateBtn())
                .shouldSee(
                    st.pages().mail().composePopup().expandedPopup().translateHeader(),
                    st.pages().mail().composePopup().expandedPopup().translateText(),
                    st.pages().mail().composePopup().expandedPopup().translateHeader().editTranslateBtn(),
                    st.pages().mail().composePopup().expandedPopup().translateHeader().translateHelp(),
                    st.pages().mail().composePopup().expandedPopup().translateHeader().translateToLink().get(0),
                    st.pages().mail().composePopup().expandedPopup().translateHeader().translateToLink().get(1)
                );

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }
}
