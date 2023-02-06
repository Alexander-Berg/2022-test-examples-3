package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.Integer.parseInt;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("[Тач] Редактирование одиночного события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.EDIT_EVENT)
@RunWith(DataProviderRunner.class)
public class TouchEditSingleEventTest {

    private static final String ALL_DAY_EVENT_INDEX = "0";
    private static final String HOUR_EVENT_INDEX = "1";
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
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        steps.user().apiCalSettingsSteps().createNewEvent(steps.user().settingsCalSteps().formDefaultEvent(layerID));
        steps.user().apiCalSettingsSteps().createNewEvent(
            steps.user().settingsCalSteps().formDefaultEvent(layerID).withIsAllDay(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Редактирование одиночного события на весь день и одиночного события на несколько часов")
    @TestCaseId("1193")
    @DataProvider({ALL_DAY_EVENT_INDEX, HOUR_EVENT_INDEX})
    public void shouldEditSimpleEvent(int eventIndex) {
        shouldEditEvent(eventIndex);
    }

    @Test
    @Title("Отмена редактирования простого события - Закрываем попап подтверждения")
    @TestCaseId("1180")
    public void shouldNotCancelEditSimpleEventClosePopup() {
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        changeEventField(parseInt(HOUR_EVENT_INDEX), newTitle, newDescription);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().cancelEdit(),
                steps.pages().cal().touchHome().cancelEditPopup().close()
            )
            .shouldContainValue(steps.pages().cal().touchHome().eventPage().editableTitle(), newTitle)
            .shouldContainValue(steps.pages().cal().touchHome().eventPage().editableDescription(), newDescription);
    }

    @Test
    @Title("Отмена редактирования простого события - Отвечаем «Нет» в попапе подтверждения")
    @TestCaseId("1180")
    public void shouldNotCancelEditSimpleEventSayNo() {
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        changeEventField(parseInt(HOUR_EVENT_INDEX), newTitle, newDescription);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().cancelEdit(),
                steps.pages().cal().touchHome().cancelEditPopup().refuseOrAllEventsBtn()
            )
            .shouldContainValue(steps.pages().cal().touchHome().eventPage().editableTitle(), newTitle)
            .shouldContainValue(steps.pages().cal().touchHome().eventPage().editableDescription(), newDescription);
    }

    @Test
    @Title("Отмена редактирования простого события - Отвечаем «Да» в попапе подтверждения")
    @TestCaseId("1180")
    public void shouldCancelEditSimpleEventSayYes() {
        String oldTitle = steps.user().apiCalSettingsSteps().getTodayEvents().get(parseInt(HOUR_EVENT_INDEX)).getName();
        String oldDescription = steps.user().apiCalSettingsSteps().getTodayEvents()
            .get(parseInt(HOUR_EVENT_INDEX)).getDescription();
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        changeEventField(parseInt(HOUR_EVENT_INDEX), newTitle, newDescription);
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().cancelEdit(),
                steps.pages().cal().touchHome().cancelEditPopup().confirmOrOneEventBtn()
            )
            .shouldContainText(steps.pages().cal().touchHome().eventPage().title(), oldTitle)
            .shouldContainText(steps.pages().cal().touchHome().eventPage().description(), oldDescription);
    }

    @Test
    @Title("Отмена редактирования простого события - Попап подтверждения не появляется, если ничего не редактировали")
    @TestCaseId("1221")
    public void shouldNotSeeConfirmIfCancelUneditedEvent() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldNotSee(steps.pages().cal().touchHome().loader())
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventPage().cancelEdit()
            )
            .shouldNotSee(steps.pages().cal().touchHome().cancelEditPopup().close());
    }

    @Test
    @Title("Сделать простое событие на весь день повторяющимся")
    @TestCaseId("1166")
    public void shouldMakeSingleAllDayEventRepeatable() {
        shouldMakeNthEventRepeatable(0);
    }

    @Test
    @Title("Сделать простое событие повторяющимся")
    @TestCaseId("1167")
    public void shouldMakeSingleEventRepeatable() {
        shouldMakeNthEventRepeatable(1);
    }

    @Step("Делаем событие с номером {0} повторяющимся")
    private void shouldMakeNthEventRepeatable(int eventNum) {
        String eventTitle = getNthEvent(eventNum).getText();
        steps.user().defaultSteps()
            .clicksOn(getNthEvent(eventNum))
            .shouldNotSee(steps.pages().cal().touchHome().loader())
            .clicksOn(
                steps.pages().cal().touchHome().eventPage().edit(),
                steps.pages().cal().touchHome().eventPage().editEventRepetition()
            )
            .clicksOnElementWithText(
                steps.pages().cal().touchHome().editRepetitionPage().menuItems().waitUntil(not(empty())),
                "Каждый день"
            )
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().editEventRepetition(),
                "Повторяется ежедневно"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .clicksOn(getNthEvent(eventNum))
            .shouldSeeThatElementTextEquals(
                steps.pages().cal().touchHome().eventPage().eventRepetition(),
                "Повторяется ежедневно"
            )
            .clicksOn(steps.pages().cal().touchHome().eventPage().cancelEdit())
            .shouldSee(steps.pages().cal().touchHome().grid());
        steps.user().calTouchGridSteps().openFutureDayGrid(1);
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(getNthEvent(0), eventTitle);
    }

    @Step ("Получаем событие с номером {0}")
    private MailElement getNthEvent(int eventNum) {
        return steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(eventNum);
    }

    @Step("Редактируем событие и сохраняем его")
    private void shouldEditEvent(int eventNum) {
        String newTitle = getRandomName();
        String newDescription = getRandomName();
        changeEventField(eventNum, newTitle, newDescription);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().eventPage().submitForm())
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(eventNum))
            .shouldContainText(steps.pages().cal().touchHome().eventPage().title(), newTitle)
            .shouldContainText(steps.pages().cal().touchHome().eventPage().description(), newDescription);
    }

    @Step("Открываем событие на редактирование и меняем заголовок на «{1}», а описание на «{2}»")
    private void changeEventField(int eventNum, String title, String description) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(eventNum))
            .shouldNotSee(steps.pages().cal().touchHome().loader())
            .clicksOn(steps.pages().cal().touchHome().eventPage().edit())
            .inputsTextInElement(steps.pages().cal().touchHome().eventPage().editableTitle(), title)
            .inputsTextInElement(steps.pages().cal().touchHome().eventPage().editableDescription(), description);
    }
}
