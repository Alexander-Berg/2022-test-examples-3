package ru.yandex.autotests.innerpochta.tests.autotests.grid;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на создание календаря/импорт")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CREATE_CAL)
public class CreateCalTest {

    private static final String SIDEBAR_URL = "?sidebar=addLayer&sidebarTab=new";
    private static final String IMPORT_URL = "https://calendar.google.com/calendar/ical/" +
        "fp2134he8d85cnfduph669o3cg%40group.calendar.google.com/public/basic.ics";
    private static final String ADD_CAL_URL = "/api/ui-start-feeding.xml?ics=https%3A%2F%2Fcalendar" +
        ".yandex.ru%2Fexport%2Fics.xml%3Fprivate_token%3D8eb679f067a909d19cc7424891c7de3c8747a09a%26tz_id%3DEurope" +
        "%2FMoscow&layer_name=Мои%20события";
    private static final String LAYER_NAME = "Мои события";
    private String name;

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
        name = getRandomName();
        steps.user().apiCalSettingsSteps()
            .updateUserSettings("Разворачиваем подписки", new Params().withIsSubscriptionsListExpanded(true));
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(SIDEBAR_URL)
            .shouldSee(steps.pages().cal().home().addCalSideBar());
    }

    @Test
    @Title("Создаём новый календарь")
    @TestCaseId("297")
    public void shouldCreateNewCal() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().addCalSideBar().nameInput())
            .inputsTextInElement(steps.pages().cal().home().addCalSideBar().nameInput(), name)
            .clicksOn(
                steps.pages().cal().home().addCalSideBar().colors().get(5),
                steps.pages().cal().home().addCalSideBar().addNotifyBtn()
            )
            .turnTrue(steps.pages().cal().home().addCalSideBar().setDefaultCalCheckbox())
            .clicksOn(steps.pages().cal().home().addCalSideBar().createBtn())
            .shouldNotSee(steps.pages().cal().home().addCalSideBar())
            .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), name);
        steps.user().defaultSteps().offsetClick(steps.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .shouldContainText(steps.pages().cal().home().newEventPopup().layerField(), name);
    }

    @Test
    @Title("Импорт в существующий календарь без событий")
    @TestCaseId("452")
    public void shouldImportEventsInOldCal() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().importLink())
            .clicksOn(steps.pages().cal().home().addCalSideBar().fromLinkBtn())
            .clicksOn(steps.pages().cal().home().addCalSideBar().urlInput())
            .inputsTextInElement(steps.pages().cal().home().addCalSideBar().urlInput(), IMPORT_URL)
            .clicksOn(steps.pages().cal().home().addCalSideBar().importBtn())
            .shouldNotSee(steps.pages().cal().home().addCalSideBar())
            .shouldSeeElementsCount(steps.pages().cal().home().leftPanel().layersList(), 1);
    }

    @Test
    @Title("Импорт в новый календарь")
    @TestCaseId("454")
    public void shouldImportEventsInNewCal() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().importLink())
            .clicksOn(steps.pages().cal().home().addCalSideBar().fromLinkBtn())
            .clicksOn(steps.pages().cal().home().addCalSideBar().urlInput())
            .inputsTextInElement(steps.pages().cal().home().addCalSideBar().urlInput(), IMPORT_URL)
            .clicksOn(steps.pages().cal().home().addCalSideBar().layerSelect())
            .clicksOn(steps.pages().cal().home().newCalImport())
            .clicksOn(steps.pages().cal().home().addCalSideBar().nameInput())
            .inputsTextInElement(steps.pages().cal().home().addCalSideBar().nameInput(), name)
            .clicksOn(steps.pages().cal().home().addCalSideBar().colors().get(3))
            .clicksOn(steps.pages().cal().home().addCalSideBar().importBtn())
            .shouldNotSee(steps.pages().cal().home().addCalSideBar())
            .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), name);
    }

    @Test
    @Title("Отписаться от календаря")
    @TestCaseId("319")
    public void shouldUnsubscribe() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(ADD_CAL_URL)
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldSeeElementInList(
                steps.pages().cal().home().leftPanel().subscriptionLayersNames().waitUntil(not(empty())),
                LAYER_NAME
            );
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().leftPanel().subsSettings())
            .clicksOn(
                steps.pages().cal().home().editCalSideBar().unsubscribe(),
                steps.pages().cal().home().warPopups().get(1).agreeBtn()
            )
            .shouldNotSeeElementInList(
                steps.pages().cal().home().leftPanel().subscriptionLayersNames().waitUntil(empty()),
                LAYER_NAME
            );
    }

    @Test
    @Title("Отмена в попапе отписки от календаря")
    @TestCaseId("319")
    public void shouldNotUnsubscribe() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(ADD_CAL_URL)
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldSeeElementInList(
                steps.pages().cal().home().leftPanel().subscriptionLayersNames().waitUntil(not(empty())),
                LAYER_NAME
            );
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().leftPanel().subsSettings())
            .clicksOn(
                steps.pages().cal().home().editCalSideBar().unsubscribe(),
                steps.pages().cal().home().warPopups().get(1).cancelBtn()
            )
            .shouldSeeElementInList(
                steps.pages().cal().home().leftPanel().subscriptionLayersNames(),
                LAYER_NAME
            );
    }
}

