package ru.yandex.market.mbo.tt.status.validators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.legacy.CheckGroupingLogManager;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.validators.enums.StatusValidationEnum;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * @author galaev@yandex-team.ru
 * @since 03/09/2018.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class LogAcceptedStatusValidatorTest {

    private TaskTracker taskTracker;
    private CheckGroupingLogManager checkGroupingLogManager;

    private LogAcceptedStatusValidator validator;

    @Before
    public void setUp() throws Exception {
        checkGroupingLogManager = Mockito.mock(CheckGroupingLogManager.class);
        taskTracker = Mockito.mock(TaskTracker.class);
        Mockito.when(taskTracker.getTaskList(anyLong())).thenReturn(getTaskList());
        Mockito.when(checkGroupingLogManager.getCheckGroupLogTaskList(anyLong()))
            .thenReturn(1L);
        Mockito.when(checkGroupingLogManager.getCheckModificationTask(anyLong()))
            .thenReturn(Collections.singletonList(new Task(1, 1, 1, Status.TASK_ACCEPTED)));

        validator = new LogAcceptedStatusValidator(taskTracker, checkGroupingLogManager);
    }

    @Test
    public void testValidateTaskStatus() {
        boolean result = validator.validateTaskStatus(1, Status.TASK_ACCEPTED, Status.TASK_CHECKED_NO_DATA, 1);
        assertThat(result).isTrue(); // always true
    }

    @Test
    public void testValidTaskListStatuses() {
        Mockito.when(taskTracker.getTasks(anyLong())).thenReturn(getTasks(true));
        StatusValidationEnum result = validator
            .validateTaskListStatus(1, Status.TASK_LIST_FINISHED, Status.TASK_LIST_ACCEPTED, 1);
        assertThat(result).isEqualTo(StatusValidationEnum.OK);
    }

    @Test
    public void testInValidTaskListStatuses() {
        Mockito.when(taskTracker.getTasks(anyLong())).thenReturn(getTasks(false));
        StatusValidationEnum result = validator
            .validateTaskListStatus(1, Status.TASK_LIST_FINISHED, Status.TASK_LIST_ACCEPTED, 1);
        assertThat(result).isEqualTo(StatusValidationEnum.NOT_CORRECT_STATUSES);
    }

    private TaskList getTaskList() {
        return new TaskList(1, 1, 1, 1, Status.TASK_LIST_FINISHED,
            TaskType.LOG, 1, new Date(System.currentTimeMillis()));
    }

    public List<Task> getTasks(boolean validStatuses) {
        List<Task> tasks = new ArrayList<>();
        if (validStatuses) {
            tasks.add(new Task(1, 1, 1, Status.TASK_CANCELED));
            tasks.add(new Task(2, 2, 1, Status.TASK_ACCEPTED));
            tasks.add(new Task(3, 3, 1, Status.TASK_ACCEPTED_WITHOUT_CHECK));
            tasks.add(new Task(4, 4, 1, Status.TASK_NO_DATA));
            tasks.add(new Task(5, 5, 1, Status.TASK_CHECKED_NO_DATA));
        } else {
            tasks.add(new Task(1, 1, 1, Status.TASK_IN_PROCESS));
            tasks.add(new Task(2, 2, 1, Status.TASK_FINISHED));
            tasks.add(new Task(3, 3, 1, Status.TASK_OPENED));
        }
        return tasks;
    }
}
