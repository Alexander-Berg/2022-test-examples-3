package ru.yandex.market.crm.campaign.services.actions.tasks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.TaskStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class ExecutePeriodicActionTaskTest {

    private static ExecutionResult select(ExecutionResult... stepResults) {
        return ExecutePeriodicActionTask.selectTaskResult(Arrays.asList(stepResults));
    }

    private static void assertTime(LocalDateTime expected, LocalDateTime actual) {
        assertNotNull(actual);

        String errorMessage = String.format("Expected: %s. Actual: %s", expected, actual);
        boolean inInterval = expected.minusSeconds(DELTA).isBefore(actual) && expected.plusSeconds(DELTA).isAfter(actual);
        assertTrue(errorMessage, inInterval);
    }

    private static final int DELTA = 2;

    @Test
    public void testAllStepsSucceed() {
        ExecutionResult result = select(
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                ExecutionResult.ofStatus(TaskStatus.COMPLETED)
        );
        assertEquals(TaskStatus.COMPLETING, result.getNextStatus());
    }

    @Test
    public void testOneOfStepsIsStillExecuting() {
        ExecutionResult result = select(
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                ExecutionResult.repeatIn(Duration.ofMinutes(1))
        );

        assertEquals(TaskStatus.WAITING, result.getNextStatus());

        LocalDateTime expectedTime = LocalDateTime.now().plusMinutes(1);
        LocalDateTime actualTime = result.getNextRunTime().orElse(null);
        assertTime(expectedTime, actualTime);
    }

    @Test
    public void testSmallestWaitTimeIsSelected() {
        ExecutionResult result = select(
                ExecutionResult.repeatIn(Duration.ofHours(5)),
                ExecutionResult.repeatIn(Duration.ofSeconds(5)),
                ExecutionResult.repeatIn(Duration.ofMinutes(5))
        );

        assertEquals(TaskStatus.WAITING, result.getNextStatus());

        LocalDateTime expectedTime = LocalDateTime.now().plusSeconds(5);
        LocalDateTime actualTime = result.getNextRunTime().orElse(null);
        assertTime(expectedTime, actualTime);
    }

    @Test
    public void testWhenOneOfStepsIsFailedAllTaskIsFailed() {
        ExecutionResult result = select(
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                ExecutionResult.ofStatus(TaskStatus.FAILED),
                ExecutionResult.ofStatus(TaskStatus.COMPLETED)
        );
        assertEquals(TaskStatus.FAILING, result.getNextStatus());
    }

    @Test
    public void testDoNotStopTaskWhenBeforeStepFailureHandlerIsExecuted() {
        ExecutionResult result = select(
                ExecutionResult.failed("Error")
        );

        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertFalse(result.getNextRunTime().isPresent());
    }

    @Test
    public void testDoNotFailTaskBeforeAllFailHandlersAreExecuted() {
        ExecutionResult result = select(
                ExecutionResult.failed("Error"),
                ExecutionResult.ofStatus(TaskStatus.FAILED)
        );

        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertFalse(result.getNextRunTime().isPresent());
    }

    @Test
    public void testOneFailedStepCancelsWholeTask() {
        ExecutionResult result = select(
                ExecutionResult.yield(),
                ExecutionResult.ofStatus(TaskStatus.FAILED),
                ExecutionResult.repeatIn(Duration.ofMinutes(15))
        );

        assertEquals(TaskStatus.FAILING, result.getNextStatus());
    }

    @Test
    public void testSelectWithJustLaunchedTask() {
        ExecutionResult result = select(
                ExecutionResult.ofStatus(TaskStatus.COMPLETED),
                null
        );

        assertEquals(TaskStatus.WAITING, result.getNextStatus());
        assertFalse(result.getNextRunTime().isPresent());
    }
}
