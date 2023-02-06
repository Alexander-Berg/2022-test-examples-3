package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ALL_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EDIT_ALL_LINK;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EDIT_ONE_LINK;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ONE_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Редактирование повторяющегося события на весь день")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.REPEATING_EVENT)
public class RepeatAllDayEditEventTest {

    private Long layerID;
    private String eventName;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем настройку «Показывать выходные»",
            new Params().withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MONTH_GRID);
        createRepeatAllDayEvent();
    }

    @Test
    @Title("Редактирование серии событий на весь день")
    @TestCaseId("160")
    public void shouldEditAllDayRepeatEventSeries() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editAllEvents())
            .shouldContainText(
                steps.pages().cal().home().newEventPage().editSeriesMsg(),
                String.format(ALL_EVENT_WARNING, " ", EDIT_ONE_LINK)
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), getRandomString())
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldSee(steps.pages().cal().home().viewEventPopup().repeatSymbol());
    }

    @Test
    @Title("Редактирование одного события в серии на весь день")
    @TestCaseId("160")
    public void shouldEditOneAllDayRepeatEvent() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
            .clicksOn(steps.pages().cal().home().editOneEvent())
            .shouldContainText(
                steps.pages().cal().home().newEventPage().editSeriesMsg(),
                String.format(ONE_EVENT_WARNING, " ", EDIT_ALL_LINK)
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), getRandomString())
            .clicksOn(steps.pages().cal().home().newEventPage().saveChangesBtn())
            .clicksOn(steps.pages().cal().home().allDayEvents().get(0))
            .shouldNotSee(steps.pages().cal().home().viewEventPopup().repeatSymbol());
    }

    @Step("Создаем повторяющиеся событие на весь день")
    private void createRepeatAllDayEvent() {
        eventName = getRandomName();
        Event event = steps.user().settingsCalSteps().formDefaultRepeatingAllDayEvent(layerID)
            .withName(eventName);
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(event);
    }
}
