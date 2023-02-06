package ru.yandex.calendar.frontend.api.todo;

import lombok.Data;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.bender.annotation.BenderPart;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class GetTodoItemsActionTest extends ApiTodoActionTestBase<GetTodoItemsActionTest.Response> {

    public GetTodoItemsActionTest() {
        super(GetTodoItemsAction.class, Response.class);
    }

    @Test
    public void iterateWithDue() {
        ListF<Long> ids = Cf.list(
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 6))),
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 13))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 17))));

        assertIterating(7, ids.reverse().paginate(3), "new-first=true&count=3");

        assertIterating(7, ids.paginate(3), "new-first=false&count=3");

        assertIterating(3, Cf.list(Cf.list(ids.get(6), ids.get(4)), Cf.list(ids.get(2))),
                "new-first=true&count=2&due-from=2018-03-06");

        assertIterating(3, Cf.list(Cf.list(ids.get(1), ids.get(5)), Cf.list(ids.get(2))),
                "soonest-first=true&count=2&due-to=2018-03-07");

        assertIterating(2, Cf.list(Cf.list(ids.get(2)), Cf.list(ids.get(4))),
                "count=1&due-from=2018-03-06&due-to=2018-03-17");

        assertIterating(Option.empty(), Cf.<ListF<Long>>list(Cf.list(ids.get(2), ids.get(4), ids.get(0), ids.get(3))),
                "due-from=2018-03-06&due-to=2018-03-17&undue-first=false");
    }

    @Test
    public void iterateWithUndue() {
        ListF<Long> ids = Cf.list(
                createTodoItem(Option.empty()),
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))));

        assertIterating(6, Cf.list(
                Cf.list(ids.get(3), ids.get(1)),
                Cf.list(ids.get(0), ids.get(5)),
                Cf.list(ids.get(4), ids.get(2))), "undue-first=true&new-first=true&count=2");

        assertIterating(6, Cf.list(
                Cf.list(ids.get(0), ids.get(1)),
                Cf.list(ids.get(3), ids.get(2)),
                Cf.list(ids.get(4), ids.get(5))), "undue-first=true&new-first=false&count=2");

        assertIterating(6, Cf.list(
                Cf.list(ids.get(5), ids.get(4)),
                Cf.list(ids.get(2), ids.get(3)),
                Cf.list(ids.get(1), ids.get(0))), "undue-first=false&new-first=true&count=2");

        assertIterating(6, Cf.list(
                Cf.list(ids.get(2), ids.get(4)),
                Cf.list(ids.get(5), ids.get(0)),
                Cf.list(ids.get(1), ids.get(3))), "undue-first=false&new-first=false&count=2");
    }

    @Test
    public void iterateWithSoonest() {
        ListF<Long> ids = Cf.list(
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 6))),
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 13))),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5))),
                createTodoItem(Option.empty()));

        assertIterating(7, Cf.list(1, 5, 2, 4, 0, 3, 6).map(ids::get).paginate(3),
                "soonest-first=true&count=3&undue-first=false");

        assertIterating(7, Cf.list(0, 3, 6, 4, 2, 1, 5).map(ids::get).paginate(3),
                "soonest-first=false&count=3&undue-first=true");

        assertIterating(7, Cf.list(6, 3, 0, 5, 1, 2, 4).map(ids::get).paginate(2),
                "soonest-first=true&count=2&new-first=true");

        assertIterating(7, Cf.list(4, 2, 5, 1, 6, 3, 0).map(ids::get).paginate(2),
                "soonest-first=false&count=2&undue-first=false&new-first=true");
    }

    @Test
    public void onlyNotCompleted() {
        ListF<Long> ids = Cf.list(
                createTodoItem(Option.empty()),
                createTodoItem(Option.empty(), true),
                createTodoItem(Option.of(new LocalDate(2018, 3, 6))),
                createTodoItem(Option.empty()),
                createTodoItem(Option.of(new LocalDate(2018, 3, 13)), true),
                createTodoItem(Option.of(new LocalDate(2018, 3, 5)), true),
                createTodoItem(Option.of(new LocalDate(2018, 3, 17))));

        assertIterating(3, Cf.list(Cf.list(ids.get(3), ids.get(0)), Cf.list(ids.get(5))),
                "count=2&due-to=2018-03-06&undue-first=true&new-first=true&count=2&only-not-completed-undue=true");

        assertIterating(Option.empty(), Cf.<ListF<Long>>list(Cf.list(ids.get(2), ids.get(4), ids.get(0), ids.get(3))),
                "due-from=2018-03-06&due-to=2018-03-17&undue-first=false&only-not-completed-undue=true");

        assertIterating(Option.empty(), Cf.<ListF<Long>>list(Cf.list(ids.get(6), ids.get(3), ids.get(2), ids.get(0))),
                "only-not-completed=true&only-not-completed-undue=false&new-first=true");

        assertIterating(Option.empty(), Cf.<ListF<Long>>list(Cf.list(ids.get(0), ids.get(2), ids.get(3), ids.get(5))),
                "due-to=2018-03-07&only-not-completed-undue=true&new-first=false");
    }


    protected void assertIterating(long expectedTotalCount, ListF<ListF<Long>> expectedIds, String parameters) {
        assertIterating(Option.of(expectedTotalCount), expectedIds, parameters);
    }

    protected void assertIterating(Option<Long> expectedTotalCount, ListF<ListF<Long>> expectedIds, String parameters) {
        IteratorF<ListF<Long>> expectedIterator = expectedIds.iterator();
        Option<String> iterationKey = Option.empty();

        while (expectedIterator.hasNext()) {
            Response response = execute(parameters + iterationKey.map(k -> "&iteration-key=" + k).mkString(""));

            Assert.equals(expectedTotalCount, response.getTotalCount());
            Assert.equals(expectedIterator.next(), response.getTodoItems().map(Item::getId));

            iterationKey = response.iterationKey;
        }
        Assert.none(iterationKey, "End of items expected but got " + iterationKey.mkString(""));
        Assert.isFalse(expectedIterator.hasNext(), "More items expected");
    }

    protected long createTodoItem(Option<LocalDate> date) {
        return createTodoItem(date, false);
    }

    protected long createTodoItem(Option<LocalDate> date, boolean completed) {
        TodoItem overrides = new TodoItem();

        overrides.setDueTs(date.map(dt -> dt.toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant()));
        overrides.setCompletionTs(Option.when(completed, Instant.now()));

        return testManager.createDefaultTodoItem(uid, "Task", listId, overrides).getId();
    }

    @Data
    @BenderBindAllFields
    public static class Response {
        @BenderPart(name = "todo-items", strictName = true)
        private final ListF<Item> todoItems;
        @BenderPart(name = "iteration-key", strictName = true)
        private final Option<String> iterationKey;
        @BenderPart(name = "total-count", strictName = true)
        private final Option<Long> totalCount;
    }

    @Data
    @BenderBindAllFields
    public static class Item {
        private final long id;
    }
}
