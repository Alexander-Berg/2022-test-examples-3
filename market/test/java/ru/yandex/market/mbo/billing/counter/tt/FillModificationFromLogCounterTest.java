package ru.yandex.market.mbo.billing.counter.tt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.AbstractBillingLoaderTest;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.billing.counter.tt.TTOperationCounter.TaskToPay;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.legacy.CheckGroupingLogManager;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.status.Status;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("checkstyle:magicnumber")
@RunWith(MockitoJUnitRunner.Silent.class)
public class FillModificationFromLogCounterTest extends AbstractBillingLoaderTest {

    private static final long TASK_ID1 = 9000593L;
    private static final long TASK_ID2 = 1044368L;
    private static final long TASK_ID3 = 6611379L;
    private static final long CATEGORY_ID = 9005L;
    private static final long EXPECTED_CONTENT_ID = 705829L;
    private static final long UID = 666L;
    private static final long UID1 = 86216L;
    private static final long UID2 = 16662L;

    @Mock
    private CheckGroupingLogManager checkGroupingLogManager;
    @Mock
    private TaskTracker taskTracker;
    @Spy
    private FillModificationFromLogCounter counter = new FillModificationFromLogCounter();

    @Before
    public void before() {
        super.setUp();
        counter.setTaskTracker(taskTracker);
        counter.setCheckGroupingLogManager(checkGroupingLogManager);
        counter.setBillingOperations(billingOperations);
    }

    @Test
    public void testGroupLogBilled() {
        initMockedServices();
        counter.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(3)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();
        assertThat(billed).containsExactlyInAnyOrder(
            createBilledAction(PaidAction.FILL_MODIFICATION_CARD, 0, CATEGORY_ID, UID),
            createBilledAction(PaidAction.FILL_MODIFICATION_CARD, 0, CATEGORY_ID, UID),
            createBilledAction(PaidAction.FILL_MODIFICATION_CARD, 0, CATEGORY_ID, UID)
        );
    }

    private void initMockedServices() {
        Timestamp now = Timestamp.from(ACTIONS_DATE.toInstant());
        doReturn(Arrays.asList(
            new TaskToPay(TASK_ID1, 1L, 0, CATEGORY_ID, UID1, Status.TASK_ACCEPTED, now),
            new TaskToPay(TASK_ID2, 2L, 0, CATEGORY_ID, UID2, Status.TASK_ACCEPTED, now),
            new TaskToPay(TASK_ID3, 3L, 0, CATEGORY_ID, UID2, Status.TASK_ACCEPTED_WITHOUT_CHECK, now)
        )).when(counter).getTasks(any());

        TaskList taskList = new TaskList(123L, CATEGORY_ID, 0, UID, Status.TASK_LIST_ACCEPTED, TaskType.LOG, 0, null);
        doReturn(taskList).when(taskTracker).getTaskList(EXPECTED_CONTENT_ID);

        Task anyTask = new Task(321L, EXPECTED_CONTENT_ID, 123L, Status.TASK_ACCEPTED);
        doReturn(anyTask).when(checkGroupingLogManager).getLogTask(anyLong());
    }
}
