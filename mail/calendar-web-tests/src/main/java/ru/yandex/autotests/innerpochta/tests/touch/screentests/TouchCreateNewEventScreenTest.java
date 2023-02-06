package ru.yandex.autotests.innerpochta.tests.touch.screentests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PARTICIPANTS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Верстка элементов страницы создания события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class TouchCreateNewEventScreenTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int NUM_OF_PARTICIPANTS_TO_ADD = 3;

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> stepsProd.user()));

    @Before
    public void setUp() {
        // На юзере заготавливаем популярные контакты путём создания встреч на вчера
        String yesterdayDate = DATE_FORMAT.format(LocalDateTime.now().minusDays(1));
        stepsProd.user().apiCalSettingsSteps().createNewEvent(
            stepsProd.user().settingsCalSteps().formDefaultEvent(
                stepsProd.user().apiCalSettingsSteps().getUserLayersIds().get(0)
            )
                .withStartTs(yesterdayDate + "T00:00:00")
                .withEndTs(yesterdayDate + "T01:00:00")
                .withAttendeesArray(Arrays.copyOfRange(PARTICIPANTS, 0, NUM_OF_PARTICIPANTS_TO_ADD))
        );
    }

    @Test
    @Title("Вёрстка попапа «Передумали создавать событие?»")
    @TestCaseId("1047")
    public void shouldSeeChangedMindPopup() {
        String title = getRandomName();
        String description = getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchCreateEventSteps().createEventBuilder()
                .withTitle(title)
                .withDescription(description);
            st.user().defaultSteps().clicksOn(st.pages().cal().touchHome().eventPage().cancelEdit())
                .shouldSee(st.pages().cal().touchHome().cancelCreatePopup().refuseOrAllEventsBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка саджеста популярных контактов")
    @TestCaseId("1057")
    public void shouldSeePopularContactsSuggest() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            st.pages().cal().touchHome().addEventButton(),
            st.pages().cal().touchHome().eventPage().changeParticipants(),
            st.pages().cal().touchHome().editParticipantsPage().input()
        )
            .shouldSee(st.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка участников на странице создания события")
    @TestCaseId("1057")
    public void shouldSeeAddedContactsAtCreationPage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchCreateEventSteps().createEventBuilder()
                .withPopularParticipants(0);
            st.user().defaultSteps().scrollTo(st.pages().cal().touchHome().eventPage().changeParticipants());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка добавленных участников на странице добавления участников")
    @TestCaseId("1057")
    public void shouldSeeAddedContactsAtEditParticipantsPage() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            st.pages().cal().touchHome().addEventButton(),
            st.pages().cal().touchHome().eventPage().changeParticipants(),
            st.pages().cal().touchHome().editParticipantsPage().input()
        )
            .clicksOn(st.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0))
            .shouldSee(st.pages().cal().touchHome().editParticipantsPage().removeItem().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка кнопки «Скрыть» участников на странице создания события")
    @TestCaseId("1059")
    public void shouldSeeHideParticipantsButton() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchCreateEventSteps().createEventBuilder()
                .withParticipants(PARTICIPANTS);
            st.user().defaultSteps().scrollTo(st.pages().cal().touchHome().eventPage().showMoreParticipants());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка кнопки «Ещё n участников» на странице создания события")
    @TestCaseId("1059")
    public void shouldSeeShowAllParticipantsButton() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().calTouchCreateEventSteps().createEventBuilder()
                .withParticipants(PARTICIPANTS);
            st.user().defaultSteps()
                .clicksOn(st.pages().cal().touchHome().eventPage().showMoreParticipants())
                .scrollTo(st.pages().cal().touchHome().eventPage().showMoreParticipants());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка попапа «Сохранить изменения?» при выходе из редактирования участников")
    @TestCaseId("1078")
    public void shouldSeeSaveChangesPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().cal().touchHome().addEventButton())
                .clicksOn(st.pages().cal().touchHome().eventPage().changeParticipants())
                .inputsTextInElement(st.pages().cal().touchHome().editParticipantsPage().input(), DEV_NULL_EMAIL)
                .clicksOn(
                    st.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0),
                    st.pages().cal().touchHome().editParticipantsPage().backToEditEvent()
                )
                .shouldSee(st.pages().cal().touchHome().saveEditParticipantsPopup().refuseOrAllEventsBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка «Ничего не нашлось» в саджесте контактов")
    @TestCaseId("1057")
    public void shouldSeeNothingFoundInParticipantsSuggest() {
        String invalidContact = Utils.getRandomString();

        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().clicksOn(
            st.pages().cal().touchHome().addEventButton(),
            st.pages().cal().touchHome().eventPage().changeParticipants()
        )
            .inputsTextInElement(st.pages().cal().touchHome().editParticipantsPage().input(), invalidContact)
            .shouldSee(st.pages().cal().touchHome().editParticipantsPage().nothingFoundSuggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка страницы создания события на весь день")
    @TestCaseId("1051")
    public void shouldSeeAllDayCreateEventPage() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().touchHome().addEventButton())
            .turnTrue(st.pages().cal().touchHome().eventPage().allDayCheckBox());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
