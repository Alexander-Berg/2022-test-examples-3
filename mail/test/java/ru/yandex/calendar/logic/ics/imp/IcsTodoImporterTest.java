package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.TodoItem;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVToDo;
import ru.yandex.calendar.logic.todo.TodoDao;
import ru.yandex.calendar.logic.todo.TodoRoutines;
import ru.yandex.calendar.logic.todo.id.TodoIdOrExternalId;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author akirakozov
 */
public class IcsTodoImporterTest extends AbstractConfTest {

    @Autowired
    private IcsTodoImporter icsTodoImporter;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TodoDao todoDao;
    @Autowired
    private TodoRoutines todoRoutines;

    @Test
    public void createTodoItem() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15901").getUid();

        IcsVToDo vtodo = new IcsVToDo();
        vtodo = vtodo.withSummary("createTodoItem");
        vtodo = vtodo.withCompleted(TestDateTimes.moscow(2010, 12, 10, 11, 30));
        vtodo = vtodo.withDue(TestDateTimes.moscow(2010, 12, 11, 12, 30));
        vtodo = vtodo.withUid(CalendarUtils.generateExternalId());

        Instant importTime = TestDateTimes.moscow(2010, 12, 12, 12, 30);
        icsTodoImporter.importTodos(uid, vtodo.makeCalendar(),
                IcsImportMode.caldavPutToDefaultLayerForTest(importTime).getActionInfo());

        TodoItem todoItem = todoDao.findNotDeletedNotArchivedTodoItemByExternalIdAndCreatorUid(vtodo.getUid().get(), uid).get();

        IcsVTimeZones tzs = IcsVTimeZones.fallback(MoscowTime.TZ);
        Assert.A.equals(todoItem.getTitle(), vtodo.getSummary().get());
        Assert.A.equals(todoItem.getDueTs(), vtodo.getDueInstant(tzs));
        Assert.A.equals(todoItem.getCompletionTs(), vtodo.getCompletedInstant(tzs));
        Assert.A.equals(todoItem.getLastUpdateTs(), importTime);
    }

    @Test
    public void updateTodoItemWithNewerVersion() {
        updateTodoItem(true);
    }

    @Test
    public void updateTodoItemWithLaterVersion() {
        updateTodoItem(false);
    }

    private void updateTodoItem(boolean newerVersion) {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-15901").getUid();

        long listId = todoRoutines.createTodoListDefault(
                uid, Option.<String>empty(), ActionInfo.webTest(TestDateTimes.moscow(2010, 12, 10, 11, 30)));
        TodoItem todoItem = testManager.createDefaultTodoItem(uid, "updateTodoItem", listId);

        IcsVToDo vtodo = new IcsVToDo();
        vtodo = vtodo.withSummary("updateTodoItem updated");
        vtodo = vtodo.withCompleted(TestDateTimes.moscow(2010, 12, 10, 11, 30));
        vtodo = vtodo.withDue(TestDateTimes.moscow(2010, 12, 11, 12, 30));
        vtodo = vtodo.withUid(todoItem.getExternalId());
        vtodo = vtodo.withDtStamp(TestDateTimes.plusHours(todoItem.getLastUpdateTs(), newerVersion ? 1 : -1));

        Instant importTime = TestDateTimes.moscow(2010, 12, 12, 12, 30);
        icsTodoImporter.importTodos(uid, vtodo.makeCalendar(),
                IcsImportMode.incomingEmailFromMailhook(importTime).getActionInfo().freezeNowForTest());

        TodoItem updatedTodoItem = todoDao.findNotDeletedNotArchivedTodoItemById(TodoIdOrExternalId.of(todoItem), uid);
        IcsVTimeZones tzs = IcsVTimeZones.fallback(MoscowTime.TZ);

        if (newerVersion) {
            Assert.A.equals(updatedTodoItem.getTitle(), vtodo.getSummary().get());
            Assert.A.equals(updatedTodoItem.getDueTs().get(), vtodo.getDueInstant(tzs).get());
            Assert.A.equals(updatedTodoItem.getCompletionTs(), vtodo.getCompletedInstant(tzs));
            Assert.A.equals(updatedTodoItem.getLastUpdateTs(), importTime);
        } else {
            Assert.A.equals(updatedTodoItem.getTitle(), todoItem.getTitle());
        }
    }
}
