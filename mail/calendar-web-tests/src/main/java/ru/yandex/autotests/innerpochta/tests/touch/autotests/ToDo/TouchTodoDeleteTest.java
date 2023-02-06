package ru.yandex.autotests.innerpochta.tests.touch.autotests.ToDo;

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

import java.time.LocalDateTime;

import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.DISABLED_TAB_SELECTOR;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.TODO_DATE_API_FORMAT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("[Тач] Todo удаление списка/дела")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.TODO_BLOCK)
public class TouchTodoDeleteTest {

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
    @Title("Удаление дела на вкладке «Дела»")
    @TestCaseId("1115")
    public void shouldDeleteItemFromList() {
        steps.user().defaultSteps().clicksOn(
            steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).itemName(),
            steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).deleteBtn()
        )
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists().get(0).items());
    }

    @Test
    @Title("Удаление непустого списка дел на вкладке «Дела»")
    @TestCaseId("1131")
    public void shouldDeleteNotEmptyList() {
        steps.user().defaultSteps().clicksOn(
            steps.user().pages().calTouch().todo().todoLists().get(0).listTitle(),
            steps.user().pages().calTouch().todo().todoLists().get(0).deleteBtn()
        )
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().cancelBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .clicksOn(
                steps.user().pages().calTouch().todo().todoLists().get(0).listTitle(),
                steps.user().pages().calTouch().todo().todoLists().get(0).deleteBtn()
            )
            .shouldSee(steps.pages().cal().home().warningPopup())
            .clicksOn(steps.pages().cal().home().warningPopup().agreeBtn())
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists());
    }

    @Test
    @Title("Удаление пустого списка дел на вкладке «Дела»")
    @TestCaseId("1132")
    public void shouldDeleteEmptyList() {
        steps.user().apiCalSettingsSteps().deleteAllTodoLists()
            .createNewTodoList(getRandomName());
        steps.user().defaultSteps().refreshPage()
            .clicksOn(
                steps.user().pages().calTouch().todo().todoLists().get(0).listTitle(),
                steps.user().pages().calTouch().todo().todoLists().get(0).deleteBtn()
            )
            .shouldNotSee(steps.pages().cal().home().warningPopup())
            .shouldNotSee(steps.user().pages().calTouch().todo().todoLists());
    }

    @Test
    @Title("Удаление списка/дела на вкладке «Просроченные»")
    @TestCaseId("1116")
    public void shouldDeleteExpiredItemAndList() {
        steps.user().apiCalSettingsSteps().createNewTodoItem(
            0,
            getRandomName(),
            TODO_DATE_API_FORMAT.format(LocalDateTime.now().minusYears(1))
        );
        steps.user().defaultSteps().refreshPage();
        steps.user().calTouchTodoSteps().openTab(steps.user().pages().calTouch().todo().tabExpired());
        steps.user().defaultSteps().clicksOn(
                steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).itemName(),
                steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).deleteBtn()
            )
            .shouldContainsAttribute(
            steps.user().pages().calTouch().todo().tabExpired(),
            "class",
            DISABLED_TAB_SELECTOR
        );
    }

    @Test
    @Title("Удаление списка/дела на вкладке «Выполненные»")
    @TestCaseId("1117")
    public void shouldDeleteCompletedItemAndList() {
        steps.user().defaultSteps()
            .turnTrue(steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).doneCheckBox());
        steps.user().calTouchTodoSteps().openTab(steps.user().pages().calTouch().todo().tabCompleted());
        steps.user().defaultSteps().clicksOn(
            steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).itemName(),
            steps.user().pages().calTouch().todo().todoLists().get(0).items().get(0).deleteBtn()
        )
            .shouldContainsAttribute(
                steps.user().pages().calTouch().todo().tabCompleted(),
                "class",
                DISABLED_TAB_SELECTOR
            );
    }
}
