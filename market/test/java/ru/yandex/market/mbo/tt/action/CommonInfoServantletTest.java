package ru.yandex.market.mbo.tt.action;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.common.framework.core.FullErrorInfo;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CommonInfoServantletTest {

    private CommonInfoServantlet servantlet;

    @Mock
    private TaskTracker taskTracker;

    @Mock
    private ServRequest req;

    @Mock
    private ServResponse resp;

    @Mock
    private Task task;

    @Mock
    private TaskList taskList;

    @Before
    public void setUp() {
        servantlet = new CommonInfoServantlet(taskTracker);
    }

    @Test
    public void testQueryByTaskId() {
        when(req.getParamAsLong(anyString(), anyLong()))
            .thenReturn(1L)
            .thenReturn(2L);
        when(task.getTaskListId()).thenReturn(2L);
        when(taskTracker.getTask(1L)).thenReturn(task);
        when(taskTracker.getTaskList(2L)).thenReturn(taskList);

        servantlet.process(req, resp);

        verify(resp, times(1)).addData(task);
        verify(resp, times(1)).addData(taskList);
    }

    @Test
    public void testQueryByTaskListId() {
        when(req.getParamAsLong(anyString(), anyLong()))
            .thenReturn(0L)
            .thenReturn(2L);
        when(taskTracker.getTaskList(2L)).thenReturn(taskList);

        servantlet.process(req, resp);

        verify(resp, times(1)).addData(taskList);
    }

    @Test
    public void testTaskListMissing() {
        when(req.getParamAsLong(anyString(), anyLong()))
            .thenReturn(0L)
            .thenReturn(2L);
        when(taskTracker.getTaskList(2L)).thenReturn(TaskList.FAKE_TASK_LIST);

        servantlet.process(req, resp);

        verify(resp, times(1)).addErrorInfo(any(FullErrorInfo.class));
    }
}
