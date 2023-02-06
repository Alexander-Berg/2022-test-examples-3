package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.rules.DeleteAllLayersRule.deleteAllLayers;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Корп] Расписание")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CORP)
public class CorpScheduleTest {

    private static final String MEMBER_SCHEDULE_URL = "/schedule/robot-mailcorp-5@yandex-team.ru";
    private static final String ROOM_PAGE = "/invite";
    private static final String MORNING_TIME = "08:00";
    private static final String BENUA = "Бенуа";
    private static final String ROOM_NAME = "Заячий";

    private Long layerID;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();


    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(deleteAllLayers(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().defaultSteps().clicksIfCanOn(steps.pages().cal().home().closeWidget());
    }

    @Test
    @Title("Просмотр чужого события из расписания")
    @TestCaseId("1360")
    public void shouldSeeScheduleMemberViewEvent() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MEMBER_SCHEDULE_URL)
            .shouldSee(steps.pages().cal().home().schedulePage().scheduleHeader())
            .clicksOn(steps.pages().cal().home().schedulePage().eventsSchedule().get(1))
            .shouldSee(
                steps.pages().cal().home().schedulePage().eventPreview().name(),
                steps.pages().cal().home().schedulePage().eventPreview().timeAndDate()
            );
    }

    @Test
    @Title("Открытие расписания из карточки переговорки на странице переговорок")
    @TestCaseId("1403")
    public void shouldOpenScheduleFromRoomCardOnInvitePage() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(ROOM_PAGE)
            .onMouseHover(steps.pages().cal().home().meetingsPage().roomName().get(3))
            .shouldSee(steps.pages().cal().home().roomCard())
            .clicksOn(steps.pages().cal().home().roomScheduleLink())
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().cal().home().schedulePage().scheduleHeader());
    }

    @Test
    @Title("Открытие расписания из карточки переговорки в попапе события")
    @TestCaseId("1403")
    public void shouldOpenScheduleFromRoomCardInEvent() {
        String name = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .clicksOn(steps.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name);
        steps.user().calCreateEventSteps().setStartTime(MORNING_TIME);
            //TODO: Проблема из-за бага с выпадушками, им нужен подскрол. Ждём починки тут https://st.yandex-team.ru/MAYA-2786
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPage().roomsList().get(0).officeResource())
            .clicksOnElementWithText(steps.pages().cal().home().officesList(), BENUA)
            .inputsTextInElement(steps.pages().cal().home().newEventPage().roomsList().get(0).roomInput(), ROOM_NAME)
            .clicksOn(
                steps.pages().cal().home().suggestItem().waitUntil(Matchers.not(empty())).get(0),
                steps.pages().cal().home().newEventPage().createFromPageBtn()
            )
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldContainText(
                steps.pages().cal().home().eventsAllList().waitUntil(Matchers.not(empty())).get(0).eventName(),
                name
            )
            .clicksOn(steps.pages().cal().home().eventsAllList().waitUntil(Matchers.not(empty())).get(0))
            .onMouseHover(steps.pages().cal().home().viewEventPopup().roomYabble())
            .shouldSee(steps.pages().cal().home().roomCard())
            .clicksOn(steps.pages().cal().home().roomScheduleLink())
            .switchOnJustOpenedWindow()
            .shouldSee(steps.pages().cal().home().schedulePage().scheduleHeader());
    }
}
