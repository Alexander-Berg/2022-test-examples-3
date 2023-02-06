package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
@Title("[Тач] Верстка модальных окон редактирования события в Календаре")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.EDIT_EVENT)
public class EditEventScreenTest {

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

    @Before
    public void setUp() {
        Long layerID = stepsProd.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        stepsProd.user().apiCalSettingsSteps()
            .createNewRepeatEvent(stepsProd.user().settingsCalSteps().formDefaultRepeatingEvent(layerID));
    }

    @Test
    @Title("Верстка модального окна подтверждения выхода из редактирования")
    @TestCaseId("1219")
    public void changedMindToEditPopup() {
        String title = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldNotSee(st.pages().cal().touchHome().loader())
            .clicksOn(
                st.pages().cal().touchHome().eventPage().edit(),
                st.pages().cal().touchHome().eventsPeriodicityPopup().confirmOrOneEventBtn()
            )
            .inputsTextInElement(st.pages().cal().touchHome().eventPage().editableTitle(), title)
            .clicksOn(st.pages().cal().touchHome().eventPage().cancelEdit());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка модального окна выбора режима редактирования регулярного события")
    @TestCaseId("1220")
    public void regularEventEditModePopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldNotSee(st.pages().cal().touchHome().loader())
            .clicksOn(st.pages().cal().touchHome().eventPage().edit());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
