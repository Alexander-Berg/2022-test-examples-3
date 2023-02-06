package ru.yandex.market.mbo.tt;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.tt.model.Priority;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.utils.db.TransactionChainCall;

public class TaskTrackerMock implements TaskTracker {
    @Override
    public long createTaskList(TaskType type, long categoryId, Collection<Long> contentIds) {
        return 0;
    }

    @Override
    public Pair<Long, List<Long>> createTaskListWithTasks(TaskType type, long guruCategoryId,
                                                          Collection<Long> contentIds) {
        return null;
    }

    @Override
    public void updateTaskListPriority(long taskListId, Priority priority) {

    }

    @Override
    public void updateTaskListPeriod(long taskListId, int period) {

    }

    @Override
    public void updatePriorityTime(long taskListId, Calendar priorityTime) {

    }

    @Override
    public int getDefaultPeriod(long guruCategoryId, TaskType taskType) {
        return 0;
    }

    @Override
    public List<Long> getTasksIds(long taskListId) {
        return null;
    }

    @Override
    public List<Task> getTasks(long taskListId) {
        return null;
    }

    @Override
    public List<Task> getTasks(Collection<Long> taskIds) {
        return null;
    }

    @Override
    public TaskList getTaskList(long taskListId) {
        return null;
    }

    @Override
    public List<TaskList> getTaskLists(Collection<Long> taskListIds) {
        return null;
    }

    @Override
    public TaskList getTaskListByTask(long taskId) {
        return null;
    }

    @Override
    public Task getTask(long taskId) {
        return null;
    }

    @Override
    public List<TaskList> getTaskListByContentId(long contentId, Integer... types) {
        return null;
    }

    @Override
    public List<Task> getTaskByContentId(long contentId, Integer... types) {
        return null;
    }

    @Override
    public List<Task> getTaskByContentIds(Collection<Long> contentIds, Integer... types) {
        return null;
    }

    @Override
    public void toUserList(long userId, TaskList taskList, long operatorId, Priority priority) {

    }

    @Override
    public void toUserList(long userId, long taskListId, long operatorId, Priority priority) {

    }

    @Override
    public void toUserList(long userId, long taskListId, long operatorId, int period, Priority priority) {

    }

    @Override
    public void cancelTask(long userId, long taskListId) {

    }

    @Override
    public long createTask(long taskListId, long contentId) {
        return 0;
    }

    @Override
    public List<TaskList> getTaskLists(TaskType taskType, Status status) {
        return null;
    }

    @Override
    public List<TaskList> getTaskLists(TaskType taskType, Status status, long userId) {
        return null;
    }

    @Override
    public List<TaskList> getTaskLists(TaskType taskType, Status status, Priority priority, int daysAfterDeadline) {
        return null;
    }

    @Override
    public List<TaskList> getTaskLists(TaskType taskType, Collection<Status> statuses, int daysAfterDeadline) {
        return null;
    }

    @Override
    public TaskType getTaskListType(long taskListId) {
        return null;
    }

    @Override
    public void cancelTasksByContent(long contentId, long userId) {

    }

    @Override
    public TransactionChainCall getTransactionChainCallOraPg() {
        return null;
    }
}
