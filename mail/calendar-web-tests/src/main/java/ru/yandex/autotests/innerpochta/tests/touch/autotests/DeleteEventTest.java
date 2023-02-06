package ru.yandex.autotests.innerpochta.tests.touch.autotests;

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
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Удаление события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
public class DeleteEventTest {

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
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Удаление простого события")
    @TestCaseId("1067")
    public void shouldDeleteSimpleEvent() {
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteEvenPopup().confirmOrOneEventBtn()
            )
            .shouldNotSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeElementsCount(steps.pages().cal().touchHome().events(), 0);
    }

    @Test
    @Title("Передумываем удалять простое событие по кнопке «Отмена» в попапе подтверждения удаления")
    @TestCaseId("1067")
    public void shouldNotDeleteSimpleEvent() {
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps()
                .formDefaultEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteEvenPopup().refuseOrAllEventsBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage());
        checkIfEventsPresentTodayAndTomorrow(1, 0);
    }

    @Test
    @Title("Удаление повторяющегося события - только это")
    @TestCaseId("1068")
    public void shouldDeleteOnlyOneOfRepeatableEvent() {
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(
            steps.user().settingsCalSteps()
                .formDefaultRepeatingEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteOneOrAllPopup().confirmOrOneEventBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().grid());
        checkIfEventsPresentTodayAndTomorrow(0, 1);
    }

    @Test
    @Title("Удаление повторяющегося события - закрываем попап выбора событий для удаления")
    @TestCaseId("1068")
    public void shouldCloseOneOrAllPopup() {
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(
            steps.user().settingsCalSteps()
                .formDefaultRepeatingEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteOneOrAllPopup().close()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage());
        checkIfEventsPresentTodayAndTomorrow(1, 1);
    }

    @Test
    @Title("Удаление повторяющегося события - всю серию")
    @TestCaseId("1069")
    public void shouldDeleteAllOfRepeatableEvent() {
        steps.user().apiCalSettingsSteps().createNewRepeatEvent(
            steps.user().settingsCalSteps()
                .formDefaultRepeatingEvent(steps.user().apiCalSettingsSteps().getUserLayersIds().get(0))
        );
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().delete(),
                steps.pages().cal().touchHome().deleteOneOrAllPopup().refuseOrAllEventsBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().grid());
        checkIfEventsPresentTodayAndTomorrow(0, 0);
    }

    @Step("Проверяем, что в календаре {0} событий на сегодня и {1} событий на завтра")
    private void checkIfEventsPresentTodayAndTomorrow(int todayEventsNum, int tomorrowEventsNum) {
        List<Event>[] eventsForTwoDays = steps.user().apiCalSettingsSteps().getEventsForTwoDays();
        assertThat("Событий на сегодня не " + todayEventsNum, eventsForTwoDays[0], hasSize(todayEventsNum));
        assertThat("Событий на завтра не " + tomorrowEventsNum, eventsForTwoDays[1], hasSize(tomorrowEventsNum));
    }
}
