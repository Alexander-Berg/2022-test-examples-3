package ru.yandex.market.wms.scheduler.service.clean;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.model.enums.TaskStatus;
import ru.yandex.market.wms.common.model.enums.TaskType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CleanReplenishmentOrderTaskServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_EXECUTION_TIME_LIMIT = 10000;

    @MockBean
    @Autowired
    private TaskDetailDao taskDetailDao;
    @MockBean
    @Autowired
    private DbConfigService dbConfigService;

    @Autowired
    private CleanReplenishmentOrderTaskService cleanReplenishmentOrderTaskService;

    @Test
    public void shouldDontCleanTasks() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_ENABLED"), anyInt(), anyInt(), anyInt())).thenReturn(0);

        cleanReplenishmentOrderTaskService.execute();

        verifyNoInteractions(taskDetailDao);
    }

    @Test
    public void shouldCleanTasks() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_ENABLED"), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_SAMPLE_SIZE"), anyInt(), anyInt(), anyInt())).thenReturn(2);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_BATCH_SIZE"), anyInt(), anyInt(), anyInt())).thenReturn(10);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_SLEEP_TIME"), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("DEL_REPLENISHMENT_TASKS_EXEC_TIME"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_EXECUTION_TIME_LIMIT);

        int expectedTasksCount = 10;
        List<String> list = Stream.iterate(0, i -> i + 1)
                .map(Object::toString)
                .limit(expectedTasksCount)
                .collect(Collectors.toList());
        when(taskDetailDao.getSerialKeysByStatusAndType(eq(TaskStatus.PENDING),
                eq(TaskType.REPLENISHMENT_ORDER), eq(10))).thenReturn(list).thenReturn(List.of());
        when(taskDetailDao.deleteTasksBySerialKey(anyList())).thenReturn(2);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_EXECUTION_TIME_LIMIT;
        String actualResult = cleanReplenishmentOrderTaskService.execute();
        String expectedResult = getResult(expectedTasksCount, 0, stopExecutionTime);

        verify(taskDetailDao, times(5)).deleteTasksBySerialKey(anyList());

        assertEquals(expectedResult, actualResult);
    }

    private String getResult(int deleteRowsCount, int failedAttemptsNumber, long stopExecutionTime) {
        return String.format("Records clean: TASKDETAIL %d%s%s", deleteRowsCount,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
