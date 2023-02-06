package ru.yandex.chemodan.app.psbilling.core.mocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.joda.time.Instant;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function1B;
import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.commune.bazinga.impl.JobId;
import ru.yandex.commune.bazinga.impl.OnetimeUtils;
import ru.yandex.commune.bazinga.impl.TaskId;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.commune.bazinga.scheduler.TaskCategory;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;

@RequiredArgsConstructor
public class BazingaTaskManagerMock extends BazingaTaskManagerStub {
    private final ApplicationContext applicationContext;
    public ListF<Tuple2<OnetimeTask, TaskParams>> tasksWithParams = Cf.arrayList();
    private boolean isNewTasksSuppressed = false;

    @Getter
    @Setter
    private ListF<Class> allowedTasks = Cf.arrayList();

    @Override
    public FullJobId schedule(OnetimeTask task, TaskCategory category, Instant date, int priority,
                              boolean forceRandomInId, Option<String> group, JobId jobId, Object session) {
        if (isNewTasksSuppressed || (allowedTasks.isNotEmpty() && !allowedTasks.containsTs(task.getClass()))) {
            return null;
        }

        if (!tasksWithParams.map(x -> OnetimeUtils.getActiveUniqueIdentifier(x._1))
                .containsTs(OnetimeUtils.getActiveUniqueIdentifier(task))) {
            tasksWithParams.add(Tuple2.tuple(task, new TaskParams(category, date, priority, forceRandomInId, group,
                    jobId, session)));
        }
        return super.schedule(task, category, date, priority, forceRandomInId, group, jobId, session);
    }

    public <TTaskType extends OnetimeTask> void executeTasks(Class<TTaskType> taskType) {
        ListF<Tuple2<TTaskType, TaskParams>> tasks = findTasks(taskType);
        executeTasks(applicationContext, tasks.stream().map(x -> x._1.id()).toArray(TaskId[]::new));
    }

    public void executeTasks() {
        executeTasks(applicationContext, tasksWithParams.stream().map(x -> x._1.id()).toArray(TaskId[]::new));
    }

    @Override
    public void executeTasks(ApplicationContext applicationContext, TaskId... taskIds) {
        super.executeTasks(applicationContext, taskIds);
        filterTaskQueue(super.tasksWithParams::containsTs);
    }

    public void executeTask(ApplicationContext applicationContext, OnetimeTask task) {
        ListF<OnetimeTask> leftTasks = super.tasksWithParams.filter(x -> !x.equals(task));
        super.tasksWithParams.clear();
        super.tasksWithParams.add(task);
        try {
            super.executeTasks(applicationContext);
        } finally {
            super.tasksWithParams.clear();
            super.tasksWithParams.addAll(leftTasks);
            filterTaskQueue(super.tasksWithParams::containsTs);
        }
    }


    public void executeWhileGotTasks(ApplicationContext applicationContext, TaskId... taskIds) {
        while (tasksWithParams.isNotEmpty()) {
            executeTasks(applicationContext, taskIds);
        }
    }

    @SuppressWarnings("unchecked")
    public <TTaskType extends OnetimeTask> ListF<Tuple2<TTaskType, BazingaTaskManagerMock.TaskParams>> findTasks(Class<TTaskType> taskType) {
        return tasksWithParams
                .filter(x -> x._1.getClass().isAssignableFrom(taskType))
                .map(x -> Tuple2.tuple((TTaskType) x._1, x._2));
    }

    public void filterTaskQueue(Function1B<OnetimeTask> filterFunc) {
        tasksWithParams = Cf.toArrayList(tasksWithParams.filter(x -> filterFunc.apply(x._1)));

        IteratorF<OnetimeTask> tasksIterator = super.tasksWithParams.iterator();
        while (tasksIterator.hasNext()) {
            OnetimeTask taskAsParamHolder = tasksIterator.next();
            if (filterFunc.apply(taskAsParamHolder)) {
                continue;
            }

            tasksIterator.remove();
        }
    }

    public void suppressNewTasksAdd() {
        isNewTasksSuppressed = true;
    }

    public void allowNewTasksAdd() {
        isNewTasksSuppressed = false;
    }

    @Override
    public void clearTasks() {
        tasksWithParams.clear();
        super.clearTasks();
    }

    public void reset() {
        clearTasks();
        allowNewTasksAdd();
        setAllowedTasks(Cf.list());
    }

    @AllArgsConstructor
    @Getter
    public static class TaskParams {
        TaskCategory category;
        Instant date;
        int priority;
        boolean forceRandomInId;
        Option<String> group;
        JobId jobId;
        Object session;

    }
}
