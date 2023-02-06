package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ALL_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.ONE_EVENT_WARNING;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Редактирование cерии")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.EDIT_EVENT)
public class TouchEditRepeatableEventTest {

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().apiCalSettingsSteps()
            .createNewRepeatEvent(steps.user().settingsCalSteps().formDefaultRepeatingEvent(layerID));
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Редактирование повторяющегося события - все события серии")
    @TestCaseId("1183")
    public void shouldEditAllRepeatableEvents() {
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventsPeriodicityPopup().refuseOrAllEventsBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().repetitionWarning())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ALL_EVENT_WARNING);
        shouldEditAllEventsInSeries(newTitle, newDescription);
    }

    @Test
    @Title("Редактирование повторяющегося события -  только это событие")
    @TestCaseId("1182")
    public void shouldEditOneOfRepeatableEvents() {
        String oldTitle = steps.user().apiCalSettingsSteps().getTodayEvents().get(0).getName();
        String oldDescription = steps.user().apiCalSettingsSteps().getTodayEvents().get(0).getDescription();
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventsPeriodicityPopup().confirmOrOneEventBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().repetitionWarning())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ONE_EVENT_WARNING);
        shouldEditOnlyOneOfSeries(newTitle, newDescription, oldTitle, oldDescription);
    }

    @Test
    @Title("Остаемся в просмотре события, если закрываем попап «Что именно вы хотите редактировать?»")
    @TestCaseId("1182")
    public void shouldStayAtEventPageIfDidntChooseOneOrSeriesToEdit() {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventsPeriodicityPopup().close()
            )
            .shouldNotSee(
                steps.pages().cal().touchHome().eventsPeriodicityPopup(),
                steps.pages().cal().touchHome().eventPage().repetitionWarning()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().edit());
    }

    @Test
    @Title("Переходим от редактирования всей серии событий к одному")
    @TestCaseId("1184")
    public void shouldGoToSingleEventEdit() {
        String oldTitle = steps.user().apiCalSettingsSteps().getTodayEvents().get(0).getName();
        String oldDescription = steps.user().apiCalSettingsSteps().getTodayEvents().get(0).getDescription();
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventsPeriodicityPopup().refuseOrAllEventsBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().repetitionWarning())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ALL_EVENT_WARNING)
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeEditTypeLink())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ONE_EVENT_WARNING);
        shouldEditOnlyOneOfSeries(newTitle, newDescription, oldTitle, oldDescription);
    }

    @Test
    @Title("Переходим от редактирования одного события серии к редактированию всей серии")
    @TestCaseId("1163")
    public void shouldEditGoToAllRepeatableEvents() {
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventsPeriodicityPopup().confirmOrOneEventBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().repetitionWarning())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ONE_EVENT_WARNING)
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeEditTypeLink())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().repetitionWarning(), ALL_EVENT_WARNING);
        shouldEditAllEventsInSeries(newTitle, newDescription);
    }

    @Step("Редактируем событие, проверяем, что изменения применились для сегодняшнего дня")
    private List<Event>[] editEventAndCheckIfChangedForToday(String newTitle, String newDescription) {
        steps.user().defaultSteps()
            .inputsTextInElement(steps.pages().cal().touchHome().eventPage().editableTitle(), newTitle)
            .inputsTextInElement(steps.pages().cal().touchHome().eventPage().editableDescription(), newDescription)
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .waitInSeconds(3); //Ждём, пока событие изменится.
        List<Event>[] events = steps.user().apiCalSettingsSteps().getEventsForTwoDays();
        assertEquals("Сегодняшнее событие не отредактировалось", newTitle, events[0].get(0).getName());
        assertEquals("Сегодняшнее событие не отредактировалось", newDescription, events[0].get(0).getDescription());
        return events;
    }

    @Step("Редактируем событие и проверяем, что все события серии отредактировались")
    private void shouldEditAllEventsInSeries(String newTitle, String newDescription) {
        List<Event>[] events = editEventAndCheckIfChangedForToday(newTitle, newDescription);
        assertEquals("Не все события серии отредактировались", newTitle, events[1].get(0).getName());
        assertEquals("Не все события серии отредактировались", newDescription, events[1].get(0).getDescription());
    }

    @Step("Редактируем событие и проверяем, что только одно из событий серии отредактировалось")
    private void shouldEditOnlyOneOfSeries(String newTitle, String newDescription, String oldTitle,
                                           String oldDescription) {
        List<Event>[] events = editEventAndCheckIfChangedForToday(newTitle, newDescription);
        assertEquals("Отредактировались все события серии", oldTitle, events[1].get(0).getName());
        assertEquals("Отредактировались все события серии", oldDescription, events[1].get(0).getDescription());
    }
}
