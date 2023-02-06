package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.jsonassert.impl.matcher.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.AVAILABILITY_MAYBE_BUSY;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.PURPLE_COLOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Создание события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class TouchCreateNewEventTest {

    private static final String NOT_TODAY_DATE = "/?show_date=2035-06-17";
    private static final String EVENT_PLACE = "Пискарёвский проспект, 1";
    private static final String ERROR_PATTERN = "Что-то пошло не так. Повторите попытку или попробуйте позже.\n" +
        "Код ошибки: [a-f0-9]{32}";
    private static final String NIGHT_TIME = "T23:00:00";

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(2);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewLayer(
            steps.user().settingsCalSteps().formDefaultLayer().withColor(PURPLE_COLOR)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание простого события")
    @TestCaseId("1050")
    public void createNewSimpleEvent() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(getRandomName())
            .withDescription(getRandomName())
            .withPlace(EVENT_PLACE)
            .withParticipants(DEV_NULL_EMAIL)
            .withLayer(steps.user().apiCalSettingsSteps().getUserLayers().get(1).getName())
            .withAvailability(AVAILABILITY_MAYBE_BUSY)
            .submit()
            .thenCheck();
    }

    @Test
    @Title("Создание события на весь день")
    @TestCaseId("1051")
    public void createNewAllDayEvent() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(getRandomName())
            .withDescription(getRandomName())
            .withAllDayCheckbox(true)
            .submit()
            .thenCheck();
    }

    @Test
    @Title("Создание повторяющегося события на весь день")
    @TestCaseId("1165")
    public void createNewAllDayRepeatableEvent() {
        String eventName = getRandomName();
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(eventName)
            .withAllDayCheckbox(true)
            .withEveryDayRepetition()
            .submit()
            .thenCheck();

        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().allDayEventsBlock())
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().eventRepetition(),
                "Повторяется ежедневно"
            );
    }

    @Test
    @Title("Отвечаем «Нет» в попапе «Передумали создавать событие?»")
    @TestCaseId("1047")
    public void shouldStayAtEditEventPageIfNoInPopup() {
        String title = getRandomName();
        String description = getRandomString();
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(title)
            .withDescription(description);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().cancelEdit(),
                steps.pages().cal().touchHome().cancelCreatePopup().refuseOrAllEventsBtn()
            )
            .shouldNotSee(steps.pages().cal().touchHome().grid())
            .shouldContainValue(steps.pages().cal().touchHome().eventPage().editableTitle(), title)
            .shouldContainText(steps.pages().cal().touchHome().eventPage().editableDescription(), description);
    }

    @Test
    @Title("Отвечаем «Да» в попапе «Передумали создавать событие?»")
    @TestCaseId("1047")
    public void shouldCancelCreateEventIfYesInPopup() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(getRandomName())
            .withDescription(getRandomName());
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().cancelEdit(),
                steps.pages().cal().touchHome().cancelCreatePopup().confirmOrOneEventBtn()
            )
            .shouldSee(steps.pages().cal().touchHome().grid());
        assertThat("Событие всё равно создалось", steps.pages().cal().touchHome().events(), withWaitFor(hasSize(0)));
    }

    @Test
    @Title("Проверяем дефолтную дату при создании события c разных дней")
    @TestCaseId("1048")
    @DataProvider({NOT_TODAY_DATE, ""})
    public void shouldSeeCorrectDateInNewEventPopup(String dayParam) {
        Pattern dayPattern = Pattern.compile(", ([0-9]* [а-я]*)");

        steps.user().defaultSteps().opensUrl(UrlProps.urlProps().getBaseUri() + dayParam);
        Matcher dayMatcher = dayPattern.matcher(steps.pages().cal().touchHome().headerDate().getText());
        dayMatcher.find();
        String dayInGrid = dayMatcher.group(1);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().addEventButton())
            .shouldContainText(steps.pages().cal().touchHome().eventPage().startDate(), dayInGrid)
            .clicksOn(steps.pages().cal().touchHome().eventPage().cancelEdit())
            .shouldContainText(steps.pages().cal().touchHome().headerDate(), dayInGrid);
    }

    @Test
    @Title("Должны показывать ошибку при создании события с указанным удаленным календарём")
    @TestCaseId("1178")
    public void shouldSeeErrorWhenCreateEventWithDeletedCalendar() {
        steps.user().apiCalSettingsSteps()
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer())
            .createNewLayer(steps.user().settingsCalSteps().formDefaultLayer().withColor(PURPLE_COLOR));
        steps.user().defaultSteps().refreshPage().clicksOn(steps.user().pages().calTouch().addEventButton());
        steps.user().apiCalSettingsSteps().deleteLayers();
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().eventPage().submitForm())
            .shouldSee(steps.user().pages().calTouch().errorNotify())
            .shouldSeeThatElementTextMatchesPattern(steps.user().pages().calTouch().errorNotify(), ERROR_PATTERN);
    }

    @Test
    @Title("Проверяем настройки быстрого выбора режима повторения")
    @TestCaseId("1157")
    public void shouldSeeCorrectRepetitionOfNewEvent() {
        String eventName = getRandomName();
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(eventName);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOn(steps.pages().cal().touchHome().editRepetitionPage().menuItems().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                "Не повторяется"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().editEventRepetition())
            .clicksOn(steps.pages().cal().touchHome().editRepetitionPage().menuItems().waitUntil(not(empty())).get(1))
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                "Повторяется ежедневно"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().eventRepetition(),
                "Повторяется ежедневно"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().cancelEdit());
        steps.user().calTouchGridSteps().openFutureDayGrid(1);
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0),
                eventName + "\n  "
            );
    }

    @Test
    @Title("Удаляем содержимое поля ввода адреса")
    @TestCaseId("1081")
    public void shouldDeleteDataInAddressField() {
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().addEventButton())
            .clicksOn(steps.user().pages().calTouch().eventPage().changeParticipants())
            .shouldSee(steps.user().pages().calTouch().editParticipantsPage())
            .clicksOn(steps.user().pages().calTouch().editParticipantsPage().input())
            .clicksOn(steps.user().pages().calTouch().editParticipantsPage().clearInputBtn())
            .shouldNotSee(steps.user().pages().calTouch().editParticipantsPage().clearInputBtn())
            .clicksOn(steps.user().pages().calTouch().editParticipantsPage().input())
            .inputsTextInElement(steps.user().pages().calTouch().editParticipantsPage().input(), getRandomString())
            .clicksOn(steps.user().pages().calTouch().editParticipantsPage().clearInputBtn())
            .shouldHasValue(steps.user().pages().calTouch().editParticipantsPage().input(), "");
    }

    @Test
    @Title("Учитываем изменение занятости при смене времени события")
    @TestCaseId("1215")
    public void shouldSeeEmploymentMember() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        String date = dateFormat.format(LocalDateTime.now());
        String startDate = date + NIGHT_TIME;
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(date + "T00:00:00")
            .withEndTs(date + "T23:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().touchHome().addEventButton(),
            steps.user().pages().calTouch().eventPage().changeParticipants(),
            steps.user().pages().calTouch().editParticipantsPage().input()
        )
            .inputsTextInElement(
                steps.user().pages().calTouch().editParticipantsPage().input(),
                lock.accNum(0).getSelfEmail()
            )
            .clicksOn(
                steps.pages().cal().touchHome().editParticipantsPage().busyMember(),
                steps.pages().cal().touchHome().editParticipantsPage().save()
            )
            .shouldSee(steps.pages().cal().touchHome().eventPage().busyMember());
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            startDate
        );
        steps.user().defaultSteps().shouldSee(steps.pages().cal().touchHome().eventPage().freeMember());
    }
}
