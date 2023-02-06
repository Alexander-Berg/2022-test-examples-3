package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.class)
public class CheckFillTaskCounterTest extends SimpleTaskCounterBaseTest {

    private static final long CHECKER_UID = 90503L;

    @Spy
    private CheckFillTaskCounter counter = new CheckFillTaskCounter();

    @Mock
    private TaskTracker taskTracker;

    private Random idGenerator = new Random(404);

    @Before
    @Override
    public void setUp() {
        getCounter().setTaskTracker(taskTracker);
        when(taskTracker.getTasksIds(anyLong())).thenReturn(Collections.singletonList(0L));
        super.setUp();
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleCheckTaskListsNotAllowed() {
        prepareManyTaskListsForEachTask();
        getCounter().doLoad(INTERVAL, tarifProvider);
    }

    @Test
    public void testSingleTaskListPerTaskBilled() {
        prepareSingleTaskListForEachTask();
        getCounter().doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(generateValidTasks().size())).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            createBilledEntityAction(PaidAction.CHECK_MODEL_CARD, 0, CATEGORY_ID, CHECKER_UID, CONTENT_ID_1,
                    AuditAction.EntityType.MODEL_GURU),
            createBilledEntityAction(PaidAction.CHECK_MODEL_CARD, 0, CATEGORY_ID, CHECKER_UID, CONTENT_ID_1,
                    AuditAction.EntityType.MODEL_GURU)
        );
    }

    /**
     * Для данного счётчика допустим только статус ACCEPTED.
     */
    @Override
    protected List<TTOperationCounter.TaskToPay> generateValidTasks() {
        return super.generateValidTasks().stream()
            .filter(task -> task.getStatus().equals(Status.TASK_ACCEPTED))
            .collect(Collectors.toList());
    }

    @Override
    protected TTOperationCounter getCounter() {
        return counter;
    }

    protected int getExpectedTaskTypeId() {
        return TaskType.CHECK_FILL_MODEL.getId();
    }

    private void prepareSingleTaskListForEachTask() {
        generateValidTasks().forEach(task -> {
            long taskListId = task.getTaskListId();
            when(taskTracker.getTaskListByContentId(taskListId, getExpectedTaskTypeId()))
                .thenReturn(Collections.singletonList(generateTaskList()));
        });
    }

    private void prepareManyTaskListsForEachTask() {
        generateValidTasks().forEach(task -> {
            long taskListId = task.getTaskListId();
            when(taskTracker.getTaskListByContentId(taskListId, getExpectedTaskTypeId()))
                .thenReturn(Arrays.asList(generateTaskList(), generateTaskList(), generateTaskList()));
        });
    }

    private TaskList generateTaskList() {
        return new TaskList(
            idGenerator.nextLong(),
            CATEGORY_ID,
            0,
            CHECKER_UID,
            Status.TASK_LIST_ACCEPTED,
            TaskType.CHECK_FILL_MODEL,
            0,
            null);
    }
}
