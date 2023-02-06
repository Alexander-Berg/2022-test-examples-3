package ru.yandex.market.mbo.tt.providers.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author eugenezag
 * @date 26.17.2017
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CheckModelFromVendorTaskProviderTest {

    @Mock
    private TaskTracker taskTracker;

    @Mock
    private StatusManager statusManager;

    @Mock
    private AutoUser autoUser;

    CheckModelFromVendorTaskProvider checkModelFromVendorTaskProvider;

    @Before
    public void setUp() throws Exception {
        checkModelFromVendorTaskProvider = new CheckModelFromVendorTaskProvider();
        checkModelFromVendorTaskProvider.setTaskTracker(taskTracker);
        checkModelFromVendorTaskProvider.setStatusManager(statusManager);
        checkModelFromVendorTaskProvider.setAutoUser(autoUser);
    }

    /**
     * В методе проверяется ситуация, когда для приходят задачи для уже существующих в базе
     * vendor_model_id. Не должны создаваться новые таски и таск-листы, должны обновляться
     * статусы существующих.
     */
    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void notCreateTaskForTest() {
        long taskListId = 36463055L;
        long categoryId = 4954975L;
        long userId = 1L;

        int firstTaskId = 36463056;
        long firstContentId = 100112522523L;
        int secondTaskId = 36463057;
        long secondContentId = 100112522870L;

        Task firstTask = new Task(firstTaskId, firstContentId, taskListId, Status.TASK_RETURNED);
        Task secondTask = new Task(secondTaskId, secondContentId, taskListId, Status.TASK_IN_PROCESS);

        List<Long> vendorModelIdsList = new ArrayList<>();
        vendorModelIdsList.add(firstContentId);
        vendorModelIdsList.add(secondContentId);

        List<Task> existingTasks = Arrays.asList(firstTask, secondTask);

        List<TaskList> taskListsToChangeStatus = Arrays.asList(
            new TaskList(
                taskListId,
                categoryId,
                4,
                0,
                Status.TASK_LIST_RETURNED,
                TaskType.CHECK_FILL_MODEL_FROM_VENDOR,
                0,
                null
            ));

        Mockito.when(taskTracker.getTaskByContentIds(vendorModelIdsList, TaskType.CHECK_FILL_MODEL_FROM_VENDOR.getId()))
               .thenReturn(existingTasks);
        Mockito.when(taskTracker.getTaskLists(Arrays.asList(taskListId)))
               .thenReturn(taskListsToChangeStatus);
        Mockito.when(
               taskTracker.createTaskList(TaskType.CHECK_FILL_MODEL_FROM_VENDOR, categoryId, vendorModelIdsList))
               .thenThrow(new RuntimeException("Метод не должен вызываться для пустого списка"));
        Mockito.when(taskTracker.getTasks(taskListId))
               .thenThrow(new RuntimeException("Метод не должен вызываться для пустого списка"));

        Mockito.when(statusManager.changeTaskStatus(userId, firstTaskId, Status.TASK_IN_PROCESS))
               .thenReturn(true);
        Mockito.when(statusManager.changeTaskListStatus(userId, taskListId, Status.TASK_LIST_IN_PROCESS))
               .thenReturn(true);

        Mockito.when(autoUser.getId()).thenReturn(userId);

        List<Task> result = checkModelFromVendorTaskProvider.createTasksFor(categoryId,
            vendorModelIdsList,
            Collections.emptyList());

        Assert.assertEquals(0, vendorModelIdsList.size()); //данные вендор-модели уже есть в базе
        Assert.assertEquals(0, result.size()); //новых задач не создается
    }

    /**
     * В методе проверяется ситуация, когда приходят новые vendor_model_id.
     * Должны создаваться новые таски.
     */
    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void createTaskForTest() {
        long taskListId = 36463055L;
        long categoryId = 4954975L;
        long userId = 1L;

        int firstTaskId = 36463056;
        long firstContentId = 100112522523L;
        int secondTaskId = 36463057;
        long secondContentId = 100112522870L;

        Task firstTask = new Task(firstTaskId, firstContentId, taskListId, Status.TASK_OPENED);
        Task secondTask = new Task(secondTaskId, secondContentId, taskListId, Status.TASK_OPENED);

        List<Long> vendorModelIdsList = new ArrayList<>();
        vendorModelIdsList.add(firstContentId);
        vendorModelIdsList.add(secondContentId);

        List<Task> existingTasks = Collections.emptyList();
        List<Task> newTasks = Arrays.asList(firstTask, secondTask);
        List<TaskList> taskListsToChangeStatus = Collections.emptyList();

        Mockito.when(taskTracker.getTaskByContentIds(vendorModelIdsList, TaskType.CHECK_FILL_MODEL_FROM_VENDOR.getId()))
               .thenReturn(existingTasks);
        Mockito.when(taskTracker.getTaskLists(Collections.emptyList()))
               .thenReturn(taskListsToChangeStatus);
        Mockito.when(taskTracker.createTaskList(TaskType.CHECK_FILL_MODEL_FROM_VENDOR, categoryId, vendorModelIdsList))
               .thenReturn(taskListId);
        Mockito.when(taskTracker.getTasks(taskListId))
               .thenReturn(newTasks);

        Mockito.when(statusManager.changeTaskStatus(userId, firstTaskId, Status.TASK_IN_PROCESS))
            .thenThrow(new RuntimeException("Создаем новые задачи. Не меняем статус у существующих."));
        Mockito.when(statusManager.changeTaskListStatus(userId, taskListId, Status.TASK_LIST_IN_PROCESS))
            .thenThrow(new RuntimeException("Создаем новые задачи. Не меняем статус у существующих."));

        Mockito.when(autoUser.getId()).thenReturn(userId);

        List<Task> createdTasks = checkModelFromVendorTaskProvider.createTasksFor(categoryId,
            vendorModelIdsList,
            Collections.emptyList());

        Assert.assertEquals(2, createdTasks.size()); //должно создаться две новые задачи
    }
}
