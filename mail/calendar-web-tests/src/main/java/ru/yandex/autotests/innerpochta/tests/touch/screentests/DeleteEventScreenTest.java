package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Верстка модальных окон удаления события в Календаре")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
public class DeleteEventScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Test
    @Title("Верстка модального окна подтверждения удаления простого события")
    @TestCaseId("1067")
    public void shouldSeeDeleteSimpleEventPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiCalSettingsSteps().createNewEvent(
                st.user().settingsCalSteps()
                    .formDefaultEvent(st.user().apiCalSettingsSteps().getUserLayersIds().get(0))
            );
            st.user().defaultSteps().refreshPage()
                .clicksOn(
                    st.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                    st.pages().cal().touchHome().eventPage().delete()
                )
                .shouldSee(st.pages().cal().touchHome().deleteEvenPopup().refuseOrAllEventsBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка модального окна удаления одного события или всей серии")
    @TestCaseId("1068")
    public void shouldSeeDeleteRepeatableEventPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiCalSettingsSteps().createNewRepeatEvent(
                st.user().settingsCalSteps()
                    .formDefaultRepeatingEvent(st.user().apiCalSettingsSteps().getUserLayersIds().get(0))
            );
            st.user().defaultSteps().refreshPage()
                .clicksOn(
                    st.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                    st.pages().cal().touchHome().eventPage().delete()
                )
                .shouldSee(st.pages().cal().touchHome().deleteEvenPopup().close());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
