package ru.yandex.autotests.innerpochta.tests.screentests.event;

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

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Выпадушки в попапе создания события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class GeneralPopupTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Test
    @Title("Вводим название события")
    @TestCaseId("381")
    public void shouldOpenCalendar() {
        String eventName = getRandomName();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .shouldSee(st.pages().cal().home().newEventPopup())
            .clicksOn(st.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(st.pages().cal().home().newEventPopup().nameInput(), eventName);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть предупреждение о потере данных")
    @TestCaseId("382")
    public void shouldOpenNewEventPopup() {
        String eventName = getRandomName();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(st.pages().cal().home().newEventPopup().nameInput(), eventName)
            .clicksOn(st.pages().cal().home().newEventPopup().closePopup())
            .shouldSee(st.pages().cal().home().warningPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть выпадушку времени")
    @TestCaseId("400")
    public void shouldOpenTimePicker() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().newEventPopup().timeDateField())
            .clicksOn(st.pages().cal().home().newEventPopup().timeInputList().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть мини-календарь")
    @TestCaseId("401")
    public void shouldOpenDatePicker() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().newEventPopup().dateInputList().get(0))
            .shouldSee(st.pages().cal().home().miniCalendar());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть задизейбленные даты")
    @TestCaseId("402")
    public void shouldSeeAllDayEventPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().newEventPopup().timeDateField())
            .turnTrue(st.pages().cal().home().newEventPopup().allDayCheckBox());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны поменяться время и уведомления")
    @TestCaseId("424")
    public void shouldSeeCorrectTimeAndNotify() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .clicksOn(st.pages().cal().home().newEventPopup().timeDateField())
            .turnTrue(st.pages().cal().home().newEventPopup().allDayCheckBox())
            .clicksOn(st.pages().cal().home().newEventPopup().descriptionAddBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Выбираем дату из мини-календаря")
    @TestCaseId("73")
    public void shouldSeeCorrectDate() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
                .clicksOn(st.pages().cal().home().newEventPopup().dateInputList().get(0))
                .shouldSee(st.pages().cal().home().miniCalendar())
                .clicksOn(st.pages().cal().home().miniCalendar().daysOutMonth().get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку слоев")
    @TestCaseId("410")
    public void shouldSeeLayerDropdown() {
        stepsTest.user().apiCalSettingsSteps().createNewLayer(stepsTest.user().settingsCalSteps().formDefaultLayer());
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .offsetClick(st.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
                .onMouseHoverAndClick(st.pages().cal().home().newEventPopup().layerFieldSelect());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
