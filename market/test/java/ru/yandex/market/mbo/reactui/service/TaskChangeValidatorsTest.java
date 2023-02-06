package ru.yandex.market.mbo.reactui.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.VisualService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.reactui.errors.ChangeOperatorTaskException;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.UserManager;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.security.MboRoles.VISUAL_OPERATOR;

@SuppressWarnings("checkstyle:magicNumber")
public class TaskChangeValidatorsTest {

    @InjectMocks
    private TaskChangeValidators taskChangeValidators;
    @Mock
    private AutoUser autoUser;
    @Mock
    private UserManager userManager;
    @Mock
    private VisualService visualService;
    @Mock
    private TovarTreeService tovarTreeService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void checkBusyTasksTestValid() throws ChangeOperatorTaskException {
        checkBusyTasks(1234, 1234);
    }

    @Test(expected = ChangeOperatorTaskException.class)
    public void checkBusyTasksTestInvalid() throws ChangeOperatorTaskException {
        checkBusyTasks(4321, 9876);
    }

    public void checkBusyTasks(long autoUserId, long ownerId) throws ChangeOperatorTaskException {
        when(autoUser.getId()).thenReturn(autoUserId);
        List<TaskList> taskLists = Collections.singletonList(
            new TaskList(123, 123, 0, ownerId, Status.INITIAL, TaskType.BARCODES_CHECK, 5,
                Date.valueOf(LocalDate.now()))
        );
        taskChangeValidators.checkBusyTasks(taskLists);
    }

    @Test
    public void checkTaskCategoryTestValid() throws ChangeOperatorTaskException {
        when(userManager.getOperatorCategories(anyLong())).thenReturn(Collections.singletonList(654321L));
        when(userManager.getRoles(anyLong())).thenReturn(Collections.singletonList(VISUAL_OPERATOR));
        final TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setGuruCategoryId(123);
        when(tovarTreeService.getCategoryByHid(anyLong())).thenReturn(tovarCategory);
        when(visualService.isVisualCategory(anyLong())).thenReturn(true);


        List<TaskList> taskLists = Collections.singletonList(
            new TaskList(123, 123, 0, 123, Status.INITIAL, TaskType.BARCODES_CHECK, 5, Date.valueOf(LocalDate.now()))
        );
        taskChangeValidators.checkTaskCategory(taskLists, 69856);
    }

    @Test(expected = ChangeOperatorTaskException.class)
    public void checkTaskCategoryTestInvalid() throws ChangeOperatorTaskException {
        when(userManager.getOperatorCategories(anyLong())).thenReturn(Collections.singletonList(654321L));
        when(userManager.getRoles(anyLong())).thenReturn(Collections.singletonList(VISUAL_OPERATOR));
        final TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setGuruCategoryId(123);
        when(tovarTreeService.getCategoryByHid(anyLong())).thenReturn(tovarCategory);
        when(visualService.isVisualCategory(anyLong())).thenReturn(true);


        List<TaskList> taskLists = Collections.singletonList(
            new TaskList(123, 123, 0, 123, Status.INITIAL, TaskType.MODEL_EXTRACTION_LEARNING, 5,
                Date.valueOf(LocalDate.now()))
        );
        taskChangeValidators.checkTaskCategory(taskLists, 69856);
    }
}
