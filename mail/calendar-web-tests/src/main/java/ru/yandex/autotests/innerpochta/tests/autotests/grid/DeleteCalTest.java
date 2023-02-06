package ru.yandex.autotests.innerpochta.tests.autotests.grid;

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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на удаление календаря")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CREATE_CAL)

public class DeleteCalTest {

    private String name;
    private String eventName;
    private Long layerID;

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
        eventName = getRandomName();
        steps.user().apiCalSettingsSteps().deleteLayers()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer().withName(name));
        layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName);
        steps.user().apiCalSettingsSteps().createNewEvent(event)
            .updateUserSettings(
                "Включаем недельный вид, разворачиваем календари",
                new Params().withDefaultView("week").withIsCalendarsListExpanded(true)
            );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаление календаря с перемещением")
    @TestCaseId("317")
    public void shouldDeleteCalWithEventsMove() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().defaultSteps().refreshPage()
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .clicksOn(steps.pages().cal().home().deleteCal())
            .shouldSee(steps.pages().cal().home().deleteCalPopup())
            .shouldBeSelected(steps.pages().cal().home().deleteCalCheckbox())
            .clicksOn(steps.pages().cal().home().deleteCalButton())
            .shouldNotSee(steps.pages().cal().home().deleteCalPopup())
            .shouldNotSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), name)
            .shouldSeeElementsCount(steps.pages().cal().home().leftPanel().layersList(), 2)
            .shouldSeeElementInList(steps.pages().cal().home().eventsAllList(), eventName);
        steps.user().defaultSteps()
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .shouldSee(steps.pages().cal().home().disabledSetDefaultCalCheckbox());
    }

    @Test
    @Title("Удаление единственного календаря")
    @TestCaseId("904")
    public void shouldDeleteLastLayer() {
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .clicksOn(
                steps.pages().cal().home().deleteCal(),
                steps.pages().cal().home().deleteLayerPopupInput()
            )
            .inputsTextInElement(
                steps.pages().cal().home().deleteLayerPopupInput(),
                steps.user().apiCalSettingsSteps().getUserLayers().get(0).getName()
            )
            .clicksOn(steps.pages().cal().home().deleteCalButton())
            .shouldNotSeeElementInList(steps.pages().cal().home().leftPanel().layersList().waitUntil(empty()), name);
    }

    @Test
    @Title("Кнопка «Удалить» в попапе удаления единственного календаря должна быть задизейблена ")
    @TestCaseId("904")
    public void shouldSeeDisabledDeleteButton() {
        steps.user().defaultSteps().onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .clicksOn(
                steps.pages().cal().home().deleteCal(),
                steps.pages().cal().home().deleteLayerPopupInput()
            )
            .inputsTextInElement(
                steps.pages().cal().home().deleteLayerPopupInput(),
                getRandomName()
            )
            .shouldBeDisabled(steps.pages().cal().home().deleteCalButton());
    }
}

