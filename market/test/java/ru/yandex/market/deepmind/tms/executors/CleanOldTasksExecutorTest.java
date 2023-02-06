package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskRecord;

/**
 * @author rbizhanov.
 */
public class CleanOldTasksExecutorTest extends DeepmindBaseDbTestClass {

    @Autowired
    private TaskQueueRepository taskQueueRepository;
    private CleanOldTasksExecutor executor;

    @Before
    public void setUp() throws Exception {
        executor = new CleanOldTasksExecutor(taskQueueRepository);
        Instant monthBefore = LocalDate
            .now()
            .minus(1, ChronoUnit.MONTHS)
            .minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();
        taskQueueRepository.insert(new TaskRecord()
            .setId(111L)
            .setTaskType("type")
            .setTaskDataVersion(1)
            .setTaskData("{}")
            .setCreated(monthBefore)
            .setNextRun(Instant.now()));
        taskQueueRepository.insert(new TaskRecord()
            .setId(222L)
            .setTaskType("type")
            .setTaskDataVersion(1)
            .setTaskData("{}")
            .setCreated(monthBefore)
            .setNextRun(Instant.now()));
        taskQueueRepository.insert(new TaskRecord()
            .setId(333L)
            .setTaskType("type")
            .setTaskDataVersion(1)
            .setTaskData("{}")
            .setCreated(Instant.now())
            .setNextRun(Instant.now()));
    }

    @Test
    public void dropOldRowsByCreatedTsTest() {
        int countBefore = taskQueueRepository.findAll().size();
        Assertions.assertThat(countBefore).isEqualTo(3);
        executor.execute();
        int countAfter = taskQueueRepository.findAll().size();
        Assertions.assertThat(countAfter).isEqualTo(1);
    }
}
