package ru.yandex.market.markup2.workflow.metrics;

import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.dao.TaskGroupMetricsPersister;
import ru.yandex.market.markup2.entries.group.ITaskGroupMetricsData;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.group.TaskGroupMetrics;
import ru.yandex.market.markup2.entries.group.TaskGroupMetricsDataStub;
import ru.yandex.market.markup2.entries.type.TaskTypeInfo;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author galaev@yandex-team.ru
 * @since 17/07/2017.
 */
public class MetricsLoggerTest {
    private static final int TYPE_INFO_ID = 1;

    private MetricsLogger metricsLogger;
    private TasksCache tasksCache;
    private TaskTypeInfo typeInfo;

    @Before
    public void setUp() throws IOException {
        tasksCache = Mockito.mock(TasksCache.class);

        TaskGroupMetricsPersister metricsPersister = Mockito.mock(TaskGroupMetricsPersister.class);
        Mockito.when(metricsPersister.writeMetricsData(Mockito.anyInt(), Mockito.any()))
            .thenAnswer(invocation -> "{\"test_metric\": 1}");

        metricsLogger = new MetricsLogger();
        metricsLogger.setMetricsPersister(metricsPersister);
        ReflectionTestUtils.setField(metricsLogger, "executorService", Mockito.mock(ScheduledExecutorService.class));

        typeInfo = new TaskTypeInfo(TYPE_INFO_ID, "", "", 1, 1,
            Pipes.SEQUENTIALLY, TaskDataUniqueStrategy.TYPE_CATEGORY);
    }

    @Test
    public void testWithNewMetrics() throws IOException {
        TaskGroupMetrics<ITaskGroupMetricsData> metrics = new TaskGroupMetrics<>(TYPE_INFO_ID,
            new TaskGroupMetricsDataStub());

        List<String> logRecords = testMetricsLogging(metrics);

        Assert.assertEquals(1, logRecords.size());
        Assert.assertTrue(metrics.getLoggingTime() > 0);
    }

    @Test
    public void testWithAlreadyLoggedMetrics() throws IOException {
        long loggingTime = new Date().getTime();
        TaskGroupMetrics<ITaskGroupMetricsData> metrics = new TaskGroupMetrics<>(TYPE_INFO_ID,
            new TaskGroupMetricsDataStub(),
            loggingTime);

        List<String> logRecords = testMetricsLogging(metrics);

        Assert.assertTrue(logRecords.isEmpty());
    }

    @Test
    public void testWithNullMetrics() throws IOException {
        List<String> logRecords = testMetricsLogging(null);

        Assert.assertTrue(logRecords.isEmpty());
    }

    private List<String> testMetricsLogging(TaskGroupMetrics<ITaskGroupMetricsData> metrics) {
        List<String> logRecords = new ArrayList<>();

        Map<Integer, Logger> loggers = new HashMap<>();
        Logger logger = Mockito.mock(Logger.class);
        Mockito.doAnswer(invocation -> {
            String logRecord = invocation.getArgument(0);
            logRecords.add(logRecord);
            return null;
        }).when(logger).info(Mockito.anyString());
        loggers.put(TYPE_INFO_ID, logger);
        ReflectionTestUtils.setField(metricsLogger, "loggers", loggers);

        Mockito.when(tasksCache.getAllTaskConfigGroups())
            .thenAnswer(invocation -> {
                TaskConfigGroupInfo groupInfo = new TaskConfigGroupInfo.Builder()
                    .setTypeInfo(typeInfo)
                    .setMetrics(metrics)
                    .build();
                return Collections.singletonList(groupInfo);
            });
        metricsLogger.setTasksCache(tasksCache);

        ReflectionTestUtils.invokeMethod(metricsLogger, "tryLogMetrics");

        return logRecords;
    }
}
