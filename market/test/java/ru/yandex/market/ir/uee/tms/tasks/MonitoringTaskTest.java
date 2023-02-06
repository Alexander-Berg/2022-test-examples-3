package ru.yandex.market.ir.uee.tms.tasks;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.ir.uee.jooq.generated.enums.TaskPriority;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.BusinessTaskRecord;
import ru.yandex.market.ir.uee.model.UserRunState;
import ru.yandex.market.ir.uee.model.UserRunType;
import ru.yandex.market.ir.uee.tms.pojos.BusinessTaskState;
import ru.yandex.market.ir.uee.tms.pojos.PriorityLimit;
import ru.yandex.market.ir.uee.tms.repository.BusinessTaskRepository;
import ru.yandex.market.ir.uee.tms.repository.MonitoringRepository;
import ru.yandex.market.ir.uee.tms.repository.UserRunRepository;
import ru.yandex.market.ir.uee.tms.service.SolomonPusherService;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MonitoringTaskTest {

    private MonitoringTask monitoringTask;
    private BusinessTaskRepository businessTaskRepository = mock(BusinessTaskRepository.class);
    private UserRunRepository userRunRepository = mock(UserRunRepository.class);
    private SolomonPusherService solomonPusherService = mock(SolomonPusherService.class);
    private MonitoringRepository monitoringRepository = mock(MonitoringRepository.class);

    @Captor
    private ArgumentCaptor<List<Sensor>> sensorsCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(businessTaskRepository.getBusinessTaskByStates(anyList())).thenReturn(List.of(
                createBusinessTaskRecord(BusinessTaskState.PLANNED, OffsetDateTime.now().minusMinutes(1)),
                createBusinessTaskRecord(BusinessTaskState.PLANNED, OffsetDateTime.now().minusMinutes(2)),
                createBusinessTaskRecord(BusinessTaskState.STARTED, OffsetDateTime.now().minusMinutes(5))
        ));
        when(monitoringRepository.getUsageAccountQuota()).thenReturn(List.of(
                new MonitoringRepository.AccountPriorityLimit("Market-IR",
                        Map.of("default", new PriorityLimit(10, TaskPriority.LOW)),
                        Map.of("default", new PriorityLimit(12, TaskPriority.LOW)))
        ));
        when(userRunRepository.getRecentAbandonedUserRunsCount(any(), any())).thenReturn(1);
        when(monitoringRepository.getActiveUserRunWithRowCount()).thenReturn(
                List.of(
                        createUserRunPojo(UserRunState.RUNNING, OffsetDateTime.now().minusMinutes(10), 100_000,
                                "default", "user1"),
                        createUserRunPojo(UserRunState.RUNNING, OffsetDateTime.now().minusMinutes(5), 100_000,
                                "default", "user1"),
                        createUserRunPojo(UserRunState.RUNNING, OffsetDateTime.now().minusMinutes(30), 1_000_000,
                                "Market-IR", "user2")
                )
        );

        when(monitoringRepository.getRunningUserRunPriority()).thenReturn(Map.of(
                TaskPriority.NORMAL, 15,
                TaskPriority.LOW, 20
        ));
        monitoringTask = new MonitoringTask(businessTaskRepository, solomonPusherService, monitoringRepository, 15,
                userRunRepository);
    }

    private BusinessTaskRecord createBusinessTaskRecord(BusinessTaskState state, OffsetDateTime startTime) {
        BusinessTaskRecord record = new BusinessTaskRecord();
        record.setState(state.name());
        record.setStartTime(startTime);
        record.setLastAction(startTime);
        return record;
    }

    private MonitoringRepository.UserRunWithRowCount createUserRunPojo(UserRunState state, OffsetDateTime startTime,
                                                                       Integer rowCount,
                                                                       String account,
                                                                       String userName) {
        return new MonitoringRepository.UserRunWithRowCount(
                startTime, rowCount, UserRunType.UC, account, TaskPriority.NORMAL, userName);
    }

    @Test
    public void execute() throws Exception {
        monitoringTask.execute(null);

        verify(solomonPusherService).asyncPush(anyMap(), sensorsCaptor.capture());
        List<Sensor> sensors = sensorsCaptor.getValue();

        assertEquals(24, sensors.size());
        assertSensorsByState(sensors, BusinessTaskState.PLANNED, 2, 90_000, 120_000);
        assertSensorsByState(sensors, BusinessTaskState.STARTED, 1, 300_000, 300_000);
        assertSensorsByAccountId(sensors, "default", 600.0, 450.0, 166.6);
        assertSensorsByAccountId(sensors, "Market-IR", 1_800.0, 1_800.0, 555.5);
        assertAccountQuotasSensor(sensors, "rest", 10);
        assertAccountQuotasSensor(sensors, "max", 12);
        asserUserRunPriority(sensors, TaskPriority.NORMAL, 15);
        asserUserRunPriority(sensors, TaskPriority.LOW, 20);
        assertSensorsByUser(sensors, "user1", 600.0, 450.0, 166.6);
        assertSensorsByUser(sensors, "user2", 1_800.0, 1_800.0, 555.5);
    }

    private void asserUserRunPriority(List<Sensor> sensors, TaskPriority normal, int count) {
        sensors.stream()
                .filter(s -> normal.name().equals(s.labels.get("userRunPriority")))
                .findFirst()
                .ifPresent(sensor -> assertEquals(count, sensor.value, 0.00001));
    }

    private void assertAccountQuotasSensor(List<Sensor> sensors, String measurement, int value) {
        sensors.stream()
                .filter(s -> s.labels.get("quotaType") != null)
                .filter(s -> measurement.equals(s.labels.get("measurement")))
                .findFirst()
                .ifPresent(sensor -> {
                    assertEquals("Market-IR", sensor.labels.get("account"));
                    assertEquals("default", sensor.labels.get("quotaType"));
                    assertEquals(TaskPriority.LOW.name(), sensor.labels.get("priority"));
                    assertEquals(value, sensor.value, 0.00001);
                });
    }

    private void assertSensorsByAccountId(List<Sensor> sensors,
                                          String account,
                                          Double maxDuration,
                                          Double avgDuration,
                                          Double minVelocity) {
        final List<Sensor> plannedStateSensors = sensors.stream()
                .filter(task -> task.labels.containsKey("userRunSize"))
                .filter(task -> account.equals(task.labels.get("account")))
                .collect(Collectors.toList());
        assertEquals(3, plannedStateSensors.size());

        plannedStateSensors.forEach(sensor -> {
            switch (sensor.labels.get("measurement")) {
                case "lowerBoundOfMinRowPerSecond":
                    assertEquals(minVelocity, sensor.value, 0.1);
                    break;
                case "avgDuration":
                    assertEquals(avgDuration, sensor.value, 1000);
                    break;
                case "maxDuration":
                    assertEquals(maxDuration, sensor.value, 1000);
                    break;
                default:
                    throw new IllegalStateException("add new metrics in test");
            }
        });
    }
    private void assertSensorsByUser(List<Sensor> sensors,
                                          String user,
                                          Double maxDuration,
                                          Double avgDuration,
                                          Double minVelocity) {
        final List<Sensor> plannedStateSensors = sensors.stream()
                .filter(task -> task.labels.containsKey("userRunSize"))
                .filter(task -> user.equals(task.labels.get("user")))
                .collect(Collectors.toList());
        assertEquals(3, plannedStateSensors.size());

        plannedStateSensors.forEach(sensor -> {
            switch (sensor.labels.get("measurement")) {
                case "lowerBoundOfMinRowPerSecond":
                    assertEquals(minVelocity, sensor.value, 0.1);
                    break;
                case "avgDuration":
                    assertEquals(avgDuration, sensor.value, 1000);
                    break;
                case "maxDuration":
                    assertEquals(maxDuration, sensor.value, 1000);
                    break;
                default:
                    throw new IllegalStateException("add new metrics in test");
            }
        });
    }

    private void assertSensorsByState(List<Sensor> sensors,
                                      BusinessTaskState planned,
                                      int count,
                                      int avgDuration,
                                      int maxDuration) {
        final List<Sensor> plannedStateSensors = sensors.stream()
                .filter(task -> planned.name().equals(task.labels.get("state")))
                .collect(Collectors.toList());
        assertEquals(3, plannedStateSensors.size());

        plannedStateSensors.forEach(sensor -> {
            switch (sensor.labels.get("measurement")) {
                case "count":
                    assertEquals(count, sensor.value, 0.00001);
                    break;
                case "avgDuration":
                    assertEquals(avgDuration, sensor.value, 1000);
                    break;
                case "maxDuration":
                    assertEquals(maxDuration, sensor.value, 1000);
                    break;
                default:
                    throw new IllegalStateException("add new metrics in test");
            }
        });
    }
}
