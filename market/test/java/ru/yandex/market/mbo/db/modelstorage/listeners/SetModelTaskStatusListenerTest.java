package ru.yandex.market.mbo.db.modelstorage.listeners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.catalogue.model.UpdateAttributesEventParams;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 24.09.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class SetModelTaskStatusListenerTest {
    private static final Long MODEL_ID = 10L;
    private static final Long USER_ID = 1L;

    private static long paramIdsSeq = 1;

    @Mock
    private TaskTracker taskTracker;

    private AutoUser autoUser;

    @Mock
    private StatusManager statusManager;

    private SetModelTaskStatusListener setModelTaskStatusListener;

    @Mock
    private CommonModel modelWithAllParams;

    @Before
    public void init() {
        when(modelWithAllParams.getId()).thenReturn(MODEL_ID);
        when(modelWithAllParams.getSingleParameterValue(anyString())).thenAnswer(invocationOnMock -> {
            String xsl = invocationOnMock.getArgument(0);
            return new ParameterValue(paramIdsSeq++, xsl, Param.Type.BOOLEAN,
                ParameterValue.ValueBuilder.newBuilder().setBooleanValue(true));
        });
        autoUser = new AutoUser(1);
        setModelTaskStatusListener = new SetModelTaskStatusListener();
        setModelTaskStatusListener.setTaskTracker(taskTracker);
        setModelTaskStatusListener.setAutoUser(autoUser);
        setModelTaskStatusListener.setStatusManager(statusManager);
    }

    @Test
    public void testDifferentTaskId() {
        Task task = new Task(100L, 1000L, 101L, Status.TASK_IN_PROCESS);
        runWithTask(task);
        verifyZeroInteractions(statusManager);
    }

    @Test
    public void testNormTaskId() {
        Task task = new Task(100L, MODEL_ID, 101L, Status.TASK_IN_PROCESS);

        when(taskTracker.getTaskListByTask(eq(task.getId()))).thenReturn(
            new TaskList(task.getTaskListId(), 1L, 0, USER_ID, Status.TASK_LIST_IN_PROCESS,
                TaskType.FILL_MODIFICATION, 1, null)
        );

        runWithTask(task);
        verify(statusManager).changeTaskStatus(eq(USER_ID), eq(task.getId()), any(Status.class));
    }

    @Test
    public void testDiffType() {
        Task task = new Task(100L, MODEL_ID, 101L, Status.TASK_IN_PROCESS);

        when(taskTracker.getTaskListByTask(eq(task.getId()))).thenReturn(
            new TaskList(task.getTaskListId(), 1L, 0, USER_ID, Status.TASK_LIST_IN_PROCESS,
                TaskType.FIX_ERROR, 1, null)
        );

        runWithTask(task);
        verifyZeroInteractions(statusManager);
    }

    private void runWithTask(Task task) {
        when(taskTracker.getTask(anyLong())).thenReturn(task);

        UpdateAttributesEventParams event = new UpdateAttributesEventParams(
            new ModelChanges(modelWithAllParams, modelWithAllParams), USER_ID, task.getId(),
            Collections.emptyMap(), Collections.emptyMap()
        );

        setModelTaskStatusListener.handleEvent(event);
    }
}
