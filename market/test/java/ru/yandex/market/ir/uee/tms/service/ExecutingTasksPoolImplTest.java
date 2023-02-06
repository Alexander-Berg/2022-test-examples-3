package ru.yandex.market.ir.uee.tms.service;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.scheduler.TaskQueue;
import ru.yandex.commune.bazinga.scheduler.TaskQueueName;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.BusinessTaskRecord;
import ru.yandex.market.ir.uee.tms.pojos.BusinessTaskType;
import ru.yandex.market.ir.uee.tms.repository.BusinessTaskRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ir.uee.tms.pojos.BusinessTaskType.EXCEL_TO_YT_WORKER_TASK;

public class ExecutingTasksPoolImplTest {

    private ExecutingTasksPoolImpl executingTasksPool;
    private final BusinessTaskRepository businessTaskRepository = Mockito.mock(BusinessTaskRepository.class);
    private final BazingaTaskManager bazingaTaskManager = Mockito.mock(BazingaTaskManager.class);

    @Before
    public void setUp() {
        executingTasksPool = new ExecutingTasksPoolImpl(businessTaskRepository, bazingaTaskManager, getTaskQueues(), 3);
        when(businessTaskRepository.getBusinessTasksByStatesForUpdate(anyList())).thenReturn(List.of(
                buildBusinessTaskRecord(EXCEL_TO_YT_WORKER_TASK),
                buildBusinessTaskRecord(EXCEL_TO_YT_WORKER_TASK),
                buildBusinessTaskRecord(EXCEL_TO_YT_WORKER_TASK),

                buildBusinessTaskRecord(BusinessTaskType.ENRICH_OFFERS_MAIN_META_TASK),
                buildBusinessTaskRecord(BusinessTaskType.ENRICH_OFFERS_MAIN_META_TASK)
        ));
    }

    @Test
    public void countFreeSlots() {
        final Map<String, Integer> countFreeSlots = executingTasksPool.countFreeSlots();
        assertEquals(Integer.valueOf(0), countFreeSlots.get("WorkerTask"));
        assertEquals(Integer.valueOf(4), countFreeSlots.get("MetaTask"));
    }

    private BusinessTaskRecord buildBusinessTaskRecord(BusinessTaskType type) {
        final BusinessTaskRecord record = new BusinessTaskRecord();
        record.setType(type.name());
        return record;
    }

    private ListF<TaskQueue> getTaskQueues() {
        ListF<TaskQueue> queues = Cf.arrayList();
        queues.add(new TaskQueue(new TaskQueueName("WorkerTask"), 1, 1));
        queues.add(new TaskQueue(new TaskQueueName("MetaTask"), 2, 2));
        return queues.makeReadOnly();
    }
}
