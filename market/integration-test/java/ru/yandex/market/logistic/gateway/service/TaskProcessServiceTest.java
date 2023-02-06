package ru.yandex.market.logistic.gateway.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.processing.exception.InvalidCloneTaskStatusException;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.flow.TaskProcessService;

public class TaskProcessServiceTest extends AbstractIntegrationTest {

    private static final long TASK_ID = 123L;

    private static final String RETRY_REASON = "Retry test reason";

    private static final String CLIENT_TASK_MESSAGE = "{\n  \"field\": \"value\"\n}";

    private static final String PROCESS_ID = "processIdABC123";

    @Autowired
    private TaskProcessService taskProcessService;

    @Autowired
    private ClientTaskRepository taskRepository;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection", value = "classpath:repository/state/empty.xml")
    public void createTaskWitNoConsumerSpecified() {
        taskProcessService.addTask(
            RequestFlow.DS_CREATE_ORDER,
            null,
            TaskStatus.NEW,
            new TaskMessage("{}"),
            null,
             null
        );

        ClientTask task = taskRepository.findTask(1L);
        softAssert.assertThat(task.getConsumer()).isNull();
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_to_retry_error.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/expected/client_task_to_retried.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void retryTaskSuccess() {
        taskProcessService.retryTask(TASK_ID, 0, RETRY_REASON);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
            value = "classpath:repository/state/client_task_to_clone_error.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
            value = "classpath:repository/expected/client_task_to_cloned.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void cloneErrorTaskSuccess() {
        taskProcessService.cloneTaskSafe(TASK_ID);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
            value = "classpath:repository/state/client_task_to_retry_in_progress.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
            value = "classpath:repository/expected/client_task_to_cloned.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void cloneInProgressTaskSuccess() {
        taskProcessService.cloneTask(TASK_ID);
    }

    @Test(expected = InvalidCloneTaskStatusException.class)
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
            value = "classpath:repository/state/client_task_to_retry_in_progress.xml")
    public void cloneInProgressTaskFail() {
        taskProcessService.cloneTaskSafe(TASK_ID);
        exceptionRule.expect(InvalidCloneTaskStatusException.class);
        exceptionRule.expectMessage("Failed to retry task with status [IN_PROGRESS]");
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_before_update_message.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/expected/client_task_after_update_message.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void cloneTaskWithNewMessageSuccess() {
        taskProcessService.cloneTaskWithNewMessage(1L, CLIENT_TASK_MESSAGE);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/state/client_task_with_process_id_to_clone.xml")
    @ExpectedDatabase(connection = "dbUnitDatabaseConnection",
        value = "classpath:repository/expected/client_task_with_process_id_cloned.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void cloneTaskWithProcessIdSuccess() {
        taskProcessService.cloneTaskWithNewMessage(123L, CLIENT_TASK_MESSAGE);
    }

    @Test
    @DatabaseSetup("/repository/state/client_task_new.xml")
    @ExpectedDatabase(
        value = "/repository/expected/client_task_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void cancelTask() {
        taskProcessService.cancelTask(1L);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection", value = "classpath:repository/state/empty.xml")
    public void addTaskWithProcessIdSuccess() {
        taskProcessService.addTask(
            RequestFlow.DS_CREATE_ORDER,
            null,
            TaskStatus.NEW,
            new TaskMessage("{}"),
            null,
            PROCESS_ID
        );

        ClientTask task = taskRepository.findTask(1L);
        softAssert.assertThat(task.getProcessId())
            .as("Asserting that the process id is valid")
            .isEqualTo(PROCESS_ID);
    }

    @Test
    @DatabaseSetup(connection = "dbUnitDatabaseConnection", value = "classpath:repository/state/empty.xml")
    public void addTaskWithoutProcessIdSuccess() {
        taskProcessService.addTask(
            RequestFlow.DS_CREATE_ORDER,
            null,
            TaskStatus.NEW,
            new TaskMessage("{}"),
            null,
            null
        );

        ClientTask task = taskRepository.findTask(1L);
        softAssert.assertThat(task.getProcessId())
            .as("Asserting that the process id is null")
            .isNull();
    }
}
