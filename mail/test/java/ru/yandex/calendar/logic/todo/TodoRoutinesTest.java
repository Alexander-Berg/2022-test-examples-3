package ru.yandex.calendar.logic.todo;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.calendar.logic.beans.generated.TodoItemHelper;
import ru.yandex.calendar.logic.beans.generated.TodoList;
import ru.yandex.calendar.logic.beans.generated.TodoListHelper;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.todo.id.ListIdOrExternalId;
import ru.yandex.calendar.logic.todo.id.TodoIdOrExternalId;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class TodoRoutinesTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private TodoRoutines todoRoutines;
    @Autowired
    private TodoDao todoDao;

    @Test
    public void createListWithDescription() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(17891).getUid();

        TodoList todoListData = new TodoList();
        todoListData.setTitle("todolist_title");
        todoListData.setDescription("some\ndescription");
        todoListData.setExternalId(CalendarUtils.generateExternalId());

        long todoListId = todoRoutines.createTodoList(
                uid, todoListData, ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));

        TodoList todoList = todoDao.findNotDeletedTodoListById(ListIdOrExternalId.id(todoListId));
        TodoList changes = TodoListHelper.INSTANCE.findChanges(todoList, todoListData);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void createListWithoutDescription() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(1234).getUid();

        TodoList todoListData = new TodoList();
        todoListData.setTitle("todolist_title");
        todoListData.setExternalId(CalendarUtils.generateExternalId());

        long todoListId = todoRoutines.createTodoList(
                uid, todoListData, ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));

        TodoList todoList = todoDao.findNotDeletedTodoListById(ListIdOrExternalId.id(todoListId));
        TodoList changes = TodoListHelper.INSTANCE.findChanges(todoList, todoListData);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void createItemTest() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15020").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));
        TodoItem todoItemData = new TodoItem();
        todoItemData.setTodoListId(listId);
        todoItemData.setTitle("title");
        todoItemData.setDueTs(TestDateTimes.moscow(2010, 11, 23, 0, 0));
        todoItemData.setCompletionTs(TestDateTimes.moscow(2010, 11, 23, 10, 0));

        final ActionInfo actionInfo = ActionInfo.webTest();
        long itemId = todoRoutines.createTodoItem(uid, todoItemData, actionInfo);

        TodoItem createdItem = todoDao.findNotDeletedNotArchivedTodoItemById(TodoIdOrExternalId.id(itemId), uid);
        TodoItem changes = TodoItemHelper.INSTANCE.findChanges(createdItem, todoItemData);
        Assert.A.isTrue(changes.isEmpty());
        Assert.A.equals(actionInfo.getNow(), createdItem.getCreationTs());
        Assert.A.equals(actionInfo.getNow(), createdItem.getLastUpdateTs());
    }

    @Test
    public void updateItemTest() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15030").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));
        TodoItem todoItemData = new TodoItem();
        todoItemData.setTodoListId(listId);
        todoItemData.setTitle("title");
        todoItemData.setDueTs(TestDateTimes.moscow(2010, 11, 23, 0, 0));
        todoItemData.setCompletionTs(TestDateTimes.moscow(2010, 11, 23, 10, 0));

        Instant creationTs = TestDateTimes.moscow(2010, 11, 23, 10, 0);
        long itemId = todoRoutines.createTodoItem(uid, todoItemData, ActionInfo.webTest(creationTs));
        TodoItem updateItemData = new TodoItem();
        updateItemData.setTodoListId(listId);
        updateItemData.setTitle("new title");
        updateItemData.setDueTs(TestDateTimes.moscow(2010, 11, 26, 0, 0));
        updateItemData.setCompletionTs(TestDateTimes.moscow(2010, 11, 26, 10, 0));

        Instant updateTs = TestDateTimes.moscow(2010, 11, 23, 13, 0);
        todoRoutines.updateTodoItem(uid, updateItemData, itemId, ActionInfo.webTest(updateTs));

        TodoItem updatedItem = todoDao.findNotDeletedNotArchivedTodoItemById(TodoIdOrExternalId.id(itemId), uid);
        TodoItem changes = TodoItemHelper.INSTANCE.findChanges(updatedItem, updateItemData);
        Assert.A.isTrue(changes.isEmpty());
        Assert.A.equals(updateTs, updatedItem.getLastUpdateTs());
    }

    @Test
    public void setItemCompletion() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15040").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));
        TodoItem todoItemData = new TodoItem();
        todoItemData.setTodoListId(listId);
        todoItemData.setTitle("title");
        todoItemData.setDueTs(TestDateTimes.moscow(2010, 11, 23, 0, 0));
        todoItemData.setCompletionTsNull();

        Instant creationTs = TestDateTimes.moscow(2010, 11, 23, 10, 0);
        long itemId = todoRoutines.createTodoItem(uid, todoItemData, ActionInfo.webTest(creationTs));

        Instant updateTs = TestDateTimes.moscow(2010, 11, 23, 13, 0);
        final ActionInfo actionInfo = ActionInfo.webTest(updateTs);
        todoRoutines.updateTodoItemSetCompleted(uid, TodoIdOrExternalId.id(itemId), true, actionInfo);

        TodoItem updatedItem = todoDao.findNotDeletedNotArchivedTodoItemById(TodoIdOrExternalId.id(itemId), uid);
        Assert.A.equals(actionInfo.getNow(), updatedItem.getCompletionTs().get());
        Assert.A.equals(updateTs, updatedItem.getLastUpdateTs());
    }

    @Test
    public void deleteNonEmptyTodoList() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15050").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));
        TodoItem todoItemData = new TodoItem();
        todoItemData.setTodoListId(listId);
        todoItemData.setCreatorUid(uid);
        todoItemData.setTitle("title");
        todoItemData.setPos(0);
        todoItemData.setExternalId(CalendarUtils.generateExternalId());
        todoDao.saveTodoItem(todoItemData, ActionInfo.webTest(Instant.now()));

        todoRoutines.deleteTodoList(uid, ListIdOrExternalId.id(listId), ActionInfo.webTest());
        Assert.A.isEmpty(todoDao.findNotDeletedUserTodoLists(uid));
    }

    @Test
    public void synchStatusConflictingFieldChange() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-15061");
        Instant lastUpdateTs = TestDateTimes.moscow(2011, 7, 15, 20, 37);

        TodoItem todoItemData = createAndSaveTodo(user, lastUpdateTs, "synchStatusWasConflictingChange");

        todoItemData.setTitle("synchStatusWasConflictingChange-change");
        TodoSynchStatus todoSynchStatus = todoRoutines.getTodoSynchStatus(
                user.getUid(), todoItemData, TodoIdOrExternalId.id(todoItemData.getId()),
                Option.<Boolean>empty(), Option.<Boolean>empty(),
                Option.of(lastUpdateTs.minus(Duration.standardHours(1))));

        Assert.A.isTrue(todoSynchStatus.found().wasChange());
        Assert.A.isTrue(todoSynchStatus.found().wasConflictingChange());
    }

    @Test
    public void synchStatusNotConflictingFieldChange() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-15071");
        Instant lastUpdateTs = TestDateTimes.moscow(2011, 7, 15, 20, 37);

        TodoItem todoItemData = createAndSaveTodo(user, lastUpdateTs, "synchStatusNotConflictingFieldChange");

        todoItemData.setTitle("synchStatusNotConflictingFieldChange-change");
        TodoSynchStatus todoSynchStatus = todoRoutines.getTodoSynchStatus(
                user.getUid(),
                todoItemData, TodoIdOrExternalId.id(todoItemData.getId()),
                Option.<Boolean>empty(), Option.<Boolean>empty(),
                Option.of(lastUpdateTs));

        Assert.A.isTrue(todoSynchStatus.found().wasChange());
        Assert.A.isFalse(todoSynchStatus.found().wasConflictingChange());
    }

    @Test
    public void synchStatusConflictingCompletionChange() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-15081");
        Instant lastUpdateTs = TestDateTimes.moscow(2011, 7, 15, 20, 37);

        TodoItem todoItemData = createAndSaveTodo(user, lastUpdateTs, "synchStatusConflictingCompletionChange");

        TodoSynchStatus todoSynchStatus = todoRoutines.getTodoSynchStatus(
                user.getUid(),
                todoItemData, TodoIdOrExternalId.id(todoItemData.getId()), Option.of(true), Option.<Boolean>empty(),
                Option.of(lastUpdateTs.minus(Duration.standardHours(1))));

        Assert.A.isTrue(todoSynchStatus.found().wasChange());
        Assert.A.isTrue(todoSynchStatus.found().wasConflictingChange());
        Assert.A.isTrue(todoSynchStatus.found().changedToCompleted());
    }

    private TodoItem createAndSaveTodo(TestUserInfo user, Instant lastUpdateTs, String name) {
        long listId = todoRoutines.createTodoListDefault(
                user.getUid(), Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2011, 9, 19, 1, 26)));
        TodoItem todoItemData = new TodoItem();
        todoItemData.setTodoListId(listId);
        todoItemData.setCreatorUid(user.getUid());
        todoItemData.setTitle(name);
        todoItemData.setPos(0);
        todoItemData.setExternalId(CalendarUtils.generateExternalId());
        long id = todoDao.saveTodoItem(todoItemData, ActionInfo.webTest(lastUpdateTs));

        todoItemData.setId(id);
        return todoItemData;
    }

}
