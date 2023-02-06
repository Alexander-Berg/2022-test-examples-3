package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.COLUMN_CENTER;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TIME_11AM;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на попап создания события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class NewEventPopupTest {

    private static final String UNFORMAT_TIME = "1234";
    private static final String START_TIME = "12:34";
    private static final String END_TIME = "13:04";
    private static final String EMPTY_NAME = "Без названия";
    private static final String EVENT_URL = "event?";

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
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().defaultSteps()
            .offsetClick(steps.pages().cal().home().currentColumn(), COLUMN_CENTER, TIME_11AM)
            .shouldSee(steps.pages().cal().home().newEventPopup());
    }

    @Test
    @Title("Создаем пустое событие")
    @TestCaseId("383")
    public void shouldCreateEmptyEvent() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsTodayList().get(0).eventName(), EMPTY_NAME);
    }

    @Test
    @Title("Закрываем попап крестиком")
    @TestCaseId("72")
    public void shouldNotSeeNewEvent() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().closePopup())
            .shouldNotSee(steps.pages().cal().home().newEventPopup());
    }

    @Test
    @Title("Закрываем попап через ESC")
    @TestCaseId("423")
    public void shouldCloseNewEventViaEsc() {
        steps.user().defaultSteps().shouldSee(steps.pages().cal().home().newEventPopup());
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ESCAPE.toString());
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().newEventPopup());
    }

    @Test
    @Title("Нажав «Да» закрываем попап")
    @TestCaseId("384")
    public void shouldCloseEventPopup() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), getRandomName())
            .clicksOn(steps.pages().cal().home().newEventPopup().closePopup())
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldNotSee(
                steps.pages().cal().home().newEventPopup(),
                steps.pages().cal().home().warningPopup()
            );
    }

    @Test
    @Title("Остаемся в попапе после нажатия «Нет»")
    @TestCaseId("385")
    public void shouldNotCloseEventPopup() {
        String eventName = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), eventName)
            .clicksOn(steps.pages().cal().home().newEventPopup().closePopup())
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .shouldHasValue(steps.pages().cal().home().newEventPopup().nameInput(), eventName);
    }

    @Test
    @Title("Переходим по ссылке «Больше параметров»")
    @TestCaseId("408")
    public void shouldOpenCreateEventPage() {
        String eventName = getRandomName();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), eventName)
            .clicksOn(steps.pages().cal().home().newEventPopup().moreParamsBtn())
            .shouldBeOnUrl(containsString(EVENT_URL));
    }

    @Test
    @Title("Вводим время вручную")
    @TestCaseId("73")
    public void shouldSeeCorrectTime() {
        steps.user().defaultSteps()
            .inputsTextInElementClearingThroughHotKeys(
                steps.pages().cal().home().newEventPopup().timeInputList().get(0),
                UNFORMAT_TIME
            )
            .shouldHasValue(steps.pages().cal().home().newEventPopup().timeInputList().get(0), START_TIME)
            .shouldHasValue(steps.pages().cal().home().newEventPopup().timeInputList().get(1), END_TIME);
    }
}
