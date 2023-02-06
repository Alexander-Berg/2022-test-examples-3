package ru.yandex.autotests.innerpochta.tests.screentests.grid;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Отображение иконок в сетке")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class IconsTest {

    private static final String DAY_URL = "/day?show_date=%s";
    private static final String WEEK_URL = "/week?show_date=%s";
    private static final String MONTH_URL = "/month?show_date=%s";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo().withScrollGrid();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Before()
    public void setUp() {
        stepsProd.user().apiCalSettingsSteps()
            .updateUserSettings("Разворачиваем блок allday", new Params().withIsAlldayExpanded(true));
    }

    @Test
    @Title("Открываем неделю с разными событиями")
    @TestCaseId("430")
    @DataProvider({"2017-01-31", "2016-10-31", "2016-11-14", "2016-12-05"})
    public void shouldSeeCorrectEventsInWeek(String week) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .waitInSeconds(5);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(String.format(WEEK_URL, week)).run();
    }

    @Test
    @Title("Открываем месяц с разными событиями")
    @TestCaseId("450")
    @DataProvider({"2016-12-05", "2016-10-31"})
    public void shouldSeeCorrectEventsInMonth(String month) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .waitInSeconds(5);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(String.format(MONTH_URL, month)).run();
    }

    @Test
    @Title("Открываем день с разными событиями")
    @TestCaseId("431")
    @DataProvider({"2017-01-23", "2017-01-24", "2017-01-25", "2017-01-26",})
    public void shouldSeeCorrectEventsInDay(String day) {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .waitInSeconds(5);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(String.format(DAY_URL, day)).run();
    }

    @Test
    @Title("События на весь день свернуты")
    @TestCaseId("451")
    @DataProvider({"/week?show_date=2016-10-31", "/day?show_date=2016-10-31"})
    public void shouldSeeCorrectAllDayEvents(String urlPath) {
        stepsProd.user().apiCalSettingsSteps()
            .updateUserSettings("Cворачиваем блок allday", new Params().withIsAlldayExpanded(false));
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .waitInSeconds(5);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(urlPath).run();
    }

}
