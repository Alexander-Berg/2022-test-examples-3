package ru.yandex.autotests.innerpochta.tests.screentests.grid;

import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.CoreMatchers;
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
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.MONTH;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.TZ_POPUP;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.WEEK;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на различные сетки")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.GENERAL)
public class GeneralGridTest {

    private static final String URL_404 = "/error/404";
    private static final String TZ_OMSK = "Asia/Omsk";

    private static final Set<Coords> IGNORED_ADD = Sets.newHashSet(
        new Coords(150, 190, 1420, 600), // реклама на общей 404
        new Coords(860, 100, 755, 460) // реклама на общей 404
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Test
    @Title("Верстка страницы 404")
    @TestCaseId("563")
    public void shouldSeeCorrect404() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldBeOnUrl(containsString(URL_404));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(URL_404).withIgnoredAreas(IGNORED_ADD).run();
    }

    @Test
    @Title("Выделение текущей даты")
    @TestCaseId("161")
    public void shouldSelectCurrentDate() {
        stepsTest.user().apiCalSettingsSteps()
            .updateUserSettings("Разворачиваем календари", new Params().withIsCalendarsListExpanded(true));
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().cal().home().leftPanel());

        parallelRun.withActions(actions).withAcc(lock.firstAcc())
            .withUrlPath(MONTH.makeUrlPart("")).withClosePromo().run();
    }

    @Test
    @Title("Должны увидеть попап изменения tz")
    @TestCaseId("613")
    public void shouldSeeChangeTzPopup() {
        stepsTest.user().apiCalSettingsSteps()
            .updateUserSettings("Меняем таймзону на Омск", new Params().withTz(TZ_OMSK));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().cal().home().warningPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(TZ_POPUP.fragment()).run();
    }

    @Test
    @Title("Недельная сетка не дёргается при открытии окна создания первого события")
    @TestCaseId("908")
    public void shouldNotSeeGridJumpForFirstEventPopup() {
        stepsProd.user().apiCalSettingsSteps().updateUserSettings(
            "Меняем время начала дня на 00:00",
            new Params().withDayStartHour(0L).withIsAsideExpanded(true)
        );
        Consumer<InitStepsRule> actions = st ->
            checkDayStartHourAndOpenNewEventPopup(st);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withIgnoredAreas(IGNORED_ADD).run();
    }

    @Test
    @Title("Недельная сетка не дёргается при создании первого события")
    @TestCaseId("908")
    public void shouldNotSeeGridJumpForFirstEvent() {
        String eventName = getRandomString();
        stepsProd.user().apiCalSettingsSteps().updateUserSettings(
            "Меняем время начала дня на 00:00",
            new Params().withDayStartHour(0L).withIsAsideExpanded(true)
        );
        Consumer<InitStepsRule> actions = st -> {
            checkDayStartHourAndOpenNewEventPopup(st);
            st.user().defaultSteps().inputsTextInElement(st.pages().cal().home().newEventPopup().nameInput(), eventName)
                .clicksOn(st.pages().cal().home().newEventPopup().createFromPopupBtn())
                .shouldSeeThatElementHasText(st.pages().cal().home().columnsList().get(0), eventName);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withIgnoredAreas(IGNORED_ADD).run();
    }

    @Step("Меняем время начала дня и открываем попап создания события")
    private void checkDayStartHourAndOpenNewEventPopup(InitStepsRule st) {
        st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().leftPanel().view())
            .clicksOn(st.pages().cal().home().selectView().get(1))
            .shouldBeOnUrl(CoreMatchers.containsString(WEEK.makeUrlPart("")))
            .clicksOn(st.pages().cal().home().calHeaderBlock().settingsButton())
            .shouldSeeThatElementHasText(st.pages().cal().home().generalSettings().dayStarts(), "00:00")
            .clicksOn(st.pages().cal().home().generalSettings().closeButton())
            .scrollTo(st.pages().cal().home().hourCell().get(st.pages().cal().home().hourCell().size() - 1))
            .clicksOn(st.pages().cal().home().columnsList().get(0))
            .shouldSee(st.pages().cal().home().newEventPopup());
    }
}
