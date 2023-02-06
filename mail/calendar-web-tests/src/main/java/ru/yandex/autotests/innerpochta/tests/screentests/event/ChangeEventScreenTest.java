package ru.yandex.autotests.innerpochta.tests.screentests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Редактирование события по приглашению")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.EDIT_EVENT)
public class ChangeEventScreenTest {

    private String eventName;
    private Long layerID;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(2);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Before
    public void setUp() {
        eventName = getRandomName();
        layerID = stepsTest.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
    }

    @Test
    @Title("Поля Название, Описание, Время, Участники доступны для редактирования в событии по приглашению")
    @TestCaseId("91")
    public void allFieldsShouldBeEditable() {
        Event event = stepsTest.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(true);
        stepsTest.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
                .clicksOn(st.pages().cal().home().viewEventPopup().editEventBtn())
                .shouldHasValue(st.pages().cal().home().newEventPage().nameInput(), eventName);

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Поля Название, Описание, Время, Участники недоступны для редактирования в событии по приглашению без прав")
    @TestCaseId("103")
    public void allFieldsShouldNotBeEditable() {
        Event event = stepsTest.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(false);
        stepsTest.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
                .clicksOn(st.pages().cal().home().viewEventPopup().editEventBtn())
                .shouldNotSee(st.pages().cal().home().newEventPage().nameInput());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }

    @Test
    @Title("Поле Участники доступно для редактирования если есть право только приглашать")
    @TestCaseId("104")
    public void participantsFieldShouldBeEditable() {
        Event event = stepsTest.user().settingsCalSteps().formDefaultEvent(layerID).withName(eventName)
            .withParticipantsCanEdit(false).withParticipantsCanInvite(true);
        stepsTest.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOnElementWithText(st.pages().cal().home().eventsAllList(), eventName)
            .clicksOn(st.pages().cal().home().viewEventPopup().editEventBtn())
                .shouldNotSee(st.pages().cal().home().newEventPage().nameInput());

        parallelRun.withActions(actions).withAcc(lock.accNum(1)).run();
    }
}
