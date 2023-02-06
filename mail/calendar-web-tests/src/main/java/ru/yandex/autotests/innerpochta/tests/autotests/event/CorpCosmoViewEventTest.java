package ru.yandex.autotests.innerpochta.tests.autotests.event;

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
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Корп] Просмотр встречи в космолёте")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class CorpCosmoViewEventTest {

    private static final String OTHER_EVENT = "https://calendar.yandex-team.ru/event/51061400";
    private static final String DAY_URL = "/day?show_date=2021-06-26";

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
    public RuleChain chain = rules.createCalendarRuleChain();

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
    }

    @Test
    @Title("Просмотр события с разными правами доступа - Чужое событие")
    @TestCaseId("991")
    public void shouldSeeOtherEvent() {
        steps.user().defaultSteps().opensUrl(OTHER_EVENT);
        shouldSeeViewEventForm();
    }

    @Test
    @Title("Просмотр события с разными правами доступа - Без прав")
    @TestCaseId("991")
    public void shouldSeeEventWithoutRules() {
        openChangeEventForm(0);
        shouldSeeViewEventForm();
    }

    @Test
    @Title("Просмотр события с разными правами доступа - С правом приглашать")
    @TestCaseId("991")
    public void shouldSeeEventWithInviteRules() {
        openChangeEventForm(1);
        steps.user().defaultSteps().shouldNotSee(
            steps.pages().cal().home().newEventPage().nameInput(),
            steps.pages().cal().home().newEventPage().descriptionAddBtn(),
            steps.pages().cal().home().newEventPage().dateStartInput()
        )
            .shouldSee(
                steps.pages().cal().home().newEventPage().membersInput(),
                steps.pages().cal().home().newEventPage().roomNameInput(),
                steps.pages().cal().home().newEventPage().notifyField(),
                steps.pages().cal().home().newEventPage().layerFieldSelect(),
                steps.pages().cal().home().newEventPage().status()
            );
    }

    @Test
    @Title("Просмотр события с разными правами доступа - С правом редактировать")
    @TestCaseId("991")
    public void shouldSeeEventWithAllRules() {
        openChangeEventForm(2);
        steps.user().defaultSteps().shouldSee(
            steps.pages().cal().home().newEventPage().nameInput(),
            steps.pages().cal().home().newEventPage().descriptionAddBtn(),
            steps.pages().cal().home().newEventPage().dateStartInput(),
            steps.pages().cal().home().newEventPage().roomNameInput(),
            steps.pages().cal().home().newEventPage().membersInput(),
            steps.pages().cal().home().newEventPage().notifyField(),
            steps.pages().cal().home().newEventPage().layerFieldSelect(),
            steps.pages().cal().home().newEventPage().status()
        );
    }

    @Step("Открываем день с событиями и событие {0} на редактирование")
    private void openChangeEventForm(int eventNum) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(DAY_URL)
            .clicksOn(steps.pages().cal().home().eventsAllList().get(eventNum))
            .clicksOn(steps.pages().cal().home().viewEventPopup().editEventBtn());
    }

    @Step("Должны видеть классическую форму просмотра события")
    private void shouldSeeViewEventForm() {
        steps.user().defaultSteps().shouldNotSee(
            steps.pages().cal().home().newEventPage().nameInput(),
            steps.pages().cal().home().newEventPage().descriptionAddBtn(),
            steps.pages().cal().home().newEventPage().membersInput(),
            steps.pages().cal().home().newEventPage().dateStartInput(),
            steps.pages().cal().home().newEventPage().roomNameInput()
        )
            .shouldSee(
                steps.pages().cal().home().newEventPage().notifyField(),
                steps.pages().cal().home().newEventPage().layerFieldSelect(),
                steps.pages().cal().home().newEventPage().status()
            );
    }
}
