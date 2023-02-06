package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Тач] Настройка режима повторения событий")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.REPEATING_EVENT)
public class TouchCreateRepeatableEventTest {

    private static final String TIME = "T12:00:00";
    private static final String ANOTHER = "Другой вариант";
    private static final String REPEAT_WEEK_SETTING = "Повторять по неделям";
    private static final String CHOOSE_DATE = "Выбрать дату";
    private static final String MONDAY = "Понедельник";
    private static final String FRIDAY = "Пятница";
    private static final String SATURDAY = "Суббота";
    private static final String REPEAT_DAY = "каждый 2-й день до 28";
    private static final String REPEAT_WEEK = "пн, пт и сб каждой 2-й недели до 28";
    private static final String REPEAT_EVERY_N = "2";


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
    @Title("Создание повторяющегося события с пользовательской настройкой режима повтора (день)")
    @TestCaseId("1191")
    public void shouldCreateRepeatableEvent() {
        String eventName = getRandomName();
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now().withDayOfMonth(1));
        String dateFuture = dateFormat.format(LocalDateTime.now().plusMonths(1).withDayOfMonth(28));
        String startDate = date + TIME;
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(eventName)
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editRepetitionPage().menuSettings(),
                ANOTHER
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().menuSettings(),
                REPEAT_WEEK_SETTING
            )
            .clicksOn(steps.pages().cal().touchHome().repetitionTypePage().menuItems().get(0))
            .shouldSee(steps.pages().cal().touchHome().customSettingsRepetitionPage());
        System.out.println(dateFuture);
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatTo(),
            dateFuture
        );
        steps.user().defaultSteps().appendTextInElement(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatSetting(),
            REPEAT_EVERY_N
        )
            .clicksOn(steps.pages().cal().touchHome().customSettingsRepetitionPage().save())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                REPEAT_DAY
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm());
        steps.user().calTouchCreateEventSteps().openEvent();
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().eventPage().eventDate(),
            REPEAT_DAY
        );
    }

    @Test
    @Title("Создание повторяющегося события с пользовательской настройкой режима повтора (неделя)")
    @TestCaseId("1191")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-1679")
    public void shouldCreateRepeatableWeekEvent() {
        String eventName = getRandomName();
        String date = "2019-11-01";
        String dateTwo = "2019-11-28";
        String startDate = date + TIME;
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(eventName)
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editRepetitionPage().menuSettings(),
                ANOTHER
            );
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatTo(),
            dateTwo
        );
        steps.user().defaultSteps().appendTextInElement(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatSetting(),
            REPEAT_EVERY_N
        )
            .clicksOn(steps.pages().cal().touchHome().customSettingsRepetitionPage().choosenWeekDay())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().menuItems(),
                MONDAY
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().menuItems(),
                FRIDAY
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().menuItems(),
                SATURDAY
            )
            .clicksOn(steps.pages().cal().touchHome().customSettingsRepetitionPage().save())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                REPEAT_WEEK
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm());
        steps.user().calTouchCreateEventSteps().openEvent();
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().eventPage().eventDate(),
            REPEAT_WEEK
        );
    }

    @Test
    @Title("Создание повторяющегося события с пользовательской настройкой «Повторять до»")
    @TestCaseId("1164")
    public void shouldCreateRepeatableEventWithUserSettings() {
        String eventName = getRandomName();
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String datePast = dateFormat.format(LocalDateTime.now().withDayOfMonth(1));
        String date = dateFormat.format(LocalDateTime.now().withDayOfMonth(5));
        String dateFuture = dateFormat.format(LocalDateTime.now().plusMonths(1).withDayOfMonth(28));
        String startDate = date + TIME;
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(eventName)
            .setDate(steps.pages().cal().touchHome().eventPage().startDateInput(), startDate);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editRepetitionPage().menuSettings(),
                ANOTHER
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().menuSettings(),
                REPEAT_WEEK_SETTING
            )
            .clicksOn(steps.pages().cal().touchHome().repetitionTypePage().menuItems().get(0))
            .shouldSee(steps.pages().cal().touchHome().customSettingsRepetitionPage());
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatTo(),
            datePast
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatToText(),
            CHOOSE_DATE
        );
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatTo(),
            dateFuture
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatToText(),
            "28"
        );
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().customSettingsRepetitionPage().clearDate())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatToText(),
                CHOOSE_DATE
            );
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().customSettingsRepetitionPage().repeatTo(),
            dateFuture
        );
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().customSettingsRepetitionPage().save())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                "ежедневно до 28"
            );
    }
}
