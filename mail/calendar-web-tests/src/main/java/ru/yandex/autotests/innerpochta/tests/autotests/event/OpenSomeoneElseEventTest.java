package ru.yandex.autotests.innerpochta.tests.autotests.event;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
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
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EXCEPTION_WARNING;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Открытие/добавление чужих событий")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.SOMEONE_ELSE_EVENT)
@RunWith(DataProviderRunner.class)
public class OpenSomeoneElseEventTest {

    private static final String SOMEONE_ELSE_SIMPLE_EVENT_LINK = "/event/1445497052";
    private static final String SOMEONE_ELSE_SIMPLE_EVENT_NAME = "someone else event 1";
    private static final String SOMEONE_ELSE_SERIES_EVENT_LINK = "/event/1446078490?applyToFuture=1";
    private static final String SOMEONE_ELSE_SERIES_EVENT_NAME = "someone else series";
    private static final String SOMEONE_ELSE_ONE_FROM_SERIES_EVENT_LINK = "/event/1446078490" +
            "?applyToFuture=0&event_date=2020-06-08";

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
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаление простого чужого события")
    @TestCaseId("124")
    public void shouldDeleteSomeoneElseEvent() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SOMEONE_ELSE_SIMPLE_EVENT_LINK)
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().nameField(),
                        SOMEONE_ELSE_SIMPLE_EVENT_NAME
                )
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().addBtn())
                .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().deleteBtn())
                .shouldNotSee(steps.pages().cal().home().eventsAllList());
    }

    @Test
    @Title("Удаление одного события чужой серии")
    @TestCaseId("827")
    public void shouldDeleteOneEventInSomeoneElseSeries() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SOMEONE_ELSE_ONE_FROM_SERIES_EVENT_LINK)
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().nameField(),
                        SOMEONE_ELSE_SERIES_EVENT_NAME
                )
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().timeAndDateField(),
                        "8 июня"
                )
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().addBtn())
                .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
                .shouldContainText(
                        steps.pages().cal().home().newEventPage().editSeriesMsg(),
                        String.format(EXCEPTION_WARNING, " ", EDIT_ALL_LINK)
                )
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().deleteBtn())
                .shouldNotSee(steps.pages().cal().home().eventsAllList());
    }

    @Test
    @Title("Удаление чужой серии")
    @TestCaseId("828")
    public void shouldDeleteSomeoneElseSeries() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SOMEONE_ELSE_SERIES_EVENT_LINK)
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().nameField(),
                        SOMEONE_ELSE_SERIES_EVENT_NAME
                )
                .shouldSee(steps.pages().cal().home().viewSomeoneElseEventPage().repeatSymbol())
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().addBtn())
                .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn())
                .shouldContainText(
                        steps.pages().cal().home().newEventPage().editSeriesMsg(),
                        String.format(ALL_EVENT_WARNING, " ", EDIT_ONE_LINK)
                )
                .clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().deleteBtn())
                .shouldNotSee(steps.pages().cal().home().eventsAllList());
    }

    @Test
    @Title("Установка уведомлений в зависимостри от настроек основного поля")
    @TestCaseId("1150")
    public void shouldSetNotificationsSomeoneElseEvent() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().expandCalendars())
                .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
                .clicksOn(steps.pages().cal().home().editCalSideBar().addNotifyBtn())
                .clicksOn(steps.pages().cal().home().editCalSideBar().notifyList().get(0).offsetNotifyInput())
                .clicksOn(steps.pages().cal().home().valueUntilList().get(3))
                .clicksOn(steps.pages().cal().home().editCalSideBar().addNotifyBtn())
                .clicksOn(steps.pages().cal().home().editCalSideBar().notifyList().get(1).unitNotifySelect())
                .clicksOn(steps.pages().cal().home().typeUntilList().get(0))
                .clicksOn(steps.pages().cal().home().editCalSideBar().saveBtn())
                .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
                .opensDefaultUrlWithPostFix(SOMEONE_ELSE_SIMPLE_EVENT_LINK);
        checkNotificationsParameters();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().addBtn())
                .clicksOn(steps.pages().cal().home().eventsAllList().get(0))
                .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn());
        checkNotificationsParameters();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().viewSomeoneElseEventPage().deleteBtn())
                .shouldNotSee(steps.pages().cal().home().eventsAllList());
    }

    @Step("Проверка параметров уведомлений")
    public void checkNotificationsParameters(){
        steps.user().defaultSteps().shouldContainText(
                steps.pages().cal().home().viewSomeoneElseEventPage().nameField(),
                SOMEONE_ELSE_SIMPLE_EVENT_NAME
        )
                .shouldContainValue(
                        steps.pages().cal().home().viewSomeoneElseEventPage().notifyList().get(0).offsetNotifyInput(),
                        "45"
                )
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().notifyList().get(0).unitNotifyText(),
                        "минут"
                )
                .shouldContainText(
                        steps.pages().cal().home().viewSomeoneElseEventPage().notifyList().get(1).unitNotifyText(),
                        "в момент события"
                );
    }
}
