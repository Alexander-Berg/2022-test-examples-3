package ru.yandex.autotests.innerpochta.tests.touch.autotests.ToDo;

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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DISABLED_TAB_SELECTOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_DATE_API_FORMAT;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_DATE_FORMAT;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_THIS_YEAR_DATE_FORMAT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Тач] Todo редактирование списка/дела")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.TODO_BLOCK)
public class TouchTodoChangeTest {

    private static String TOMORROW = "Завтра";

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
        steps.user().apiCalSettingsSteps().createNewTodoList(getRandomName())
            .createNewTodoItem(0, getRandomString(), null);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().calTouchTodoSteps().openTodo();
    }

    @Test
    @Title("Редактирование списка дел")
    @TestCaseId("1118")
    public void shouldChangeListName() {
        String changedTitle = getRandomString();
        steps.user().defaultSteps().clicksOn(steps.user().pages().calTouch().todo().todoLists().get(0).listTitle())
            .shouldSee(steps.user().pages().calTouch().todo().todoLists().get(0).deleteBtn())
            .appendTextInElement(
                steps.user().pages().calTouch().todo().todoLists().get(0).inputTitle(),
                changedTitle
            )
            .clicksOn(steps.pages().cal().touchHome().todo())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).inputTitle())
            .shouldSeeThatElementHasText(
                steps.user().pages().calTouch().todo().todoLists().get(0).listTitle(),
                changedTitle
            );
    }

    @Test
    @Title("Редактирование списка дел не доступно в табах «Просроченные» и «Выполненные»")
    @TestCaseId("1118")
    public void shouldNotChangeListNameInExpAndCompTabs() {
        steps.user().apiCalSettingsSteps()
            .createNewTodoItem(
                0,
                getRandomString(),
                TODO_DATE_API_FORMAT.format(LocalDateTime.now().minusYears(1))
            );
        steps.user().defaultSteps().refreshPage()
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox());
        steps.user().calTouchTodoSteps().openTab(steps.user().pages().calTouch().todo().tabExpired());
        steps.user().defaultSteps().clicksOn(steps.user().pages().calTouch().todo().todoLists().get(0).listTitle())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).inputTitle());
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabCompleted());
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().todo().todoLists().get(0).listTitle())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).inputTitle());
    }

    @Test
    @Title("Редактирование дела на вкладке «Дела»")
    @TestCaseId("1114")
    public void shouldChangeItemDateInAllTab() {
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1);
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
        String changedName = getRandomString();

        changeItemName(0, 0, changedName);
        changeItemDate(0, 0, nextMonth);
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, TODO_THIS_YEAR_DATE_FORMAT.format(nextMonth));
        changeItemDate(0, 0, lastMonth);
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, TODO_THIS_YEAR_DATE_FORMAT.format(lastMonth))
            .openTab(steps.pages().cal().touchHome().todo().tabExpired())
            .shouldSeeItemDate(0, 0, TODO_THIS_YEAR_DATE_FORMAT.format(lastMonth));
    }

    @Test
    @Title("Редактирование даты дела на вкладке «Просроченные»")
    @TestCaseId("1119")
    public void shouldChangeItemDateInExpired() {
        LocalDateTime lastYear = LocalDateTime.now().minusYears(1);
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
        LocalDateTime lastMonthYear = LocalDateTime.now().minusMonths(1).minusYears(1);

        steps.user().apiCalSettingsSteps()
            .createNewTodoItem(0, getRandomString(), TODO_DATE_API_FORMAT.format(lastYear))
            .createNewTodoItem(0, getRandomString(), TODO_DATE_API_FORMAT.format(lastMonth));
        steps.user().defaultSteps().refreshPage();
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabExpired());
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, TODO_DATE_FORMAT.format(lastYear))
            .shouldSeeItemDate(0, 1, TODO_THIS_YEAR_DATE_FORMAT.format(lastMonth));
        changeItemDate(0, 1, lastMonthYear);
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, TODO_DATE_FORMAT.format(lastMonthYear));
    }

    @Test
    @Title("Редактирование даты дела на актуальную на вкладке «Просроченные»")
    @TestCaseId("1119")
    public void shouldChangeItemName() {
        LocalDateTime actualDate = LocalDateTime.now().plusDays(1);
        String changedItem = getRandomString();
        steps.user().apiCalSettingsSteps()
            .createNewTodoItem(
                0,
                getRandomString(),
                TODO_DATE_API_FORMAT.format(LocalDateTime.now().minusMonths(1))
            )
            .createNewTodoItem(
                0,
                changedItem,
                TODO_DATE_API_FORMAT.format(LocalDateTime.now().minusYears(1))
            );
        steps.user().defaultSteps().refreshPage();
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabExpired());
        changeItemDate(0, 0, actualDate);
        steps.user().defaultSteps()
            .shouldNotSeeElementInList(steps.user().pages().calTouch().todo().todoLists().get(0).items(), changedItem);
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabAll());
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 2, TOMORROW);
    }

    @Test
    @Title("Редактирование даты дела на вкладке «Выполненные»")
    @TestCaseId("1120")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("MAYA-1679")
    public void shouldChangeItemDateInCompleted() {
        LocalDateTime nextDay = LocalDateTime.now().plusDays(1);
        steps.user().defaultSteps()
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox());
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabCompleted());
        changeItemDate(0, 0, nextDay);
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, TOMORROW);
        steps.user().defaultSteps().shouldContainsAttribute(
            steps.pages().cal().touchHome().todo().tabExpired(),
            "class",
            DISABLED_TAB_SELECTOR
        );
        steps.user().calTouchTodoSteps().openTab(steps.pages().cal().touchHome().todo().tabAll());
        steps.user().defaultSteps().shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).items());
    }

    @Step("В списке «{0}» у элемента «{1}» меняем имя на «{2}»")
    public void changeItemName(int list, int item, String changedName) {
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().todo().todoLists().get(list).items().get(item).itemName())
            .appendTextInElement(
                steps.user().pages().calTouch().todo().todoLists().get(list).items().get(item).inputName(),
                changedName
            )
            .clicksOn(steps.pages().cal().touchHome().todo())
            .shouldSeeThatElementHasText(
                steps.user().pages().calTouch().todo().todoLists().get(list).items().get(item).itemName(),
                changedName
            );
    }

    @Step("В списке «{0}» у элемента «{1}» меняем дату на «{2}»")
    public void changeItemDate(int list, int item, LocalDateTime changedDate) {
        steps.user().defaultSteps()
            .clicksOn(steps.user().pages().calTouch().todo().todoLists().get(list).items().get(item).itemName())
            .appendTextInElement(
                steps.user().pages().calTouch().todo().todoLists().get(list).items().get(item).inputDate(),
                steps.user().calTouchTodoSteps().getRegionDateFormat(changedDate)
            )
            .clicksOn(steps.pages().cal().touchHome().todo());
    }
}
