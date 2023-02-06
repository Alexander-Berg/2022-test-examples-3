package ru.yandex.market.gutgin.tms.engine.schedule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.TaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Task;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * @author s-ermakov
 */
public class TaskSchedulerTest {

    private TaskScheduler taskScheduler;

    @Before
    public void setUp() {
        taskScheduler = new TaskScheduler(null, null);
        PipelineService pipelineService = Mockito.mock(PipelineService.class);
        Pipeline pipeline1 = new Pipeline();
        Pipeline pipeline2 = new Pipeline();
        pipeline1.setType(PipelineType.DATA_CAMP);
        pipeline2.setType(PipelineType.CW_YT_READER);
        Mockito.when(pipelineService.getPipeline(1L)).thenReturn(pipeline1);
        Mockito.when(pipelineService.getPipeline(2L)).thenReturn(pipeline2);
        taskScheduler.setPipelineService(pipelineService);
    }

    @Test
    public void canProcessDoesntThrowException() {
        for (TaskStatus status : TaskStatus.values()) {
            Task task = new Task();
            task.setStatus(status);
            task.setUpdateDate(Timestamp.from(Instant.now()));
            taskScheduler.canProcess(task);
        }
    }

    @Test
    public void testWhenNoActionAndLargeStartCountDynamicTimeIsCalculatedOk() {
        Task task = new Task();
        task.setPipelineId(1L);
        task.setStartCnt(Integer.MAX_VALUE - 10);
        task.setTaskAction(null);
        taskScheduler.setDelayAfterFailInSec(386547054789L);
        Assert.assertEquals(386547054780L, taskScheduler.getDynamicTaskDelayTimeInSec(task));
    }

    @Test
    public void testWhenNoActionAndDynamicDelayIsLargerThenDefaultThenDefaultIsReturned() {
        Task task = new Task();
        task.setPipelineId(1L);
        task.setStartCnt(Integer.MAX_VALUE - 10);
        task.setTaskAction(null);
        long delayAfterFail = 1800L;
        taskScheduler.setDelayAfterFailInSec(delayAfterFail);
        Assert.assertEquals(delayAfterFail, taskScheduler.getDynamicTaskDelayTimeInSec(task));
    }

    @Test
    public void testCwReaderDelay() {
        Task task = new Task();
        task.setPipelineId(2L);
        task.setStartCnt(0);
        task.setTaskAction(null);
        long defaultDelayAfterFail = 1800L;
        long startDelay = defaultDelayAfterFail / 2 ;
        taskScheduler.setDelayAfterFailInSec(defaultDelayAfterFail);
        Assert.assertEquals(startDelay, taskScheduler.getDynamicTaskDelayTimeInSec(task));
    }
}