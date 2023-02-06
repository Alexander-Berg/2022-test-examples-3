package ru.yandex.autotests.innerpochta.tests.screentests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Страница создания события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_PAGE)
public class CreateEventPageTest {

    private static final String EMAILS = "asd@ya.ru; asd@ya.ru; asd1@yandex.ru, asd1@rambler.ru 111@mail.ru";
    private static final String EMAIL1 = "testbot1@yandex.ru";
    private static final String EMAIL2 = "testbot2@yandex.ru";
    private static final String EMAIL3 = "testbot3@yandex.ru";
    private static final String EMAIL4 = "testbot4@yandex.ru";
    private static final String LAST_WEEK = "/week?show_date=2017-04-10";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Test
    @Title("Должны видеть предупреждение при выходе из создания")
    @TestCaseId("64")
    public void shouldSeeWarningPopupForNewEvent() {
        String eventName = getRandomName();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(st.pages().cal().home().newEventPage().nameInput(), eventName)
            .clicksOn(st.pages().cal().home().calHeaderBlock().calLink())
            .shouldSee(st.pages().cal().home().warningPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны видеть предупреждение при выходе из редактирования")
    @TestCaseId("594")
    public void shouldSeeWarningPopupForOldEvent() {
        Long layerID = stepsTest.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = stepsTest.user().settingsCalSteps().formDefaultEvent(layerID);
        String eventId = stepsTest.user().apiCalSettingsSteps().createNewEvent(event)
            .getAllEvents().get(0).getId().toString();
        String eventName = getRandomName();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().newEventPage().nameInput())
            .inputsTextInElement(st.pages().cal().home().newEventPage().nameInput(), eventName)
            .clicksOn(st.pages().cal().home().calHeaderBlock().calLink())
            .shouldSee(st.pages().cal().home().warningPopup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart(eventId)).run();
    }

    @Test
    @Title("Должны сформироваться яблы")
    @TestCaseId("116")
    public void shouldSeeYabbleInMembers() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().inputsTextInElement(st.pages().cal().home().newEventPage().nameInput(), EMAILS);
            st.user().hotkeySteps()
                .pressHotKeysWithDestination(
                    st.pages().cal().home().newEventPage().nameInput(),
                    Keys.chord(Keys.CONTROL, "a")
                )
                .pressHotKeysWithDestination(
                    st.pages().cal().home().newEventPage().nameInput(),
                    Keys.chord(Keys.CONTROL, "c")
                )
                .pressHotKeysWithDestination(
                    st.pages().cal().home().newEventPage().membersInput(),
                    Keys.chord(Keys.CONTROL, "v")
                );
            st.user().defaultSteps().clicksOn(st.pages().cal().home().newEventPage().nameInput())
                .shouldSee(st.pages().cal().home().newEventPage().membersList().waitUntil(not(empty())).get(3));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны сформироваться яблы после ручного ввода")
    @TestCaseId("116")
    public void shouldSeeYabbleInMembersAfterSendKeys() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL2)
                .clicksOn(st.pages().cal().home().newEventPage().nameInput())
                .shouldSee(st.pages().cal().home().newEventPage().membersList().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны видеть попап для повторения события")
    @TestCaseId("82")
    public void shouldSeeRepeatEventPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .turnTrue(st.pages().cal().home().newEventPage().repeatEventCheckBox());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Открываем выпадушку слоев")
    @TestCaseId("98")
    public void shouldSeeLayerDropdown() {
        stepsProd.user().apiCalSettingsSteps().createNewLayer(stepsProd.user().settingsCalSteps().formDefaultLayer());
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().cal().home().newEventPage().layerField());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны видеть аватарки при добавлении участников в событие")
    @TestCaseId("1237")
    public void shouldSeeContactAvatar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(
                    st.pages().cal().home().newEventPage().membersField(),
                    st.pages().cal().home().newEventPage().membersInput()
                )
                .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL2)
                .shouldSee(st.pages().cal().home().suggest())
                .clicksOnElementWithText(st.pages().cal().home().suggestItem(), EMAIL2);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны видеть пустую сетку занятости")
    @TestCaseId("1232")
    public void shouldSeeEmptyBusyGreed() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps()
                .clicksOn(st.pages().cal().home().leftPanel().createEvent())
                .clicksOn(st.pages().cal().home().newEventPage().membersField())
                .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL1);
            st.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
            st.user().defaultSteps().inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL2);
            st.user().hotkeySteps().pressCalHotKeys(Keys.ENTER.toString());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(LAST_WEEK).run();
    }

    @Test
    @Title("Отображение виджета занятости")
    @TestCaseId("766")
    public void shouldSeeEmploymentWidget() {
        Long layerID = stepsTest.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        createEventWithStartAndEndTimeAndParticipants(layerID,  "12:00:00", "13:00:00");
        createEventWithStartAndEndTimeAndParticipants(layerID,  "12:00:00", "13:00:00");
        createEventWithStartAndEndTimeAndParticipants(layerID,  "12:00:00", "13:00:00");
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().cal().home().leftPanel().createEvent())
                    .onMouseHover(st.pages().cal().home().editEventPage().busyTime().get(0))
                    .shouldSee(st.pages().cal().home().eventsCard());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Создание события с временем старта, окончанием и участниками")
    public void createEventWithStartAndEndTimeAndParticipants(Long layerID, String startTime, String endTime){
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime dateTime = LocalDateTime.now();
        Event event = stepsTest.user().settingsCalSteps().formDefaultEvent(layerID)
                .withStartTs(dateFormat.format(dateTime).split("[T]")[0] + "T" + startTime)
                .withEndTs(dateFormat.format(dateTime).split("[T]")[0] + "T" + endTime)
                .withParticipantsCanEdit(true);
        stepsTest.user().apiCalSettingsSteps()
                .createNewEventWithAttendees(event, Arrays.asList(EMAIL3, EMAIL4));
    }
}
