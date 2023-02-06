package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Блок событий на весь день")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.ALL_DAY_EVENT)
public class AllDayEventsTest {

    private static final int MIN_EVENTS_TO_SHOW_MORE_BTN = 4;
    private static final int MIN_EVENTS_TO_SCROLL = 25;
    private static final int NUM_OF_EVENTS_WHEN_MORE_BUTTON_PRESENT = 2;
    private static final String EVENT_PAST_LABEL = "Событие прошло";

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
    @Title("Появление блока «Весь день» при создании первого события на весь день")
    @TestCaseId("1065")
    public void shouldSeeAllDayBlock() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withAllDayCheckbox(true)
            .submit()
            .thenCheck();
        steps.user().defaultSteps().shouldSeeInViewport(steps.pages().cal().touchHome().allDayEventsBlock());
    }

    @Test
    @Title("Появление контрола разоварачивания блока «Весь день», если событий на весь день больше трёх")
    @TestCaseId("1061")
    public void shouldSeeExpandAllDayControl() {
        steps.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(MIN_EVENTS_TO_SHOW_MORE_BTN);
        steps.user().defaultSteps().refreshPage()
            .shouldSeeInViewport(steps.pages().cal().touchHome().allDayEventsBlock())
            .shouldContainText(
                steps.pages().cal().touchHome().allDayEventsMoreButton(),
                String.format("Ещё %s", NUM_OF_EVENTS_WHEN_MORE_BUTTON_PRESENT)
            );
    }

    @Test
    @Title("Нет контрола разоварачивания блока «Весь день», если событий на весь день меньше четырёх")
    @TestCaseId("1061")
    public void shouldNotSeeExpandAllDayControl() {
        steps.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(MIN_EVENTS_TO_SHOW_MORE_BTN - 1);
        steps.user().defaultSteps().refreshPage()
            .shouldNotSee(
                steps.pages().cal().touchHome().allDayEventsMoreButton(),
                steps.pages().cal().touchHome().allDayEventsMoreArrowControl()
            );
    }

    @Test
    @Title("Разворачиваем список событий на день по «Ещё n» в блоке «Весь день»")
    @TestCaseId("1062")
    public void shouldExpandAllDayEventsByMoreControl() {
        steps.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(MIN_EVENTS_TO_SHOW_MORE_BTN);
        steps.user().defaultSteps().refreshPage()
            .shouldSeeInViewport(steps.pages().cal().touchHome().allDayEventsBlock())
            .clicksOn(steps.pages().cal().touchHome().allDayEventsMoreButton())
            .shouldNotSee(steps.pages().cal().touchHome().allDayEventsMoreButton())
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())),
                MIN_EVENTS_TO_SHOW_MORE_BTN
            );
    }

    @Test
    @Title("Развёрнутый блок «Весь день» сворачивается после рефреша")
    @TestCaseId("1062")
    public void shouldWrapAllDayEventsAfterRefresh() {
        createEventsAndExpandAllDayEventsBlock(MIN_EVENTS_TO_SHOW_MORE_BTN);
        steps.user().defaultSteps()
            .refreshPage()
            .shouldSee(steps.pages().cal().touchHome().allDayEventsMoreButton())
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())),
                NUM_OF_EVENTS_WHEN_MORE_BUTTON_PRESENT
            );
    }

    @Test
    @Title("Разворачиваем список событий на день по стрелке в блоке «Весь день»")
    @TestCaseId("1063")
    public void shouldExpandAllDayEventsByArrowControl() {
        createEventsAndExpandAllDayEventsBlock(MIN_EVENTS_TO_SHOW_MORE_BTN);
    }

    @Test
    @Title("Сворачиваем список событий на день по повторному нажатию на стрелку в блоке «Весь день»")
    @TestCaseId("1063")
    public void shouldWrapAllDayEventsByArrowControl() {
        createEventsAndExpandAllDayEventsBlock(MIN_EVENTS_TO_SHOW_MORE_BTN);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().allDayEventsMoreArrowControl())
            .shouldSee(steps.pages().cal().touchHome().allDayEventsMoreButton())
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())),
                NUM_OF_EVENTS_WHEN_MORE_BUTTON_PRESENT
            );
    }

    @Test
    @Title("Должны видеть надпись «Событие прошло» у события на весь день в прошлом")
    @TestCaseId("1066")
    public void shouldSeeAllDayEventPastLabel() {
        Long defaultLayerId = steps.user().apiCalSettingsSteps().getUserLayersIds().get(0);
        steps.user().apiCalSettingsSteps()
            .createNewEvent(
                steps.user().settingsCalSteps().formEventInPast(
                    defaultLayerId,
                    1
                )
            );
        steps.user().calTouchGridSteps().openPastDayGrid(1);
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().events().waitUntil(not(empty())).get(0))
            .shouldSee(steps.user().pages().calTouch().eventPage().eventPastInformer())
            .shouldContainText(steps.user().pages().calTouch().eventPage().eventPastInformer(), EVENT_PAST_LABEL);
    }

    @Test
    @Title("Проскролл длинного списка событий в блоке «Весь день»")
    @TestCaseId("1064")
    public void shouldScrollAllDayEvents() {
        createEventsAndExpandAllDayEventsBlock(MIN_EVENTS_TO_SCROLL);
        steps.user().defaultSteps()
            .setsWindowSize(400, 400)
            .shouldNotSeeInViewport(steps.pages().cal().touchHome().events().get(MIN_EVENTS_TO_SCROLL - 1))
            .scrollTo(steps.pages().cal().touchHome().events().get(MIN_EVENTS_TO_SCROLL - 1))
            .shouldSeeInViewport(steps.pages().cal().touchHome().events().get(MIN_EVENTS_TO_SCROLL - 1));
    }

    @Step("Создаем достаточно событий на весь день и разворачиваем блок этих событий по стрелке")
    private void createEventsAndExpandAllDayEventsBlock(int eventsNumber) {
        steps.user().apiCalSettingsSteps().createCoupleOfAllDayEvents(eventsNumber);
        steps.user().defaultSteps().refreshPage()
            .shouldSeeInViewport(steps.pages().cal().touchHome().allDayEventsBlock())
            .clicksOn(steps.pages().cal().touchHome().allDayEventsMoreArrowControl())
            .shouldNotSee(steps.pages().cal().touchHome().allDayEventsMoreButton())
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())),
                eventsNumber
            );
    }
}
