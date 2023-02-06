package ru.yandex.market.mbo.mdm.common.masterdata.services.monitoring;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroupUpdateTask;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.taskqueue.TaskQueueHandlerRegistry;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.utils.MdmProperties.TASK_QUEUE_MAX_RUNNING_TIME;

public class TaskQueueMonitoringTest extends MdmBaseDbTestClass {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private StorageKeyValueService keyValueService;

    private TaskQueueMonitoring taskQueueMonitoring;
    private TaskQueueRepository taskQueueRepository;
    private ObjectMapper objectMapper;
    private ComplexMonitoring complexMonitoring;

    private final String MONITORING_NAME = "TaskQueueMonitoring";
    private final String taskqueueTablesSchema = "mdm";

    @Before
    public void setUp() throws Exception {
        complexMonitoring = new ComplexMonitoring();
        objectMapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        taskQueueRepository = new TaskQueueRepository(jdbcTemplate, transactionTemplate, taskqueueTablesSchema);
        taskQueueMonitoring = new TaskQueueMonitoring(complexMonitoring, taskQueueRepository, keyValueService);
    }

    @Test
    public void whenThereIsFailedTasksMonitoringIsCritical() throws JsonProcessingException {
        MdmGoodGroupUpdateTask task = new MdmGoodGroupUpdateTask();
        TaskRecord failedTask = new TaskRecord()
            .setLockName(null)
            .setTaskType(TaskQueueHandlerRegistry.getTaskType(task))
            .setTaskData(objectMapper.writeValueAsString(task))
            .setTaskDataVersion(task.getSchemaVersion())
            .setNextRun(Instant.now())
            .setTaskState(TaskRecord.TaskState.FAILED);

        taskQueueRepository.insert(failedTask);
        taskQueueMonitoring.monitor();
        assertThat(complexMonitoring.getResult(MONITORING_NAME).getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
    }

    @Test
    public void whenThereIsFrozenTasksMonitoringIsCritical() throws JsonProcessingException {
        MdmGoodGroupUpdateTask task = new MdmGoodGroupUpdateTask();
        TaskRecord runningTask = new TaskRecord()
            .setLockName(null)
            .setTaskType(TaskQueueHandlerRegistry.getTaskType(task))
            .setTaskData(objectMapper.writeValueAsString(task))
            .setTaskDataVersion(task.getSchemaVersion())
            .setNextRun(Instant.now().minusSeconds(1))
            .setTaskState(TaskRecord.TaskState.ACTIVE)
            .setCreated(Instant.now().minus(30, ChronoUnit.MINUTES));

        taskQueueRepository.insert(runningTask);
        taskQueueMonitoring.monitor();
        assertThat(complexMonitoring.getResult(MONITORING_NAME).getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
    }

    @Test
    public void whenChangeMaxRunningTimeTimingChange() throws JsonProcessingException {
        MdmGoodGroupUpdateTask task = new MdmGoodGroupUpdateTask();
        TaskRecord runningTask = new TaskRecord()
            .setLockName(null)
            .setTaskType(TaskQueueHandlerRegistry.getTaskType(task))
            .setTaskData(objectMapper.writeValueAsString(task))
            .setTaskDataVersion(task.getSchemaVersion())
            .setNextRun(Instant.now().minusSeconds(1))
            .setTaskState(TaskRecord.TaskState.ACTIVE)
            .setCreated(Instant.now().minus(30, ChronoUnit.MINUTES));

        taskQueueRepository.insert(runningTask);
        taskQueueMonitoring.monitor();
        assertThat(complexMonitoring.getResult(MONITORING_NAME).getStatus()).isEqualTo(MonitoringStatus.CRITICAL);

        keyValueService.putValue(TASK_QUEUE_MAX_RUNNING_TIME, 100);
        taskQueueMonitoring.monitor();
        assertThat(complexMonitoring.getResult(MONITORING_NAME).getStatus()).isEqualTo(MonitoringStatus.OK);
    }
}
