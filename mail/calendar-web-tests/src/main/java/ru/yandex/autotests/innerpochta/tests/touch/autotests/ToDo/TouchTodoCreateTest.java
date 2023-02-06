package ru.yandex.autotests.innerpochta.tests.touch.autotests.ToDo;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
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
@Title("[Тач] Todo создание списка/дела")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.TODO_BLOCK)
@RunWith(DataProviderRunner.class)
public class TouchTodoCreateTest {

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

    @DataProvider
    public static Object[][] dates() {
        return new Object[][]{
            {LocalDateTime.now(), "Сегодня"},
            {LocalDateTime.now().plusDays(1), "Завтра"},
        };
    }

    @DataProvider
    public static Object[][] actualDates() {
        return new Object[][]{
            {LocalDateTime.now().plusDays(2), TODO_THIS_YEAR_DATE_FORMAT.format(LocalDateTime.now().plusDays(2))},
            {LocalDateTime.now().plusYears(1), TODO_DATE_FORMAT.format(LocalDateTime.now().plusYears(1))},
        };
    }

    @DataProvider
    public static Object[][] expiredDates() {
        return new Object[][]{
            {LocalDateTime.now().minusDays(1), "Вчера"},
            {LocalDateTime.now().withDayOfMonth(1).withMonth(2), "1 февр."},
            {
                LocalDateTime.now().minusYears(1),
                TODO_DATE_FORMAT.format(LocalDateTime.now().minusYears(1))
            }
        };
    }

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().createNewTodoList(getRandomName())
            .createNewTodoItem(0, getRandomString(), null);
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
        steps.user().calTouchTodoSteps().openTodo();
    }

    @Test
    @Title("Создание дела без даты")
    @TestCaseId("1124")
    public void shouldCreateItemWithoutDate() {
        createToDoItem(getRandomName(), steps.pages().cal().touchHome().todo());
    }

    @Test
    @Title("Создание дела на сегодня/завтра")
    @TestCaseId("1125")
    @UseDataProvider("dates")
    public void shouldCreateTodayItem(LocalDateTime date, String expectation) {
        steps.user().calTouchTodoSteps()
            .createToDoItemWithDate(getRandomName(), steps.user().calTouchTodoSteps().getRegionDateFormat(date))
            .shouldSeeItemDate(0, 1, expectation);
    }

    @Test
    @Title("Создание дела с актуальной датой")
    @TestCaseId("1128")
    @UseDataProvider("actualDates")
    public void shouldCreateFutureItem(LocalDateTime date, String expectation) {
        steps.user().calTouchTodoSteps()
            .createToDoItemWithDate(getRandomName(), steps.user().calTouchTodoSteps().getRegionDateFormat(date))
            .shouldSeeItemDate(0, 1, expectation);
    }

    @Test
    @Title("Создание дела с просроченной датой")
    @TestCaseId("1222")
    @UseDataProvider("expiredDates")
    public void shouldCreateExpiredItem(LocalDateTime date, String expectation) {
        String todoItem = getRandomName();
        steps.user().calTouchTodoSteps().createToDoItemWithDate(
            todoItem,
            steps.user().calTouchTodoSteps().getRegionDateFormat(date)
        )
            .shouldSeeItemDate(0, 1, expectation);
        steps.user().calTouchTodoSteps().openTab(steps.user().pages().calTouch().todo().tabExpired());
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().cal().touchHome().todo().todoLists().get(0).items().get(0).itemName(),
            todoItem
        );
        steps.user().calTouchTodoSteps().shouldSeeItemDate(0, 0, expectation);
    }

    @Test
    @Title("Сохранение дела нажатием «+Новое дело»")
    @TestCaseId("1127")
    public void shouldCreateItemByNewItemBtn() {
        createToDoItem(
            getRandomName(),
            steps.pages().cal().touchHome().todo().todoLists().get(0).newTodoItemBtn()
        );
    }

    @Step("Добавляем дело с именем «{0}» и при добавлении жмём на элемент «{1}»")
    private void createToDoItem(String name, WebElement button) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().touchHome().todo().todoLists().waitUntil(not(empty())).get(0).newTodoItemBtn()
            )
            .inputsTextInElement(
                steps.pages().cal().touchHome().todo().todoLists().get(0).items()
                    .waitUntil(not(empty())).get(1).inputName(),
                name
            )
            .clicksOn(button)
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().todo().todoLists().waitUntil(not(empty())).get(0)
                    .items().waitUntil(not(empty())).get(1).itemName(),
                name
            );
    }
}
