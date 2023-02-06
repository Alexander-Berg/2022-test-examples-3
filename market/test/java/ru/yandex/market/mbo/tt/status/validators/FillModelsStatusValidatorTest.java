package ru.yandex.market.mbo.tt.status.validators;

import java.sql.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.legacy.TaskTrackerBeans;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.FillModelsStatusTransition;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.validators.enums.StatusValidationEnum;
import ru.yandex.market.mbo.user.UserManager;

@SuppressWarnings("checkstyle:MagicNumber")
public class FillModelsStatusValidatorTest {
    public static final TaskList NOT_FILL = new TaskList(1,
        1,
        0,
        1,
        Status.TASK_LIST_OPENED,
        TaskType.BARCODES_CHECK,
        20,
        new Date(System.currentTimeMillis()));

    public static final TaskList FILL_MODEL_OPENED = new TaskList(2,
        2,
        0,
        1,
        Status.TASK_LIST_OPENED,
        TaskType.FILL_MODEL,
        100, new Date(System.currentTimeMillis()));

    public static final TaskList FILL_MODEL_FINISHED = new TaskList(3,
        2,
        0,
        1,
        Status.TASK_LIST_FINISHED,
        TaskType.FILL_MODEL,
        100, new Date(System.currentTimeMillis()));


    private FillModelsStatusValidator validator;

    @Before
    public void setUp() {
        UserManager userManager = Mockito.mock(UserManager.class);
        TaskTracker taskTracker = Mockito.mock(TaskTracker.class);

        Mockito.when(taskTracker.getTaskList(1)).thenReturn(NOT_FILL);
        Mockito.when(taskTracker.getTaskList(2)).thenReturn(FILL_MODEL_OPENED);
        Mockito.when(taskTracker.getTaskList(3)).thenReturn(FILL_MODEL_FINISHED);

        Mockito.when(userManager.isAdmin(1L)).thenReturn(true);

        FillModelsStatusTransition fillModelsStatusTransition =
            new FillModelsStatusTransition(new TaskTrackerBeans(taskTracker,
            null,
            null,
            null,
            null,
            null,
            userManager));

        validator = new FillModelsStatusValidator(taskTracker, fillModelsStatusTransition);
    }

    @Test
    public void testNotFill() {
        Assert.assertEquals(validator.validateTaskListStatus(1,
            Status.TASK_LIST_OPENED,
            Status.TASK_LIST_IN_PROCESS,
            1), StatusValidationEnum.OK);
    }

    @Test
    public void testNotFinishStatuses() {
        Assert.assertEquals(validator.validateTaskListStatus(2,
            Status.TASK_LIST_OPENED,
            Status.TASK_LIST_IN_PROCESS,
            1), StatusValidationEnum.OK);
    }

    @Test
    public void testNotAllowedTransition() {
        Assert.assertEquals(validator.validateTaskListStatus(2,
            Status.TASK_LIST_OPENED,
            Status.TASK_LIST_ACCEPTED,
            1), StatusValidationEnum.NOT_ALLOWED_TRANSITION);
    }

    @Test
    public void testAllowedTransition() {
        Assert.assertEquals(validator.validateTaskListStatus(3,
            Status.TASK_LIST_FINISHED,
            Status.TASK_LIST_ACCEPTED,
            1), StatusValidationEnum.OK);
    }
}
