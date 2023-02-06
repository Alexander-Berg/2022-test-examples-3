package ru.yandex.autotests.innerpochta.tests.todo;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.SingleTodoBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoItemsBlock.EMPTY_TODO_TEXT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;

/**
 * Created by mabelpines
 */
@Aqua.Test
@Title("Создаем/Удаляем/Редактируем список дел")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TODO_BLOCK)
public class CreateAndDeleteToDoBlockTest extends BaseTest {

    private static final String DO_NOT_FORGET_LABEL = "Не забыть";

    private String title = Utils.getRandomName();
    private String newTitle = Utils.getRandomName();

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiTodoSteps().deleteAllTodoLists()
            .todoSettingsSetOpenTodoList();
        user.apiSettingsSteps().callWith(of(SettingsConstants.SHOW_TODO, STATUS_ON));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(user.pages().HomePage().todoListBlock().closeTodoBlockBtn());
    }

    @Test
    @Title("Создание списка дел")
    @TestCaseId("2113")
    public void shouldCreateTodoList() {
        user.defaultSteps().clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock())
            .clicksOn(onHomePage().todoListBlock().todoTitleInput())
            .inputsTextInElement(onHomePage().todoListBlock().todoTitleInput(), title)
            .clicksOn(onHomePage().todoListBlock().submitTodoBtn())
            .shouldSeeThatElementTextEquals(onHomePage().todoItemsBlock().todoListTitle(), title.toUpperCase())
            .shouldSeeThatElementTextEquals(onHomePage().todoItemsBlock().emptyTodoLabel(), EMPTY_TODO_TEXT);
    }

    @Test
    @Title("Отмена редактирования списка дел")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66622")
    @Description("Уменьшаем количество тестов на TODO")
    @TestCaseId("2121")
    public void shouldCancelEditTodoList() {
        user.apiTodoSteps().createTodoList(title);
        user.defaultSteps().refreshPage()
            .clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock().todoList().waitUntil(not(empty())).get(0))
            .onMouseHover(getTodoBlock(0))
            .clicksOn(getTodoBlock(0).editTodoListBtn())
            .shouldSee(getTodoBlock(0).resetTodoNameInput())
            .clicksOn(getTodoBlock(0).resetTodoNameInput())
            .shouldSeeThatElementTextEquals(getTodoBlock(0).todoNameInput(), "")
            .inputsTextInElement(getTodoBlock(0).todoNameInput(), newTitle)
            .clicksOn(getTodoBlock(0).cancelTodoEditBtn())
            .shouldNotSee(getTodoBlock(0).cancelTodoEditBtn())
            .shouldSeeThatElementTextEquals(getTodoBlock(0).title(), title);
    }

    @Test
    @Title("Редактирование списка дел")
    @TestCaseId("3296")
    public void shouldEditTodoList() {
        user.apiTodoSteps().createTodoList(title);
        user.defaultSteps().refreshPage()
            .clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock().todoList().waitUntil(not(empty())).get(0))
            .onMouseHover(getTodoBlock(0))
            .clicksOn(
                getTodoBlock(0).editTodoListBtn(),
                getTodoBlock(0).resetTodoNameInput()
            )
            .shouldSeeThatElementTextEquals(getTodoBlock(0).todoNameInput(), "")
            .inputsTextInElement(getTodoBlock(0).todoNameInput(), newTitle)
            .clicksOn(getTodoBlock(0).saveTodoBtn())
            .shouldNotSee(getTodoBlock(0).saveTodoBtn())
            .shouldSeeThatElementTextEquals(getTodoBlock(0).title(), newTitle);
    }

    @Test
    @Title("Удалить список дел")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66622")
    @Description("Уменьшаем количество тестов на TODO")
    @TestCaseId("2152")
    public void shouldDeleteTodoList() {
        user.apiTodoSteps().createTodoList(title);
        user.defaultSteps().refreshPage()
            .clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock().todoList().waitUntil(not(empty())).get(0))
            .onMouseHover(getTodoBlock(0))
            .clicksOn(
                getTodoBlock(0).editTodoListBtn(),
                getTodoBlock(0).deleteTodoBtn()
            )
            .shouldSee(onHomePage().todoListBlock())
            .shouldNotSeeElementInList(onHomePage().todoListBlock().todoList(), title);
    }

    @Test
    @Title("Восстановить удаленный список дел")
    @TestCaseId("2124")
    public void shouldRecoverDeletedTodoList() {
        user.apiTodoSteps().createTodoList(title);
        user.defaultSteps().refreshPage()
            .clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock().todoList().waitUntil(not(empty())).get(0))
            .onMouseHover(getTodoBlock(0))
            .shouldSee(getTodoBlock(0).editTodoListBtn())
            .clicksOn(
                getTodoBlock(0).editTodoListBtn(),
                getTodoBlock(0).deleteTodoBtn()
            )
            .clicksOn(onHomePage().todoListRestore())
            .shouldSee(onHomePage().todoListBlock())
            .shouldSeeElementInList(onHomePage().todoListBlock().todoList().waitUntil(not(empty())), title);
    }

    @Test
    @Title("Удалить список дел “Не забыть“")
    @TestCaseId("2122")
    public void shouldRecoverDoNotForgetList() {
        user.apiTodoSteps().createTodo(
            Utils.getRandomName(),
            DO_NOT_FORGET_LABEL,
            user.apiTodoSteps().getTodoListByTitle(DO_NOT_FORGET_LABEL)
        );
        user.defaultSteps().clicksOn(onHomePage().toDoWindow())
            .shouldSee(onHomePage().todoListBlock().todoList().waitUntil(not(empty())).get(0))
            .onMouseHover(getTodoBlock(0))
            .clicksOn(
                getTodoBlock(0).editTodoListBtn(),
                getTodoBlock(0).deleteTodoBtn()
            )
            .shouldSee(
                onHomePage().todoListRestore(),
                onHomePage().todoListBlock()
            )
            .shouldSeeElementInList(onHomePage().todoListBlock().todoList(), DO_NOT_FORGET_LABEL);
        user.defaultSteps().clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .shouldSee(onHomePage().todoItemsBlock().emptyTodoLabel());
    }

    private SingleTodoBlock getTodoBlock(int index) {
        return onHomePage().todoListBlock().todoList().get(index);
    }
}
