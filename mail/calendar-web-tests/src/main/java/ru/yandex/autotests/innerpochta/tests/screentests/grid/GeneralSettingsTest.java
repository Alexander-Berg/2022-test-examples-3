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
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
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
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на настройки")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SETTINGS)
@RunWith(DataProviderRunner.class)
public class GeneralSettingsTest {

    private static final String TZ_OMSK = "Asia/Omsk";
    private static final String FIRST_DAY = "Первый день недели\nСуббота";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo().withScrollGrid();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Before
    public void setUp() {
        Long layerID = stepsProd.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = stepsProd.user().settingsCalSteps().formDefaultEvent(layerID);
        stepsProd.user().apiCalSettingsSteps().createNewEvent(event)
            .updateUserSettings(
                "Выставляем пользователю дефолтные настройки и меняем таймзону",
                new Params().withWeekStartDay(1L)
                    .withShowTodosInGrid(true)
                    .withShowWeekNumber(true)
                    .withShowWeekends(true)
                    .withTz(TZ_OMSK)
                    .withDayStartHour(8L)
                    .withShowAvailabilityToAnyone(true)
            );
    }

    @Test
    @Title("Смена первого дня недели в сетке на неделю/месяц")
    @TestCaseId("236")
    @DataProvider({WEEK_GRID, MONTH_GRID})
    public void shouldChangeFirstDay(String data) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().cal().home().calHeaderBlock().settingsButton())
                .shouldSee(st.pages().cal().home().generalSettings().disabledSaveButton())
                .clicksOn(
                    st.pages().cal().home().generalSettings().weekStarts(),
                    st.pages().cal().home().weekStartsList().get(1)
                )
                .shouldSeeThatElementTextEquals(
                    st.pages().cal().home().generalSettings().weekStarts(),
                    FIRST_DAY
                )
                .shouldNotSee(st.pages().cal().home().weekStartsList().get(1))
                .clicksOn(st.pages().cal().home().generalSettings().enabledSaveButton())
                .shouldSee(st.pages().cal().home().successNotify())
                .shouldNotSee(st.pages().cal().home().generalSettings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(data).run();
    }

    @Test
    @Title("Не показываем выходные в сетке на неделю/месяц с выключенной настройкой «Показывать выходные»")
    @TestCaseId("970")
    @DataProvider({WEEK_GRID, MONTH_GRID})
    public void shouldNotSeeWeekends(String data) {
        Consumer<InitStepsRule> actions = st -> {
            String url = st.getDriver().getCurrentUrl();
            st.user().defaultSteps().clicksOn(st.pages().cal().home().calHeaderBlock().settingsButton())
                .deselects(st.pages().cal().home().showWeekends())
                .clicksOn(st.pages().cal().home().generalSettings().enabledSaveButton())
                .shouldNotSee(st.pages().cal().home().generalSettings())
                .shouldBeOnUrl(url);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(data).run();

    }

    @Test
    @Title("Показываем выходные в сетке на неделю/месяц с включенной настройкой «Показывать выходные»")
    @TestCaseId("970")
    @DataProvider({WEEK_GRID, MONTH_GRID})
    public void shouldSeeWeekends(String data) {
        stepsProd.user().apiCalSettingsSteps().updateUserSettings(
            "Выключаем настройку «Показывать выходные»",
            new Params().withShowWeekends(false)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().cal().home().calHeaderBlock().settingsButton())
                .turnTrue(st.pages().cal().home().showWeekends())
                .clicksOn(st.pages().cal().home().generalSettings().enabledSaveButton())
                .shouldNotSee(st.pages().cal().home().generalSettings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(data).run();

    }

    @Test
    @Title("Не показываем выходные в сетке на неделю/месяц, если первый день недели - выходной")
    @TestCaseId("987")
    @DataProvider({WEEK_GRID, MONTH_GRID})
    public void shouldNotSeeWeekendsWithFirstDaySaturday(String data) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().cal().home().calHeaderBlock().settingsButton())
                .clicksOn(
                    st.pages().cal().home().generalSettings().weekStarts(),
                    st.pages().cal().home().weekStartsList().get(1)
                )
                .shouldSeeThatElementTextEquals(
                    st.pages().cal().home().generalSettings().weekStarts(),
                    FIRST_DAY
                )
                .deselects(st.pages().cal().home().showWeekends())
                .clicksOn(st.pages().cal().home().generalSettings().enabledSaveButton())
                .shouldNotSee(st.pages().cal().home().generalSettings());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(data).run();
    }

}
