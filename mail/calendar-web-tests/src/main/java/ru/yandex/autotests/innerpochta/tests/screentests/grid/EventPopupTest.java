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
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DAY_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.MONTH_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попап просмотра встречи")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class EventPopupTest {

    private static final String BAD_EVENT_ID = "/event/299782724";
    private static final String ALIEN = "/event/1496524640";
    private static final String EXPAND_LINK_TEXT = "Ещё 5 участников";
    private static final String HIDE_LINK_TEXT = "Скрыть";
    private List<String> MANY_ATTENDEES = Arrays.asList(
        "yandex-team-mailt-190@yandex.ru",
        "yandex-team-mailt-191@yandex.ru",
        "yandex-team-mailt-192@yandex.ru",
        "yandex-team-mailt-193@yandex.ru",
        "yandex-team-mailt-194@yandex.ru",
        "yandex-team-mailt-195@yandex.ru",
        "yandex-team-mailt-196@yandex.ru",
        "yandex-team-mailt-198@yandex.ru",
        "yandex-team-mailt-199@yandex.ru",
        "yandex-team-mailt-200@yandex.ru",
        "yandex-team-mailt-201@yandex.ru",
        "yandex-team-mailt-202@yandex.ru",
        "yandex-team-mailt-203@yandex.ru",
        "yandex-team-mailt-204@yandex.ru"
    );
    private Long layerID;
    private String eventName;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo().withScrollGrid();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(auth2)
        .around(clearAcc(() -> stepsProd.user()));

    @Before
    public void prepare() {
        stepsTest.user().apiCalSettingsSteps().withAuth(auth2).deleteAllAndCreateNewLayer();
        layerID = stepsTest.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = stepsTest.user().settingsCalSteps()
            .formDefaultEvent(layerID)
            .withLocation("набережная реки Фонтанки, Санкт-Петербург");
        stepsTest.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
    }

    @Test
    @Title("Открываем попап просмотра своей встречи")
    @TestCaseId("92")
    public void shouldSeeShowEventPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().eventsAllList().get(0))
            .shouldSee(st.pages().cal().home().viewEventPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Просмотр закрытого события")
    @TestCaseId("120")
    public void shouldSeeCorrect404() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldNotSee(st.pages().cal().home().currentColumn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(BAD_EVENT_ID).run();
    }

    @Test
    @Title("Открываем попап просмотра приглашения")
    @Description("Подготовлено событие-приглашение")
    @TestCaseId("16")
    public void shouldSeeShowInvitationEventPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().eventsAllList().get(0))
            .shouldSee(st.pages().cal().home().viewEventPopup().solutionsBtns());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).withUrlPath(WEEK_GRID).run();
    }

    @Test
    @Title("Открываем редактирование приглашения")
    @Description("Подготовлено событие-приглашение")
    @TestCaseId("62")
    public void shouldSeeShowInvitationEventPage() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(
                st.pages().cal().home().eventsAllList().get(0),
                st.pages().cal().home().viewEventPopup().editEventBtn()
            )
            .shouldBeOnUrl(containsString(EVENT.fragment("")));

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).withUrlPath(WEEK_GRID).run();
    }

    @Test
    @Title("Открываем чужое событие")
    @Description("Добавляем чужое событие")
    @TestCaseId("255")
    public void shouldSeeOtherEventPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().viewSomeoneElseEventPage().addBtn())
            .clicksOn(st.pages().cal().home().eventsAllList().get(0))
            .shouldSee(st.pages().cal().home().viewEventPopup().removeEventBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(ALIEN).run();
    }

    @Test
    @Title("Клик по кнопке «Еще n участников»/«Скрыть» в поп-апе созданного события")
    @Description("Добавлено событие с 15 участниками")
    @TestCaseId("734")
    @DataProvider({WEEK_GRID, MONTH_GRID})
    public void shouldHideOfParticipants(String url) {
        eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        ArrayList<String> totalAttendees = new ArrayList<>(MANY_ATTENDEES);
        totalAttendees.add(lock.accNum(1).getSelfEmail());
        stepsProd.user().apiCalSettingsSteps().createNewEventWithAttendees(event, totalAttendees);
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
                .clicksOn(st.pages().cal().home().viewEventPopup().membersListToggler())
                .shouldContainText(st.pages().cal().home().viewEventPopup().membersListToggler(), HIDE_LINK_TEXT)
                .clicksOn(st.pages().cal().home().viewEventPopup().membersListToggler())
                .shouldContainText(st.pages().cal().home().viewEventPopup().membersListToggler(), EXPAND_LINK_TEXT);

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).withUrlPath(url).run();
    }

    @Test
    @Title("Контролл «Еще n участников» для событий с разным количеством участников")
    @Description("Добавлены события с 14 участниками")
    @TestCaseId("551")
    public void shouldSeeParticipants() {
        eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        ArrayList<String> totalAttendees = new ArrayList<>(MANY_ATTENDEES);
        totalAttendees.add(lock.accNum(1).getSelfEmail());
        stepsProd.user().apiCalSettingsSteps().createNewEventWithAttendees(event, totalAttendees);
        stepsProd.user().apiCalSettingsSteps().withAuth(auth2)
            .createNewLayer(stepsProd.user().settingsCalSteps().formDefaultLayer());
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
                .scrollTo(st.pages().cal().home().viewEventPopup().layerField());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).withUrlPath(WEEK_GRID).run();
    }

    @Test
    @Title("Контролл «Еще n участников» для событий с разным количеством участников")
    @Description("Добавлены события с 16 участниками")
    @TestCaseId("551")
    public void shouldSeeMore15Participants() {
        eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        ArrayList<String> totalAttendees = new ArrayList<>(MANY_ATTENDEES);
        totalAttendees.add(lock.accNum(1).getSelfEmail());
        totalAttendees.add(DEV_NULL_EMAIL);
        stepsProd.user().apiCalSettingsSteps().createNewEventWithAttendees(event, totalAttendees);
        stepsProd.user().apiCalSettingsSteps().withAuth(auth2)
            .createNewLayer(stepsProd.user().settingsCalSteps().formDefaultLayer());
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
                .scrollTo(st.pages().cal().home().viewEventPopup().layerField());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).withUrlPath(WEEK_GRID).run();
    }

    @Test
    @Title("Попап просмотра события на весь день в дневной сетке")
    @TestCaseId("146")
    public void shouldSeeAllDayEventPopupInDayGrid() {
        String eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName)
            .withIsAllDay(true);
        stepsProd.user().apiCalSettingsSteps().createNewRepeatEvent(event);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
            .shouldSee(st.pages().cal().home().viewEventPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(DAY_GRID).run();
    }

    @Test
    @Title("Попап просмотра события на весь день в месячной сетке")
    @TestCaseId("146")
    public void shouldSeeAllDayEventPopupInMonthGrid() {
        String eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName)
            .withIsAllDay(true);
        Event event2 = stepsProd.user().settingsCalSteps().formDefaultEvent(layerID).withIsAllDay(true);
        stepsProd.user().apiCalSettingsSteps().createNewRepeatEvent(event)
            .createNewEvent(event2);
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
            .shouldSee(st.pages().cal().home().viewEventPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(WEEK_GRID).run();
    }

    @Test
    @Title("Модальное окно при отказе от повторяющейся встречи: кнопка «Отменить»")
    @Description("Добавлено повторяющееся событие")
    @TestCaseId("791")
    public void shouldSeeCancelDialog() {
        String eventName = getRandomString();
        Event event = stepsProd.user().settingsCalSteps().formDefaultRepeatingEvent(layerID).withName(eventName);
        stepsProd.user().apiCalSettingsSteps()
            .createNewRepeatEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
            .clicksOn(st.pages().cal().home().viewEventPopup().buttonNo())
            .shouldSee(st.pages().cal().home().eventDecisionPopup())
            .clicksOn(st.pages().cal().home().eventDecisionPopup().cancelPopupBtn());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }
}
