package ru.yandex.autotests.innerpochta.tests.autotests.settings;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на настройки")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SETTINGS)
public class CalSettingsTest {

    private Long layerID;
    private String name;
    private String anotherName;

    private static final String TZ_OMSK = "Asia/Omsk";
    private static final String FIRST_DAY_MONDAY = "Первый день недели\nПонедельник";
    private static final String DAY_START = "Начало дня\n08:00";
    private static final String TZ_OMSK_SETTINGS = "Часовой пояс\n(UTC+06:00) Омск";
    private static final String DAY_URL = "/day?show_date=2019-07-27";
    private static final String ANOTHER_DAY_URL = "/day?show_date=2019-07-28";

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
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Выставляем пользователю дефолтные настройки и меняем таймзону",
            new Params().withWeekStartDay(1L)
                .withShowTodosInGrid(true)
                .withShowWeekNumber(true)
                .withShowWeekends(true)
                .withTz(TZ_OMSK)
                .withDayStartHour(8L)
                .withShowAvailabilityToAnyone(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Закрываем настройки без сохранения изменений")
    @TestCaseId("616")
    public void shouldNotSaveSettings() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().settingsButton())
            .clicksOn(
                steps.pages().cal().home().generalSettings().weekStarts(),
                steps.pages().cal().home().weekStartsList().get(1)
            )
            .clicksOn(
                steps.pages().cal().home().generalSettings().dayStarts(),
                steps.pages().cal().home().dayStartsList().get(1)
            )
            .clicksOn(
                steps.pages().cal().home().generalSettings().timezone(),
                steps.pages().cal().home().timezoneList().get(30)
            )
            .deselects(
                steps.pages().cal().home().showTodos(),
                steps.pages().cal().home().showWeekNumber(),
                steps.pages().cal().home().showWeekends()
            )
            .clicksOn(steps.pages().cal().home().generalSettings().closeButton())
            .shouldNotSee(steps.pages().cal().home().generalSettings())
            .clicksOn(steps.pages().cal().home().calHeaderBlock().settingsButton())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().generalSettings().weekStarts(),
                FIRST_DAY_MONDAY
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().generalSettings().dayStarts(),
                DAY_START
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().home().generalSettings().timezone(),
                TZ_OMSK_SETTINGS
            )
            .shouldSeeCheckBoxesInState(
                true,
                steps.pages().cal().home().showTodos(),
                steps.pages().cal().home().showWeekNumber(),
                steps.pages().cal().home().showWeekends()
            );
    }

    @Test
    @Title("Выключаем настройку «Показывать выходные»")
    @TestCaseId("1252")
    public void shouldSeeEventsOnWeekends() {
        name = getRandomName();
        anotherName = getRandomName();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID)
            .withStartTs("2019-07-28T16:00:00").withEndTs("2019-07-28T18:00:00").withName(name);
        Event eventAllDay = steps.user().settingsCalSteps().formDefaultEvent(layerID)
            .withIsAllDay(true).withStartTs("2019-07-27T16:00:00").withEndTs("2019-07-27T18:00:00")
            .withName(anotherName);
        steps.user().apiCalSettingsSteps().createNewEvent(event).createNewEvent(eventAllDay);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().calHeaderBlock().settingsButton())
            .deselects(steps.pages().cal().home().showWeekends())
            .clicksOn(steps.pages().cal().home().generalSettings().enabledSaveButton())
            .opensDefaultUrlWithPostFix(DAY_URL)
            .shouldSeeElementInList(steps.pages().cal().home().allDayEventsAllList(), anotherName);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(ANOTHER_DAY_URL)
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), name);
    }
}
