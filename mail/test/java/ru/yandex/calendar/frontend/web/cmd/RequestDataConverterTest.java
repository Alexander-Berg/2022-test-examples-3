package ru.yandex.calendar.frontend.web.cmd;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.RepetitionHelper;
import ru.yandex.calendar.logic.beans.generated.TodoList;
import ru.yandex.calendar.logic.beans.generated.TodoListHelper;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.util.data.MapDataProvider;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author akirakozov
 */
public class RequestDataConverterTest extends TestBase {
    @Test
    public void dailyRepetitonWithWeeklyFields() {
        MapF<String, String> map = Cf.hashMap();
        map.put("type", "daily");
        map.put("r_each", "1");
        map.put("r_weekly_days", "sat");
        map.put("r_monthly_lastweek", "0");
        map.put("due_ts", "2010-12-19T00:00:00");
        MapDataProvider repetitionDataProvider = new MapDataProvider(map);
        Repetition repetition = RequestDataConverter.convertRepetition(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, repetitionDataProvider,
                TestDateTimes.moscow(2010, 11, 1, 0, 0));

        Repetition repetitonData = new Repetition();
        repetitonData.setType(RegularRepetitionRule.DAILY);
        repetitonData.setREach(1);
        repetitonData.setDueTs(TestDateTimes.moscow(2010, 12, 19, 0, 0));

        Repetition changes = RepetitionHelper.INSTANCE.findChanges(repetitonData, repetition);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void infinityDailyRepetiton() {
        MapF<String, String> map = Cf.hashMap();
        map.put("type", "daily");
        map.put("r_each", "1");
        map.put("due_ts", "");
        MapDataProvider repetitionDataProvider = new MapDataProvider(map);
        Repetition repetition = RequestDataConverter.convertRepetition(
                TimeUtils.EUROPE_MOSCOW_TIME_ZONE, repetitionDataProvider,
                TestDateTimes.moscow(2010, 11, 1, 12, 0));

        Repetition repetitonData = new Repetition();
        repetitonData.setType(RegularRepetitionRule.DAILY);
        repetitonData.setREach(1);
        repetitonData.setDueTsNull();

        Repetition changes = RepetitionHelper.INSTANCE.findChanges(repetitonData, repetition);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void todoList() {
        MapF<String, String> map = Cf.hashMap();
        map.put("todo-list-id", "1");
        map.put("title", "deals");
        map.put("description", "it's my deals list");
        MapDataProvider todoListDataProvider = new MapDataProvider(map);

        TodoList todoList = RequestDataConverter.convertTodoList(todoListDataProvider);
        TodoList todoListData = new TodoList();
        todoListData.setTitle("deals");
        todoListData.setDescription("it's my deals list");
        todoListData.setExternalId(todoList.getExternalId());

        TodoList changes = TodoListHelper.INSTANCE.findChanges(todoListData, todoList);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void todoListWithEmptyDescription() {
        MapF<String, String> map = Cf.hashMap();
        map.put("todo-list-id", "1");
        map.put("title", "deals");
        map.put("description", "");
        MapDataProvider todoListDataProvider = new MapDataProvider(map);

        TodoList todoList = RequestDataConverter.convertTodoList(todoListDataProvider);
        TodoList todoListData = new TodoList();
        todoListData.setTitle("deals");
        todoListData.setDescription("");
        todoListData.setExternalId(todoList.getExternalId());

        TodoList changes = TodoListHelper.INSTANCE.findChanges(todoListData, todoList);
        Assert.assertTrue(changes.isEmpty());
    }

}
