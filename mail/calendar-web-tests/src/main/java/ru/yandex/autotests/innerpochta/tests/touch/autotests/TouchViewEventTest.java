package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.Tag;
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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static edu.emory.mathcs.backport.java.util.Arrays.asList;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PARTICIPANTS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Просмотр события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
public class TouchViewEventTest {

    private static final String EVENT_PAST_LABEL = "Событие прошло";
    private final int DAYS_BEFORE_OF_SIMPLE_EVENT_WITH_ATTENDEES = 2;
    private final int DAYS_BEFORE_OF_REPEATABLE_EVENT_START = 1;
    private final int NUMBER_OF_PARTICIPANTS = 10;

    private List<String> participants = asList(Arrays.copyOfRange(PARTICIPANTS, 0, 4));
    private List<String> manyParticipants = asList(Arrays.copyOfRange(PARTICIPANTS, 0, NUMBER_OF_PARTICIPANTS));
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
        Long defaultLayerId = steps.user().apiCalSettingsSteps().getUserLayersIds().get(0);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(
                steps.user().settingsCalSteps().formDefaultEvent(defaultLayerId),
                manyParticipants
            )
            .createNewEventWithAttendees(
                steps.user().settingsCalSteps().formEventInPast(
                    defaultLayerId,
                    DAYS_BEFORE_OF_SIMPLE_EVENT_WITH_ATTENDEES
                ),
                participants
            )
            .createNewRepeatEvent(
                steps.user().settingsCalSteps().formDefaultRepeatingEventInPast(
                    defaultLayerId,
                    DAYS_BEFORE_OF_REPEATABLE_EVENT_START
                )
            );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Просмотр одиночного события в прошлом")
    @TestCaseId("1022")
    public void viewEventPastInformer() {
        steps.user().calTouchGridSteps().openPastDayGrid(DAYS_BEFORE_OF_SIMPLE_EVENT_WITH_ATTENDEES);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().cal().touchHome().eventPage().eventPastInformer())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().eventPastInformer(), EVENT_PAST_LABEL);
    }

    @Test
    @Title("Показываем «Событие прошло» только у прошедших событий серии")
    @TestCaseId("1022")
    public void viewPastNotificationOnlyForPastRepeatableEvent() {
        steps.user().calTouchGridSteps().openPastDayGrid(DAYS_BEFORE_OF_REPEATABLE_EVENT_START);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().cal().touchHome().eventPage().eventPastInformer())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().eventPastInformer(), EVENT_PAST_LABEL);
        steps.user().calTouchGridSteps().openFutureDayGrid(1);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldNotSee(steps.pages().cal().touchHome().eventPage().eventPastInformer());
    }

    @Test
    @Title("Должны перейти в почту по клику на «Написать участникам»")
    @TestCaseId("1034")
    public void shouldGoToMail() throws UnsupportedEncodingException {
        steps.user().calTouchGridSteps().openPastDayGrid(DAYS_BEFORE_OF_SIMPLE_EVENT_WITH_ATTENDEES);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0));
        String eventName = steps.pages().cal().touchHome().eventPage().title().getText();
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().eventPage().writeToAllParticipants())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(String.format("%s/touch/compose", MAIL_BASE_URL));
        steps.user().defaultSteps().shouldSee(steps.user().touchPages().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE)
            .clicksIfCanOn(steps.pages().touch().composeIframe().yabbleMore());
        for (int i = 0; i < participants.size(); i++) {
            steps.user().defaultSteps()
                .shouldContainText(steps.pages().touch().composeIframe().inputTo(), participants.get(i));
        }
        steps.user().defaultSteps().shouldHasValue(steps.pages().touch().composeIframe().inputSubject(), eventName);
    }

    @Test
    @Title("Просмотр события с количеством юзеров не менее 10")
    @TestCaseId("1046")
    public void viewEventWithManyParticipants() {
        final int NUMBER_OF_MEMBERS_DISPLAYED_SHORTEN = 5;
        final int NUMBER_OF_MEMBERS_NOT_DISPLAYED = 5;
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().eventPage().members(),
                NUMBER_OF_MEMBERS_DISPLAYED_SHORTEN + 1
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().showMoreParticipants(),
                "Ещё " + NUMBER_OF_MEMBERS_NOT_DISPLAYED + " участников"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().showMoreParticipants())
            .shouldSeeElementsCount(steps.pages().cal().touchHome().eventPage().members(), NUMBER_OF_PARTICIPANTS + 1)
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().showMoreParticipants(),
                "Скрыть"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().showMoreParticipants())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().showMoreParticipants(),
                "Ещё " + NUMBER_OF_MEMBERS_NOT_DISPLAYED + " участников"
            )
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().eventPage().members(),
                NUMBER_OF_MEMBERS_DISPLAYED_SHORTEN + 1
            );
    }

    @Test
    @Title("Должны отображаться все участники события, когда их меньше 10, после редактирования")
    @TestCaseId("1046")
    public void viewEventWithManyParticipantsAfterEdit() {
        final int NUMBER_OF_MEMBERS_AFTER_EDIT = 9;
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventPage().changeParticipants()
            );
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().removeItem().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().save(),
                steps.pages().cal().touchHome().eventPage().submitForm(),
                steps.pages().cal().touchHome().mailToAllParticipantsPopup().confirmOrOneEventBtn()
            );
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().eventPage().members(),
                NUMBER_OF_MEMBERS_AFTER_EDIT + 1
            )
            .shouldNotSee(steps.pages().cal().touchHome().eventPage().showMoreParticipants());
    }

}
