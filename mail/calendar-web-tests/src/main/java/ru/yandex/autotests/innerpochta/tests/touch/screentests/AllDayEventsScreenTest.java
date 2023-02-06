package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static java.lang.Integer.parseInt;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Вёрстка элементов блока событий на весь день")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.ALL_DAY_EVENT)
@RunWith(DataProviderRunner.class)
public class AllDayEventsScreenTest {

    private static final String MIN_EVENTS_TO_SHOW_MORE_BTN = "4";
    private static final String MAX_EVENTS_NOT_TO_SHOW_MORE_BTN = "3";

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
    @Title("Вёрстка блока событий на весь день в зависимости от числа событий")
    @TestCaseId("1061")
    @DataProvider({MAX_EVENTS_NOT_TO_SHOW_MORE_BTN, MIN_EVENTS_TO_SHOW_MORE_BTN})
    public void shouldSeeEventsBlock(int messageNumber) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSeeInViewport(st.pages().cal().touchHome().allDayEventsBlock());

        stepsProd.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(messageNumber);
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка развёрнутого блока событий на весь день")
    @TestCaseId("1062")
    public void shouldSeeExpandedEventsBlock() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSeeInViewport(st.pages().cal().touchHome().allDayEventsBlock())
            .clicksOn(st.pages().cal().touchHome().allDayEventsMoreButton());

        stepsProd.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(parseInt(MIN_EVENTS_TO_SHOW_MORE_BTN));
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
