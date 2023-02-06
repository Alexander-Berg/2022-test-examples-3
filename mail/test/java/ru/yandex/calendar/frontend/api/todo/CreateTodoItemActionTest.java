package ru.yandex.calendar.frontend.api.todo;

import lombok.Data;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.TodoList;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.annotation.BenderPart;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class CreateTodoItemActionTest extends ApiTodoActionTestBase<CreateTodoItemActionTest.Response> {

    public CreateTodoItemActionTest() {
        super(CreateTodoItemAction.class, Response.class);
    }

    @Test
    public void failIfNoList() {
        Assert.some(execute("list-external-id=XXX").getItemId());

        Assert.some(Situation.TODO_LIST_NOT_FOUND.getTitle(),
                execute("fail-if-no-list=true&list-external-id=XXX").getErrorName());

        TodoList data = new TodoList();
        data.setExternalId("XXX");
        data.setTitle("ToDo");

        todoRoutines.createTodoList(uid, data, ActionInfo.webTest());

        Assert.some(execute("fail-if-no-list=true&list-external-id=XXX").getItemId());
    }

    @Data
    @BenderBindAllFields
    public static class Response {
        private final Option<Error> error;
        @BenderPart(name = "todo-item", strictName = true)
        private final Option<Item> todoItem;

        public Option<String> getErrorName() {
            return error.map(Error::getName);
        }

        public Option<Long> getItemId() {
            return todoItem.map(Item::getId);
        }
    }

    @Data
    @BenderBindAllFields
    public static class Error {
        private final String name;
    }

    @Data
    @BenderBindAllFields
    public static class Item {
        private final long id;
    }
}
