package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
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

import java.time.LocalTime;
import java.util.function.Consumer;

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

@Aqua.Test
@Title("[Тач] Верстка сетки календаря")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class TouchGridScreenTest {

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
    @TestCaseId("1011")
    @Title("Проверяем вёрстку сетки календря после свайпа влево")
    public void shouldSeeNextDayGridAfterSwipe() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().swipeLeft(getGridRows(st).get(LocalTime.now().getHour()));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("1011")
    @Title("Проверяем вёрстку сетки календря после свайпа вправо")
    public void shouldSeeYesterdayGridAfterSwipe() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().swipeRight(getGridRows(st).get(LocalTime.now().getHour()));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("1011")
    @Title("Проверяем, что остались на том же уровне сетки после скролла и свайпа влево")
    public void shouldSeeNextDayGridAfterScrollAndSwipe() {
        int rowNumber = Utils.getRandomNumber(24, 0);
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .scrollTo(getGridRows(st).get(rowNumber))
                .swipeLeft(getGridRows(st).get(rowNumber));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("1011")
    @Title("Проверяем, что остались на том же уровне сетки после скролла и свайпа влево")
    public void shouldSeeYesterdayGridAfterScrollAndSwipe() {
        int rowNumber = Utils.getRandomNumber(24, 0);
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .scrollTo(getGridRows(st).get(rowNumber))
                .swipeRight(getGridRows(st).get(rowNumber));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    private ElementsCollection<MailElement> getGridRows(InitStepsRule steps) {
        return steps.pages().cal().touchHome().gridRows().waitUntil(not(empty()));
    }
}
