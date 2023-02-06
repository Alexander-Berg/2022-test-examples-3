package ru.yandex.market.mbo.reactui.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.reactui.errors.ChangeOperatorTaskException;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.tt.status.validators.enums.StatusValidationEnum;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicNumber")
public class TaskChangeServiceImplTest {

    @InjectMocks
    private TaskChangeServiceImpl taskChangeService;
    @Mock
    private TaskTracker taskTracker;
    @Mock
    private StatusManager statusManager;
    @Mock
    private TaskChangeValidators taskChangeValidators;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(taskTracker.getTaskLists(anyList())).thenReturn(Collections.singletonList(
            new TaskList(123, 123, 1, 123, Status.INITIAL, TaskType.BARCODES_CHECK, 2, Date.valueOf(LocalDate.now()))
        ));
        doThrow(ChangeOperatorTaskException.class).when(taskChangeValidators).checkBusyTasks(anyList());
    }

    @Test
    public void setTaskListStatusTestValid() throws ChangeOperatorTaskException {
        when(statusManager.changeTaskListStatusWithResult(anyLong(), anyLong(), any(Status.class)))
            .thenReturn(StatusValidationEnum.OK);
        taskChangeService.setTaskListStatus(123, Status.TASK_LIST_ACCEPTED, Arrays.asList(1L, 2L, 3L));
    }

    @Test(expected = ChangeOperatorTaskException.class)
    public void setTaskListStatusTestInvalid() throws ChangeOperatorTaskException {
        when(statusManager.changeTaskListStatusWithResult(anyLong(), anyLong(), any(Status.class)))
            .thenReturn(StatusValidationEnum.NOT_CORRECT_STATUSES);
        taskChangeService.setTaskListStatus(123, Status.TASK_LIST_ACCEPTED, Arrays.asList(1L, 2L, 3L));
    }

    @Test(expected = ChangeOperatorTaskException.class)
    public void moveTaskListToAnotherOperatorTestBusyTask() throws ChangeOperatorTaskException {
        taskChangeService.moveTaskListToAnotherOperator(123, new User(), false, Arrays.asList(1L, 2L, 3L));
    }

    @Test
    public void moveTaskListToAnotherOperatorValid() throws ChangeOperatorTaskException {
        taskChangeService.moveTaskListToAnotherOperator(123, new User(), true, Arrays.asList(1L, 2L, 3L));
    }
}
