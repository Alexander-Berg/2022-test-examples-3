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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.SingleTodoBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.todo.Todo;
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
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Создаем/Удаляем/Редактируем дела в списке дел")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TODO_BLOCK)
public class CreateAndDeleteTodoItemTest extends BaseTest {

    private static final String DONE_TODO_LIST_LABEL = "Выполненные дела";
    private static final String SENT_TODO_LIST_MSG_TITLE = "Список дел «%s»";

    private String todoListTitle = Utils.getRandomName();
    private String todoItemTitle = Utils.getRandomName();
    private String newTodoItemTitle = Utils.getRandomName();
    private String tomorrowDate = Utils.getTomorrowDate("d MMM");
    private String timeValForSelection = "08:00";
    private Todo todoList;

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
        todoList = user.apiTodoSteps().createTodoList(todoListTitle);
        user.apiTodoSteps().createTodo(todoItemTitle, todoListTitle, todoList);
        user.apiSettingsSteps().callWith(of(SettingsConstants.SHOW_TODO, STATUS_ON));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().shouldSee(onMessagePage().mail360HeaderBlock());
    }

    @Test
    @Title("Создания дела в Списке дел")
    @TestCaseId("2114")
    public void shouldCreateTodoListItem() {
        user.defaultSteps().shouldSeeElementsCount(onHomePage().todoListBlock().todoList(), 4)
            .clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .shouldSee(onHomePage().todoItemsBlock().newTodoItemInput())
            .inputsTextInElement(onHomePage().todoItemsBlock().newTodoItemInput(), todoItemTitle)
            .clicksOn(onHomePage().todoItemsBlock().submitTodoItemBtn())
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems().waitUntil(not(empty())), todoItemTitle);
    }

    @Test
    @Title("Отменить изменения при редактировании дела")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66622")
    @Description("Уменьшаем количество тестов на TODO")
    @TestCaseId("2115")
    public void shouldNotSaveTodoItem() {
        user.apiTodoSteps().createTodo(todoItemTitle, todoListTitle, todoList);
        user.defaultSteps().clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .clicksOn(onHomePage().todoItemsBlock().todoItems().get(0).todoTitle())
            .inputsTextInElement(onHomePage().todoItemEditBlock().editTitleInput(), newTodoItemTitle)
            .clicksOn(onHomePage().todoItemEditBlock().setTomorrowDateBtn())
            .selectsOption(onHomePage().todoItemEditBlock().editTimeSelect(), timeValForSelection)
            .clicksOn(onHomePage().todoItemEditBlock().cancelEditBtn())
            .shouldNotSeeElementInList(onHomePage().todoItemsBlock().todoItems(), newTodoItemTitle)
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems(), todoItemTitle);
        user.defaultSteps()
            .shouldSeeThatElementTextEquals(onHomePage().todoItemsBlock().todoItems().get(0).todoDate(), "");
    }

    @Test
    @Title("Отредактировать дело")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66622")
    @Description("Уменьшаем количество тестов на TODO")
    @TestCaseId("4914")
    public void shouldEditTodoItem() {
        user.apiTodoSteps().createTodo(todoItemTitle, todoListTitle, todoList);
        user.defaultSteps().clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .clicksOn(onHomePage().todoItemsBlock().todoItems().get(0).todoTitle())
            .inputsTextInElement(onHomePage().todoItemEditBlock().editTitleInput(), newTodoItemTitle)
            .clicksOn(onHomePage().todoItemEditBlock().setTomorrowDateBtn())
            .selectsOption(onHomePage().todoItemEditBlock().editTimeSelect(), timeValForSelection)
            .clicksOn(onHomePage().todoItemEditBlock().submitTodoItemBtn())
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems(), newTodoItemTitle);
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onHomePage().todoItemsBlock().todoItems().get(0).todoDate(),
            tomorrowDate
        );
    }

    @Test
    @Title("Удалить дело")
    @TestCaseId("2153")
    public void shouldRemoveTodoItem() {
        user.defaultSteps().shouldSeeElementsCount(onHomePage().todoListBlock().todoList(), 4)
            .clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .shouldSeeElementsCount(onHomePage().todoItemsBlock().todoItems(), 1)
            .clicksOn(onHomePage().todoItemsBlock().todoItems().get(0).todoTitle())
            .clicksOn(onHomePage().todoItemEditBlock().deleteTodoItemBtn())
            .shouldNotSeeElementInList(onHomePage().todoItemsBlock().todoItems(), todoItemTitle);
    }

    @Test
    @Title("Отметить дело выполненным")
    @TestCaseId("2117")
    public void shouldCheckTodoItemAsDone() {
        user.defaultSteps().shouldSeeElementsCount(onHomePage().todoListBlock().todoList(), 4)
            .clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .shouldSeeElementsCount(onHomePage().todoItemsBlock().todoItems(), 1)
            .turnTrue(onHomePage().todoItemsBlock().todoItems().get(0).completeTodoItemCheckbox())
            .shouldSee(onHomePage().todoItemsBlock().emptyTodoLabel())
            .clicksOn(onHomePage().todoItemsBlock().itemsBackLink());
        SingleTodoBlock todoListBlock = (SingleTodoBlock) user.defaultSteps().shouldSeeElementInList(
            onHomePage().todoListBlock().todoList(),
            DONE_TODO_LIST_LABEL
        );
        user.defaultSteps().clicksOn(todoListBlock.title())
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems().waitUntil(not(empty())), todoItemTitle);
        user.defaultSteps().clicksOn(onHomePage().todoItemsBlock().todoItems().get(0).completeTodoItemCheckbox())
            .clicksOn(onHomePage().todoItemsBlock().todoBackLink());
        todoListBlock = (SingleTodoBlock) user.defaultSteps().shouldSeeElementInList(
            onHomePage().todoListBlock().todoList().waitUntil(not(empty())),
            todoListTitle
        );
        user.defaultSteps().clicksOn(todoListBlock.title())
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems(), todoItemTitle);
    }

    @Test
    @Title("Отправить список дел письмом")
    @TestCaseId("2118")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66622")
    @Description("Уменьшаем количество тестов на TODO")
    public void shouldSentTodoByMessage() {
        user.apiTodoSteps().createTodo(todoItemTitle, todoListTitle, todoList);
        user.defaultSteps().clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .onMouseHoverAndClick(onHomePage().todoItemsBlock().todoListTitle())
            .clicksOn(onHomePage().todoItemsBlock().sentTodoItemsIcon())
            .inputsTextInElement(onHomePage().todoListSentBlock().todoEmailsInput(), lock.firstAcc().getSelfEmail())
            .clicksOn(onHomePage().todoListSentBlock().sentTodoListBtn())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(String.format(SENT_TODO_LIST_MSG_TITLE, todoListTitle));
    }

    @Test
    @Title("Восстановить удаленное дело")
    @TestCaseId("2123")
    public void shouldRestoreDeletedTodoItem() {
        user.defaultSteps().shouldSeeElementsCount(onHomePage().todoListBlock().todoList(), 4)
            .clicksOn(onHomePage().todoListBlock().todoList().get(0).title())
            .shouldSeeElementsCount(onHomePage().todoItemsBlock().todoItems(), 1)
            .clicksOn(
                onHomePage().todoItemsBlock().todoItems().get(0).todoTitle(),
                onHomePage().todoItemEditBlock().deleteTodoItemBtn(),
                onHomePage().todoListRestore()
            )
            .shouldSeeElementInList(onHomePage().todoItemsBlock().todoItems().waitUntil(not(empty())), todoItemTitle);
    }
}
