package ru.yandex.calendar.logic.ics.exp;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVToDo;
import ru.yandex.calendar.logic.todo.TodoRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author akirakozov
 */
public class IcsTodoExporterTest extends AbstractConfTest {

    @Autowired
    TestManager testManager;
    @Autowired
    TodoRoutines todoRoutines;
    @Autowired
    IcsTodoExporter icsTodoExporter;

    @Test
    public void exportTodoItem() {
        DateTimeZone tz = MoscowTime.TZ;
        IcsVTimeZones tzs = IcsVTimeZones.fallback(tz);
        PassportUid uid = testManager.prepareUser("yandex-team-mm-16001").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2010, 12, 23, 0, 0)));

        TodoItem overrides = new TodoItem();
        overrides.setDueTs(TestDateTimes.moscow(2010, 12, 23, 0, 0));
        overrides.setCompletionTs(TestDateTimes.moscow(2010, 12, 23, 0, 0));
        TodoItem todoItem = testManager.createDefaultTodoItem(uid, "exportTodoItem", listId, overrides);

        IcsVToDo icsTodo = icsTodoExporter.exportTodoItemByExternalIdForCaldav(uid, todoItem.getExternalId()).get().getIcsToDo();
        Assert.A.equals(todoItem.getTitle(), icsTodo.getSummary().get());
        Assert.A.equals(
                todoItem.getDueTs().get().toDateTime(tz).toLocalDate(),
                icsTodo.getDueInstant(tzs).get().toDateTime(tz).toLocalDate());
        Assert.equals(todoItem.getCompletionTs(), icsTodo.getCompletedInstant(tzs));

    }

}
