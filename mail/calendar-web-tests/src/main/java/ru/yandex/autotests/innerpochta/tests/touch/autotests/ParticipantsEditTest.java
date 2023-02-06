package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PARTICIPANTS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Работа с участниками события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class ParticipantsEditTest {

    private static final int NUM_OF_PARTICIPANTS_SHOWN_BY_DEFAULT = 5;
    private static final int NUM_OF_PARTICIPANTS_TO_ADD = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();
    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        // На юзере заготавливаем популярные контакты путём создания встреч на вчера
        String yesterdayDate = DATE_FORMAT.format(LocalDateTime.now().minusDays(1));
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps().formDefaultEvent(
                steps.user().apiCalSettingsSteps().getUserLayersIds().get(0)
            )
                .withStartTs(yesterdayDate + "T00:00:00")
                .withEndTs(yesterdayDate + "T01:00:00")
                .withAttendeesArray(Arrays.copyOfRange(PARTICIPANTS, 0, NUM_OF_PARTICIPANTS_TO_ADD))
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Добавляем контакт из саджеста популярных контактов")
    @TestCaseId("1057")
    public void shouldAddParticipantFromPopularContactsSuggest() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withPopularParticipants(0)
            .submit()
            .thenCheck();
    }

    @Test
    @Title("Добавляем контакт вручную по Enter")
    @TestCaseId("1058")
    public void shouldAddParticipantByEnter() {
        steps.user().defaultSteps().clicksOn(steps.user().pages().calTouch().addEventButton())
            .clicksOn(steps.user().pages().calTouch().eventPage().changeParticipants())
            .inputsTextInElement(steps.user().pages().calTouch().editParticipantsPage().input(), DEV_NULL_EMAIL);
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.user().pages().calTouch().editParticipantsPage().input(),
            Keys.ENTER.toString()
        );
        steps.user().defaultSteps().clicksOn(steps.user().pages().calTouch().editParticipantsPage().save())
            .shouldSeeElementInList(steps.user().pages().calTouch().eventPage().members(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Проверяем плашку «Ещё n участников», если добавили 10 и больше")
    @TestCaseId("1059")
    public void shouldSeeMoreBtnIfAddedMoreThan10Participants() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withParticipants(PARTICIPANTS);
        steps.user().defaultSteps()
            .shouldContainText(steps.pages().cal().touchHome().eventPage().showMoreParticipants(), "Скрыть")
            .clicksOn(steps.pages().cal().touchHome().eventPage().showMoreParticipants())
            .shouldContainText(
                steps.pages().cal().touchHome().eventPage().showMoreParticipants(),
                String.format("Ещё %s участников", PARTICIPANTS.length - NUM_OF_PARTICIPANTS_SHOWN_BY_DEFAULT)
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeParticipants())
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().removeItem().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().save()
            )
            .shouldNotSee(steps.pages().cal().touchHome().eventPage().showMoreParticipants());

    }

    @Test
    @Title("Остаемся на странице после закрытия попапа сохранения редактирования участников")
    @TestCaseId("1078")
    public void shouldStayAtParticipantsPageIfCanceledSaveChanges() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().addEventButton())
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeParticipants())
            .inputsTextInElement(steps.pages().cal().touchHome().editParticipantsPage().input(), DEV_NULL_EMAIL)
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().backToEditEvent(),
                steps.pages().cal().touchHome().saveEditParticipantsPopup().close()
            )
            .shouldNotSee(steps.pages().cal().touchHome().saveEditParticipantsPopup())
            .shouldSee(steps.pages().cal().touchHome().editParticipantsPage());
    }

    @Test
    @Title("Отменяем изменения после нажатия на «Нет» попапа сохранения редактирования участников")
    @TestCaseId("1078")
    public void shouldDiscardParticipantsChangeIfSavedChanges() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().addEventButton())
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeParticipants())
            .inputsTextInElement(steps.pages().cal().touchHome().editParticipantsPage().input(), DEV_NULL_EMAIL)
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().backToEditEvent(),
                steps.pages().cal().touchHome().saveEditParticipantsPopup().refuseOrAllEventsBtn()
            )
            .shouldNotSee(steps.pages().cal().touchHome().saveEditParticipantsPopup())
            .shouldNotSee(steps.pages().cal().touchHome().eventPage().members());
    }

    @Test
    @Title("Сохраняем изменения после нажатия на «Да» попапа сохранения редактирования участников")
    @TestCaseId("1079")
    public void shouldSaveParticipantsChangeIfSavedChanges() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().touchHome().addEventButton())
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeParticipants())
            .inputsTextInElement(steps.pages().cal().touchHome().editParticipantsPage().input(), DEV_NULL_EMAIL)
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().suggested().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().backToEditEvent(),
                steps.pages().cal().touchHome().saveEditParticipantsPopup().confirmOrOneEventBtn()
            )
            .shouldNotSee(steps.pages().cal().touchHome().saveEditParticipantsPopup())
            .shouldSeeElementInList(steps.pages().cal().touchHome().eventPage().members(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Удаляем уже добавленного участника")
    @TestCaseId("1080")
    public void shouldRemoveParticipant() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withParticipants(Arrays.copyOfRange(PARTICIPANTS, 0, NUM_OF_PARTICIPANTS_TO_ADD));
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventPage().changeParticipants())
            .inputsTextInElement(
                steps.pages().cal().touchHome().editParticipantsPage().input(),
                PARTICIPANTS[0]
            )
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().removeItem().waitUntil(not(empty())).get(0),
                steps.pages().cal().touchHome().editParticipantsPage().save()
            )
            .shouldSeeElementsCount(
                steps.pages().cal().touchHome().eventPage().members().waitUntil(not(empty())),
                NUM_OF_PARTICIPANTS_TO_ADD - 1
            );
    }
}
