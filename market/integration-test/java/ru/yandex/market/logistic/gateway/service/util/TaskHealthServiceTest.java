package ru.yandex.market.logistic.gateway.service.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.flow.TaskProcessService;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TaskHealthServiceTest extends AbstractIntegrationTest {

    @SpyBean
    private TaskProcessService taskProcessService;

    @Autowired
    private TaskHealthService taskHealthService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ClientTaskRepository clientTaskRepository;

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_create_order_new_actual.xml")
    public void findAndResolveLostTasksActualTest() {
        taskHealthService.findAndResolveLostTasks();

        verify(taskProcessService, never()).cloneTaskSafe(anyLong());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_create_order_ready_outdated.xml")
    public void findAndResolveLostTasksReadyTest() {
        taskHealthService.findAndResolveLostTasks();

        verify(taskProcessService, never()).cloneTaskSafe(anyLong());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_create_order_new_outdated.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/expected/client_task_create_order_new_outdated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void findAndResolveLostTasksSuccessTest() {
        taskHealthService.findAndResolveLostTasks();

        verify(taskProcessService, times(2)).cloneTaskSafe(anyLong());
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_stalled_new.xml")
    public void findAndReportStalledNewTasksZeroTest() {
        softAssert.assertThat(meterRegistry.find("tasks.stalled-new").gauge()).isNotNull();

        checkNumberOfStalledNewTasks(11, 58, 0);
        checkNumberOfStalledNewTasks(12, 2, 1);
        checkNumberOfStalledNewTasks(12, 8, 2);
        setClientTaskStatus(1, TaskStatus.IN_PROGRESS);
        checkNumberOfStalledNewTasks(12, 10, 1);
        setClientTaskStatus(1, TaskStatus.READY);
        setClientTaskStatus(2, TaskStatus.IN_PROGRESS);
        checkNumberOfStalledNewTasks(12, 12, 0);
    }

    private void checkNumberOfStalledNewTasks(int hour, int minute, long expectedGaugeValue) {
        clock.setFixed(LocalDateTime.of(2022, 7, 1, hour, minute, 0).toInstant(ZoneOffset.ofHours(3)),
                ZoneId.of("Europe/Moscow"));
        taskHealthService.findAndReportStalledNewTasks();
        softAssert.assertThat(Objects.requireNonNull(meterRegistry.find("tasks.stalled-new").gauge()).value())
            .isEqualTo(expectedGaugeValue);
    }

    private void setClientTaskStatus(long id, TaskStatus status) {
        ClientTask task = clientTaskRepository.findTask(id);
        task.setStatus(status);
        clientTaskRepository.save(task);
    }
}
